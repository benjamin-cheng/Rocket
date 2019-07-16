/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.webkit

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Message
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebHistoryItem
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewDatabase
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import org.jsoup.Jsoup
import org.jsoup.nodes.TextNode
import org.mozilla.fileutils.FileUtils
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.history.BrowsingHistoryManager
import org.mozilla.focus.utils.AppConstants
import org.mozilla.focus.utils.SupportUtils
import org.mozilla.focus.web.WebViewProvider
import org.mozilla.httprequest.HttpRequest
import org.mozilla.rocket.tabs.SiteIdentity
import org.mozilla.rocket.tabs.TabChromeClient
import org.mozilla.rocket.tabs.TabView
import org.mozilla.rocket.tabs.TabViewClient
import org.mozilla.rocket.tabs.web.Download
import org.mozilla.rocket.tabs.web.DownloadCallback
import org.mozilla.rocket.translate.TranslateViewModel
import org.mozilla.threadutils.ThreadUtils
import org.mozilla.urlutils.UrlUtils
import java.net.MalformedURLException
import java.net.URL

class WebkitView(context: Context, attrs: AttributeSet?) : NestedWebView(context, attrs), TabView {

    private var downloadCallback: DownloadCallback? = null
    private val webViewClient: FocusWebViewClient
    private val webChromeClient: FocusWebChromeClient
    private val linkHandler: LinkHandler

    private var shouldReloadOnAttached = false

    private var lastNonErrorPageUrl: String? = null
    private var errorPageDelegate: ErrorPageDelegate? = null

    private val debugOverlay: WebViewDebugOverlay

    private val translateViewModel: TranslateViewModel

    init {
        webViewClient = object : FocusWebViewClient(getContext().applicationContext) {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                if (!UrlUtils.isInternalErrorURL(url)) {
                    lastNonErrorPageUrl = url
                }
                super.onPageStarted(view, url, favicon)
            }
        }
        webViewClient.setErrorPageDelegate(ErrorPageDelegate(this))

        webChromeClient = FocusWebChromeClient(this)
        setWebViewClient(webViewClient)
        setWebChromeClient(webChromeClient)
        setDownloadListener(createDownloadListener())

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        isLongClickable = true

        linkHandler = LinkHandler(this, this)
        setOnLongClickListener(linkHandler)

        debugOverlay = WebViewDebugOverlay.create(context)
        debugOverlay.bindWebView(this)
        webViewClient.setDebugOverlay(debugOverlay)

        translateViewModel = ViewModelProviders.of(getContext() as FragmentActivity).get(TranslateViewModel::class.java)
        // Update sync toggle button states based on downloaded models list.
        translateViewModel.availableModels.observe(getContext() as FragmentActivity, Observer { firebaseTranslateRemoteModels -> Log.d("WebkitView", "availableModels: $firebaseTranslateRemoteModels") })
        translateViewModel.translatedText.observe(getContext() as FragmentActivity, Observer { resultOrError ->
            if (resultOrError.error != null) {
                Toast.makeText(getContext(), resultOrError.error!!.localizedMessage, Toast.LENGTH_LONG).show()
            } else {
                Log.d("WebkitView", "result: " + resultOrError.result!!)
                //loadDataWithBaseURL("https://www.wikipedia.org/", resultOrError.getResult(), "text/html", "UTF-8", null);
            }
        })
        ThreadUtils.postToBackgroundThread {
            translateViewModel.downloadLanguage(TranslateViewModel.Language("en"))
            translateViewModel.downloadLanguage(TranslateViewModel.Language("zh"))
        }

        ThreadUtils.postToBackgroundThread {
            translateViewModel.sourceLang.postValue(TranslateViewModel.Language("zh"))
            translateViewModel.targetLang.postValue(TranslateViewModel.Language("en"))

            try {
                val content = HttpRequest.get(URL("http://www.atmovies.com.tw/movie/fpkr46751668/"), "Android")

                //StringBuilder buffer = new StringBuilder();
                val doc = Jsoup.parse(content)
                val els = doc.body().allElements
                for (e in els) {
                    for (child in e.childNodes()) {
                        if (child is TextNode && !child.isBlank) {
                            translateViewModel.translate(child.text()).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d("WebkitView", "text: " + child.text() + " => " + task.result)
                                    child.text(task.result)
                                } else {
                                    Log.d("WebkitView", "text: " + child.text() + " => no result")
                                }
                            }
                        }
                    }
                }

                ThreadUtils.postToMainThreadDelayed({ loadDataWithBaseURL("http://www.atmovies.com.tw/movie/fpkr46751668/", doc.toString(), "text/html", "UTF-8", null) }, 10000)
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (shouldReloadOnAttached) {
            shouldReloadOnAttached = false
            reload()
        }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        this.errorPageDelegate?.onWebViewScrolled(l, t)
        this.debugOverlay.onWebViewScrolled(l, t)
    }

    override fun restoreViewState(savedInstanceState: Bundle) {
        // We need to have a different method name because restoreState() returns
        // a WebBackForwardList, and we can't overload with different return types:
        val backForwardList = restoreState(savedInstanceState)

        // Pages are only added to the back/forward list when loading finishes. If a new page is
        // loading when the Activity is paused/killed, then that page won't be in the list,
        // and needs to be restored separately to the history list. We detect this by checking
        // whether the last fully loaded page (getCurrentItem()) matches the last page that the
        // WebView was actively loading (which was retrieved during saveViewState():
        // WebView.getUrl() always returns the currently loading or loaded page).
        // If the app is paused/killed before the initial page finished loading, then the entire
        // list will be null - so we need to additionally check whether the list even exists.

        val desiredURL = savedInstanceState.getString(KEY_CURRENTURL)

        // If WebView was connecting to a non-exist host (ie. 1.1.1.1:42), getUrl() returns null
        // in saveViewState. In any cases we can not get desiredURL, no need to load it.
        if (TextUtils.isEmpty(desiredURL)) {
            return
        }

        webViewClient.notifyCurrentURL(desiredURL)

        var currentItem: WebHistoryItem
        if (backForwardList != null && backForwardList.currentItem != null) {
            currentItem = backForwardList.currentItem!!
            val latestHistoryUrl = currentItem.url

            if (desiredURL == latestHistoryUrl) {
                // The last url WebView saved is consistent with what we saved.
                reload()

            } else {
                // Two possible cases:
                // Case 1: last url ends up become an error page, the url saved in back-forward list is an error-url.
                // Case 2: last url is not yet being saved by the WebView (WebView didn't finished loading)

                //noinspection StatementWithEmptyBody, for clearer logic and comment
                if (UrlUtils.isInternalErrorURL(latestHistoryUrl) && desiredURL == url) {
                    // Case 1: What we get from WebHistoryItem:
                    // WebHistoryItem#getOriginalUrl(): error-url
                    // WebHistoryItem#getUrl(): error-url
                    //
                    // However, after WebView restores this error-url, WebView#getUrl() will instead
                    // return a virtual url given when we called loadDataWithBaseUrl(..., virtualUrl)
                    //
                    // What we get from WebView after error-url is restored:
                    // WebView#getOriginalUrl(): error-url
                    // WebView#getUrl(): virtual-url

                    // The condition here means "WebView and us agree the last page is <virtual-url>,
                    // which however, ends up become an error page.

                } else {
                    // Case 2, we have more up-to-date url than WebView, load it
                    loadUrl(desiredURL)
                }
            }
        } else {
            // WebView saved nothing, so directly load what we recorded
            loadUrl(desiredURL)
        }
    }

    override fun saveViewState(outState: Bundle) {
        super.saveState(outState)
        // See restoreWebViewState() for an explanation of why we need to save this in _addition_
        // to WebView's state
        outState.putString(KEY_CURRENTURL, url)
    }

    override fun setContentBlockingEnabled(enable: Boolean) {
        if (webViewClient.isBlockingEnabled == enable) {
            return
        }

        webViewClient.isBlockingEnabled = enable

        if (!enable) {
            reloadOnAttached()
        }
    }

    override fun bindOnNewWindowCreation(msg: Message) {
        if (msg.obj !is WebView.WebViewTransport) {
            throw IllegalArgumentException("Message payload is not a WebViewTransport instance")
        }

        val transport = msg.obj as WebView.WebViewTransport
        transport.webView = this
        msg.sendToTarget()
    }

    override fun isBlockingEnabled(): Boolean {
        return webViewClient.isBlockingEnabled
    }

    override fun setImageBlockingEnabled(enable: Boolean) {
        val settings = settings
        if (enable == settings.blockNetworkImage && enable == !settings.loadsImagesAutomatically) {
            return
        }

        WebViewProvider.applyAppSettings(context, getSettings())

        if (enable) {
            reloadOnAttached()
        }
    }

    override fun performExitFullScreen() {
        evaluateJavascript("(function() { return document.webkitExitFullscreen(); })();", null)
    }

    override fun setViewClient(viewClient: TabViewClient?) {
        this.webViewClient.setViewClient(viewClient)
    }

    override fun setChromeClient(chromeClient: TabChromeClient?) {
        linkHandler.setChromeClient(chromeClient)
        this.webChromeClient.setChromeClient(chromeClient)
    }

    override fun setDownloadCallback(callback: DownloadCallback) {
        this.downloadCallback = callback
    }

    override fun setFindListener(listener: TabView.FindListener?) {
        val findListener: WebView.FindListener?
        if (listener == null) {
            findListener = null
        } else {
            findListener = WebView.FindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting -> listener.onFindResultReceived(activeMatchOrdinal, numberOfMatches, isDoneCounting) }
        }
        setFindListener(findListener)
    }

    override fun loadUrl(url: String?) {
        debugOverlay.onLoadUrlCalled()

        // We need to check external URL handling here - shouldOverrideUrlLoading() is only
        // called by webview when clicking on a link, and not when opening a new page for the
        // first time using loadUrl().
        @Suppress("DEPRECATION")
        if (!webViewClient.shouldOverrideUrlLoading(this, url)) {
            super.loadUrl(url)
        }

        webViewClient.notifyCurrentURL(url)
    }

    override fun reload() {
        if (UrlUtils.isInternalErrorURL(originalUrl)) {
            super.loadUrl(url)
        } else {
            super.reload()
        }
        this.debugOverlay.updateHistory()
    }

    override fun goBack() {
        super.goBack()
        this.debugOverlay.updateHistory()
    }

    override fun getUrl(): String? {
        val currentUrl = super.getUrl()
        return if (UrlUtils.isInternalErrorURL(currentUrl)) {
            lastNonErrorPageUrl
        } else {
            currentUrl
        }
    }

    private fun reloadOnAttached() {
        if (isAttachedToWindow) {
            reload()
        } else {
            shouldReloadOnAttached = true
        }
    }

    @SiteIdentity.SecurityState
    override fun getSecurityState(): Int {
        // FIXME: Having certificate doesn't mean the connection is secure, see #1562
        return if (certificate == null) SiteIdentity.INSECURE else SiteIdentity.SECURE
    }

    override fun cleanup() {
        clearFormData()
        clearHistory()
        clearMatches()
        clearSslPreferences()
        clearCache(true)

        // We don't care about the viewClient - we just want to make sure cookies are gone
        CookieManager.getInstance().removeAllCookies(null)

        WebStorage.getInstance().deleteAllData()

        val webViewDatabase = WebViewDatabase.getInstance(context)
        // It isn't entirely clear how this differs from WebView.clearFormData()
        @Suppress("DEPRECATION")
        webViewDatabase.clearFormData()
        webViewDatabase.clearHttpAuthUsernamePassword()
    }

    override fun insertBrowsingHistory() {
        val url = url
        if (TextUtils.isEmpty(url)) {
            return
        } else if (SupportUtils.BLANK_URL == url) {
            return
        }

        if (!UrlUtils.isHttpOrHttps(url)) {
            return
        }

        evaluateJavascript("(function() { return document.getElementById('mozillaErrorPage'); })();"
        ) { errorPage ->
            if ("null" != errorPage) {
                return@evaluateJavascript
            }

            val site = BrowsingHistoryManager.prepareSiteForFirstInsert(url, title, System.currentTimeMillis())
            BrowsingHistoryManager.getInstance().insert(site, null)
        }
    }

    override fun getView(): View {
        return this
    }

    private fun createDownloadListener(): DownloadListener {
        return DownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            if (!AppConstants.supportsDownloadingFiles()) {
                return@DownloadListener
            }

            if (downloadCallback != null) {
                val name = URLUtil.guessFileName(url, contentDisposition, mimetype)
                val download = Download(url, name, userAgent, contentDisposition, mimetype, contentLength, false)
                downloadCallback!!.onDownloadStart(download)
            }
        }
    }

    companion object {
        private val KEY_CURRENTURL = "currenturl"

        fun deleteContentFromKnownLocations(context: Context) {
            ThreadUtils.postToBackgroundThread {
                // We call all methods on WebView to delete data. But some traces still remain
                // on disk. This will wipe the whole webview directory.
                FileUtils.deleteWebViewDirectory(context)

                // WebView stores some files in the cache directory. We do not use it ourselves
                // so let's truncate it.
                FileUtils.truncateCacheDirectory(context)
            }
        }
    }
}

package org.mozilla.rocket.content.news.data.dailyhunt

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.rocket.content.news.data.NewsItem
import org.mozilla.rocket.content.news.data.NewsTrackingDataSource
import org.mozilla.rocket.util.sendHttpRequest
import java.net.URLEncoder

class DailyHuntTrackingRemoteDataSource(
    private val newsProvider: DailyHuntProvider?
) : NewsTrackingDataSource {

    override suspend fun trackPageView(newsItems: List<NewsItem>) = withContext(Dispatchers.IO) {
        if (newsItems.isEmpty() || newsItems[0] !is NewsItem.NewsContentItem) {
            return@withContext
        }
        val params = parseUrlParams((newsItems[0] as NewsItem.NewsContentItem).trackingUrl).toMutableMap().apply {
            put("partner", newsProvider?.partnerCode ?: "")
            put("puid", newsProvider?.userId ?: "")
            put("ts", System.currentTimeMillis().toString())
        }
        sendHttpRequest(
            request = Request(
                url = getTrackingApiEndpoint(params),
                method = Request.Method.POST,
                headers = createTrackingApiHeaders(params),
                body = Request.Body.fromString(createTrackingBody(newsItems))
            ),
            onSuccess = {
                val body = it.body.string()
                Log.d("DailyHunt", "trackItemsShown - success: $body")
            },
            onError = {
                Log.d("DailyHunt", "trackItemsShown - error: ${it.message}")
            }
        )
        return@withContext
    }

    override suspend fun trackAttribution(attributionUrl: String) = withContext(Dispatchers.IO) {
        sendHttpRequest(
            request = Request(
                url = attributionUrl,
                method = Request.Method.GET
            ),
            onSuccess = {
                val body = it.body.string()
                Log.d("DailyHunt", "trackAttribution - success: $body")
            },
            onError = {
                Log.d("DailyHunt", "trackAttribution - error: ${it.message}")
            }
        )
        return@withContext
    }

    private fun parseUrlParams(url: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val uri = Uri.parse(url)
            val args: Set<String> = uri.queryParameterNames
            args.forEach { key ->
                map[key] = uri.getQueryParameter(key) ?: ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return map
    }

    private fun getTrackingApiEndpoint(params: Map<String, String>): String = Uri.parse(TRACKING_API_URL)
        .buildUpon()
        .apply {
            for ((key, value) in params.entries) {
                appendQueryParameter(key, value)
            }
        }
        .build()
        .toString()

    private fun createTrackingApiHeaders(params: Map<String, String>) = MutableHeaders().apply {
        set("Content-Type", "application/json")

        newsProvider?.apiKey?.let {
            set("Authorization", it)
        }

        newsProvider?.secretKey?.let {
            val signature = DailyHuntUtils.generateSignature(it, Request.Method.POST.name, urlEncodeParams(params))
            set("Signature", signature)
        }
    }

    private fun createTrackingBody(items: List<NewsItem>): String {
        val json = JSONObject()
        json.put("viewedDate", System.currentTimeMillis().toString())

        val jsonArray = JSONArray()
        for (item in items) {
            if (item is NewsItem.NewsContentItem) {
                jsonArray.put(
                    JSONObject()
                        .put("id", item.trackingId)
                        .put("trackData", item.trackingData)
                )
            }
        }
        json.put("stories", jsonArray)
        return json.toString()
    }

    private fun urlEncodeParams(params: Map<String, String>): Map<String, String> {
        val encodedParams = mutableMapOf<String, String>()
        params.forEach {
            encodedParams[it.key] = URLEncoder.encode(it.value, "UTF-8")
        }

        return encodedParams
    }

    companion object {
        private const val TRACKING_API_URL = "http://track.dailyhunt.in/api/v2/syndication/tracking"
    }
}
package org.mozilla.rocket.content.news.data.dailyhunt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.Result.Success
import org.mozilla.rocket.content.news.data.NewsCategory
import org.mozilla.rocket.content.news.data.NewsLanguage
import org.mozilla.rocket.content.news.data.NewsSettingsDataSource
import org.mozilla.rocket.util.safeApiCall
import org.mozilla.rocket.util.sendHttpRequest
import org.mozilla.rocket.util.toJsonObject

class DailyHuntSettingsRemoteDataSource(private val newsProvider: DailyHuntProvider?) : NewsSettingsDataSource {

    override suspend fun getSupportLanguages(): Result<List<NewsLanguage>> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
            call = {
                val partner = newsProvider?.partnerCode ?: ""
                val timestamp = System.currentTimeMillis().toString()
                sendHttpRequest(
                    request = Request(
                        url = getLanguageApiEndpoint(partner, timestamp),
                        method = Request.Method.GET,
                        headers = createLanguageApiHeaders(partner, timestamp)
                    ),
                    onSuccess = {
                        Success(toNewsLanguage(it.body.string()))
                    },
                    onError = {
                        Result.Error(it)
                    })
            },
            errorMessage = "Unable to get remote news languages"
        )
    }

    override suspend fun setSupportLanguages(languages: List<NewsLanguage>) {
        throw UnsupportedOperationException("Can't set news languages setting to server")
    }

    override suspend fun getUserPreferenceLanguage(): Result<NewsLanguage?> {
        throw UnsupportedOperationException("Can't get user preference news languages setting from server")
    }

    override suspend fun setUserPreferenceLanguage(language: NewsLanguage) {
        throw UnsupportedOperationException("Can't set user preference news languages setting to server")
    }

    override suspend fun getSupportCategories(language: String): Result<List<NewsCategory>> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
            call = {
                val partner = newsProvider?.partnerCode ?: ""
                val timestamp = System.currentTimeMillis().toString()
                val uid = "test1234"
                sendHttpRequest(
                    request = Request(
                        url = getCategoryApiEndpoint(partner, language, timestamp, uid),
                        method = Request.Method.GET,
                        headers = createCategoryApiHeaders(partner, language, timestamp, uid)),
                    onSuccess = {
                        val categories = parseCategoriesResult(it.body.string())
                        Success(categories)
                    },
                    onError = {
                        Result.Error(it)
                    }
                )
            },
            errorMessage = "Unable to get remote news categories"
        )
    }

    override suspend fun setSupportCategories(language: String, supportCategories: List<String>) {
        throw UnsupportedOperationException("Can't set news categories to server")
    }

    override suspend fun getUserPreferenceCategories(language: String): Result<List<String>> {
        throw UnsupportedOperationException("Can't get user preference news category setting from server")
    }

    override suspend fun setUserPreferenceCategories(language: String, userPreferenceCategories: List<String>) {
        throw UnsupportedOperationException("Can't set user preference news category setting to server")
    }

    override fun shouldEnableNewsSettings(): Boolean {
        throw UnsupportedOperationException("Can't get menu setting from server")
    }

    private fun getLanguageApiEndpoint(partner: String, timestamp: String): String {
        val url = newsProvider?.languagesUrl ?: DEFAULT_LANGUAGE_LIST_URL
        return String.format(url, partner, timestamp)
    }

    private fun createLanguageApiHeaders(partner: String, timestamp: String) = MutableHeaders().apply {
        newsProvider?.apiKey?.let {
            set("Authorization", it)
        }

        newsProvider?.secretKey?.let {
            val params = mapOf("partner" to partner, "ts" to timestamp)
            val generateSignatureBase = DailyHuntUtils.generateSignatureBase(params)
            val signature = DailyHuntUtils.calculateRFC2104HMAC(
                generateSignatureBase + "GET",
                it
            ) ?: ""
            if (signature.isNotEmpty()) {
                set("Signature", signature)
            }
        }
    }

    private fun toNewsLanguage(apiResult: String): List<NewsLanguage> {
        val jsonObject = apiResult.toJsonObject()
        val languageArray = jsonObject.optJSONObject("data").optJSONArray("rows")
        return (0 until languageArray.length())
            .map { index ->
                val language = languageArray.getJSONObject(index)
                NewsLanguage(language.optString("code"), language.optString("code"), language.optString("nameUni"))
            }
    }

    private fun getCategoryApiEndpoint(partner: String, languageCode: String, timestamp: String, uid: String): String {
        val remoteApiEndpoint = newsProvider?.categoriesUrl ?: ""
        val apiEndpoint = if (remoteApiEndpoint.isNotEmpty()) {
            remoteApiEndpoint
        } else {
            DEFAULT_CATEGORY_LIST_URL
        }

        return String.format(apiEndpoint, partner, languageCode, timestamp, uid)
    }

    private fun createCategoryApiHeaders(partner: String, languageCode: String, timestamp: String, uid: String) = MutableHeaders().apply {
        newsProvider?.apiKey?.let {
            set("Authorization", it)
        }

        newsProvider?.secretKey?.let {
            val params = mapOf("partner" to partner, "langCode" to languageCode, "ts" to timestamp, "puid" to uid, "pfm" to "16")
            val generateSignatureBase = DailyHuntUtils.generateSignatureBase(params)
            val signature = DailyHuntUtils.calculateRFC2104HMAC(
                generateSignatureBase + "GET",
                it
            ) ?: ""
            if (signature.isNotEmpty()) {
                set("Signature", signature)
            }
        }
    }

    private fun parseCategoriesResult(jsonString: String): List<NewsCategory> {
        val jsonObject = jsonString.toJsonObject()
        val categoryArray = jsonObject.optJSONObject("data").optJSONArray("rows")
        return (0 until categoryArray.length())
            .filterIndexed { index, _ -> categoryArray.getJSONObject(index).optString("type") == "TOPIC" }
            .map { index ->
                val category = categoryArray.getJSONObject(index)
                NewsCategory(category.optString("id"), 0, index, true, name = category.optString("name"))
            }
    }

    companion object {
        private const val DEFAULT_LANGUAGE_LIST_URL = "https://feed.dailyhunt.in/api/v2/syndication/languages?partner=%s&ts=%s"
        private const val DEFAULT_CATEGORY_LIST_URL = "https://feed.dailyhunt.in/api/v2/syndication/channels?partner=%s&langCode=%s&ts=%s&puid=%s&pfm=16"
    }
}
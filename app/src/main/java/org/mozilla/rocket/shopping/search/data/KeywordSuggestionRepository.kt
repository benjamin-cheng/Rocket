package org.mozilla.rocket.shopping.search.data

import android.content.Context
import android.text.TextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import org.json.JSONObject
import org.mozilla.focus.search.SearchEngineManager
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.util.safeApiCall
import org.mozilla.rocket.util.sendHttpRequest
import java.net.URLEncoder

class KeywordSuggestionRepository(appContext: Context) {

    private val searchEngine = SearchEngineManager.getInstance().getDefaultSearchEngine(appContext)

    suspend fun fetchSuggestions(keyword: String): Result<List<String>> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
            call = {
                sendHttpRequest(
                    request = Request(
                        url = getSuggestionApiEndpoint(keyword),
                        method = Request.Method.GET,
                        headers = MutableHeaders().apply {
                            set("Fk-Affiliate-Id", "xxxx")
                            set("Fk-Affiliate-Token", "xxxx")
                        }
                    ),
                    onSuccess = {
                        Result.Success(parseSuggestionResult(it.body.string()))
                    },
                    onError = {
                        Result.Error(it)
                    }
                )
            },
            errorMessage = "Unable to get keyword suggestion"
        )
    }

    private fun getSuggestionApiEndpoint(keyword: String): String {
        return "https://affiliate-api.flipkart.net/affiliate/1.0/search.json?query=${URLEncoder.encode(keyword, "UTF-8")}&resultCount=$MAX_SUGGESTION_COUNT"
    }

    private fun parseSuggestionResult(response: String): List<String> {
        val suggestions = arrayListOf<String>()
        if (!TextUtils.isEmpty(response)) {
            val jsonObject = JSONObject(response)
            val suggestionItems = jsonObject.getJSONArray("products")
            val size = suggestionItems.length()

            for (i in 0 until size) {
                val product = suggestionItems.getJSONObject(i)
                suggestions.add(product.getJSONObject("productBaseInfoV1").getString("title"))
            }
        }
        return suggestions
    }

    companion object {
        private const val MAX_SUGGESTION_COUNT = 5
    }
}
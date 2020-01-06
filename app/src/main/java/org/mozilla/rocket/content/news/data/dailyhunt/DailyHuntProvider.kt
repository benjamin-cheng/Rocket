package org.mozilla.rocket.content.news.data.dailyhunt

import org.json.JSONException
import org.json.JSONObject
import org.mozilla.focus.utils.FirebaseHelper

data class DailyHuntProvider(
    val apiKey: String,
    val secretKey: String,
    val partnerCode: String,
    val languagesUrl: String,
    val categoriesUrl: String,
    val newsUrl: String
) {
    companion object {
        fun getProvider(): DailyHuntProvider? {
            val config = FirebaseHelper.getFirebase().getRcString("str_dailyhunt_provider")
            try {
                val jsonObject = JSONObject(config)
                val apiKey = jsonObject.optString("api_key")
                val secretKey = jsonObject.optString("secret_key")
                val partnerCode = jsonObject.optString("partner_code")
                val languagesUrl = jsonObject.optString("language_listing_url")
                val categoriesUrl = jsonObject.optString("category_listing_url")
                val newsUrl = jsonObject.optString("news_listing_url")

                return DailyHuntProvider(apiKey, secretKey, partnerCode, languagesUrl, categoriesUrl, newsUrl)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            return null
        }
    }
}
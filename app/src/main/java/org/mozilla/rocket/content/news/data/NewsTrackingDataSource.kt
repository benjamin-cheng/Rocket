package org.mozilla.rocket.content.news.data

interface NewsTrackingDataSource {
    suspend fun trackPageView(newsItems: List<NewsItem>)
    suspend fun trackAttribution(attributionUrl: String)
}
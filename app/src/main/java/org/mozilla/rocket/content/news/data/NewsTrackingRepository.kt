package org.mozilla.rocket.content.news.data

class NewsTrackingRepository(
    private val remoteDataSource: NewsTrackingDataSource
) {

    suspend fun trackPageView(newsItems: List<NewsItem>) = remoteDataSource.trackPageView(newsItems)

    suspend fun trackAttribution(attributionUrl: String) = remoteDataSource.trackAttribution(attributionUrl)
}
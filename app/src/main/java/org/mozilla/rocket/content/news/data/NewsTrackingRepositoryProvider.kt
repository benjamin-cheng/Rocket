package org.mozilla.rocket.content.news.data

import android.content.Context
import org.mozilla.rocket.content.news.data.dailyhunt.DailyHuntProvider
import org.mozilla.rocket.content.news.data.dailyhunt.DailyHuntTrackingRemoteDataSource

class NewsTrackingRepositoryProvider(private val appContext: Context) {

    fun provideNewsTrackingRepository(): NewsTrackingRepository {
        val newsProvider = NewsProvider.getNewsProvider()
        if (newsProvider?.isNewsPoint() == true) {
            val dailyHuntProvider = DailyHuntProvider.getProvider(appContext)
            if (dailyHuntProvider?.shouldEnable(appContext) == true) {
                return NewsTrackingRepository(DailyHuntTrackingRemoteDataSource(dailyHuntProvider))
            }
        }

        return NewsTrackingRepository(NewsNoTrackingRemoteDataSource())
    }
}

class NewsNoTrackingRemoteDataSource() : NewsTrackingDataSource {

    override suspend fun trackPageView(newsItems: List<NewsItem>) = Unit

    override suspend fun trackAttribution(attributionUrl: String) = Unit
}
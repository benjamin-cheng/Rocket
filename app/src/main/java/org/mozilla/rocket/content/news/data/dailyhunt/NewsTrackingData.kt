package org.mozilla.rocket.content.news.data.newspoint

data class NewsTrackingData(
    val viewedDate: Long,
    val stories: List<NewsTrackingItem>,
    val comscoreUrl: String
)

data class NewsTrackingItem(
    val id: String,
    val trackData: String
)

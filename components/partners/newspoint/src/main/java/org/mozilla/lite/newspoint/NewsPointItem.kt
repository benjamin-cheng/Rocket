package org.mozilla.lite.newspoint

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.mozilla.lite.partner.NewsItem

@Serializable
data class NewsPointItemHolder(
    val items: List<NewsPointItem>
)

@Serializable
data class NewsPointItem(
    override val id: String,
    // TODO: find a solution to parse images array without breaking the field definition
    //  e.g. custom converter
    override val imageUrl: String = "",
    @SerialName("hl")
    override val title: String,
    @SerialName("mwu")
    override val newsUrl: String,
    override val time: Long = System.currentTimeMillis(),
    val imageid: String? = "",
    @SerialName("pn")
    override val partner: String?,
    val dm: String?,
    val pid: Long?,
    val lid: Long?,
    val lang: String?,
    @SerialName("tn")
    override val category: String?,
    val wu: String?,
    val pnu: String?,
    val fu: String?,
    @SerialName("sec")
    override val subcategory: String = "",
    val m: String?,
    val tags: List<String>?
) : NewsItem {
    override val source: String = "Newspoint"
}
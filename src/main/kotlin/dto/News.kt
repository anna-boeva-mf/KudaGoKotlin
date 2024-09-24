package ru.tbank.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import java.time.LocalDate
import kotlin.math.exp

@Serializable
data class News(
    val id: Int? = null,
    val title: String? = null,
    val place: Place?,
    @JsonNames("description")
    val description: String? = null,
    @JsonNames("site_url")
    val siteUrl: String? = null,
    @JsonNames("favorites_count")
    val favoritesCount: Int = 0,
    @JsonNames("comments_count")
    val commentsCount: Int = 0,
    @JsonNames("publication_date")
    val publicationDate: Long? = 0
) {
    val rating: Double by lazy {
        1 / (1 + exp(-(favoritesCount / (commentsCount + 1.0))))
    }
}

fun Long.toHumanDate(): LocalDate {
    return LocalDate.ofEpochDay(this?.div((24 * 60 * 60)) ?: 0)
}
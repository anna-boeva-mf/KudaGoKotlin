package ru.tbank

import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import ru.tbank.dto.News
import ru.tbank.dto.toHumanDate
import java.time.LocalDate

private val logger = LoggerFactory.getLogger(::NewsResponse.javaClass)

var URL: String = "https://kudago.com/public-api/v1.4/news/"

@Serializable
data class NewsResponse(val results: List<News>)

fun List<News>.getMostRatedNews(count: Int, period: ClosedRange<LocalDate>): List<News> {
    logger.info("Получаем все новости за указанный промежуток времени, после чего сортируем их по возрастанию по рейтингу")
    return this.asSequence()
        .filter { it.publicationDate?.toHumanDate()!! in period }
        .sortedByDescending { it.rating }
        .take(count)
        .toList()
}

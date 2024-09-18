package ru.tbank

import kotlinx.coroutines.runBlocking
import ru.tbank.dsl.newsAll
import ru.tbank.dsl.newsHTML
import ru.tbank.dsl.saveToFileAll
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


fun main() = runBlocking {
    var lDT = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    val news = getNews(100)
    saveNews("out/news_$lDT.csv", news)

    val mostRatedNews = news.getMostRatedNews(5, LocalDate.now().minusDays(5)..LocalDate.now())
    saveNews("out/mostRatedNews_$lDT.csv", mostRatedNews)

    val output = newsHTML(mostRatedNews.get(2)) { //одна новость
    }
    output.saveToFile("out/prettyPrintNews_$lDT.html")

    val output1 = newsAll(mostRatedNews) {
    }
    saveToFileAll(output1, "out/prettyPrintNewsAll_$lDT.html")
}


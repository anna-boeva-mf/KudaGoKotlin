package ru.tbank.dsl

import org.slf4j.LoggerFactory
import ru.tbank.dto.News
import ru.tbank.dto.toHumanDate
import java.io.File
import java.io.IOException
import java.nio.charset.Charset


interface Element {
    fun render(builder: StringBuilder, indent: String)
}

class TextElement(val text: String) : Element {
    override fun render(builder: StringBuilder, indent: String) {
        builder.append("$indent$text\n")
    }
}

@DslMarker
annotation class HtmlTagMarker

@HtmlTagMarker
abstract class Tag(val name: String) : Element {
    val children = arrayListOf<Element>()
    val attributes = hashMapOf<String, String>()

    protected fun <T : Element> initTag(tag: T, init: T.() -> Unit): T {
        tag.init()
        children.add(tag)
        return tag
    }

    override fun render(builder: StringBuilder, indent: String) {
        builder.append("$indent<$name${renderAttributes()}>\n")
        for (c in children) {
            c.render(builder, indent + "  ")
        }
        builder.append("$indent</$name>\n")
    }

    private fun renderAttributes(): String {
        val builder = StringBuilder()
        for ((attr, value) in attributes) {
            builder.append(" $attr=\"$value\"")
        }
        return builder.toString()
    }

    override fun toString(): String {
        val builder = StringBuilder()
        render(builder, "")
        return builder.toString()
    }
}

// Для одинарного тега <meta ... >
@HtmlTagMarker
abstract class TagOnce(val name: String) : Element {
    val children = arrayListOf<Element>()
    val attributes = hashMapOf<String, String>()

    protected fun <T : Element> initTag(tag: T, init: T.() -> Unit): T {
        tag.init()
        children.add(tag)
        return tag
    }

    override fun render(builder: StringBuilder, indent: String) {
        builder.append("$indent<$name${renderAttributes()}\n")
        for (c in children) {
            c.render(builder, indent + "  ")
        }
        builder.append("$indent>\n")
    }

    private fun renderAttributes(): String {
        val builder = StringBuilder()
        for ((attr, value) in attributes) {
            builder.append(" $attr=\"$value\"")
        }
        return builder.toString()
    }

    override fun toString(): String {
        val builder = StringBuilder()
        render(builder, "")
        return builder.toString()
    }
}

abstract class TagWithText(name: String) : Tag(name) {
    operator fun String.unaryPlus() {
        children.add(TextElement(this))
    }
}

abstract class TagWithTextOnce(name: String) : TagOnce(name) {
    operator fun String.unaryPlus() {
        children.add(TextElement(this))
    }
}

private val logger = LoggerFactory.getLogger(HTML::class.java)

class HTML : TagWithText("html") {
    override fun toString(): String {
        val builder = StringBuilder()
        render(builder, "")
        return builder.toString().trimIndent()
    }
}

class Head : TagWithText("head") {
    fun title(init: Title.() -> Unit) = initTag(Title(), init)
    fun meta(init: Meta.() -> Unit) = initTag(Meta(), init)
}

class Title : TagWithText("title")
class Meta : TagWithTextOnce("meta")

abstract class BodyTag(name: String) : TagWithText(name) {
    fun b(init: B.() -> Unit) = initTag(B(), init)
    fun p(init: P.() -> Unit) = initTag(P(), init)
    fun h1(init: H1.() -> Unit) = initTag(H1(), init)
    fun h2(init: H2.() -> Unit) = initTag(H2(), init)
    fun h3(init: H3.() -> Unit) = initTag(H3(), init)
    fun a(href: String, init: A.() -> Unit) {
        val a = initTag(A(), init)
        a.href = href
    }

    fun ul(init: UL.() -> Unit) = initTag(UL(), init)
}

class Body : BodyTag("body")
class B : BodyTag("b")
class P : BodyTag("p")
class H1 : BodyTag("h1")
class H2 : BodyTag("h2")
class H3 : BodyTag("h3")
class UL : BodyTag("ul")

class A : BodyTag("a") {
    var href: String
        get() = attributes["href"]!!
        set(value) {
            attributes["href"] = value
        }
}


@HtmlTagMarker
class NewsHTML(val news: News) : TagWithText("html") {
    fun head(init: Head.() -> Unit) = initTag(Head(), init)
    fun body(init: Body.() -> Unit) = initTag(Body(), init)

    init {
        val meta = "charset=\"UTF-8\""
        val title = news.title
        val place = news.place
        val description = news.description
        val siteUrl = news.siteUrl
        val favoritesCount = news.favoritesCount
        val commentsCount = news.commentsCount
        val publicationDate = news.publicationDate
        head {
            meta { +(meta) }
            title { +(title ?: "News Article") }
        }
        body {
            h1 { +(title ?: "Unknown Title") }
            if (place != null) {
                p { +"Place: ${place}" }
            }
            b { +(description ?: "No description available.") }
            if (siteUrl != null) {
                a(siteUrl) { +"Читать подробности" }
            }
            if (favoritesCount >= 0 || commentsCount >= 0) {
                h3 { +"Statistics" }
                ul { +"Favorites: ${favoritesCount}" }
                ul { +"Comments: ${commentsCount}" }
            }
            if (publicationDate != null) {
                p { +"Publication Date: ${publicationDate?.toHumanDate()}" }
            }
        }
    }

    override fun toString(): String {
        val builder = StringBuilder()
        render(builder, "")
        return builder.toString().trimIndent()
    }

    fun saveToFile(fileName: String, charset: Charset = Charsets.UTF_8) {
        logger.info("Сохраняем новости в файл по указанному пути: $fileName")
        File(fileName).bufferedWriter(charset).use { writer ->
            try {
                writer.write(toString())
            } catch (e: IOException) {
                logger.error("Error writing to file: ${e.message}")
            }
        }
    }
}

fun newsHTML(news: News, init: NewsHTML.() -> Unit): NewsHTML {
    val newsHtml = NewsHTML(news)
    newsHtml.init()
    return newsHtml
}

fun newsAll(listNews: List<News>, init: NewsHTML.() -> Unit): List<NewsHTML> {
    val newsAll = mutableListOf<NewsHTML>()
    for (news in listNews) {
        val newsHtml = NewsHTML(news)
        newsHtml.init()
        newsAll.add(newsHtml)
    }
    return newsAll
}

fun saveToFileAll(listNewsHTML: List<NewsHTML>, fileName: String, charset: Charset = Charsets.UTF_8) {
    logger.info("Сохраняем новости в файл по указанному пути: $fileName")
    val separator = ""
    val str = listNewsHTML.joinToString(separator)
    File(fileName).bufferedWriter(charset).use { writer ->
        try {
            writer.write(str)
        } catch (e: IOException) {
            logger.error("Error writing to file: ${e.message}")
        }
    }
}

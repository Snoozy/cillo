package com.cillo.utils

import java.util.regex.{Matcher, Pattern}

import org.apache.commons.lang3.StringEscapeUtils.escapeHtml4
import com.googlecode.htmlcompressor.compressor.HtmlCompressor
import collection.JavaConversions._
import scala.collection.JavaConversions.mapAsScalaMap

import scala.util.matching.Regex

object Etc {

    implicit def bool2int(b: Boolean): Int = if (b) 1 else 0
    implicit def int2bool(i: Int): Boolean = if (i == 1) true else false

    def parseFirstName(s: String) = {
        val i = s.indexOf(' ')
        if (i > -1)
            s.substring(0, i)
        else
            s
    }

    def checkPasswordValidity(p: String) = {
        true
    }

    def makeDigest(pass: String): String = {
        Password.createHash(pass)
    }

    def checkPass(pass: String, hash: String): Boolean = {
        Password.validatePassword(pass, hash)
    }

    def convertEpochToTimestamp(millis: scala.Long): String = {
        val curr = System.currentTimeMillis()
        val time = curr - millis
        time match {
            case x if 0L <= x && x < 59999L =>
                "1 min"
            case x if 60000L <= x && x < 3599999L =>
                val res = time / 60000L
                if (res == 1)
                    res + " min"
                else
                    res + " mins"
            case x if 3600000L <= x && x < 86399999L =>
                val res = time / 3600000L
                if (res == 1)
                    res + " hour"
                else
                    res + " hours"
            case x if x >= 86400000L && x < 31535999999L =>
                val res = time / 86400000L
                if (res == 1)
                    res + " day"
                else
                    res + " days"
            case x if x >= 31536000000L && x < Long.MaxValue =>
                val res = time / 31536000000L
                if (res == 1)
                    res + " year"
                else
                    res + " years"
        }
    }

    //Ellipsize strings.

    private val NonThin = "[^iIl1\\.,']"

    private def textWidth(s: String): Int = {
        s.length - s.replaceAll(NonThin, "").length / 2
    }

    def ellipsize(text: String, max: Int): String = {
        if (textWidth(text) <= max) {
            return text
        }

        var end = text.lastIndexOf(' ', max - 3)
        if (end == -1) {
            return text.substring(0, max - 3) + "..."
        }

        var newEnd = end
        do {
            end = newEnd
            newEnd = text.indexOf(' ', end + 1)
            if (newEnd == -1) {
                newEnd = text.length
            }
        } while (textWidth(text.substring(0, newEnd) + "...") < max)

        text.substring(0, end) + "..."
    }

    private val multiNewLineRegex: Pattern = Pattern.compile("\n{2,}")
    private val newLineRegex: Pattern = Pattern.compile("\n", Pattern.LITERAL)
    private val linkRegex = "(?i)\\b((?:[a-z][\\w-]+:(?:/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))".r
    private val usernameRegex: Pattern = Pattern.compile("""(?<=^|(?<=[^a-zA-Z0-9-\.]))@([A-Za-z_]+[A-Za-z0-9_]+)""")
    private val hashtagRegex: Pattern = Pattern.compile("""(?<=^|(?<=[^a-zA-Z0-9-\.]))#([A-Za-z_]+[A-Za-z0-9_]+)""")


    def parseHTML(raw: String): String = {
        parseSpecial(parseLinks(parseRaw(raw)))
    }

    def parseRaw(raw: String): String = {
        newLineRegex.matcher(multiNewLineRegex.matcher(escapeHtml4(raw)).replaceAll("</p><p class=\"post-text\">")).replaceAll(Matcher.quoteReplacement("<br/>"))
    }

    def parseSpecial(raw: String): String = {
        hashtagRegex.matcher(usernameRegex.matcher(raw).replaceAll("<a href=\"https://www.cillo.co/user/$1\" target=\"_blank\">@$1</a>")).replaceAll("<a href=\"https://www.cillo.co/$1\" target=\"_blank\">#$1</a>")
    }

    def parseLinks(raw: String): String = {
        linkRegex.replaceAllIn(raw, m => linkParser(m))
        /*val matcher = linkRegex.matcher(raw)
        if (matcher.find()) {
            val num = matcher.groupCount()
            for ()
            val groups = matcher.group(0)
            val parsed = {
                if (groups.substring(0, 7).indexOf(':') < 0) {
                    "http://" + groups
                } else {
                    groups
                }
            }
            "<a href=\"" + parsed + "\" target=\"_blank\">" + groups + "</a>"
        } else {
            raw
        }*/
    }

    def linkParser(m: Regex.Match): String = {
        val groups = m.group(0)
        val parsed = {
            if (groups.substring(0, 7).indexOf(':') < 0) {
                "http://" + groups
            } else {
                groups
            }
        }
        "<a href=\"" + parsed + "\" target=\"_blank\">" + groups + "</a>"
    }

    def serializeMap(m: Map[String, String]): String = {
        MapUtils.serializeMap(m)
    }

    def deserializeMap(s: String): Map[String, String] = {
        val m = Option(MapUtils.deserializeMap(s))
        if (m.isDefined) {
            mapAsScalaMap(m.get).toMap
        } else Map()
    }

    val htmlCompressor = new HtmlCompressor()
    htmlCompressor.setPreserveLineBreaks(false)
    htmlCompressor.setRemoveComments(true)
    htmlCompressor.setRemoveIntertagSpaces(true)
    htmlCompressor.setRemoveHttpProtocol(false)
    htmlCompressor.setRemoveHttpsProtocol(false)


    def compressHtml(s: String) = {
        htmlCompressor.compress(s)
    }

}
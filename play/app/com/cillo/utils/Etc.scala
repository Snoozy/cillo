package com.cillo.utils

import org.apache.commons.lang3.StringEscapeUtils.escapeHtml4
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import collection.JavaConversions._
import scala.collection.JavaConversions.mapAsScalaMap

import scala.collection.immutable.HashMap

object Etc {

    implicit def bool2int(b: Boolean): Int = if (b) 1 else 0
    implicit def int2bool(i: Int): Boolean = if (i == 1) true else false

    def cleanseData(s: String): String = {
        ""
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
                "1m"
            case x if 60000L <= x && x < 3599999L =>
                (time / 60000L) + "m"
            case x if 3600000L <= x && x < 86399999L =>
                (time / 3600000L) + "h"
            case x if x >= 86400000L && x < 31535999999L =>
                (time / 86400000L) + "d"
            case x if x >= 31536000000L && x < Long.MaxValue =>
                (time / 31536000000L) + "y"
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

    def parseHTML(raw: String): String = {
        escapeHtml4(raw).replaceAll("\n{2,}", "</p><p class=\"post-text\">").replace("\n", "<br/>").replaceAll("(?i)\\b((?:[a-z][\\w-]+:(?:/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))", "<a href=\"http://$1\" target=\"_blank\">$1</a>")
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

}
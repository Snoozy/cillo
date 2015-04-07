package com.cillo.core.data.cache

import scala.collection.JavaConversions._
import com.cillo.utils.MapUtils._

class Session(t: String) {

    val token: String = t

    var map: Option[java.util.Map[String, String]] = {
        try {
            val string = Memcached.getTouch[String](token)
            if (string.isDefined) {
                Option(deserializeMap(string.get))
            } else
                None
        } catch {
            case e: java.lang.ClassCastException => None
        }
    }

    def refresh() = {
        val string = Memcached.getTouch[String](token)
        if (string.isDefined) {
            this.map = Option(deserializeMap(string.get))
        } else
            this.map = None
    }

    def newSession(id: String) = {
        val currTime  = System.currentTimeMillis().toString
        multiSet(Map("creation_time" -> currTime, "user_id" -> id))
    }

    def get(key: String): Option[String] = {
        map match {
            case Some(_) => Option(map.get.get(key))
            case None => None
        }
    }

    def set(key: String, value: String) = multiSet(Map(key -> value))

    def multiSet(m: Map[String, String]) = {
        try {
            val curr = Memcached.getTouch[String](token)
            if (curr.isDefined) {
                val newMap: java.util.Map[String, String] = deserializeMap(curr.get)
                newMap.putAll(m)
                Memcached.set(token, serializeMap(newMap))
                this.map = Option(newMap)
            } else {
                Memcached.set(token, serializeMap(m))
                this.map = Option(m)
            }
        } catch {
            case e: java.lang.ClassCastException => Memcached.set(token, serializeMap(m))
        }
    }

    def remove(key: String) = {
        try {
            val curr = Memcached.getTouch[String](token)
            if (curr.isDefined) {
                val newMap = deserializeMap(curr.get)
                newMap.remove(key)
                Memcached.set(token, serializeMap(newMap))
                this.map = Option(newMap)
            }
        } catch {
            case e: java.lang.ClassCastException => Memcached.set(token, "{}")
        }
    }

    override def toString = {
        map.toString
    }

}
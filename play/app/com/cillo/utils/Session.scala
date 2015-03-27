package com.cillo.utils

import _root_.play.api.libs.json._
import com.cillo.core.data.cache.Memcached
import com.cillo.utils.Etc

import scala.collection.mutable
import scala.collection.mutable.HashMap

class Session(t: String) {

    val token: String = t

    def get(key: String): Option[String] = Etc.deserializeMap(Memcached.get(token)).get(key)

    def set(key: String, value: String) = multiSet(Map(key -> value))

    def multiSet(m: Map[String, String]) = {
        val curr = Etc.deserializeMap(Memcached.get(token))
        val newMap: mutable.Map[String, String] = new mutable.HashMap[String, String]()
        curr.foreach {
            case (k, v) =>
                newMap.put(k, v)
        }
        m.foreach {
            case (k, v) =>
                newMap.put(k, v)
        }
        val s = Etc.serializeMap(newMap.toMap)
        Memcached.set(token, s)
    }

    def remove(key: String) = {
        val curr = Memcached.get(key)
        val newStr = curr.substring(0, curr.indexOf("\"" + key + "\"")) + curr.substring(curr.indexOf(",", curr.indexOf("\"" + key +"\"")) + 1)
        Memcached.set(key, newStr)
    }

}
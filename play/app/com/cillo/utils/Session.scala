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
        val curr = Option(Etc.deserializeMap(Memcached.get(token)))
        val newMap: mutable.Map[String, String] = new mutable.HashMap[String, String]()
        if (curr.isDefined) {
            curr.get.foreach {
                case (k, v) =>
                    newMap.put(k, v)
            }
        }
        m.foreach {
            case (k, v) =>
                newMap.put(k, v)
        }
        val s = Etc.serializeMap(newMap.toMap)
        Memcached.set(token, s)
    }

    def remove(key: String) = {
        val curr = Option(Etc.deserializeMap(Memcached.get(token)))
        if (curr.isDefined) {
            val newMap = curr.get - key
            Memcached.set(token, Etc.serializeMap(newMap))
        }
    }

    override def toString = {
        Memcached.get(token)
    }

}
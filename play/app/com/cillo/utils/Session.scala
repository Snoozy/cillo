package com.cillo.utils

import com.cillo.core.data.cache.Memcached
import scala.collection.mutable

class Session(t: String) {

    val token: String = t

    def get(key: String): Option[String] = Etc.deserializeMap(Memcached.getTouch(token).getOrElse("")).get(key)

    def set(key: String, value: String) = multiSet(Map(key -> value))

    def multiSet(m: Map[String, String]) = {
        val curr = Option(Etc.deserializeMap(Memcached.getTouch(token).getOrElse("")))
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
        val curr = Option(Etc.deserializeMap(Memcached.getTouch(token).getOrElse("")))
        if (curr.isDefined) {
            val newMap = curr.get - key
            Memcached.set(token, Etc.serializeMap(newMap))
        }
    }

    override def toString = {
        Memcached.getTouch(token).getOrElse("")
    }

}
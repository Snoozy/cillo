package com.cillo.core.data.cache

class Session(t: String) {

    val token: String = t

    def newSession(id: String) = {
        val currTime  = System.currentTimeMillis().toString
        multiSet(Map("creation_time" -> currTime, "user_id" -> id))
    }

    def get(key: String): Option[String] = {
        val cache = Memcached.getTouch[Map[String, String]](token)
        if (cache.isDefined) {
            cache.get.get(key)
        } else {
            None
        }
    }

    def set(key: String, value: String) = multiSet(Map(key -> value))

    def multiSet(m: Map[String, String]) = {
        val curr = Memcached.getTouch[Map[String, String]](token)
        if (curr.isDefined) {
            Memcached.set(token, curr.get ++ m)
        } else {
            Memcached.set(token, m)
        }
    }

    def remove(key: String) = {
        val curr = Memcached.getTouch[Map[String, String]](token)
        if (curr.isDefined) {
            Memcached.set(token, curr.get - key)
        }
    }

    override def toString = {
        Memcached.getTouch[Map[String, String]](token).toString
    }

}
package com.cillo.core.data.cache

import com.redis._
import serialization._
import Parse.Implicits._

case class RedisAddressUndefined(message: String) extends Exception(message)

object Redis {

    private lazy val clients = new RedisClientPool(addr.get.split(":")(0), addr.get.split(":")(1).toInt)
    private var addr: Option[String] = None

    def init(addr: String) = {
        this.addr = Some(addr)
        clients.withClient {
            client => {
                client.get[String]("init")
            }
        }
    }

    def get[A](key: String)(implicit format: Format, parse: Parse[A]): Option[A] = {
        if (addr.isEmpty)
            throw new RedisAddressUndefined("Redis client not initialized.")
        clients.withClient {
            client => {
                client.get[A](key)
            }
        }
    }

    def lrange[A](key: String, start: Int, end: Int)(implicit format: Format, parse: Parse[A]): Option[List[Option[A]]] = {
        if (addr.isEmpty)
            throw new RedisAddressUndefined("Redis client not initialized.")
        clients.withClient {
            client => {
                client.lrange[A](key, start, end)
            }
        }
    }

    def set[A](key: String, value: Any) = {
        if (addr.isEmpty)
            throw new RedisAddressUndefined("Redis client not initialized.")
        clients.withClient {
            client => {
                client.set(key, value)
            }
        }
    }

    def setex[A](key: String, value: Any, expire: Int = 86400) = {
        if (addr.isEmpty)
            throw new RedisAddressUndefined("Redis client not initialized.")
        clients.withClient {
            client => {
                client.setex(key, expire, value)
            }
        }
    }

    def lpush[A](key: String, values: List[A]) = {
        if (addr.isEmpty)
            throw new RedisAddressUndefined("Redis client not initialized.")
        clients.withClient {
            client => {
                client.lpush(key, values)
            }
        }
    }

    def llen(key: String): Option[Int] = {
        if (addr.isEmpty)
            throw new RedisAddressUndefined("Redis client not initialized.")
        clients.withClient {
            client => {
                client.llen(key).map(_.toInt)
            }
        }
    }

    def del(key: String) = {
        if (addr.isEmpty)
            throw new RedisAddressUndefined("Redis client is not initialized.")
        clients.withClient {
            client => {
                client.del(key)
            }
        }
    }

}
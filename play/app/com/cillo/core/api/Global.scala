package com.cillo.core.api

import com.cillo.core.api.apple.PushNotifications
import com.cillo.core.api.controllers.EtcController
import com.cillo.core.data.cache.Memcached
import com.cillo.core.data.cache.Redis
import play.api.Play.current
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc._
import play.api.Logger
import play.api.{Application, GlobalSettings, Play}
import play.filters.gzip.GzipFilter

import scala.concurrent.Future

/**
 * Global file for the application that handles http request rejection if and initializes the
 * memcached instance on application start.
 */

object Global extends WithFilters(new GzipFilter()) with GlobalSettings {

    override def onRouteRequest(request: RequestHeader): Option[Handler] = {
        super.onRouteRequest(request)
        /*
        val x = request.headers.get("X-Forwarded-Proto")
        val ua = request.headers.get("User-Agent")
        if (Play.isProd && (!x.isDefined || x.size == 0 || !x.get.contains("https")) && !(ua.isDefined && ua.get.startsWith("ELB-HealthChecker"))) {
            Some(EtcController.rejectHttp)
        } else if (ua.isDefined && ua.get.startsWith("ELB-HealthChecker")) {
            Some(EtcController.healthCheck)
        } else {
            super.onRouteRequest(request)
        }
        */
    }

    override def onError(request: RequestHeader, ex: Throwable) = {
        Future.successful(InternalServerError(Json.obj("error" -> "Unknown error.")))
    }

    override def onHandlerNotFound(request: RequestHeader) = {
        Future.successful(NotFound(Json.obj("error" -> "Route not found.")))
    }

    override def onStart(app: Application) {
        val addr = Play.current.configuration.getString("redis.address").getOrElse("127.0.0.1:6379")
        Redis.init(addr)

        val certPath = Play.current.configuration.getString("apns.certPath").getOrElse("")
        val certPass = Play.current.configuration.getString("apns.certPassword").getOrElse("")
        PushNotifications.init(certPath, certPass)

        /* TRANSFER LOGICAL CACHE TO REDIS, USE MEMCACHED WITH OBJECT CACHE
        val addr = Play.current.configuration.getString("memcached.address").getOrElse("127.0.0.1:11211")
        Memcached.setAddr(addr)
        Memcached.get("initializing...") // warm up connection pool and lazy val for memcached builder
        */
    }

}
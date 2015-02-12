package com.cillo.core.api

import com.cillo.core.api.controllers.EtcController
import com.cillo.core.data.cache.Memcached
import play.api.Play.current
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc._
import play.api.{Application, GlobalSettings, Play}
import play.filters.gzip.GzipFilter

import scala.concurrent.Future

/**
 * Global file for the application that handles http request rejection if and initializes the
 * memcached instance on application start.
 */

object Global extends WithFilters(new GzipFilter()) with GlobalSettings {

    override def onRouteRequest(request: RequestHeader): Option[Handler] = {
        val x = request.headers.get("X-Forwarded-Proto")
        if (Play.isProd && (!x.isDefined || x.size == 0 || !x.get.contains("https")) && request.path != "/debug/health") {
            Some(EtcController.rejectHttp)
        } else {
            super.onRouteRequest(request)
        }
    }

    override def onError(request: RequestHeader, ex: Throwable) = {
        Future.successful(InternalServerError(Json.obj("error" -> "Unknown error.")))
    }

    override def onHandlerNotFound(request: RequestHeader) = {
        Future.successful(NotFound(Json.obj("error" -> "Route not found.")))
    }

    override def onStart(app: Application) {
        val addr = Play.current.configuration.getString("memcached.address").getOrElse("127.0.0.1:11211")
        Memcached.setAddr(addr)
        Memcached.get("initializing...") // warm up connection pool and lazy val for memcached builder
    }

}
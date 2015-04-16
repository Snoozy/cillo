package com.cillo.core.web

import com.cillo.core.data.cache.Redis
import com.cillo.core.web.controllers.EtcController
import com.googlecode.htmlcompressor.compressor.HtmlCompressor
import com.mohiva.play.htmlcompressor.HTMLCompressorFilter
import play.api.Play.current
import play.api.mvc.Results._
import play.api.mvc._
import play.api.{Application, GlobalSettings, Play}
import com.cillo.core.social.FB
import play.filters.gzip.GzipFilter

import scala.concurrent.Future

object Global extends WithFilters(new GzipFilter(), HTMLCompressorFilter()) with GlobalSettings {

    override def onRouteRequest(request: RequestHeader): Option[Handler] = {
        val x = request.headers.get("X-Forwarded-Proto")
        val ua = request.headers.get("User-Agent")
        if (Play.isProd && (!x.isDefined || x.size == 0 || !x.get.contains("https")) && !(ua.isDefined && ua.get.startsWith("ELB-HealthChecker"))) {
            Some(com.cillo.core.web.controllers.EtcController.redirectHttp)
        } else if (ua.isDefined && ua.get.startsWith("ELB-HealthChecker")) {
            Some(EtcController.healthCheck)
        } else {
            super.onRouteRequest(request)
        }
    }

    override def onError(request: RequestHeader, ex: Throwable) = {
        if (Play.isProd) {
            Future.successful(InternalServerError("Oops. Something broke..."))
        } else {
            super.onError(request, ex)
        }
    }

    override def onStart(app: Application) {
        val addr: String = Play.current.configuration.getString("redis.address").getOrElse("127.0.0.1:6379")
        Redis.init(addr)

        val staticPrefix = Play.current.configuration.getString("static.prefix")
        if (Play.isProd && !staticPrefix.isDefined) {
            throw new StaticPrefixMissing("Static prefix is missing.")
        }

        /* USING REDIS FOR LOGICAL CACHE, MOVE OBJECT CACHE TO MEMCACHED
        val addr: String = Play.current.configuration.getString("memcached.address").getOrElse("127.0.0.1:11211")
        Memcached.setAddr(addr)
        Memcached.get("init")
        */
        val fbId: Option[String] = Play.current.configuration.getString("facebook.client_id")
        val fbSecret: Option[String] = Play.current.configuration.getString("facebook.client_secret")
        if (fbId.isDefined && fbSecret.isDefined)
            FB.init(fbId.get, fbSecret.get)
    }

}

object HTMLCompressorFilter {

    def apply() = new HTMLCompressorFilter({
        val compressor = new HtmlCompressor()
        if (Play.isDev) {
            compressor.setPreserveLineBreaks(true)
        }
        compressor.setRemoveComments(true)
        compressor.setRemoveIntertagSpaces(true)
        compressor.setRemoveHttpProtocol(false)
        compressor.setRemoveHttpsProtocol(false)
        compressor
    })

}

case class StaticPrefixMissing(message: String) extends Exception(message)
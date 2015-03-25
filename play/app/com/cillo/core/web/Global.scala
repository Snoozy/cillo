package com.cillo.core.web

import com.cillo.core.data.cache.Memcached
import com.cillo.core.web.controllers.EtcController
import com.mohiva.play.htmlcompressor.HTMLCompressorFilter
import play.api.Play.current
import play.api.mvc._
import play.api.{Application, GlobalSettings, Play}
import play.filters.gzip.GzipFilter

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

    override def onStart(app: Application) {
        val addr: String = Play.current.configuration.getString("memcached.address").getOrElse("127.0.0.1:11211")
        Memcached.setAddr(addr)
        Memcached.get("init")
    }

}
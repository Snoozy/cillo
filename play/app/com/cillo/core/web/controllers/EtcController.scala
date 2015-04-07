package com.cillo.core.web.controllers

import play.api.mvc._
import com.cillo.utils.play.Auth._
import com.cillo.core.email._
import com.cillo.core.data.search.Search

object EtcController extends Controller {

    def healthCheck = Action {
        Ok("Healthy")
    }

    def debug = AuthAction { implicit user => implicit request =>
        val b = Search.autoComplete("asdf")
        Ok(b.toString())
    }

    def redirectHttp = Action { implicit request =>
        MovedPermanently("https://" + request.host + request.uri).withHeaders(("Strict-Transport-Security", "max-age=31536000"))
    }

}
package com.cillo.core.web.controllers

import play.api.mvc._
import com.cillo.utils.play.Auth._

object EtcController extends Controller {

    def healthCheck = Action {
        Ok("Healthy")
    }

    def debug = AuthAction { implicit user => implicit request =>
        user.get.session.get.remove("creation_time")
        Ok(user.get.session.get.toString())
    }

    def redirectHttp = Action { implicit request =>
        MovedPermanently("https://" + request.host + request.uri).withHeaders(("Strict-Transport-Security", "max-age=31536000"))
    }

}
package com.cillo.core.web.controllers

import play.api.mvc._
import com.cillo.utils.play.Auth._
import com.cillo.core.email.Email

object EtcController extends Controller {

    def healthCheck = Action {
        Ok("Healthy")
    }

    def debug = AuthAction { implicit user => implicit request =>
        Email.sendHTML("testing", "danielli803@gmail.com", "info@cillo.co", "<p>Hi</p>")
        user.get.session.get.remove("getting_started")
        Ok(user.get.session.get.toString())
    }

    def redirectHttp = Action { implicit request =>
        MovedPermanently("https://" + request.host + request.uri).withHeaders(("Strict-Transport-Security", "max-age=31536000"))
    }

}
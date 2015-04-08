package com.cillo.core.web.controllers

import play.api.mvc._
import com.cillo.utils.play.Auth._
import com.cillo.core.email._
import com.cillo.core.data.search.Search
import com.cillo.core.web.controllers.RegisterController.sendWelcomeEmail

object EtcController extends Controller {

    def healthCheck = Action {
        Ok("Healthy")
    }

    def debug = AuthAction { implicit user => implicit request =>
        //sendWelcomeEmail("Daniel", "danielli803@gmail.com")
        Ok("Ok")
    }

    def redirectHttp = Action { implicit request =>
        MovedPermanently("https://" + request.host + request.uri).withHeaders(("Strict-Transport-Security", "max-age=31536000"))
    }

}
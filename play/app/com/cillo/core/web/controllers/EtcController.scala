package com.cillo.core.web.controllers

import play.api.mvc._

object EtcController extends Controller {

    def healthCheck = Action {
        Ok("Healthy")
    }

    def redirectHttp = Action { implicit request =>
        MovedPermanently("https://" + request.host + request.uri)
    }

}
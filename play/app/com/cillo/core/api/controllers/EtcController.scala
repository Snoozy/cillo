package com.cillo.core.api.controllers

import play.api.libs.json.Json
import play.api.mvc._

object EtcController extends Controller {

    def healthCheck = Action {
        Ok("Instance healthy.")
    }

    def rejectHttp = Action {
        BadRequest(Json.obj("error" -> "Https required."))
    }

}
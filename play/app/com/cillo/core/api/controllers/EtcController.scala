package com.cillo.core.api.controllers

import play.api.libs.json.Json
import play.api.mvc._
import com.cillo.core.api.apple.PushNotifications

object EtcController extends Controller {

    def healthCheck = Action {
        Ok("Instance healthy.")
    }

    def rejectHttp = Action {
        BadRequest(Json.obj("error" -> "Https required."))
    }

    def etc = Action {
        PushNotifications.sendNotification(76, "Hi.")
        Ok("asdf")
    }

}
package com.cillo.core.web.controllers

import com.cillo.core.data.db.models._
import com.cillo.utils.play.Auth.AuthAction
import play.api.mvc._
import play.api.libs.json.Json

object NotificationController extends Controller {

    def readNotifications = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User not authenticated."))
            case Some(_) =>
                Notification.read(user.get.userId.get)
                Ok(Json.obj("status" -> "success"))
        }
    }

}
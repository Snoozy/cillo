package com.cillo.core.api.controllers

import com.cillo.core.data.db.models._
import com.cillo.utils.play.Auth.AuthAction
import play.api.mvc._
import play.api.libs.json.Json

object NotificationController extends Controller {

    def getNotifications = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User must be authenticated."))
            case Some(u) =>
                val notifs = Notification.getNotifications(u.userId.get)
                Ok(Notification.toJsonSeq(notifs))
        }
    }

}
package com.cillo.core.api.controllers

import com.cillo.core.data.db.models._
import com.cillo.utils.play.Auth._
import play.api.mvc._
import play.api.libs.json.Json

object NotificationController extends Controller {

    def getNotifications = ApiAuthAction { implicit user => implicit request =>
            val notifs = Notification.getNotifications(user.get.userId.get)
            Ok(Json.obj("notifications" -> Notification.toJsonSeq(notifs)))
    }

    def readNotifications = ApiAuthAction { implicit user => implicit request =>
        Notification.read(user.get.userId.get)
        Ok(Json.obj("success" -> "Successful"))
    }

}
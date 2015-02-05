package com.cillo.core.api.controllers

import com.cillo.core.data.db.models._
import com.cillo.utils.play.Auth.AuthAction
import play.api.libs.json.Json
import play.api.mvc._

object SettingController extends Controller{

    def updateSelf = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User must be authenticated."))
            case Some(_) =>
                val body: AnyContent = request.body
                body.asFormUrlEncoded.map { form =>
                    val name = form.get("name").map(_.head).getOrElse(user.get.name)
                    val bio = form.get("bio").map(_.head).getOrElse(user.get.bio)
                    var pic = 1
                    try {
                         pic = form.get("photo").map(_.head).getOrElse(user.get.photo_id + "").toInt
                    } catch {
                        case e: java.lang.NumberFormatException =>
                    }
                    if (User.update(user.get.user_id.get, name, bio, pic) > 0) {
                        Ok(User.toJsonByUserID(user.get.user_id.get, self = user))
                    } else {
                        BadRequest(Json.obj("error" -> "Error updating user information."))
                    }
                }.getOrElse(BadRequest(Json.obj("error" -> "Request content type invalid.")))
        }
    }

    def updateGroup(group_id: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User must be authenticated."))
            case Some(_) =>
                val group = Group.find(group_id)
                if (group.isDefined && group.get.creator_id == user.get.user_id.get) {
                    val body: AnyContent = request.body
                    body.asFormUrlEncoded.map { form =>
                        val descr = form.get("description").map(_.head).getOrElse(group.get.description)
                        var pic = 1
                        try {
                            pic = form.get("picture").map(_.head).getOrElse(group.get.photo_id + "").toInt
                        } catch {
                            case e: java.lang.NumberFormatException =>
                        }
                        if (Group.update(group_id, descr, pic) > 0) {
                            Ok(Group.toJson(Group.find(group_id).get, true))
                        } else {
                            BadRequest(Json.obj("error" -> "Error updating group information."))
                        }
                    }.getOrElse(BadRequest(Json.obj("error" -> "Request content type invalid.")))
                } else {
                    BadRequest(Json.obj("error" -> "Invalid request."))
                }
        }
    }

}
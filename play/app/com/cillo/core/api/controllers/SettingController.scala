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
                    val username = form.get("username").map(_.head).getOrElse(user.get.bio)
                    val bio = form.get("bio").map(_.head).getOrElse(user.get.bio)
                    val pic = {
                        try {
                            form.get("photo").map(_.head).getOrElse(user.get.photo_id + "").toInt
                        } catch {
                            case e: java.lang.NumberFormatException => user.get.photo_id
                        }
                    }
                    if (User.update(user.get.user_id.get, name, username, bio, pic) > 0) {
                        Ok(User.toJsonByUserID(user.get.user_id.get, self = user))
                    } else {
                        BadRequest(Json.obj("error" -> "Error updating user information."))
                    }
                }.getOrElse(BadRequest(Json.obj("error" -> "Request content type invalid.")))
        }
    }

    def updateBoard(board_id: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User must be authenticated."))
            case Some(_) =>
                val board = Board.find(board_id)
                if (board.isDefined && board.get.creator_id == user.get.user_id.get) {
                    val body: AnyContent = request.body
                    body.asFormUrlEncoded.map { form =>
                        val descr = form.get("description").map(_.head).getOrElse(board.get.description)
                        var pic = 1
                        try {
                            pic = form.get("picture").map(_.head).getOrElse(board.get.photo_id + "").toInt
                        } catch {
                            case e: java.lang.NumberFormatException =>
                        }
                        if (Board.update(board_id, descr, pic) > 0) {
                            Ok(Board.toJson(Board.find(board_id).get, true))
                        } else {
                            BadRequest(Json.obj("error" -> "Error updating board information."))
                        }
                    }.getOrElse(BadRequest(Json.obj("error" -> "Request content type invalid.")))
                } else {
                    BadRequest(Json.obj("error" -> "Invalid request."))
                }
        }
    }

}
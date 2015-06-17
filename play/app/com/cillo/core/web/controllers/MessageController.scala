package com.cillo.core.web.controllers

import com.cillo.core.data.db.models._
import com.cillo.utils.play.Auth.AuthAction
import com.cillo.core.web.views.html.core
import play.api.mvc._
import play.api.libs.json.Json

object MessageController extends Controller {

    def inbox = AuthAction { implicit user => implicit request =>
        user match {
            case None =>
                Found("/login")
            case Some(_) =>
                val conversations = Conversation.byUser(user.get.userId.get)
                user.get.readInbox()
                Ok(core.inbox(conversations, user.get))
        }
    }

    def send(id: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None =>
                BadRequest(Json.obj("error" -> "User must be authenticated"))
            case Some(u) =>
                val receiver = User.find(id)
                if (receiver.isDefined) {
                    request.body.asFormUrlEncoded.map { form =>
                        val content = form.get("content").map(_.head)
                        if (content.isDefined) {
                            val msg = Message.create(u.userId.get, receiver.get.userId.get, content.get)
                            if (msg.isDefined)
                                Ok(Message.toJsonSingle(Message.find(msg.get.toInt).get))
                            else
                                BadRequest(Json.obj("error" -> "Error creating message."))
                        } else {
                            BadRequest(Json.obj("error" -> "Content does not exist."))
                        }
                    }.getOrElse(BadRequest(Json.obj("error" -> "Request format invalid.")))
                } else {
                    BadRequest(Json.obj("error" -> "Recipient does not exist."))
                }
        }
    }

}
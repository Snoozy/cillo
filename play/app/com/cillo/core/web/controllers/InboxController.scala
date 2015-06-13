package com.cillo.core.web.controllers

import com.cillo.core.data.db.models._
import com.cillo.utils.play.Auth.AuthAction
import com.cillo.core.web.views.html.components
import play.api.mvc._


object InboxController extends Controller {

    def inbox = AuthAction { implicit user => implicit request =>
        user match {
            case None =>
                Found("/login")
            case Some(_) =>
                val conversations = Conversation.byUser(user.get.userId.get)
                user.get.readInbox()
                Ok("Messages coming soon...")
        }
    }

}
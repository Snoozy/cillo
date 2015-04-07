package com.cillo.core.web.controllers

import com.cillo.core.data.db.models._
import com.cillo.utils.play.Auth.AuthAction
import com.cillo.core.web.views.html.components
import play.api.mvc._
import play.api.libs.json.Json

object GettingStartedController extends Controller {

    def gettingStarted = AuthAction { implicit user => implicit request =>
        user match {
            case None => Found("/")
            case Some(_) =>
                val follow = request.getQueryString("follow")
                if (follow.isDefined) {
                    try {
                        val group_ids = follow.get.split(",").map(_.toInt)
                        group_ids.foreach { id =>
                            Board.addFollower(user.get.user_id.get, id)
                        }
                    } catch {
                        case e: java.lang.NumberFormatException =>
                    }
                    if (user.get.session.isDefined) {
                        user.get.session.get.remove("getting_started")
                    }
                }
                Found("/")
        }
    }

}
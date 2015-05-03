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
                if (user.get.session.isDefined) {
                    user.get.session.get.remove("getting_started")
                }
                Found("/")
        }
    }

}
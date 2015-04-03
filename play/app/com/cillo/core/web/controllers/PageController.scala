package com.cillo.core.web.controllers

import com.cillo.core.data.db.models._
import com.cillo.core.web.views.html.core
import com.cillo.utils.play.Auth.AuthAction
import play.api.mvc._
import play.api.libs.json.Json

object PageController extends Controller {

    def home = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required."))
            case Some(_) =>
                val page = request.getQueryString("p")
        }
    }

}
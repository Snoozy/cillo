package com.cillo.core.web.controllers

import com.cillo.core.data.db.models._
import com.cillo.core.web.views.html.components
import com.cillo.core.web.views.html.core
import com.cillo.utils.play.Auth.AuthAction
import play.api.libs.json.Json
import com.cillo.core.social.FB
import play.api.mvc._

object SocialController extends Controller {

    def fbLogin = AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => Found("/")
            case None =>
                val token = request.getQueryString("token")
                if (token.isDefined) {
                    val fb = FB.createFBInstance(token.get)
                    Ok("s")
                } else {
                    NotFound("Token needed for facebook login")
                }
        }
    }

}
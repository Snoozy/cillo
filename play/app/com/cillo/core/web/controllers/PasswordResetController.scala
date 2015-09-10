package com.cillo.core.web.controllers

import com.cillo.core.data.db.models._
import com.cillo.utils.play.Auth._
import play.api.libs.json.Json
import play.api.mvc._

object PasswordResetController extends Controller {

    /*def resetPage = AuthAction { implicit user => implicit request =>
        val token = request.getQueryString("token")
        token match {
            case None => BadRequest("Reset token required.")
            case Some(_) =>

        }
    }*/

}
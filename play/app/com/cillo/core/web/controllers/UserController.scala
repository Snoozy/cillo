package com.cillo.core.web.controllers

import com.cillo.utils.play.Auth.AuthAction
import play.api.mvc._

object UserController extends Controller {

    def userPage(username: String) = AuthAction { implicit user => implicit request =>
        Ok("TODO")
    }

}
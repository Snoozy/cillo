package com.cillo.core.web.controllers

import com.cillo.core.data.db.models._
import com.cillo.utils.play.Auth.AuthAction
import play.api.mvc._


object SettingsController extends Controller {

    def settingsPage = AuthAction { implicit user => implicit request =>
        user match {
            case None => Found("/login")
            case Some(_) => Ok("asdf")
        }
    }

}
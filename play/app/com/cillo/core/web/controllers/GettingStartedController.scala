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
                val boards = Map[String, Seq[Int]]("adsf" -> Seq(1,2,1,2,1), "qwerty" -> Seq(1,1,1,1,1,1,1), "zxcv" -> Seq(1,1,1,1,1,1))
                Ok(com.cillo.core.web.views.html.core.getting_started(boards, user.get))
        }
    }

}
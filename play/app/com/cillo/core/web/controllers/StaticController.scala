package com.cillo.core.web.controllers

import play.api.mvc._
import play.api.Play

object StaticController extends Controller {

    def support = Action {
        Ok(com.cillo.core.web.views.html.desktop.core.support())
    }

}
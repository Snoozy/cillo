package com.cillo.core.web.controllers

import com.cillo.core.data.db.models._
import com.cillo.utils.play.Auth.AuthAction
import com.cillo.core.web.views.html.components
import play.api.mvc._
import play.api.libs.json.Json

object ExploreController extends Controller {

    def explore = AuthAction { implicit user => implicit request =>
        val boards = Board.getTrendingBoards
        Ok(com.cillo.core.web.views.html.core.explore(user, boards))
    }

}
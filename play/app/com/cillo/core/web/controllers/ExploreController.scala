package com.cillo.core.web.controllers

import com.cillo.core.data.db.models._
import com.cillo.utils.play.Auth.AuthAction
import play.api.mvc._

object ExploreController extends Controller {

    def explore = AuthAction { implicit user => implicit request =>
        val boards = Board.getTrendingBoards(limit = 50)
        Ok(com.cillo.core.web.views.html.desktop.core.explore(user, boards))
    }

}
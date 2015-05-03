package com.cillo.core.web.controllers

import com.cillo.core.data.db.models._
import com.cillo.core.data.search.Search
import com.cillo.utils.play.Auth.AuthAction
import play.api.mvc._

object SearchController extends Controller {

    def searchPage = AuthAction { implicit user => implicit request =>
        val q = request.getQueryString("q")
        if (q.isDefined) {
            val boards = Search.boardSearch(q.get)
            val users = Search.userSearch(q.get)
            Ok(com.cillo.core.web.views.html.core.search(user, boards, users, q.get))
        } else {
            Found("/")
        }
    }

}
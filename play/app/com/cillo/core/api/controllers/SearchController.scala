package com.cillo.core.api.controllers

import com.cillo.utils.play.Auth.AuthAction
import play.api.libs.json.Json
import play.api.mvc._
import com.cillo.core.data.search.Search
import com.cillo.core.data.db.models._


object SearchController extends Controller {

    def fullSearchBoard = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required. Code: 10"))
            case Some(_) =>
                val query = request.getQueryString("q")
                if (query.isDefined) {
                    val boards = Search.boardSearch(query.get)
                    Ok(Json.obj("results" -> Board.toJsonSeq(boards, user = user)))
                } else {
                    BadRequest(Json.obj("error" -> "Search query required."))
                }
        }
    }

    def autoCompleteBoard = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required. Code: 10"))
            case Some(_) =>
                val query = request.getQueryString("q")
                if (query.isDefined) {
                    val boards = Search.autoCompleteBoard(query.get)
                    Ok(Json.obj("results" -> Board.toJsonSeq(boards, user = user)))
                } else {
                    BadRequest(Json.obj("error" -> "Search query required."))
                }

        }
    }

}
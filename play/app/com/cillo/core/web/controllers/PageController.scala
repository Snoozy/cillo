package com.cillo.core.web.controllers

import com.cillo.core.data.db.models._
import com.cillo.core.web.views.html.core
import com.cillo.utils.play.Auth.AuthAction
import play.api.mvc._
import play.api.libs.json.Json

object PageController extends Controller {

    def neverending = AuthAction { implicit user => implicit request =>
        val context = request.getQueryString("context")
        if (context.isDefined) {
            if (context.get == "home") {
                val afterPost = request.getQueryString("after")
                val posts = {
                    if (afterPost.isDefined) {
                        User.getFeedPaged(user.get.user_id.get, afterPost.get.toInt)
                    } else {
                        User.getFeed(user.get.user_id.get)
                    }
                }
                Ok(Json.obj("item_html" -> Post.toHTMLWIthUser(posts, user)))
            } else if (context.get == "user") {
                val afterPost = request.getQueryString("after")
                val userId = request.getQueryString("user")
                val describeUser = User.find(userId.get.toInt)
                if (describeUser.isDefined) {
                    val posts = {
                        if (afterPost.isDefined) {
                            User.getPostsPaged(describeUser.get.user_id.get, afterPost.get.toInt)
                        } else {
                            User.getPosts(describeUser.get.user_id.get)
                        }
                    }
                    Ok(Json.obj("item_html" -> Post.toHTMLWIthUser(posts, user)))
                } else
                    BadRequest(Json.obj("error" -> "User does not exist."))
            } else if (context.get == "board") {
                val afterPost = request.getQueryString("after")
                val boardId = request.getQueryString("board")
                val board = Board.find(boardId.get.toInt)
                if (board.isDefined) {
                    val posts = {
                        if (afterPost.isDefined) {
                            Board.getTrendingPostsPaged(board.get.board_id.get, afterPost.get.toInt)
                        } else {
                            Board.getTrendingPosts(board.get.board_id.get)
                        }
                    }
                    Ok(Json.obj("item_html" -> Post.toHTMLWIthUser(posts, user)))
                } else
                    BadRequest(Json.obj("error" -> "Board does not exist."))

            } else BadRequest(Json.obj("error" -> "Invalid context."))
        } else
            BadRequest(Json.obj("error" -> "Context required."))
    }

}
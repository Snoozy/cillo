package com.cillo.core.web.controllers

import com.cillo.utils.play.Auth.AuthAction
import play.api.mvc._
import com.cillo.core.data.db.models._
import com.cillo.core.web.views.html.core

object UserController extends Controller {

    def userPage(username: String) = AuthAction { implicit user => implicit request =>
        val describeUser = User.find(username)
        if (describeUser.isDefined) {
            val posts = User.getPosts(describeUser.get.user_id.get)
            val comments = User.getComments(describeUser.get.user_id.get)
            val boards = User.getBoards(describeUser.get.user_id.get)
            val postsCount = User.getPostsCount(describeUser.get.user_id.get)
            Ok(core.user(describeUser.get, user, posts, comments, boards, postsCount))
        } else {
            NotFound("User not found.")
        }
    }

}
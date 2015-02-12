package com.cillo.core.web.controllers

import com.cillo.core.data.db.models.{Group, Post, User}
import com.cillo.utils.play.Auth.AuthAction
import play.api.mvc._

object IndexController extends Controller {

    def homePage = AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) =>
                val posts = User.getFeed(user.get.user_id.get)
                Ok(com.cillo.core.web.views.html.core.index(posts, user.get, User.getGroups(user.get.user_id.get)))
            case None =>
                Ok(com.cillo.core.web.views.html.core.welcome(getWelcomeGroups))
        }
    }

    def getWelcomeGroups: Seq[Group] = {
        Group.getTrendingGroups
    }

    def extractPostIDS(posts: Seq[Post]): Seq[Int] = {
        posts.map(_.post_id.get)
    }

}
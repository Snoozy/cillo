package com.cillo.core.web.controllers

import com.cillo.core.data.db.models.{Board, Post, User}
import com.cillo.utils.play.Auth.AuthAction
import play.api.mvc._
import com.cillo.core.data.cache.Memcached

object IndexController extends Controller {

    private val gettingStartedBoards = Map[String, Seq[Int]]("adsf" -> Seq(1,2,1,2,1), "qwerty" -> Seq(1,1,1,1,1,1,1), "zxcv" -> Seq(1,1,1,1,1,1))

    def homePage = AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) =>
                if (user.get.session.isDefined && user.get.session.get.get("getting_started").isDefined) {
                    Ok(com.cillo.core.web.views.html.core.getting_started(gettingStartedBoards, user.get))
                } else {
                    val posts = User.getFeed(user.get.user_id.get)
                    Ok(com.cillo.core.web.views.html.core.index(posts, user.get))
                }
            case None =>
                Ok(com.cillo.core.web.views.html.core.welcome(getWelcomeBoards))
        }
    }

    def cachedWelcomeHtml = {
        val cached = Memcached.get("welcome_cache")
        if (cached.isDefined) {
            Ok(cached.get)
        } else {
            val comp = com.cillo.core.web.views.html.core.welcome(getWelcomeBoards)
            Memcached.set("welcome_cache", comp.toString(), duration = 3600)
            Ok(comp)
        }
    }

    def getWelcomeBoards: Seq[Board] = {
        Board.getTrendingBoards
    }

    def extractPostIDS(posts: Seq[Post]): Seq[Int] = {
        posts.map(_.post_id.get)
    }

}
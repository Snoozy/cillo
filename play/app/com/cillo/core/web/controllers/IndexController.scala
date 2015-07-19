package com.cillo.core.web.controllers

import com.cillo.core.data.db.models.{Board, Post, User}
import com.cillo.utils.play.Auth.AuthAction
import com.cillo.core.data.Constants
import play.api.mvc._
import com.cillo.core.data.cache.Redis

object IndexController extends Controller {

    def homePage = AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) =>
                if (user.get.session.isDefined && user.get.session.get.get("getting_started").isDefined) {
                    cachedGettingStartedHtml
                } else {
                    val boards = User.getBoards(user.get.userId.get)
                    val posts = User.getFeed(user.get.userId.get, boardIds = Some(boards.map(_.boardId.get)))
                    Ok(com.cillo.core.web.views.html.desktop.core.index(posts, user.get, boards)())
                }
            case None =>
                cachedWelcomeHtml
        }
    }

    def cachedGettingStartedHtml = {
        val cached = Redis.get("gettingStarted_cache")
        if (cached.isDefined) {
            Ok(cached.get).as(HTML)
        } else {
            val comp = com.cillo.core.web.views.html.desktop.core.getting_started(Constants.GettingStartedBoards, Constants.FeaturedBoards)
            Redis.setex("gettingStarted_cache", comp.toString(), expire = 3600)
            Ok(comp)
        }
    }

    def cachedWelcomeHtml = {
        val cached = Redis.get("welcome_cache")
        if (cached.isDefined) {
            Ok(cached.get).as(HTML)
        } else {
            val comp = com.cillo.core.web.views.html.desktop.core.welcome(getWelcomeBoards)
            Redis.setex("welcome_cache", comp.toString(), expire = 3600)
            Ok(comp)
        }
    }

    def getWelcomeBoards: Seq[Board] = {
        Board.getTrendingBoards(20)
    }

}
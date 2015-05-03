package com.cillo.core.web.controllers

import com.cillo.core.data.db.models.{Board, Post, User}
import com.cillo.utils.play.Auth.AuthAction
import play.Play
import play.api.mvc._
import com.cillo.core.data.cache.Redis

object IndexController extends Controller {

    private val gettingStartedBoards = {
        if (Play.isDev) {
            Map[String, Seq[Int]]("adsf" -> Seq(1, 2, 1, 2, 1), "qwerty" -> Seq(1, 1, 1, 1, 1, 1, 1), "zxcv" -> Seq(1, 1, 1, 1, 1, 1))
        } else {
            Map[String, Seq[Int]]("Featured" -> Seq(37, 6, 27, 31, 38, 9), "Entertainment" -> Seq(9, 7, 15, 16, 17, 8), "Sports" -> Seq(1, 5, 6, 31, 32, 33),
                "Science" -> Seq(13, 29, 26, 27, 30), "News and Politics" -> Seq(14, 23, 25), "Educational" -> Seq(18, 19, 20, 21, 22))
        }
    }

    def homePage = AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) =>
                if (user.get.session.isDefined && user.get.session.get.get("getting_started").isDefined) {
                    cachedGettingStartedHtml
                } else {
                    val intro = request.getQueryString("intro")
                    val boards = User.getBoards(user.get.user_id.get)
                    val posts = User.getFeed(user.get.user_id.get, board_ids = Some(boards.map(_.board_id.get)))
                    Ok(com.cillo.core.web.views.html.core.index(posts, user.get, boards, intro = intro.isDefined && intro.get == "1"))
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
            val comp = com.cillo.core.web.views.html.core.getting_started(gettingStartedBoards)
            Redis.setex("gettingStarted_cache", comp.toString(), expire = 3600)
            Ok(comp)
        }
    }

    def cachedWelcomeHtml = {
        val cached = Redis.get("welcome_cache")
        if (cached.isDefined) {
            Ok(cached.get).as(HTML)
        } else {
            val comp = com.cillo.core.web.views.html.core.welcome(getWelcomeBoards)
            Redis.setex("welcome_cache", comp.toString(), expire = 3600)
            Ok(comp)
        }
    }

    def getWelcomeBoards: Seq[Board] = {
        Board.getTrendingBoards(20)
    }

}
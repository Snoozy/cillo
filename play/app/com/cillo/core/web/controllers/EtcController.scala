package com.cillo.core.web.controllers

import com.github.jreddit.entity.Submission
import play.api.mvc._
import com.cillo.utils.play.Auth._
import com.cillo.core.email._
import com.cillo.utils.reddit.Reddit
import com.cillo.core.data.db.models._
import com.cillo.core.data.aws.S3
import com.cillo.core.data.search.Search
import com.cillo.core.web.controllers.RegisterController.sendWelcomeEmail

import scala.collection.immutable.HashMap

object EtcController extends Controller {

    val subreddits = HashMap("worldnews" -> List("worldnews", "news"), "nba" -> List("nba"), "photography" -> List("earthporn"), "programming" -> List("programming"), "soccer" -> List("soccer"))

    def healthCheck = Action {
        Ok("Healthy")
    }

    def debug = AuthAction { implicit user => implicit request =>

        subreddits.get("photography").get.foreach { s =>
            if (s == "earthporn") {
                val subms = Reddit.getSubredditPosts(s, count = 1)
                val board = Board.find("asdf")
                subms.foreach { p =>
                    if (p.getTitle.length < 100 && p.getURL.indexOf("imgur") > 0) {
                        val url = {
                            if (p.getDomain == "imgur.com") {
                                p.getURL + ".jpg"
                            } else {
                                p.getURL
                            }
                        }
                        val id = S3.uploadURL(url)
                        if (id.isDefined) {
                            val media = Media.create(0, id.get)
                            val title = p.getTitle.substring(0, p.getTitle.indexOf("["))
                            Post.createMediaPost(1, Some(title), "", board.get.board_id.get, Seq(media.get.toInt))
                        }
                    }
                }
            } else {
                val subms = Reddit.getSubredditPosts(s, count = 5)
                val board = Board.find("asdf")
                subms.foreach { p =>
                    val title = {
                        if (p.getTitle.length < 100) {
                            Some(p.getTitle)
                        } else {
                            None
                        }
                    }
                    val data = {
                        if (p.getTitle.length < 100) {
                            p.getURL
                        } else {
                            p.getTitle + "\n\n" + p.getURL
                        }
                    }
                    Post.createSimplePost(1, title, data, board.get.board_id.get)
                }
            }
        }
        Ok("asdf")
    }

    def log(s: String) = {
        play.api.Logger.debug(s)
    }

    def redirectHttp = Action { implicit request =>
        MovedPermanently("https://" + request.host + request.uri).withHeaders(("Strict-Transport-Security", "max-age=31536000"))
    }

}
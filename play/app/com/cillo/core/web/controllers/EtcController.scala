package com.cillo.core.web.controllers

import play.api.mvc._
import com.cillo.utils.play.Auth._
import com.cillo.utils.reddit.Reddit
import com.cillo.core.data.db.models._
import org.apache.commons.lang3.StringEscapeUtils._
import com.cillo.core.data.aws.S3
import scala.util.Random

object EtcController extends Controller {

    val subreddits = Map[String, List[String]]("worldnews" -> List("worldnews", "news"), "earthpics" -> List("earthporn"), "nba" -> List("nba"), "programming" -> List("programming"), "soccer" -> List("soccer"))
    val users = Vector(1, 2, 3, 4, 8, 13, 14, 12, 10)

    def healthCheck = Action {
        Ok("Healthy")
    }

    def debug = Action {
        Ok("asdf")
    }

    def reddit = AuthAction { implicit user => implicit request =>
        subreddits.foreach {
            case (key, value) =>
                value.foreach { s =>
                    val board = Board.find(key)
                    if (s == "earthporn") {
                        val subms = Reddit.getSubredditPosts(s, count = 1)
                        subms.foreach { p =>
                            val user_id = users(Random.nextInt(users.size))
                            val time = System.currentTimeMillis() - (Random.nextInt(13) * 1800000)
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
                                    val title = p.getTitle.substring(0, p.getTitle.indexOf("[")).replace("&amp;", "&").replace("&gt;", ">").replace("&lt;", "<").replace("&quot;", "\"")
                                    Post.createMediaPost(user_id, Some(title), "", board.get.board_id.get, Seq(media.get.toInt), time = time)
                                }
                            }
                        }
                    } else {
                        val subms = Reddit.getSubredditPosts(s, count = 3)
                        subms.foreach { p =>
                            val user_id = users(Random.nextInt(users.size))
                            val time = System.currentTimeMillis() - (Random.nextInt(13) * 1800000)
                            val title = {
                                if (p.getTitle.length < 100) {
                                    Some(unescapeHtml4(p.getTitle))
                                } else {
                                    None
                                }
                            }
                            val data = {
                                if (p.getTitle.length < 100) {
                                    p.getURL
                                } else {
                                    unescapeHtml4(p.getTitle) + "\n\n" + p.getURL
                                }
                            }
                            Post.createSimplePost(user_id, title, data, board.get.board_id.get, time = time)
                        }
                    }
                }
        }
        Ok("done")
    }

    def redirectHttp = Action { implicit request =>
        MovedPermanently("https://" + request.host + request.uri).withHeaders(("Strict-Transport-Security", "max-age=31536000"))
    }

}
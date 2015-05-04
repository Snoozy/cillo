package com.cillo.core.web.controllers

import java.util.regex.Pattern
import com.cillo.core.data.cache.Redis
import play.api.mvc._
import com.cillo.utils.play.Auth._
import com.cillo.utils.reddit.Reddit
import com.cillo.core.data.db.models._
import org.apache.commons.lang3.StringEscapeUtils._
import com.cillo.core.data.aws.S3._
import scala.util.Random

object EtcController extends Controller {

    val subreddits = Map[String, List[String]]("worldnews" -> List("worldnews", "news"), "earthpics" -> List("earthporn"),
        "nba" -> List("nba"), "programming" -> List("programming", "programmerhumor"), "soccer" -> List("soccer"), "politics" -> List("politics"),
        "tech" -> List("technology"), "sports" -> List("sports"), "funny" -> List("funny"), "food" -> List("food", "foodporn"), "music" -> List("music"))
    val users = Vector(1, 2, 3, 4, 8, 13, 14, 12, 10)

    def healthCheck = Action {
        Ok("Healthy")
    }

    def debug = Action {
        Redis.del("gettingStarted_cache")
        Redis.del("welcome_cache")
        Ok("asdf")
    }

    private val imgurRegex = Pattern.compile("^https?:\\/\\/(\\w+\\.)?imgur.com\\/(\\w*\\d\\w*)+(\\.[a-zA-Z]{3})?$")

    def reddit = AuthAction { implicit user => implicit request =>
        subreddits.foreach {
            case (key, value) =>
                value.foreach { s =>
                    val board = Board.find(key)
                    if (board.isDefined) {
                        if (s == "earthporn" || s == "foodporn") {
                            val subms = Reddit.getSubredditPosts(s)
                            subms.foreach { p =>
                                val user_id = users(Random.nextInt(users.size))
                                val time = System.currentTimeMillis() - (Random.nextInt(48) * 1800000)
                                if (p.getURL.indexOf("imgur") > 0) {
                                    val matcher = imgurRegex.matcher(p.getUrl)
                                    if (matcher.find()) {
                                        val url = {
                                            if (!p.getUrl.contains("i.imgur.com")) {
                                                p.getURL + ".jpg"
                                            } else {
                                                p.getURL
                                            }
                                        }
                                        val testTitle = unescapeHtml4(p.getTitle.replace("[OC]", "").substring(0, p.getTitle.indexOf("[")))
                                        val title = {
                                            if (testTitle.length < 100) {
                                                Some(testTitle)
                                            } else {
                                                None
                                            }
                                        }
                                        val data = {
                                            if (testTitle.length < 100) {
                                                ""
                                            } else {
                                                testTitle
                                            }
                                        }
                                        val id = uploadURL(url)
                                        if (id.isDefined) {
                                            val media = Media.create(0, id.get)
                                            Post.createMediaPost(user_id, title, data, board.get.board_id.get, Seq(media.get.toInt), time = time)
                                        }
                                    }
                                }
                            }
                        } else {
                            val subms = Reddit.getSubredditPosts(s)
                            subms.foreach { p =>
                                val user_id = users(Random.nextInt(users.size))
                                val time = System.currentTimeMillis() - (Random.nextInt(13) * 1800000)
                                val matcher = imgurRegex.matcher(p.getUrl)
                                if (matcher.find()) {
                                    val url = {
                                        if (!p.getUrl.contains("i.imgur.com")) {
                                            p.getURL + ".jpg"
                                        } else {
                                            p.getURL
                                        }
                                    }
                                    val testTitle = unescapeHtml4(p.getTitle)
                                    val title = {
                                        if (testTitle.length < 100) {
                                            Some(testTitle)
                                        } else {
                                            None
                                        }
                                    }
                                    val data = {
                                        if (testTitle.length < 100) {
                                            ""
                                        } else {
                                            testTitle
                                        }
                                    }
                                    val id = uploadURL(url)
                                    if (id.isDefined) {
                                        val media = Media.create(0, id.get)
                                        Post.createMediaPost(user_id, title, data, board.get.board_id.get, Seq(media.get.toInt), time = time)
                                    }
                                } else {
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
                }
        }
        Ok("done")
    }

    def redirectHttp = Action { implicit request =>
        MovedPermanently("https://" + request.host + request.uri).withHeaders(("Strict-Transport-Security", "max-age=31536000"))
    }

}
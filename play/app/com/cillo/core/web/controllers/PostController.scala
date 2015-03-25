package com.cillo.core.web.controllers

import com.cillo.core.data.db.models._
import com.cillo.core.web.views.html.components
import com.cillo.core.web.views.html.core
import com.cillo.utils.play.Auth.AuthAction
import play.api.libs.json.Json
import play.api.mvc._

object PostController extends Controller {

    def viewPostPage(board_name: String, post_id: Int) = AuthAction { implicit user => implicit request =>
        val board = Board.find(board_name)
        val post = Post.find(post_id)
        if (post.isDefined && board.isDefined && post.get.board_id == board.get.board_id.get) {
            Ok(core.view_post(user, post.get))
        } else {
            NotFound("Post not found.")
        }
    }

    def post = AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => processPost(request, user.get)
            case None => BadRequest("User not authenticated.")
        }
    }

    def repost = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest("User not authenticated.")
            case Some(_) => processRepost(request, user.get)
        }
    }

    def processRepost(request: Request[AnyContent], user: User): Result = {
        val body: AnyContent = request.body
        body.asFormUrlEncoded.map { form =>
            try {
                val repost_id = form.get("repost").map(_.head)
                val comment = form.get("comment").map(_.head)
                val board_name = form.get("board").map(_.head)
                if (board_name.isDefined) {
                    val board = Board.find(board_name.get)
                    if (board.isDefined) {
                        val repost = Post.find(repost_id.get.toInt)
                        if (repost.isDefined) {
                            val newPost = Post.createSimplePost(user.user_id.get, None, comment.getOrElse(""), board.get.board_id.get, repost.get.post_id)
                            if (newPost.isDefined) {
                                Ok(Json.obj("item_html" -> Json.toJson(components.post(Post.find(newPost.get.toInt).get, Some(user)).toString())))
                            } else {
                                BadRequest("Invalid parameters.")
                            }
                        } else {
                            BadRequest("Post does not exist.")
                        }
                    } else {
                        BadRequest("Board does not exist.")
                    }
                } else {
                    BadRequest("Board name not defined.")
                }
            } catch {
                case e: java.lang.NumberFormatException => return BadRequest("Invalid parameters.")
            }
        }.getOrElse(BadRequest("FormUrlEncoded required."))
    }

    def deletePost(post_id: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest("User not authenticated.")
            case Some(_) =>
                val post = Post.find(post_id)
                if (post.isDefined && post.get.user_id == user.get.user_id.get) {
                    if (Post.deletePost(post_id)) {
                        Ok(Json.obj("success" -> "Successfully deleted post."))
                    } else {
                        BadRequest(Json.obj("error" -> "Unknown error deleting post."))
                    }
                } else {
                    BadRequest(Json.obj("error" -> "Error deleting post."))
                }
        }
    }

    private def processPost(request: Request[AnyContent], user: User): Result = {
        val body: AnyContent = request.body
        body.asFormUrlEncoded.map { form =>
            try {
                var title = form.get("title").map(_.head)
                val data = form.get("data").map(_.head)
                val board_name = form.get("board_name").map(_.head)
                val media_ids = form.get("media").map(_.head)
                if (data.isDefined && board_name.isDefined) {
                    val board = Board.find(board_name.get)
                    if (board.isDefined) {
                        if (title.isDefined && title.get == "") {
                            title = None
                        }
                        val newPost = {
                            if (!media_ids.isDefined || media_ids.get == "") {
                                Post.createSimplePost(user.user_id.get, title, data.get, board.get.board_id.get)
                            } else {
                                Post.createMediaPost(user.user_id.get, title, data.get, board.get.board_id.get, media_ids.get.split("~").map(_.toInt))
                            }
                        }
                        if (newPost.isDefined)
                            Ok(Json.obj("item_html" -> Json.toJson(components.post(Post.find(newPost.get.toInt).get, Some(user)).toString())))
                        else
                            BadRequest("Invalid request.")
                    } else {
                        BadRequest("Invalid board name.")
                    }
                } else
                    BadRequest("Invalid request.")
            } catch {
                case e: java.lang.NumberFormatException => return BadRequest("Invalid parameters.")
            }
        }.getOrElse {
            BadRequest("Only json or form url encoded content types accepted.")
        }
    }

}
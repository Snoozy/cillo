package com.cillo.core.api.controllers

import com.cillo.core.data.db.models.{CommentTree, Board, Post}
import com.cillo.utils.play.Auth.AuthAction
import play.api.libs.json.Json
import play.api.mvc._

/**
 * Controls everything related to posts including:
 *      Describing
 *      Creating
 *      Getting comments
 */

object PostController extends Controller {

    /**
     * Describes a specific post.
     *
     * @param post_id Id of the post to be described.
     * @return Fully hydrated Json of the post.
     */
    def describe(post_id: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required."))
            case Some(_) =>
                val postExists = Post.find(post_id)
                if (!postExists.isDefined)
                    BadRequest(Json.obj("error" -> "Post does not exist."))
                else {
                    val post = postExists.get
                    Ok(Post.toJsonWithUser(Seq(post), user))
                }
        }
    }

    /**
     * Creates a new post.
     *
     * @return
     */
    def create = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required. Code: 10"))
            case Some(_) =>
                val body: AnyContent = request.body
                body.asFormUrlEncoded.map { form =>
                    val repost_id = form.get("repost_id").map(_.head)
                    var post_id: Option[Long] = None
                    if (repost_id.isDefined) {
                        val board_id = form.get("board_id").map(_.head.toInt)
                        val board_name = form.get("board_name").map(_.head)
                        try {
                            if (board_id.isDefined)
                                post_id = Post.createSimplePost(user.get.user_id.get, None, repost_id.get, board_id.get, true)
                            else if (board_name.isDefined) {
                                val board = Board.find(board_name.get)
                                if (board.isDefined)
                                    post_id = Post.createSimplePost(user.get.user_id.get, None, repost_id.get, board.get.board_id.get, true)
                            }
                        } catch {
                            case e: NumberFormatException => // Do nothing so post stays None and if statement is not triggered.
                        }
                    } else {
                        val data = form.get("data").map(_.head)
                        val media_ids = form.get("media").map(_.head)
                        try {
                            val board_id = form.get("board_id").map(_.head.toInt)
                            val board_name = form.get("board_name").map(_.head)
                            if (!media_ids.isDefined) {
                                if (data.isDefined && board_id.isDefined) {
                                    post_id = Post.createSimplePost(user.get.user_id.get, form.get("title").map(_.head), data.get,
                                        board_id.get, false)
                                } else if (board_name.isDefined && data.isDefined) {
                                    val board = Board.find(board_name.get)
                                    if (board.isDefined)
                                        post_id = Post.createSimplePost(user.get.user_id.get, form.get("title").map(_.head), data.get,
                                            board.get.board_id.get, false)
                                }
                            } else {
                                if (data.isDefined && board_id.isDefined) {
                                    post_id = Post.createMediaPost(user.get.user_id.get, form.get("title").map(_.head), data.get,
                                        board_id.get, media_ids.get.split(",").map(_.toInt))
                                } else if (board_name.isDefined && data.isDefined) {
                                    val board = Board.find(board_name.get)
                                    post_id = Post.createMediaPost(user.get.user_id.get, form.get("title").map(_.head), data.get,
                                        board.get.board_id.get, media_ids.get.split(",").map(_.toInt))
                                }
                            }
                        } catch {
                            case e: NumberFormatException => // Do nothing so post stays None and if statement is not triggered.
                        }
                    }
                    if (post_id.isDefined) {
                        Ok(Post.toJsonSingle(Post.find(post_id.get.toInt).get, user))
                    } else {
                        BadRequest(Json.obj("error" -> "Request invalid."))
                    }
                }.getOrElse(BadRequest(Json.obj("error" -> "Request Content-Type Incorrect.")))
        }
    }

    def topComments(post_id: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required."))
            case Some(_) =>
                Ok(CommentTree.commentTreeToJson(CommentTree.getPostCommentsTop(post_id), user))
        }
    }

}
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
                    val repost_id = form.get("repost_id").map(_.head.toInt)
                    var post_id: Option[Long] = None
                    val data = form.get("data").map(_.head)
                    if (repost_id.isDefined) {
                        val boardId = form.get("boardId").map(_.head.toInt)
                        val board_name = form.get("board_name").map(_.head)
                        try {
                            if (boardId.isDefined)
                                post_id = Post.createSimplePost(user.get.userId.get, None, data.getOrElse(""), boardId.get, repost_id)
                            else if (board_name.isDefined) {
                                val board = Board.find(board_name.get)
                                if (board.isDefined)
                                    post_id = Post.createSimplePost(user.get.userId.get, None, data.getOrElse(""), board.get.boardId.get, repost_id)
                            }
                        } catch {
                            case e: NumberFormatException => // Do nothing so post stays None and if statement is not triggered.
                        }
                    } else {
                        val media_ids = form.get("media").map(_.head)
                        try {
                            val boardId = form.get("boardId").map(_.head.toInt)
                            val board_name = form.get("board_name").map(_.head)
                            if (!media_ids.isDefined) {
                                if (data.isDefined && boardId.isDefined) {
                                    post_id = Post.createSimplePost(user.get.userId.get, form.get("title").map(_.head), data.get,
                                        boardId.get)
                                } else if (board_name.isDefined && data.isDefined) {
                                    val board = Board.find(board_name.get)
                                    if (board.isDefined)
                                        post_id = Post.createSimplePost(user.get.userId.get, form.get("title").map(_.head), data.get,
                                            board.get.boardId.get)
                                }
                            } else {
                                if (data.isDefined && boardId.isDefined) {
                                    post_id = Post.createMediaPost(user.get.userId.get, form.get("title").map(_.head), data.get,
                                        boardId.get, media_ids.get.split(",").map(_.toInt))
                                } else if (board_name.isDefined && data.isDefined) {
                                    val board = Board.find(board_name.get)
                                    post_id = Post.createMediaPost(user.get.userId.get, form.get("title").map(_.head), data.get,
                                        board.get.boardId.get, media_ids.get.split(",").map(_.toInt))
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
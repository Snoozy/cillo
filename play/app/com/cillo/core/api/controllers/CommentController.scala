package com.cillo.core.api.controllers

import com.cillo.core.data.db.models.{CommentTree, Comment}
import com.cillo.utils.play.Auth._
import play.api.libs.json.Json
import play.api.mvc._

/**
 * Handles all Comment API requests including:
 *      Describing
 *      Creating
 */

object CommentController extends Controller {

    /**
     * Creates a new comment from the supplied parameters: parent_id, post_id, data.
     *
     * @return If successful returns the newly created comment.
     */
    def create = ApiAuthAction { implicit user => implicit request =>
        val body: AnyContent = request.body
        body.asFormUrlEncoded.map { form =>
            try {
                val parentId = form.get("parent_id").map(_.head.toInt)
                val postId = form.get("post_id").map(_.head)
                val data = form.get("data").map(_.head)
                if (postId.isDefined && data.isDefined) {
                    val comment_id = Comment.create(postId.get.toInt, user.get.userId.get, data.get, parentId)
                    if (comment_id.isDefined)
                        Ok(Comment.toJson(Comment.find(comment_id.get.toInt).get, user = user))
                    else
                        BadRequest(Json.obj("error" -> "Invalid request format."))
                } else
                    BadRequest(Json.obj("error" -> "Invalid request format."))
            } catch {
                case e: NumberFormatException => BadRequest(Json.obj("error" -> "Invalid request format."))
            }
        }.getOrElse(BadRequest(Json.obj("error" -> "Invalid request format.")))
    }

    def describe(id: Int) = ApiAuthAction { implicit user => implicit request =>
        val comment = Comment.find(id, status = None)
        if (comment.isDefined) {
            val tree = CommentTree.getCommentTree(comment.get)
            Ok(Json.obj("comment_tree" -> CommentTree.commentTreeToJson(tree)))
        } else {
            BadRequest(Json.obj("error" -> "Entity does not exist."))
        }
    }

}
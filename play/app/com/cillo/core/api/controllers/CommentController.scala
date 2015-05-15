package com.cillo.core.api.controllers

import com.cillo.core.data.db.models.Comment
import com.cillo.utils.play.Auth.AuthAction
import play.api.libs.json.Json
import play.api.mvc._

/**
 * Handles all Comment API requests including:
 *      Describing
 *      Creating
 */

object CommentController extends Controller {

    /*
    /**
     * Describes the a comment.
     *
     * @param post_id Id of post to be described.
     * @return Json
     */
    def describe(post_id: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required."))
            case Some(_) =>
                val comments = CommentTree.getPostCommentsTop(post_id)
                Ok(CommentTree.commentTreeToJson(comments, user))
        }
    }
    */

    /**
     * Creates a new comment from the supplied parameters: parent_id, post_id, data.
     *
     * @return If successful returns the newly created comment.
     */
    def create = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required."))
            case Some(_) =>
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
    }

}
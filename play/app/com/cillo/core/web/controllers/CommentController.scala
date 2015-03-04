package com.cillo.core.web.controllers

import com.cillo.core.data.db.models._
import com.cillo.utils.play.Auth.AuthAction
import play.api.mvc._
import play.api.libs.json.Json

object CommentController extends Controller {

    def createComment(post_id: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User not authenticated."))
            case Some(_) =>
                val post = Post.find(post_id)
                val data = request.body.asFormUrlEncoded.flatMap(_.get("data").flatMap(_.headOption))
                try {
                    val parent_id = request.body.asFormUrlEncoded.flatMap(_.get("parent").map(_.head.toInt))
                    if (post.isDefined && data.isDefined) {
                        val comment_id = Comment.create(post_id, user.get.user_id.get, data.get, parent_id)
                        if (comment_id.isDefined) {
                            Ok(Json.obj("status" -> "success", "item" -> Comment.toJson(Comment.find(comment_id.get.toInt).get)))
                        } else {
                            BadRequest(Json.obj("error" -> "Request invalid."))
                        }
                    } else {
                        BadRequest(Json.obj("error" -> "Request format invalid."))
                    }
                } catch {
                    case e: NumberFormatException => BadRequest(Json.obj("error" -> "Request invalid."))
                }
        }
    }

}
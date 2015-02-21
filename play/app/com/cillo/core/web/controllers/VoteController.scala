package com.cillo.core.web.controllers

import com.cillo.core.data.db.models._
import com.cillo.utils.play.Auth.AuthAction
import play.api.libs.json.Json
import play.api.mvc._

object VoteController extends Controller {

    def upvotePost(post_id: Int) = votePost(post_id, 1)

    def downvotePost(post_id: Int) = votePost(post_id, -1)

    def upvoteComment(comment_id: Int) = voteComment(comment_id, 1)

    def downvoteComment(comment_id: Int) = voteComment(comment_id, -1)

    private def votePost(post_id: Int, value: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User must be authenticated"))
            case Some(_) =>
                val postExists = Post.find(post_id)
                if (postExists.isDefined && PostVote.votePost(postExists.get.post_id.get, user.get.user_id.get, value)) {
                    Ok(Json.obj("success" -> "Yea!"))
                } else {
                    BadRequest(Json.obj("error" -> "Vote error."))
                }
        }
    }

    private def voteComment(comment_id: Int, value: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User must be authenticated"))
            case Some(_) =>
                val commExists = Comment.find(comment_id)
                if (commExists.isDefined && CommentVote.voteComment(commExists.get.comment_id.get, user.get.user_id.get, value)) {
                    Ok(Json.obj("success" -> "Yea!"))
                } else {
                    BadRequest(Json.obj("error" -> "Vote error."))
                }
        }
    }

}
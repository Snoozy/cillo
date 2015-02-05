package com.cillo.core.api.controllers

import com.cillo.core.data.db.models._
import com.cillo.utils.play.Auth.AuthAction
import play.api.libs.json.Json
import play.api.mvc._

/**
 * Handles all voting both on posts and comments.
 */

object VoteController extends Controller {

    /**
     * Upvotes a specific comment by id.
     *
     * @param comment_id Id of the comment to be upvoted.
     * @return The success/error of the upvote.
     */
    def upvoteComment(comment_id: Int) = voteComment(comment_id, 1)

    /**
     * Downvotes a specific comment by id.
     *
     * @param comment_id Id of the comment to be downvoted.
     * @return The success/error of the downvote.
     */
    def downvoteComment(comment_id: Int) = voteComment(comment_id, -1)

    /**
     * Upvotes a specific post by id.
     *
     * @param post_id Id of the post to be upvoted.
     * @return The success or error of the upvote.
     */
    def upvotePost(post_id: Int) = votePost(post_id, 1)

    /**
     * Downvotes a specific post.
     *
     * @param post_id Id of the post to be downvoted.
     * @return The success or error of the downvote.
     */
    def downvotePost(post_id: Int) = votePost(post_id, -1)

    /**
     * Votes on a comment.
     *
     * @param comment_id Id of comment to be voted on.
     * @param value Value of the vote.
     * @return The success or error of the vote.
     */
    private def voteComment(comment_id: Int, value: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required."))
            case Some(_) =>
                val commentExists = Comment.find(comment_id)
                if (commentExists.isDefined) {
                    if (CommentVote.voteComment(commentExists.get.comment_id.get, user.get.user_id.get, value))
                        Ok(Json.obj("success" -> "Comment vote successful."))
                    else
                        BadRequest(Json.obj("error" -> "Request format invalid. Code: 200"))
                } else
                    BadRequest(Json.obj("error" -> "Comment does not exist."))
        }
    }

    /**
     * Votes on a post.
     *
     * @param post_id Id of the post to be voted on.
     * @param value Value of the vote.
     * @return The success or error of the vote.
     */
    private def votePost(post_id: Int, value: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required."))
            case Some(_) =>
                val postExists = Post.find(post_id)
                if (postExists.isDefined) {
                    if (PostVote.votePost(postExists.get.post_id.get, user.get.user_id.get, value))
                        Ok(Json.obj("success" -> "Post vote successful."))
                    else
                        BadRequest(Json.obj("error" -> "Request format invalid."))
                } else
                    BadRequest(Json.obj("error" -> "Vote does not exist."))
        }
    }

}
package com.cillo.core.api.controllers

import com.cillo.core.data.db.models.{CommentTree, Group, Post}
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
                        val group_id = form.get("group_id").map(_.head.toInt)
                        val group_name = form.get("group_name").map(_.head)
                        try {
                            if (group_id.isDefined)
                                post_id = Post.createSimplePost(user.get.user_id.get, None, repost_id.get, group_id.get, true)
                            else if (group_name.isDefined) {
                                val group = Group.find(group_name.get)
                                if (group.isDefined)
                                    post_id = Post.createSimplePost(user.get.user_id.get, None, repost_id.get, group.get.group_id.get, true)
                            }
                        } catch {
                            case e: NumberFormatException => // Do nothing so post stays None and if statement is not triggered.
                        }
                    } else {
                        val data = form.get("data").map(_.head)
                        val media_ids = form.get("media").map(_.head)
                        try {
                            val group_id = form.get("group_id").map(_.head.toInt)
                            val group_name = form.get("group_name").map(_.head)
                            if (!media_ids.isDefined) {
                                if (data.isDefined && group_id.isDefined) {
                                    post_id = Post.createSimplePost(user.get.user_id.get, form.get("title").map(_.head), data.get,
                                        group_id.get, false)
                                } else if (group_name.isDefined && data.isDefined) {
                                    val group = Group.find(group_name.get)
                                    if (group.isDefined)
                                        post_id = Post.createSimplePost(user.get.user_id.get, form.get("title").map(_.head), data.get,
                                            group.get.group_id.get, false)
                                }
                            } else {
                                if (data.isDefined && group_id.isDefined) {
                                    post_id = Post.createMediaPost(user.get.user_id.get, form.get("title").map(_.head), data.get,
                                        group_id.get, media_ids.get.split(",").map(_.toInt))
                                } else if (group_name.isDefined && data.isDefined) {
                                    val group = Group.find(group_name.get)
                                    post_id = Post.createMediaPost(user.get.user_id.get, form.get("title").map(_.head), data.get,
                                        group.get.group_id.get, media_ids.get.split(",").map(_.toInt))
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
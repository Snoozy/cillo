package com.cillo.core.api.controllers

import com.cillo.core.data.db.models.{Group, Post, User}
import com.cillo.utils.play.Auth.AuthAction
import play.api.libs.json.Json
import play.api.mvc._

/**
 * Handles everything with groups including:
 *      Describing
 *      Creating
 *      Getting Posts
 */

object GroupController extends Controller {

    /**
     * Describes a specific group by id.
     *
     * @param group_id Id of group to be described.
     * @return
     */
    def describe(group_id: Int)  = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required."))
            case Some(_) =>
                val group = Group.find(group_id)
                if (group.isDefined)
                    Ok(Group.toJson(group.get, User.userIsFollowing(user.get.user_id.get, group_id)))
                else
                    BadRequest(Json.obj("error" -> "Group does not exist."))
        }
    }

    /**
     * Creates a new group from the supplied parameters:
     *      name
     *      creator_id
     *      description
     *
     * @return Fully hydrated Json of the new group.
     */
    def create = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required."))
            case Some(_) =>
                val body: AnyContent = request.body
                body.asFormUrlEncoded.map { form =>
                    val name = form.get("name").map(_.head)
                    val descr = form.get("description").map(_.head)
                    val photo = form.get("photo").map(_.head.toInt)
                    if (name.isDefined) {
                        var group_id: Option[Long] = None
                        group_id = Group.create(name.get, descr, user.get.user_id.get, photo = photo)
                        if (group_id.isDefined) {
                            Group.addFollower(user.get.user_id.get, group_id.get.toInt)
                            Ok(Group.toJson(Group.find(group_id.get.toInt).get, true))
                        } else {
                            BadRequest(Json.obj("error" -> "Group creation failed."))
                        }
                    } else {
                        BadRequest(Json.obj("error" -> "Request format invalid."))
                    }
                }.getOrElse(BadRequest(Json.obj("error" -> "Request Content-Type invalid.")))
        }
    }

    /**
     * Gets a groups trending posts.
     *
     * @param group_id Group to get trending posts for.
     * @return Fully hydrated posts
     */
    def getGroupTrendingPosts(group_id: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required."))
            case Some(_) =>
                val group = Group.find(group_id)
                if (!group.isDefined)
                    BadRequest(Json.obj("error" -> "Group does not exist."))
                else {
                    val posts = Group.getTrendingPosts(group_id)
                    Ok(Json.obj("posts" -> Post.toJsonWithUser(posts, user)))
                }
        }
    }

    def followGroup(group_id: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required."))
            case Some(_) =>
                val groupExists = Group.find(group_id)
                if (!groupExists.isDefined)
                    BadRequest(Json.obj("error" -> "Group does not exist."))
                else {
                    val success = Group.addFollower(user.get.user_id.get, group_id)
                    if (success)
                        Ok(Json.obj("success" -> "Group successfully followed"))
                    else
                        BadRequest(Json.obj("error" -> "Unknown error."))
                }
        }
    }

    def unfollowGroup(group_id: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required."))
            case Some(_) =>
                val groupExists = Group.find(group_id)
                if (!groupExists.isDefined)
                    BadRequest(Json.obj("error" -> "Group does not exist."))
                else {
                    val success = Group.removeFollower(user.get.user_id.get, group_id)
                    if (success)
                        Ok(Json.obj("success" -> "Group successfully followed."))
                    else
                        BadRequest(Json.obj("error" -> "Unknown error."))
                }
        }
    }

}
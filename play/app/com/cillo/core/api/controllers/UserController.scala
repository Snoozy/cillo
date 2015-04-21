package com.cillo.core.api.controllers

import com.cillo.core.data.db.models.{Comment, Board, Post, User}
import com.cillo.utils.play.Auth.AuthAction
import play.api.libs.json.{JsValue, Json}
import com.cillo.core.data.Constants
import play.api.mvc._

/**
 * Handles User API requests including:
 *      Describing
 *      Describing the currently logged in user
 *      Creating user
 *      Get a users boards
 *      Get a users comments
 *      Get a users posts by time
 *      Get a users feed
 */

object UserController extends Controller {

    /**
     * Describes the user given by the querystring that can either be a user_id or username.
     *
     * @return Json of the user.
     */
    def describe = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required."))
            case Some(_) =>
                val query = request.queryString.map {
                    case (k, v) => k -> v.mkString
                }
            if (query.contains("user_id")) {
                try {
                    Ok(User.toJsonByUserID(query.get("user_id").get.toInt, self = user))
                } catch {
                    case e: java.lang.NumberFormatException => BadRequest(Json.obj("error" -> "Invalid request format."))
                }
            } else if (query.contains("username")) {
                Ok(User.toJsonByUsername(query.get("username").get, self = user))
            } else {
                BadRequest(Json.obj("error" -> "Invalid request format."))
            }
        }
    }

    /**
     * Describes the currently logged in user.
     *
     * @return Json of the user.
     */
    def describeSelf = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required."))
            case Some(_) =>
                Ok(User.toJson(user.get, self = user))
        }
    }

    /**
     * Creates a new user with the supplied fields:
     *      name
     *      username
     *      password
     *      email
     *      bio
     *
     * @return Json of the newly created user.
     */
    def create = AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => BadRequest(Json.obj("error" -> "User must be logged out."))
            case None =>
                val body: AnyContent = request.body
                body.asFormUrlEncoded.map { form =>
                    val username = form.get("username").map(_.head)
                    val name = form.get("name").map(_.head)
                    val password = form.get("password").map(_.head)
                    val email = form.get("email").map(_.head)
                    val bio = form.get("bio").map(_.head)
                    if (username.isDefined && username.get.length < Constants.MaxUsernameLength && name.isDefined && name.get.length < Constants.MaxNameLength
                        && password.isDefined && email.isDefined) {
                        val usernameExists = User.find(username.get)
                        if (usernameExists.isDefined) {
                            BadRequest(Json.obj("error" -> "Username exists."))
                        } else {
                            val newUser = User.create(username.get, name.get, password.get, email.get, bio)
                            if (newUser.isDefined) {
                                Ok(User.toJsonByUserID(newUser.get.toInt))
                            }
                            else
                                BadRequest(Json.obj("error" -> "User creation failed."))
                        }
                    } else {
                        BadRequest(Json.obj("error" -> "Request format invalid."))
                    }
                }.getOrElse(BadRequest(Json.obj("error" -> "Request format invalid.")))
        }
    }

    /**
     * Get the boards of a user.
     *
     * @param user_id user_id of entity to get boards of
     * @return Json of fully hydrated boards.
     */
    def getBoards(user_id: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required."))
            case Some(_) =>
                val userExists = User.find(user_id)
                if (!userExists.isDefined)
                    BadRequest(Json.obj("error" -> "User does not exist."))
                else {
                    val describingUser = userExists.get
                    val json: JsValue = Json.obj(
                        "boards" -> Board.toJsonSeq(User.getBoards(describingUser.user_id.get), Some(true))
                    )
                    Ok(json)
                }
        }
    }

    /**
     * Gets the comments of a specific user by time.
     *
     * @param user_id User_id of entity to get comments for.
     * @return Json for a list of fully hydrated comments.
     */
    def getComments(user_id: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required."))
            case Some(_) =>
                val userExists = User.find(user_id)
                if (!userExists.isDefined)
                    BadRequest(Json.obj("error" -> "User does not exist."))
                else {
                    val after = request.getQueryString("after")
                    val describingUser = userExists.get
                    val comments = {
                        if (after.isDefined) {
                            User.getCommentsPaged(describingUser.user_id.get, after.get.toInt)
                        } else {
                            User.getComments(describingUser.user_id.get)
                        }
                    }
                    Ok(Json.obj("comments" -> Comment.toJsonSeqWithUser(comments, user)))
                }
        }
    }

    /**
     * Gets the posts of a specific user by time.
     *
     * @param user_id User_id of the entity to get posts for.
     * @return Fully hydrated posts for this user.
     */
    def getPosts(user_id: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required."))
            case Some(_) =>
                val userExists = User.find(user_id)
                if (!userExists.isDefined)
                    BadRequest(Json.obj("error" -> "User does not exist."))
                else {
                    val after = request.getQueryString("after")
                    val describingUser = userExists.get
                    val posts = {
                        if (after.isDefined && after.get != "") {
                            User.getPostsPaged(describingUser.user_id.get, after.get.toInt)
                        } else {
                            User.getPosts(describingUser.user_id.get)
                        }
                    }
                    Ok(Json.obj("posts" -> Post.toJsonWithUser(posts, user)))
                }
        }
    }

    /**
     * Gets the feed for the currently logged in user.
     *
     * @return Fully hydrated posts for this users feed.
     */
    def getFeed = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User must be authenticated."))
            case Some(_) =>
                val afterPost = request.getQueryString("after")
                val posts = {
                    if (afterPost.isDefined)
                        User.getFeedPaged(user.get.user_id.get, afterPost.get.toInt)
                    else
                        User.getFeed(user.get.user_id.get)
                }
                Ok(Json.obj("posts" -> Post.toJsonWithUser(posts, user)))
        }
    }

}
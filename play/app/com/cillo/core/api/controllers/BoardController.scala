package com.cillo.core.api.controllers

import com.cillo.core.data.db.models.{Board, Post, User}
import com.cillo.utils.play.Auth.AuthAction
import play.api.libs.json.Json
import play.api.mvc._

/**
 * Handles everything with boards including:
 *      Describing
 *      Creating
 *      Getting Posts
 */

object BoardController extends Controller {

    /**
     * Describes a specific board by id.
     *
     * @param board_id Id of board to be described.
     * @return
     */
    def describe(board_id: Int)  = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required."))
            case Some(_) =>
                val board = Board.find(board_id)
                if (board.isDefined)
                    Ok(Board.toJson(board.get, User.userIsFollowing(user.get.userId.get, board_id)))
                else
                    BadRequest(Json.obj("error" -> "Board does not exist."))
        }
    }

    /**
     * Creates a new board from the supplied parameters:
     *      name
     *      creator_id
     *      description
     *
     * @return Fully hydrated Json of the new board.
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
                        var board_id: Option[Long] = None
                        board_id = Board.create(name.get, descr, user.get.userId.get, photo = photo)
                        if (board_id.isDefined) {
                            Board.addFollower(user.get.userId.get, board_id.get.toInt)
                            Ok(Board.toJson(Board.find(board_id.get.toInt).get, following = true))
                        } else {
                            BadRequest(Json.obj("error" -> "Board creation failed."))
                        }
                    } else {
                        BadRequest(Json.obj("error" -> "Request format invalid."))
                    }
                }.getOrElse(BadRequest(Json.obj("error" -> "Request Content-Type invalid.")))
        }
    }

    /**
     * Gets a boards trending posts.
     *
     * @param board_id Board to get trending posts for.
     * @return Fully hydrated posts
     */
    def getBoardTrendingPosts(board_id: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required."))
            case Some(_) =>
                val board = Board.find(board_id)
                if (!board.isDefined)
                    BadRequest(Json.obj("error" -> "Board does not exist."))
                else {
                    val after = request.getQueryString("after")
                    val posts = {
                        if (after.isDefined) {
                            Board.getFeedPaged(board_id, after.get.toInt)
                        } else {
                            Board.getFeed(board_id)
                        }
                    }
                    Ok(Json.obj("posts" -> Post.toJsonWithUser(posts, user)))
                }
        }
    }

    def followBoard(board_id: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required."))
            case Some(_) =>
                val boardExists = Board.find(board_id)
                if (!boardExists.isDefined)
                    BadRequest(Json.obj("error" -> "Board does not exist."))
                else {
                    val success = Board.addFollower(user.get.userId.get, board_id)
                    if (success)
                        Ok(Json.obj("success" -> "Board successfully followed"))
                    else
                        BadRequest(Json.obj("error" -> "Unknown error."))
                }
        }
    }

    def unfollowBoard(board_id: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required."))
            case Some(_) =>
                val boardExists = Board.find(board_id)
                if (!boardExists.isDefined)
                    BadRequest(Json.obj("error" -> "Board does not exist."))
                else {
                    val success = Board.removeFollower(user.get.userId.get, board_id)
                    if (success)
                        Ok(Json.obj("success" -> "Board successfully followed."))
                    else
                        BadRequest(Json.obj("error" -> "Unknown error."))
                }
        }
    }

}
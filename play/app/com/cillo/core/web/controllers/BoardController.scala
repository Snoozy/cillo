package com.cillo.core.web.controllers

import com.cillo.core.data.db.models._
import com.cillo.core.web.views.html.core
import com.cillo.utils.play.Auth.AuthAction
import play.api.mvc._
import play.api.libs.json.Json

object BoardController extends Controller {

    def boardPage(name: String) = AuthAction { implicit user => implicit request =>
        val board = Board.find(name)
        if (board.isDefined) {
            val posts = Board.getFeed(board.get.board_id.get)
            Ok(core.board(board.get, user, posts))
        } else {
            NotFound("Board not found.")
        }
    }

    def followBoard(board_id: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required."))
            case Some(_) =>
                val groupExists = Board.find(board_id)
                if (groupExists.isDefined) {
                    val followExists = User.userIsFollowing(user.get.user_id.get, board_id)
                    if (followExists)
                        Ok(Json.obj("success" -> "User is following group."))
                    else{
                        if (Board.addFollower(user.get.user_id.get, board_id)) {
                            Ok(Json.obj("success" -> "Add follower successful."))
                        } else {
                            BadRequest(Json.obj("error" -> "Unknown error following group."))
                        }
                    }
                } else {
                    BadRequest(Json.obj("error" -> "Group does not exist."))
                }
        }
    }

    def unfollowBoard(board_id: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required."))
            case Some(_) =>
                val groupExists = Board.find(board_id)
                if (groupExists.isDefined) {
                    val followExists = User.userIsFollowing(user.get.user_id.get, board_id)
                    if (!followExists)
                        Ok(Json.obj("success" -> "User is not following group."))
                    else{
                        if (Board.removeFollower(user.get.user_id.get, board_id)) {
                            Ok(Json.obj("success" -> "Remove follower successful."))
                        } else {
                            BadRequest(Json.obj("error" -> "Unknown error unfollowing group."))
                        }
                    }
                } else {
                    BadRequest(Json.obj("error" -> "Group does not exist."))
                }
        }
    }

    def boardSettingsPage(name: String) = AuthAction { implicit user => implicit request =>
        Ok("TODO")
    }

    def createBoardPage = AuthAction { implicit user => implicit request =>
        user match {
            case None => Redirect("/")
            case Some(_) => Ok(core.create_board(user.get))
        }
    }

    def attemptCreateBoard = AuthAction { implicit user => implicit request =>
        user match {
            case None => Redirect("/")
            case Some(_) =>
                val body: AnyContent = request.body
                body.asFormUrlEncoded.map { form =>
                    val board_name = form.get("name").map(_.head)
                    val board_descr = form.get("description").map(_.head)
                    if (board_name.isDefined) {
                        val boardExists = Board.find(board_name.get)
                        if (!boardExists.isDefined) {
                            val board_id = Board.create(board_name.get, board_descr, user.get.user_id.get)
                            if (board_id.isDefined) {
                                val newBoard = Board.find(board_id.get.toInt)
                                Board.addFollower(user.get.user_id.get, newBoard.get.board_id.get)
                                Redirect("/" + newBoard.get.name)
                            } else {
                                Ok(core.create_board(user.get))
                            }
                        } else {
                            Ok(core.create_board(user.get, "Board name already exists.", board_name.get, board_descr.getOrElse("")))
                        }
                    } else {
                        Ok(core.create_board(user.get, "Board needs a name.", "", board_descr.getOrElse("")))
                    }
                }.getOrElse(Ok(core.create_board(user.get, "Unknown error. Please try again later.")))
        }
    }

}
package com.cillo.core.web.controllers

import com.cillo.core.data.aws.S3
import com.cillo.core.data.db.models._
import com.cillo.utils.play.Auth.AuthAction
import play.api.mvc._
import com.sksamuel.scrimage._


object SettingsController extends Controller {

    def userSettingsPage = AuthAction { implicit user => implicit request =>
        user match {
            case None => Found("/login")
            case Some(_) => Ok(com.cillo.core.web.views.html.core.settings(user.get))
        }
    }

    def boardSettingsPage(name: String) = AuthAction { implicit user => implicit request =>
        user match {
            case None => Found("/login")
            case Some(_) =>
                val board = Board.find(name)
                if (board.isDefined) {
                    if (board.get.creator_id == user.get.user_id.get || user.get.admin) {
                        Ok(com.cillo.core.web.views.html.core.board_settings(user.get, board.get))
                    } else {
                        NotFound("Permission denied.")
                    }
                } else {
                    NotFound("Board not found.")
                }
        }
    }

    def boardSettingsChange(name: String) = AuthAction { implicit user => implicit request =>
        user match {
            case None => Found("/login")
            case Some(_) =>
                val board = Board.find(name)
                if (board.isDefined && (user.get.user_id.get == board.get.creator_id || user.get.admin)) {
                    val body = request.body.asMultipartFormData
                    if (body.isDefined) {
                        val res = body.get.asFormUrlEncoded
                        val picID: Int = {
                            val file = body.get.file("picture")
                            if (file.isDefined) {
                                val pic = file.get.ref.file
                                if (pic.length() < 3145728) {
                                    val x = res.get("picture-x").map(_.head.toDouble)
                                    val y = res.get("picture-y").map(_.head.toDouble)
                                    val width = res.get("picture-width").map(_.head.toDouble)
                                    val height = res.get("picture-height").map(_.head.toDouble)
                                    val id = S3.upload(pic, profile = true, x = x, y = y, width = width, height = height)
                                    if (id.isDefined) {
                                        val mediaID = Media.create(0, id.get)
                                        if (mediaID.isDefined) {
                                            mediaID.get.toInt
                                        } else
                                            board.get.photo_id
                                    } else
                                        board.get.photo_id
                                } else
                                    board.get.photo_id
                            } else
                                board.get.photo_id
                        }
                        val desc = res.get("desc").map(_.head).getOrElse(user.get.name)
                        Board.update(board.get.board_id.get, desc, picID)
                    }
                    Found("/" + board.get.name)
                } else {
                    NotFound("Board not found.")
                }
        }
    }

    def userSettingsChange = AuthAction { implicit user => implicit request =>
        user match {
            case None => Found("/login")
            case Some(_) =>
                val body = request.body.asMultipartFormData
                if (body.isDefined) {
                    val res = body.get.asFormUrlEncoded
                    val picID: Int = {
                        val file = body.get.file("picture")
                        if (file.isDefined) {
                            val pic = file.get.ref.file
                            if (pic.length() < 3145728) {
                                val x = res.get("picture-x").map(_.head.toDouble)
                                val y = res.get("picture-y").map(_.head.toDouble)
                                val width = res.get("picture-width").map(_.head.toDouble)
                                val height = res.get("picture-height").map(_.head.toDouble)
                                val id = S3.upload(pic, profile = true, x = x, y = y, width = width, height = height)
                                if (id.isDefined) {
                                    val mediaID = Media.create(0, id.get)
                                    if (mediaID.isDefined) {
                                        mediaID.get.toInt
                                    } else
                                        user.get.photo_id
                                } else
                                    user.get.photo_id
                            } else
                                user.get.photo_id
                        } else
                            user.get.photo_id
                    }
                    val name = res.get("name").map(_.head).getOrElse(user.get.name)
                    val username = res.get("username").map(_.head).getOrElse(user.get.name)
                    val bio = res.get("bio").map(_.head).getOrElse(user.get.name)
                    User.update(user.get.user_id.get, name, username, bio, picID)
                }
                Found("/user/" + user.get.username)
        }
    }

}
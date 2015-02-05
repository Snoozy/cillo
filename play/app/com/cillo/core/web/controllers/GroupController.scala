package com.cillo.core.web.controllers

import com.cillo.utils.play.Auth.AuthAction
import play.api.mvc._
import com.cillo.core.data.db.models._
import com.cillo.core.web.views.html.core

object GroupController extends Controller {

    def groupPage(name: String) = AuthAction { implicit user => implicit request =>
        val group = Group.find(name)
        if (group.isDefined) {
            val posts = Group.getTrendingPosts(group.get.group_id.get)
            Ok("TODO")
        } else {
            NotFound("Page not found.")
        }
    }

    def groupSettingsPage(name: String) = AuthAction { implicit user => implicit request =>
        Ok("TODO")
    }

    def createGroupPage = AuthAction { implicit user => implicit request =>
        user match {
            case None => Redirect("/")
            case Some(_) => Ok(core.create_group(user.get))
        }
    }

    def attemptCreateGroup = AuthAction { implicit user => implicit request =>
        user match {
            case None => Redirect("/")
            case Some(_) =>
                val body: AnyContent = request.body
                body.asFormUrlEncoded.map { form =>
                    val group_name = form.get("name").map(_.head)
                    val group_descr = form.get("description").map(_.head)
                    if (group_name.isDefined) {
                        val groupExists = Group.find(group_name.get)
                        if (!groupExists.isDefined) {
                            val group_id = Group.create(group_name.get, group_descr, user.get.user_id.get)
                            if (group_id.isDefined) {
                                val newGroup = Group.find(group_id.get.toInt)
                                Group.addFollower(user.get.user_id.get, newGroup.get.group_id.get)
                                Redirect("/" + newGroup.get.name)
                            } else {
                                Ok(core.create_group(user.get))
                            }
                        } else {
                            Ok(core.create_group(user.get, "Board name already exists.", group_name.get, group_descr.getOrElse("")))
                        }
                    } else {
                        Ok(core.create_group(user.get, "Board needs a name.", "", group_descr.getOrElse("")))
                    }
                }.getOrElse(Ok(core.create_group(user.get, "Unknown error. Please try again later.")))
        }
    }

}
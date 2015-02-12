package com.cillo.core.web.controllers

import com.cillo.core.data.db.models._
import com.cillo.core.web.views.html.components
import com.cillo.utils.play.Auth.AuthAction
import play.api.libs.json.Json
import play.api.mvc._

object PostController extends Controller {

    def viewPostPage(group_name: String, post_id: Int) = AuthAction { implicit user => implicit request =>
        Ok("TODO")
    }

    def post = AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => processPost(request, user.get)
            case None => BadRequest("User not authenticated.")
        }
    }

    def processPost(request: Request[AnyContent], user: User): Result = {
        val body: AnyContent = request.body
        body.asJson.map { json =>
            var title = (json \ "title").asOpt[String]
            val data = (json \ "data").asOpt[String]
            val group_id = (json \ "group_id").asOpt[Int]
            val repost = (json \ "repost").asOpt[Boolean]
            if (data.isDefined && group_id.isDefined && repost.isDefined) {
                if (title.isDefined && title.get == "") {
                    title = None
                }
                val newPost = Post.createSimplePost(user.user_id.get, title, data.get, group_id.get, repost.get)
                if (newPost.isDefined)
                    Ok("Success")
                else
                    BadRequest("Invalid request.")
            } else
                BadRequest("Invalid request.")
        }.getOrElse {
            body.asFormUrlEncoded.map { form =>
                try {
                    var title = form.get("title").map(_.head)
                    val data = form.get("data").map(_.head)
                    val group_name = form.get("group_name").map(_.head)
                    val repost = form.get("repost").exists(_.head.toBoolean)
                    if (data.isDefined && group_name.isDefined) {
                        val group = Group.find(group_name.get)
                        if (group.isDefined) {
                            if (title.isDefined && title.get == "") {
                                title = None
                            }
                            val newPost = Post.createSimplePost(user.user_id.get, title, data.get, group.get.group_id.get, repost)
                            if (newPost.isDefined)
                                Ok(Json.obj("item_html" -> Json.toJson(components.post(Post.find(newPost.get.toInt).get, user).toString())))
                            else
                                BadRequest("Invalid request.")
                        } else {
                            BadRequest("Invalid group name.")
                        }
                    } else
                        BadRequest("Invalid request.")
                } catch {
                    case e: java.lang.NumberFormatException => return BadRequest("Invalid parameters.")
                }
            }.getOrElse {
                BadRequest("Only json or form url encoded content types accepted.")
            }
        }
    }

}
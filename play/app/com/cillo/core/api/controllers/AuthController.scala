package com.cillo.core.api.controllers

import com.cillo.utils.play.Auth._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._

/**
 * Handles API authentication which includes login and logout.
 */

object AuthController extends Controller {

    def login = AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => val json: JsValue = Json.obj("error" -> "User is already logged in.")
                BadRequest(json)
            case None =>
                val body: AnyContent = request.body
                body.asFormUrlEncoded.map { form =>
                    val username = form.get("username").map(_.head)
                    val password = form.get("password").map(_.head)
                    if (username.isDefined && password.isDefined)
                        attemptLogin(username.get, password.get)
                    else
                        BadRequest(Json.obj("error" -> "Request format invalid."))
                }.getOrElse(BadRequest(Json.obj("error" -> "Request format invalid.")))
        }
    }

    def logout = AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => val success = logOutSession(user.get.token.get)
                if (success)
                    Ok(Json.obj("success" -> "Successfully logged out."))
                else
                    BadRequest(Json.obj("error" -> "Unknown error."))
            case None => Ok(Json.obj("error" -> "User not logged in."))
        }
    }

    private def attemptLogin(username: String, password: String): Result = {
        val token = logInSession(username, password)
        if (!token.isDefined)
            BadRequest(Json.obj("error" -> "Invalid Credentials."))
        else {
            Ok(Json.obj("auth_token" -> token.get))
        }
    }

}
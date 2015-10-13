package com.cillo.core.api.controllers

import com.cillo.utils.play.Auth._
import com.cillo.utils.play.errors._
import play.api.libs.json.{JsValue, Json}
import com.cillo.core.data.db.models.{UserInfo, User, AppleDeviceToken}
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
                    val email = form.get("email").map(_.head)
                    val password = form.get("password").map(_.head)
                    if (email.isDefined && password.isDefined) {
                        val iosId = form.get("ios_id").map(_.head)
                        attemptLogin(email.get, password.get)
                    }
                    else
                        BadRequest(Json.obj("error" -> "Request format invalid."))
                }.getOrElse(BadRequest(Json.obj("error" -> "Request format invalid.")))
        }
    }

    def logout = AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) =>
                logOutSession(user.get.token.get)
                Ok(Json.obj("success" -> "Successfully logged out."))
            case None => Ok(Json.obj("error" -> "User not logged in."))
        }
    }

    def ping = AuthAction { implicit user => implicit request =>
        user match {
            case None =>
                Ok(Json.obj("error" -> "User not logged in."))
            case Some(_) =>
                request.body.asFormUrlEncoded.map { form =>
                    val deviceToken = form.get("device_token").map(_.head)
                    if (deviceToken.isDefined && AppleDeviceToken.getDeviceTokens(user.get.userId.get).contains(deviceToken.get)) {
                        AppleDeviceToken.createToken(user.get.userId.get, deviceToken.get)
                    }
                    Ok(Json.obj("success" -> "Ping acknowledged"))
                }.getOrElse(BadRequest(Json.obj("error" -> "Ping format incorrect.")))
        }
    }

    private def attemptLogin(email: String, password: String): Result = {
        val token = logInSession(email, password)
        if (token.isEmpty)
            BadRequest(PasswordInvalid.toJson)
        else {
            val user = User.findByEmail(email).get
            Ok(Json.obj("auth_token" -> token.get, "user" -> User.toJson(user)))
        }
    }

}
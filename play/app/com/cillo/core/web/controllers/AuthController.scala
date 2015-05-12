package com.cillo.core.web.controllers

import com.cillo.core.data.db.models._
import com.cillo.core.web.views.html
import com.cillo.utils.play.Auth._
import play.api.mvc._
import play.api.libs.json.Json
import com.cillo.utils.Etc
import play.api.Logger
import com.cillo.core.data.cache.Session

//noinspection MutatorLikeMethodIsParameterless
object AuthController extends Controller {

    def cleanLoginPage = AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => Redirect("/")
            case None => Ok(html.core.login())
        }
    }

    def attemptLogin = AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => Redirect("/")
            case None => processLogin(request)
        }
    }

    def logout = AuthAction { implicit user => implicit request =>
        if (user.isDefined)
            logOutSession(user.get.token.get)
        Redirect("/").discardingCookies(deleteSessionCookies)
    }

    def changePassword = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest("User is not authenticated.")
            case Some(_) =>
                val body = request.body
                body.asFormUrlEncoded.map { form =>
                    val currentPass = form.get("current").map(_.head)
                    val newPass = form.get("new").map(_.head)
                    if (currentPass.isDefined && newPass.isDefined) {
                        val check = Etc.checkPass(currentPass.get, user.get.password)
                        if (check && Etc.checkPasswordValidity(newPass.get)) {
                            User.updatePassword(user.get.user_id.get, newPass.get)
                            Ok(Json.obj("success" -> "Success"))
                        } else {
                            BadRequest(Json.obj("error" -> "Password is invalid."))
                        }
                    } else {
                        BadRequest(Json.obj("error" -> "Request needs to contain current and new password."))
                    }
                }.getOrElse(BadRequest(Json.obj("error" -> "Request format invalid.")))
        }
    }

    def setPassword = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest("User is not authenticated.")
            case Some(_) =>
                val body = request.body
                body.asFormUrlEncoded.map { form =>
                    val newPass = form.get("new").map(_.head)
                    if (newPass.isDefined) {
                        if (SocialUser.userIsSocial(user.get.user_id.get) && Etc.checkPasswordValidity(newPass.get) && user.get.password == "") {
                            User.updatePassword(user.get.user_id.get, newPass.get)
                            Ok("Success.")
                        } else {
                            BadRequest("Password invalid.")
                        }
                    } else {
                        BadRequest("Request needs to contain new password.")
                    }
                }.getOrElse(BadRequest("Request format invalid."))
        }
    }

    private def processLogin(request: Request[AnyContent])(implicit r: RequestHeader): Result = {
        val body: AnyContent = request.body
        body.asFormUrlEncoded.map { form =>
            val email = form.get("email").map(_.head)
            val password = form.get("password").map(_.head)
            if (email.isDefined && password.isDefined) {
                val token = logInSession(email.get, password.get)
                if (!token.isDefined)
                    Ok(html.core.login(error = true, email = email.get))
                else {
                    val admin = User.isUserAdmin(User.findByEmail(email.get).get.user_id.get)
                    if (admin) {
                        val sess = new Session(token.get)
                        sess.set("admin", "true")
                    }
                    Redirect("/").withCookies(Cookie("auth_token", token.get))
                }
            } else {
                Ok(html.core.login(error = true))
            }
        }.getOrElse(Ok(html.core.login(error = true)))
    }

}
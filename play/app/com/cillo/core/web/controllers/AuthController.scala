package com.cillo.core.web.controllers

import com.cillo.core.data.db.models._
import com.cillo.core.web.views.html
import com.cillo.utils.play.Auth._
import play.api.mvc._
import com.cillo.core.data.cache.Session

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

    private def processLogin(request: Request[AnyContent])(implicit r: RequestHeader): Result = {
        val body: AnyContent = request.body
        body.asFormUrlEncoded.map { form =>
            val email = form.get("email").map(_.head)
            val password = form.get("password").map(_.head)
            if (email.isDefined && password.isDefined) {
                val token = logInSession(email.get, password.get)
                if (!token.isDefined)
                    Ok(html.core.login(error = true))
                else {
                    val admin = Admin.isUserAdmin(User.findByEmail(email.get).get.user_id.get)
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
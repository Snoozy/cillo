package com.cillo.core.web.controllers

import com.cillo.utils.play.Auth.AuthAction
import play.api.mvc._
import com.cillo.core.web.views.html
import com.cillo.utils.play.Auth._

object LoginController extends Controller {

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

    private def processLogin(request: Request[AnyContent])(implicit r: RequestHeader): Result = {
        val body: AnyContent = request.body
        body.asFormUrlEncoded.map { form =>
            val username = form.get("username").map(_.head)
            val password = form.get("password").map(_.head)
            if (username.isDefined && password.isDefined) {
                val token = logInSession(username.get, password.get)
                if (!token.isDefined)
                    Ok(html.core.login())
                else
                    Redirect("/").withCookies(Cookie("auth_token", token.get))
            } else {
                Ok(html.core.login())
            }
        }.getOrElse(Ok(html.core.login()))
    }

}
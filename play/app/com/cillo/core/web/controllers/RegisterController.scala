package com.cillo.core.web.controllers

import com.cillo.core.data.db.models._
import com.cillo.utils.Etc
import com.cillo.utils.play.Auth
import com.cillo.utils.play.Auth.AuthAction
import play.api.mvc._
import com.cillo.core.social.FB

object RegisterController extends Controller {

    def cleanRegisterPage = AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => Redirect("/")
            case None => Ok(com.cillo.core.web.views.html.core.register())
        }
    }

    def attemptRegister = AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => Redirect("/")
            case None =>
                val body: AnyContent = request.body

                body.asFormUrlEncoded.map { form =>
                    if (form.get("wel_submit").map(_.head).isDefined) {
                        val name = form.get("wel_name").map(_.head).getOrElse("")
                        val password = form.get("wel_password").map(_.head).getOrElse("")
                        val email = form.get("wel_email").map(_.head).getOrElse("")
                        Ok(com.cillo.core.web.views.html.core.register(name = name, password = password, email = email))
                    }  else {
                        processRegister(request)
                    }
                }.getOrElse(Ok(com.cillo.core.web.views.html.core.register()))
        }
    }

    private def processRegister(request: Request[AnyContent]): Result = {
        val body: AnyContent = request.body
        body.asFormUrlEncoded.map { form =>
            val username = form.get("username").map(_.head)
            if (!username.isDefined)
                return BadRequest("Username invalid.")
            val password = form.get("password").map(_.head)
            val userExists = User.find(username.get)
            if (userExists.isDefined && password.isDefined) {
                if (Etc.checkPass(password.get, userExists.get.password))
                    return Found("/").withCookies(Auth.newSessionCookies(userExists.get.user_id.get))
                return BadRequest("Username taken..")
            }
            val email = form.get("email").map(_.head)
            val name = form.get("name").map(_.head)
            if (username.isDefined && name.isDefined && password.isDefined && email.isDefined) {
                val newUser = User.create(username.get, name.get, password.get, email.get, None)
                if (newUser.isDefined)
                    Found("/gettingstarted").withCookies(Auth.newSessionCookies(User.find(newUser.get.toInt).getOrElse(return BadRequest("Error.")).user_id.get))
                else
                    BadRequest("Error: user creation failed.")
            } else
                BadRequest("Error: request format invalid.")
        }.getOrElse(return BadRequest("Error."))
    }

}
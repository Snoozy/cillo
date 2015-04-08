package com.cillo.core.web.controllers

import com.cillo.core.data.cache.Session
import com.cillo.core.data.db.models._
import com.cillo.core.email.{AsyncMailer, EmailAddress, Email}
import com.cillo.utils.Etc
import com.cillo.utils.play.Auth
import com.cillo.utils.play.Auth.AuthAction
import play.api.mvc._
import com.cillo.core.social.FB
import scala.concurrent.ExecutionContext.Implicits.global

object RegisterController extends Controller {

    def cleanRegisterPage = AuthAction { implicit user => implicit request =>
        Redirect("/")
    }

    def attemptRegister = AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => Redirect("/")
            case None =>
                processRegister(request)
        }
    }

    private def processRegister(request: Request[AnyContent]): Result = {
        val body: AnyContent = request.body
        body.asFormUrlEncoded.map { form =>
            val email = form.get("email").map(_.head)
            val name = form.get("name").map(_.head)
            val password = form.get("password").map(_.head)
            if (email.isDefined && name.isDefined && password.isDefined) {
                val userExists = User.findByEmail(email.get)
                if (userExists.isDefined && Etc.checkPass(password.get, userExists.get.password))
                    Found("/").withCookies(Auth.newSessionCookies(userExists.get.user_id.get))
                val newUser = User.create(User.genUsername(email.get, backup = name.get.replace(" ", "")), name.get, password.get, email.get, None)
                if (newUser.isDefined) {
                    val token = Auth.getNewUserSessionId(User.find(newUser.get.toInt).getOrElse(return BadRequest("Error.")).user_id.get)
                    val sess = new Session(token)
                    sess.set("getting_started", "true")
                    val firstName = Etc.parseFirstName(name.get)
                    sendWelcomEmail(firstName, email.get)
                    Found("/").withCookies(Auth.newSessionCookies(token))
                }
                else
                    BadRequest("Error: user creation failed.")
            } else
                BadRequest("Error: request format invalid.")
        }.getOrElse(return BadRequest("Error."))
    }

    def sendWelcomEmail(firstName: String, email: String) = {
        val sendEmail = Email(
            subject = "Welcome to Cillo!",
            from = EmailAddress("Cillo", "info@cillo.co"),
            text = emailTextGen(firstName),
            htmlText = com.cillo.core.web.views.html.email.welcome(firstName).toString()
        ).to(firstName, email)
        AsyncMailer.sendEmail(sendEmail)
    }

    private def emailTextGen(name: String): String = {
        s"""Hey, $name \n Now that you have secured your spot on Cillo (nice!), you're ready to start
           |exploring boards. \n\n What's a board? Boards are places for anyone to talk about anything. For example, you can: \n\n
           |1. Talk about the latest sports game. \n
           |2. Marvel at the latest high tech. \n
           |3. Weigh in on the next election. \n
           |4. See which movie to watch next. \n\n
           |Hope to see you around!
           |\n\n\n
           |Facebook: https://www.facebook.com/CilloHQ
           |Twitter: https://www.twitter.com/CilloHQ
           |Google: https://plus.google.com/+CilloHQ
           |Blog: http://blog.cillo.co
         """.stripMargin
    }

}
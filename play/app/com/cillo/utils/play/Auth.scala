package com.cillo.utils.play

import com.cillo.core.data.cache.{Session, Redis}
import com.cillo.core.data.db.models.User
import com.cillo.utils.Etc
import com.cillo.utils.security.SecureRand
import play.api.mvc._
import com.cillo.utils.play.errors._

object Auth {

    def AuthAction(f: (Option[User]) => (Request[AnyContent]) => Result) = Action { implicit request: Request[AnyContent] =>
        try {
            val user = parseUserFromCookie orElse parseUserFromQueryString orElse parseUserFromHeader orElse parseUserFromPostData
            f(user)(request)
        } catch {
            case e: AuthTokenCookieExpired =>
                f(None)(request).discardingCookies(deleteSessionCookies)
        }
    }

    def ApiAuthAction(f: (Option[User]) => (Request[AnyContent]) => Result) = Action { implicit request: Request[AnyContent] =>
        try {
            val user = parseUserFromCookie orElse parseUserFromPostData orElse parseUserFromQueryString orElse parseUserFromHeader
            if (user.isDefined)
                f(user)(request)
            else
                Results.BadRequest(UserNotAuthenticated.toJson)
        } catch {
            case e: AuthTokenCookieExpired =>
                Results.BadRequest(UserNotAuthenticated.toJson).discardingCookies(deleteSessionCookies)
        }
    }

    def logInSession(email: String, password: String): Option[String] = {
        val user = User.findByEmail(email)
        if (user.isEmpty)
            throw new EmailDoesNotExist("Email does not exist.")
        if (Etc.checkPass(password, user.get.password)) {
            Some(getNewUserSessionId(user.get.userId.get))
        } else
            None
    }

    def logOutSession(token: String) = {
        Redis.del(token)
    }

    def newSessionCookies(userId: Int): Cookie = {
        Cookie("auth_token", getNewUserSessionId(userId))
    }

    def newSessionCookies(token: String): Cookie ={
        Cookie("auth_token", token)
    }

    def deleteSessionCookies: DiscardingCookie = {
        DiscardingCookie("auth_token")
    }

    def getNewUserSessionId(id: Int): String = {
        val newSeshId = SecureRand.newSessionId()
        val currTime = System.currentTimeMillis().toString
        new Session(newSeshId).newSession(id.toString)
        newSeshId
    }

    private def parseUserFromCookie(implicit request: Request[AnyContent]): Option[User] = {
        val byToken = request.cookies.get("auth_token").getOrElse(return None).value
        parseMemcached(byToken)
    }

    private def parseUserFromPostData(implicit request: Request[AnyContent]): Option[User] = {
        val token = request.body.asFormUrlEncoded.getOrElse(return None).getOrElse("auth_token", return None).head
        parseMemcached(token)
    }

    private def parseUserFromQueryString(implicit request: Request[AnyContent]): Option[User] = {
        val token = request.getQueryString("auth_token").getOrElse(return None)
        parseMemcached(token)
    }

    private def parseUserFromHeader(implicit request: Request[AnyContent]): Option[User] = {
        val token = request.headers.get("X-Auth-Token").getOrElse(return None)
        parseMemcached(token)
    }

    private def parseMemcached(token: String): Option[User] = {
        val userId = new Session(token).get("user_id").getOrElse(return None).toInt
        val user = User.find(userId)
        if (user.isDefined) {
            Some(user.get.copy(token = Some(token), session = Some(new Session(token))))
        } else user
    }

}

case class AuthTokenCookieExpired(message: String) extends Exception(message)

case class EmailDoesNotExist(message: String) extends Exception(message)

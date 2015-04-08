package com.cillo.utils.play

import com.cillo.core.data.cache.{Session, Redis}
import com.cillo.core.data.db.models.User
import com.cillo.utils.Etc
import com.cillo.utils.security.SecureRand
import play.api.mvc._

object Auth {

    def AuthAction(f: (Option[User]) => (Request[AnyContent]) => Result) = Action { implicit request: Request[AnyContent] =>
        try {
            val user = parseUserFromCookie orElse parseUserFromPostData orElse parseUserFromQueryString  orElse parseUserFromHeader
            f(user)(request)
        } catch {
            case e: AuthTokenCookieExpired =>
                f(None)(request).discardingCookies(deleteSessionCookies)
        }
    }

    def logInSession(username: String, password: String): Option[String] = {
        val user = User.find(username)
        Etc.checkPass(password, user.getOrElse(return None).password)
        Some(getNewUserSessionId(user.get.user_id.get))
    }

    def logOutSession(token: String) = {
        Redis.del(token)
    }

    def newSessionCookies(user_id: Int): Cookie = {
        Cookie("auth_token", getNewUserSessionId(user_id))
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
        val user_id = new Session(token).get("user_id").getOrElse(return None).toInt
        val user = User.find(user_id)
        if (user.isDefined) {
            Some(user.get.copy(token = Some(token), session = Some(new Session(token))))
        } else user
    }

}

case class AuthTokenCookieExpired(message: String) extends Exception(message)
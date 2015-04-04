package com.cillo.utils.play

import com.cillo.core.data.cache.Memcached
import com.cillo.core.data.db.models.User
import com.cillo.utils.Etc
import com.cillo.utils.security.SecureRand
import play.api.mvc._
import com.cillo.utils.Session

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

    def logOutSession(token: String): Boolean = {
        Memcached.delete(token)
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
        new Session(newSeshId).multiSet(Map("creation_time" -> currTime, "user_id" -> id.toString))
        newSeshId
    }

    private def parseUserFromCookie(implicit request: Request[AnyContent]): Option[User] = {
        val byToken = request.cookies.get("auth_token").getOrElse(return None).value
        val memcachedRes: Option[String] = Memcached.getTouch(byToken)
        if (!memcachedRes.isDefined || memcachedRes.get.isEmpty)
            throw AuthTokenCookieExpired("Cookie auth token expired.")
        parseMemcached(memcachedRes.get, byToken)
    }

    private def parseUserFromPostData(implicit request: Request[AnyContent]): Option[User] = {
        val token = request.body.asFormUrlEncoded.getOrElse(return None).getOrElse("auth_token", return None).head
        val memcachedRes: Option[String] = Memcached.getTouch(token)
        if (!memcachedRes.isDefined || memcachedRes.get.isEmpty)
            return None
        parseMemcached(memcachedRes.get, token)
    }

    private def parseUserFromQueryString(implicit request: Request[AnyContent]): Option[User] = {
        val token = request.getQueryString("auth_token").getOrElse(return None)
        val memcachedRes: Option[String] = Memcached.getTouch(token)
        if (!memcachedRes.isDefined || memcachedRes.get.isEmpty)
            return None
        parseMemcached(memcachedRes.get, token)
    }

    private def parseUserFromHeader(implicit request: Request[AnyContent]): Option[User] = {
        val token = request.headers.get("X-Auth-Token").getOrElse(return None)
        val memcachedRes: Option[String] = Memcached.getTouch(token)
        if (!memcachedRes.isDefined || memcachedRes.get.isEmpty)
            return None
        parseMemcached(memcachedRes.get, token)
    }

    private def parseMemcached(memcached: String, token: String): Option[User] = {
        val map = Etc.deserializeMap(memcached)
        val user_id = map.getOrElse("user_id", return None).toInt
        val user = User.find(user_id)
        if (user.isDefined) {
            Some(user.get.copy(token = Some(token), session = Some(new Session(token))))
        } else user
    }

}

case class AuthTokenCookieExpired(message: String) extends Exception(message)
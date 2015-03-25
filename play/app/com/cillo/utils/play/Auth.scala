package com.cillo.utils.play

import com.cillo.core.data.cache.Memcached
import com.cillo.core.data.db.models.User
import com.cillo.utils.Etc
import com.cillo.utils.security.SecureRand
import play.api.libs.json._
import play.api.mvc._

object Auth {

    def AuthAction(f: (Option[User]) => (Request[AnyContent]) => Result) = Action { implicit request: Request[AnyContent] =>
        try {
            val user = parseUserFromPostData orElse parseUserFromQueryString orElse parseUserFromCookie orElse parseUserFromHeader
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

    def deleteSessionCookies: DiscardingCookie = {
        DiscardingCookie("auth_token")
    }

    private def getNewUserSessionId(id: Int): String = {
        val newSeshId = SecureRand.newSessionId()
        val currTime = System.currentTimeMillis().toString
        val json: JsValue = Json.obj("creation_time" -> currTime, "user_id" -> id)
        Memcached.set(newSeshId, Json.stringify(json))
        newSeshId
    }

    private def parseUserFromCookie(implicit request: Request[AnyContent]): Option[User] = {
        val byToken = request.cookies.get("auth_token").getOrElse(return None).value
        val memcachedRes: Option[String] = Option(Memcached.get(byToken))
        if (!memcachedRes.isDefined || memcachedRes.get.isEmpty)
            throw AuthTokenCookieExpired("Cookie auth token expired.")
        val session = Json.parse(memcachedRes.get)
        val user_id = (session \ "user_id").asOpt[Int].getOrElse(return None)
        val user = User.find(user_id)
        if (user.isDefined)
            Some(user.get.copy(token = Some(byToken), session = Some(session)))
        else
            user
    }

    private def parseUserFromPostData(implicit request: Request[AnyContent]): Option[User] = {
        val token = request.body.asFormUrlEncoded.getOrElse(return None).getOrElse("auth_token", return None).head
        val memcachedRes: Option[String] = Option(Memcached.get(token))
        if (!memcachedRes.isDefined || memcachedRes.get.isEmpty)
            return None
        val session = Json.parse(memcachedRes.get)
        val user_id = (session \ "user_id").asOpt[Int].getOrElse(return None)
        val user = User.find(user_id)
        if (user.isDefined)
            Some(user.get.copy(token = Some(token), session = Some(session)))
        else
            user
    }

    private def parseUserFromQueryString(implicit request: Request[AnyContent]): Option[User] = {
        val token = request.getQueryString("auth_token").getOrElse(return None)
        val memcachedRes: Option[String] = Option(Memcached.get(token))
        if (!memcachedRes.isDefined || memcachedRes.get.isEmpty)
            return None
        val session = Json.parse(memcachedRes.get)
        val user_id = (session \ "user_id").asOpt[Int].getOrElse(return None)
        val user = User.find(user_id)
        if (user.isDefined)
            Some(user.get.copy(token = Some(token), session = Some(session)))
        else
            user
    }

    private def parseUserFromHeader(implicit request: Request[AnyContent]): Option[User] = {
        val token = request.headers.get("X-Auth-Token").getOrElse(return None)
        val memcachedRes: Option[String] = Option(Memcached.get(token))
        if (!memcachedRes.isDefined || memcachedRes.get.isEmpty)
            return None
        val session = Json.parse(memcachedRes.get)
        val user_id = (session \ "user_id").asOpt[Int].getOrElse(return None)
        val user = User.find(user_id)
        if (user.isDefined)
            Some(user.get.copy(token = Some(token), session = Some(session)))
        else
            user
    }

}

case class AuthTokenCookieExpired(message: String) extends Exception(message)
package com.cillo.core.web.controllers

import com.cillo.core.data.cache.Session
import com.cillo.core.data.db.models._
import com.cillo.utils.play.Auth
import com.cillo.utils.play.Auth.AuthAction
import com.cillo.core.social.FB
import com.cillo.core.data.aws.S3
import play.api.mvc._

object SocialController extends Controller {

    def facebookAuth = AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => Found("/")
            case None =>
                if (request.getQueryString("fb_token").isDefined) {
                    processFacebookAuth(request)
                } else {
                    NotFound("Oops.")
                }
        }
    }

    private def processFacebookAuth(request: Request[AnyContent]): Result = {
        val token = request.getQueryString("fb_token").get
        val fb = FB.createFBInstance(token)
        val info = fb.getBasicInfo
        val fbId = (info \ "id").as[String].toLong
        val user_id = SocialUser.findFbUserId(fbId)
        if (user_id.isDefined) {
            val user = User.find(user_id.get)
            val admin = Admin.isUserAdmin(user.get.user_id.get)
            val token = Auth.getNewUserSessionId(user.get.user_id.get)
            if (admin) {
                val sess = new Session(token)
                sess.set("admin", "true")
            }
            Found("/").withCookies(Cookie("auth_token", token))
        } else {
            val fbEmail = (info \ "email").asOpt[String]
            if (fbEmail.isDefined) {
                val user = User.findByEmail(fbEmail.get)
                if (user.isDefined) {
                    val social = SocialUser.findFbUserId(fbId)
                    if (!social.isDefined) {
                        SocialUser.createFBUser(fbId, user.get.user_id.get)
                    }
                    val admin = Admin.isUserAdmin(user.get.user_id.get)
                    if (admin) {
                        user.get.session.get.set("admin", "true")
                    }
                    return Found("/").withCookies(Auth.newSessionCookies(user.get.user_id.get))
                }
            }
            val fbName = (info \ "name").as[String]
            val username = {
                if (fbEmail.isDefined)
                    User.genUsername(fbEmail.get)
                else
                    User.genUsername(fbName.replace(" ", ""))
            }
            val pic: Option[Int] = {
                val id = S3.uploadURL(fb.getPictureUrl)
                if (id.isDefined) {
                    val media = Media.create(0, id.get)
                    if (media.isDefined) {
                        Some(media.get.toInt)
                    } else None
                } else None
            }
            val newUser = User.create(username, if(fbName.length < 21) fbName else fbName.substring(0, 20) , "", fbEmail.getOrElse(""), None, pic = pic)
            if (newUser.isDefined) {
                SocialUser.createFBUser(fbId, newUser.get.toInt)
                val sess_token = Auth.getNewUserSessionId(User.find(newUser.get.toInt).get.user_id.get)
                val sess = new Session(sess_token)
                sess.multiSet(Map("getting_started" -> "true", "fb_token" -> token))
                Found("/").withCookies(Auth.newSessionCookies(sess_token))
            } else {
                InternalServerError("Oops.")
            }
        }
    }

}
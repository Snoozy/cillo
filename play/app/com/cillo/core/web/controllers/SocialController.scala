package com.cillo.core.web.controllers

import com.cillo.core.data.db.models._
import com.cillo.core.web.views.html.components
import com.cillo.core.web.views.html.core
import com.cillo.utils.play.Auth
import com.cillo.utils.play.Auth.AuthAction
import play.api.libs.json.Json
import com.cillo.core.social.FB
import com.cillo.core.data.aws.S3
import play.api.mvc._

object SocialController extends Controller {

    def facebookAuth = AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => Found("/")
            case None =>
                if (request.getQueryString("fb_token").isDefined) {
                    facebookRegister(request)
                } else {
                    NotFound("Opps.")
                }
        }
    }

    private def facebookRegister(request: Request[AnyContent]): Result = {
        val token = request.getQueryString("fb_token").get
        val fb = FB.createFBInstance(token)
        val info = fb.getBasicInfo
        val fbId = (info \ "id").as[String].toLong
        val user_id = SocialUser.findFbUserId(fbId)
        if (user_id.isDefined) {
            Found("/").withCookies(Auth.newSessionCookies(User.find(user_id.get).get.user_id.get))
        } else {
            print("enter fb register.")
            val fbEmail = (info \ "email").asOpt[String]
            val fbName = (info \ "name").as[String]
            val username = {
                if (fbEmail.isDefined)
                    User.genUsername(fbEmail.get)
                else User.genUsername(fbName.replace(" ", ""))
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
            val newUser = User.create(username, fbName, "", fbEmail.getOrElse(""), None, pic = pic)
            if (newUser.isDefined) {
                SocialUser.createFBUser(fbId, newUser.get.toInt)
                Found("/gettingstarted").withCookies(Auth.newSessionCookies(User.find(newUser.get.toInt).get.user_id.get))
            } else {
                InternalServerError("Opps.")
            }
        }
    }

}
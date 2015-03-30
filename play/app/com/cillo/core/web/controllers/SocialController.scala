package com.cillo.core.web.controllers

import com.cillo.core.data.db.models._
import com.cillo.core.web.views.html.components
import com.cillo.core.web.views.html.core
import com.cillo.utils.play.Auth
import com.cillo.utils.play.Auth.AuthAction
import org.scribe.builder.api.TwitterApi
import org.scribe.oauth.OAuthService
import play.api.Play
import play.api.libs.json.Json
import com.cillo.core.data.cache.Memcached
import com.cillo.core.social.FB
import com.cillo.core.data.aws.S3
import play.api.mvc._
import org.scribe.builder.ServiceBuilder
import org.scribe.builder.api.TwitterApi
import org.scribe.model.{Verb, Verifier, Token, OAuthRequest}

object SocialController extends Controller {

    def facebookAuth = AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => Found("/")
            case None =>
                if (request.getQueryString("fb_token").isDefined) {
                    facebookAuth(request)
                } else {
                    NotFound("Oops.")
                }
        }
    }

    private val twitService: OAuthService = {
        if (Play.isProd) {
            new ServiceBuilder()
                .provider(TwitterApi)
                .apiKey("key")
                .apiSecret("secret")
                .callback("https://www.cillo.co/twitter/auth")
                .build()
        } else {
            new ServiceBuilder()
                .provider(TwitterApi)
                .apiKey("key")
                .apiSecret("secret")
                .callback("http://127.0.0.1:9000")
                .build()
        }
    }


    def twitterAuth = AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => Found("/")
            case None =>
                if (request.getQueryString("oauth_token").isDefined) {
                    finalTwitterAuth(request)
                } else {
                    initTwitterAuth(request)
                }
        }
    }

    private def initTwitterAuth(request: Request[AnyContent]): Result = {
        val reqToken: Token = twitService.getRequestToken
        Memcached.set("twit_oauth-" + reqToken.getToken, reqToken.getSecret)
        val authUrl = twitService.getAuthorizationUrl(reqToken)
        Found(authUrl)
    }

    private def finalTwitterAuth(request: Request[AnyContent]): Result = {
        val oauth_token = request.getQueryString("oauth_token")
        val oauth_verifier = request.getQueryString("oauth_verifier")
        if (oauth_token.isDefined && oauth_verifier.isDefined) {
            val verf = new Verifier(oauth_verifier.get)
            val reqToken = new Token(oauth_token.get, Memcached.get("twit_oauth-" + oauth_token.get))
            val token = twitService.getAccessToken(reqToken, verf)
            val req = new OAuthRequest(Verb.GET)
            val resp = req.send()
        } else {
            NotFound(com.cillo.core.web.views.html.core.close_window())
        }
    }

    private def facebookAuth(request: Request[AnyContent]): Result = {
        val token = request.getQueryString("fb_token").get
        val fb = FB.createFBInstance(token)
        val info = fb.getBasicInfo
        val fbId = (info \ "id").as[String].toLong
        val user_id = SocialUser.findFbUserId(fbId)
        if (user_id.isDefined) {
            Found("/").withCookies(Auth.newSessionCookies(User.find(user_id.get).get.user_id.get))
        } else {
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
            val newUser = User.create(username, if(fbName.length < 21) fbName else fbName.substring(0, 20) , "", fbEmail.getOrElse(""), None, pic = pic)
            if (newUser.isDefined) {
                SocialUser.createFBUser(fbId, newUser.get.toInt)
                Found("/gettingstarted").withCookies(Auth.newSessionCookies(User.find(newUser.get.toInt).get.user_id.get))
            } else {
                InternalServerError("Oops.")
            }
        }
    }

}
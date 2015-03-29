package com.cillo.core.social

import com.ning.http.client.{AsyncHttpClientConfig, AsyncHttpClient}
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws._
import scala.concurrent.duration._
import play.api.Play.current
import scala.concurrent.{Await, Future}

case class FBUninitialized(message: String) extends Exception(message)
case class FBTokenInvalid(message: String) extends Exception(message)


object FB {

    implicit val context = scala.concurrent.ExecutionContext.Implicits.global
    val facebookServerUrl = "https://graph.facebook.com"
    private var cilloFBClientId: Option[String] = None
    private var cilloFBClientSecret: Option[String] = None

    def init(id: String, secret: String) = {
        cilloFBClientId = Some(id)
        cilloFBClientSecret = Some(secret)
    }

    /*
        Creates new facebook api instance with a short lived token
     */
    def createFBInstance(token: String): FBInstance = {
        (cilloFBClientId, cilloFBClientSecret) match {
            case (Some(_), Some(_)) =>
                val longToken = WS.url(facebookServerUrl + "/oauth/access_token?" +
                    "client_id=" + cilloFBClientId.get +
                    "&client_secret=" + cilloFBClientSecret.get +
                    "&grant_type=fb_exchange_token" +
                    "&fb_exchange_token=" + token)
                    .get()
                    .map { response =>
                        val body = response.body
                        print(body)
                        body.substring(body.indexOf('=') + 1, body.indexOf('&'))
                    }
                val resp = Await.result(longToken, 5 seconds)
                if (resp.length > 10) {
                    new FBInstance(resp)
                } else {
                    throw new FBTokenInvalid("Facebook token invalid.")
                }
            case (None, None) =>
                throw new FBUninitialized("Facebook instance uninitialized")
        }
    }

}

class FBInstance(t: String) {
    val token: String = t
    implicit val context = scala.concurrent.ExecutionContext.Implicits.global

    def getBasicInfo: JsValue = {
        val res = WS.url(FB.facebookServerUrl + "/v2.3/me").withQueryString("access_token" -> token).get()
            .map { response =>
                response.json
            }
        Await.result(res, 5 seconds)
    }

    def getPictureUrl: String = {
        val res = WS.url(FB.facebookServerUrl + "/v2.3/me/picture").withQueryString("access_token" -> token, "redirect" -> "false", "width" -> "160", "height" -> "160").get()
            .map { response =>
                (response.json \ "data" \ "url").as[String]
            }
        Await.result(res, 5 seconds)
    }

}
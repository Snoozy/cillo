package com.cillo.core.social

import com.ning.http.client.{AsyncHttpClientConfig, AsyncHttpClient}
import play.api.libs.json._
import play.api.libs.ws._
import scala.concurrent.duration._
import play.api.Play.current
import scala.concurrent.{Await, Future}

case class FBUninitialized(message: String) extends Exception(message)


object FB {

    implicit val context = scala.concurrent.ExecutionContext.Implicits.global
    val facebookServerUrl = "https://graph.facebook.com/"
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
                val longToken = WS.url(facebookServerUrl + "/oauth/access_token")
                    .withQueryString("grant_type" -> "exchange_token",
                        "client_id" -> cilloFBClientId.get,
                        "client_secret" -> cilloFBClientSecret.get,
                        "fb_exchange_token" -> token).get()
                    .map { response =>
                        (response.json \ "access_token").as[String]
                    }
                new FBInstance(Await.result(longToken, 5 seconds))
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
        val res = WS.url(FB.facebookServerUrl + "/v2.3/me/picture").withQueryString("access_token" -> token).get()
            .map { response =>
                (response.json \ "data" \ "url").as[String]
            }
        Await.result(res, 5 seconds)
    }

}
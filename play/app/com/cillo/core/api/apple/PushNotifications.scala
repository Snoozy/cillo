package com.cillo.core.api.apple

import com.notnoop.apns._
import com.cillo.core.data.db.models.AppleDeviceToken

object PushNotifications {

    private lazy val service = APNS.newService().withCert(certPath.get, certPassword.get)
        .withSandboxDestination().build()
    private var certPath: Option[String] = None
    private var certPassword: Option[String] = None

    def init(path: String, password: String) = {
        certPath = Some(path)
        certPassword = Some(password)
    }

    def sendNotification(userId: Int, message: String) = {
        val tokens = AppleDeviceToken.getDeviceTokens(userId)
        tokens.foreach { t =>
            send(message, t)
        }
    }

    private def send(message: String, token: String) = {
        val payload = APNS.newPayload().alertBody(message).build()
        service.push(token, payload)
    }

}
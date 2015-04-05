package com.cillo.core.email

import play.api.libs.mailer
import play.api.Play.current

object Email {

    def sendHTML(sub: String, to: String, from: String, body: String) = {
        val email = mailer.Email(
            sub,
            from,
            Seq(to),
            bodyHtml = Some(body)
        )
        mailer.MailerPlugin.send(email)
    }

}
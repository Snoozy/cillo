package com.cillo.utils.play.errors

import play.api.libs.json.Json

object UserNotAuthenticated {

    def toJson = {
        Json.obj("error" -> "User must be authenticated", "code" -> Code.UserNotAuthenticated.id)
    }

}
package com.cillo.utils.play.errors

import com.cillo.utils.play.errors.ErrorCode
import play.api.libs.json.Json

trait Error {

    def message: String
    def code: ErrorCode.Code

    def toJson = {
        Json.obj("error" -> message, "code" -> code.id  )
    }
}
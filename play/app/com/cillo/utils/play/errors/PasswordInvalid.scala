package com.cillo.utils.play.errors

object PasswordInvalid extends Error {
    val message = "Password invalid"
    val code = ErrorCode.PasswordInvalid
}
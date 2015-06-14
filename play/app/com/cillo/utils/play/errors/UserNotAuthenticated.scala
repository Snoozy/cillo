package com.cillo.utils.play.errors

object UserNotAuthenticated extends Error {
    val message = "User not authenticated"
    val code = ErrorCode.UserNotAuthenticated
}
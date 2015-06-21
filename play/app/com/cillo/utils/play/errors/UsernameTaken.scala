package com.cillo.utils.play.errors

object UsernameTaken extends Error {
    val message = "Username taken"
    val code = ErrorCode.UsernameTaken
}
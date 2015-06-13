package com.cillo.utils.play.errors

object Code {

    sealed trait Code { def id: Int}

    case object UserNotAuthenticated extends Code { val id = 1 }

}
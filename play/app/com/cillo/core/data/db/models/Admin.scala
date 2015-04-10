package com.cillo.core.data.db.models

import anorm.SqlParser._
import anorm._
import play.api.Play.current
import play.api.db._
import play.api.libs.json._

object Admin {

    def isUserAdmin(user_id: Int): Boolean = {
        val admin = SQL("SELECT * FROM admin WHERE user_id = {user}").on('user -> user_id).as(scalar[Long].singleOpt)
        admin.isDefined
    }

}
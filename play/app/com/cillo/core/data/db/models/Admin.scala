package com.cillo.core.data.db.models

import anorm.SqlParser._
import anorm._
import play.api.Play.current
import play.api.db._

object Admin {

    def isUserAdmin(user_id: Int): Boolean = {
        DB.withConnection { implicit connection =>
            val admin = SQL("SELECT * FROM admin WHERE user_id = {user}").on('user -> user_id).as(scalar[Long].singleOpt)
            admin.isDefined
        }
    }

}
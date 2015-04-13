package com.cillo.core.data.db.models

import anorm.SqlParser._
import anorm._
import com.cillo.core.data.db.models.Comment.commentParser
import com.cillo.utils.Etc.{bool2int, int2bool}
import play.api.Play.current
import play.api.db._
import play.api.libs.json._

case class SocialUser(
    social_user_id: Option[Int],
    user_id: Int,
    social_id: String
)

object SocialUser {

    def createFBUser(fb: Long, user_id: Int): Option[Long] = {
        DB.withConnection { implicit connection =>
            val fbId = "fb-" + fb.toString
            SQL("INSERT INTO social_user (user_id, social_id) VALUES ({user}, {fb})").on('user -> user_id, 'fb -> fbId).executeInsert()
        }
    }

    def findFbUserId(fb: Long): Option[Int] = {
        DB.withConnection { implicit connection =>
            val fbId = "fb-" + fb.toString
            SQL("SELECT user_id FROM social_user WHERE social_id = {id}").on('id -> fbId).as(scalar[Int].singleOpt)
        }
    }

    def userIsSocial(id: Int): Boolean = {
        DB.withConnection { implicit connection =>
            val check = SQL("SELECT user_id FROM social_user WHERE user_id = {id}").on('id -> id).as(scalar[Int].singleOpt)
            check.isDefined
        }
    }

}
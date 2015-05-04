package com.cillo.core.data.db.models

import anorm.SqlParser._
import anorm._
import com.cillo.utils.Hash
import play.api.Play.current
import com.cillo.core.data.Constants
import play.api.db._
import com.cillo.core.data.Constants
import play.api.libs.json._


case class Referral(
    referral_id: Option[Int],
    code: String,
    user_id: Int
)

object Referral {

    private val hasher = Hash(Constants.HashSalt)

    private[data] val referralParser: RowParser[Referral] = {
        get[Option[Int]]("referral_id") ~
            get[Int]("user_id") map {
            case referral_id ~ user_id =>
                val code = hasher.encode(referral_id.get)
                Referral(referral_id, code, user_id)
        }
    }

    def find(referral_id: Int): Option[Referral] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM referral WHERE referral_id = {ref}").on('ref -> referral_id).as(referralParser.singleOpt)
        }
    }

    def findByUser(user_id: Int): Option[Referral] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM referral WHERE user_id = {user}").on('user -> user_id).as(referralParser.singleOpt)
        }
    }

    def findByHash(hash: String): Option[Referral] = {
        val user = hasher.decode(hash).head
        findByUser(user.toInt)
    }

    def create(user_id: Int): Option[Referral] = {
        val exists = Referral.findByUser(user_id)
        if (exists.isDefined) {
            exists
        } else {
            DB.withConnection { implicit connection =>
                val id = SQL("INSERT INTO referral (user_id) VALUES ({user})").on('user -> user_id).executeInsert(scalar[Long].singleOpt)
                if (id.isDefined) {
                    find(id.get.toInt)
                } else {
                    None
                }
            }
        }
    }

}
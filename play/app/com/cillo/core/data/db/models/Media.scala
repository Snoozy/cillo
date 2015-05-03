package com.cillo.core.data.db.models

import anorm.SqlParser._
import anorm._
import play.api.Play.current
import play.api.db._

case class Media(
    media_id: Option[Int],
    media_type: Int,
    media_name: String,
    media_url: String
)

object Media {

    val BaseMediaURL = "https://static.cillo.co/image/"

    private[models] val mediaParser: RowParser[Media] = {
        get[Option[Int]]("media_id") ~
            get[Int]("media_type") ~
            get[String]("media_name") map {
            case media_id ~ media_type ~ media_name =>
                Media(media_id, media_type, media_name, BaseMediaURL + media_name)
        }
    }

    def find(media_id: Int): Option[Media] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM media WHERE media_id = {media}").on('media -> media_id).as(mediaParser.singleOpt)
        }
    }

    def create(media_type: Int, media_name: String): Option[Long] = {
        DB.withConnection { implicit connection =>
            SQL("INSERT INTO media (media_type, media_name) VALUES ({media_type}, {media_name})")
                .on('media_type -> media_type, 'media_name -> media_name).executeInsert()
        }
    }

    def getAll: Seq[Media] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM media").as(mediaParser *)
        }
    }

}
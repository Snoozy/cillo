package com.cillo.core.data.db.models

import anorm.SqlParser._
import anorm._
import play.api.Play.current
import play.api.db._
import play.api.libs.concurrent.Akka
import scala.concurrent.duration._

case class Conversation (
    conversationId: Option[Int],
    user1Id: Int,
    user2Id: Int,
    created: Long,
    updated: Long,
    read: Int,
    preview: String
)

object Conversation {

    private[data] val conversationParser: RowParser[Conversation] = {
        get[Option[Int]]("conversation_id") ~
            get[Int]("user1_id") ~
            get[Int]("user2_id") ~
            get[Long]("created_time") ~
            get[Long]("updated_time") ~
            get[Int]("read") ~
            get[String]("preview") map {
            case conversationId ~ user1Id ~ user2Id ~ created ~ updated ~ read ~ preview =>
                Conversation(conversationId, user1Id, user2Id, created, updated, read, preview)
        }
    }

    def byUsers(user1: Int, user2: Int): Option[Int] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM conversation WHERE user1 = {user1} AND user2 = {user2} OR user1 = {user2} AND user2 = {user1} LIMIT 1")
                .on('user1 -> user1, 'user2 -> user2).as(scalar[Int].singleOpt)
        }
    }

    def byUser(userId: Int, limit: Int = 15): Seq[Conversation] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM conversation WHERE user1_id = {user} OR user2_id = {user} ORDER BY updated_time DESC LIMIT {limit}")
                .on('user -> userId, 'limit -> limit).as(conversationParser *)
        }
    }

    def read(conversationId: Int) = {
        Akka.system.scheduler.scheduleOnce(10.milliseconds) {
            DB.withConnection { implicit connection =>
                SQL("UPDATE conversation SET read = 1 WHERE conversation_id = {conversation_id}").on('conversation_id -> conversationId).executeUpdate()
            }
        }
    }

    def update(user1: Int, user2: Int, preview: String): Option[Int] = {
        DB.withConnection { implicit connection =>
            val time = System.currentTimeMillis()
            val conversation = Conversation.byUsers(user1, user2)
            if (conversation.isDefined) {
                SQL("UPDATE conversation SET read = 0, updated_time = {time}, preview = {preview} WHERE conversation_id = {conversation_id}")
                    .on('conversation_id -> conversation.get, 'time -> time, 'preview -> preview.substring(0, 295)).executeUpdate()
                conversation
            } else {
                SQL("INSERT INTO conversation (user1_id, user2_id, created_time, updated_time, preview) VALUES ({user1_id}, {user2_id}, {time}, {time}, {preview})")
                    .on('user1_id -> user1, 'user2_id -> user2, 'time -> time, 'preview -> preview.substring(0, 295)).executeInsert(scalar[Int].singleOpt)
            }
        }
    }

}
package com.cillo.core.data.db.models

import anorm.SqlParser._
import anorm._
import play.api.Play.current
import play.api.db._

case class Message (
    messageId: Option[Int],
    conversationId: Int,
    userId: Int,
    content: String,
    time: Long
)

object Message {

    private[data] val messageParser: RowParser[Message] = {
        get[Option[Int]]("message_id") ~
            get[Int]("conversation_id") ~
            get[Int]("user_id") ~
            get[String]("content") ~
            get[Long]("time") map {
            case messageId ~ conversationId ~ userId ~ content ~ time =>
                Message(messageId, conversationId, userId, content, time)
        }
    }
    
    def find(id: Int): Option[Message] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM message WHERE message_id = {message_id} LIMIT 1").on('message_id -> id).as(messageParser.singleOpt)
        }
    }
    
    def byConversation(id: Int, limit: Int = 20): Seq[Message] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM message WHERE conversation_id = {conversation_id} ORDER BY time DESC LIMIT {limit}").on('conversation_id -> id).as(messageParser *)
        }
    }
    
    def create(sender: Int, receiver: Int, content: String): Option[Long] = {
        DB.withConnection { implicit connection =>
            val time = System.currentTimeMillis()
            val conversation = Conversation.update(sender, receiver, content)
            if (conversation.isDefined) {
                SQL("INSERT INTO message (conversation_id, user1_id, user2_id, content, time) VALUES ({conversation_id}, {user1_id}, {user2_id}, {content}, {time})")
                    .on('conversation_id -> conversation.get, 'user1_id -> sender, 'user2_id -> receiver, 'content -> content, 'time -> time).executeInsert()
            } else {
                None
            }
        }
    }

}
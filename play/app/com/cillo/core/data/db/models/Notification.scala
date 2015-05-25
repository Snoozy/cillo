package com.cillo.core.data.db.models

import anorm.SqlParser._
import anorm._
import com.cillo.core.data.db.models.Enum._
import play.api.Play.current
import play.api.db._

case class Notification (
    notificationId: Option[Int],
    entityId: Int,
    entityType: EntityType,
    actionType: ActionType,
    titleUser: Int,
    userId: Int,
    count: Int,
    read: Boolean,
    time: Long
)

object Notification {

    private[data] val notificationParser: RowParser[Notification] = {
        get[Option[Int]]("notification_id") ~
            get[Int]("entity_id") ~
            get[Int]("entity_type") ~
            get[Int]("title_user") ~
            get[Int]("action_type") ~
            get[Int]("count") ~
            get[Int]("user_id") ~
            get[Int]("read") ~
            get[Long]("time") map {
            case notificationId ~ entityId ~ entityType ~ titleUser ~ actionType ~ count ~ userId ~ read ~ time =>
                Notification(notificationId, entityId, EntityType.fromInt(entityId).get, ActionType.fromInt(actionType).get, titleUser, userId, count, read != 0, time)
        }
    }

    def addListener(entityId: Int, entityType: EntityType, userId: Int) = {
        DB.withConnection { implicit connection =>
            SQL("INSERT INTO notification_listener (entity_id, entity_type, user_id) VALUES ({entity_id}, {entity_type}, {user_id})")
                .on('entity_id -> entityId, 'entity_type -> entityType.id, 'user_id -> userId).executeInsert()
        }
    }

    def create(entityId: Int, entityType: EntityType, actionType: ActionType, titleUser: Int) = {
        DB.withConnection { implicit connection =>
            val exists = SQL("SELECT * FROM notification WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND action_type = {action_type} AND read = 0")
                .on('entity_id -> entityId, 'entity_type -> entityType.id, 'action_type -> actionType.id).as(notificationParser.singleOpt)
            if (exists.isDefined) {
                SQL("UPDATE notification SET count = count + 1 WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND action_type = {action_type} AND read = 0")
                    .on('entity_id -> entityId, 'entity_type -> entityType.id).executeUpdate()
            } else {
                val time = System.currentTimeMillis()
                val listeners = SQL("SELECT user_id FROM notification_listener WHERE entity_id = {entity_id} AND entity_type = {entity_type}")
                    .on('entity_id -> entityId, 'entity_type -> entityType.id).as(scalar[Int] *)
                listeners.par map { l =>
                    SQL("INSERT INTO notification (entity_id, entity_type, title_user, action_type, count, user_id, time) VALUES ({entity_id}, {entity_type}, {title_user}," +
                        " {action_type}, 0, {user_id}, {time})")
                        .on('entity_id -> entityId, 'entity_type -> entityType.id, 'title_user -> titleUser, 'action_type -> actionType.id, 'user_id -> l, 'time -> time).executeInsert()
                }
            }
        }
    }

    def delete(entityId: Int, entityType: EntityType, actionType: ActionType, userId: Int): Boolean = {
        DB.withConnection { implicit connection =>
            val exists = SQL("SELECT * FROM notification WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND action_type = {action_type}")
                .on('entity_id -> entityId, 'entity_type -> entityType.id, 'action_type -> actionType.id).as(notificationParser.singleOpt)
            if (exists.isDefined) {
                if (exists.get.titleUser == userId && exists.get.count > 0) {
                    entityType match {
                        case EntityType.Comment =>
                            actionType match {
                                case ActionType.Vote =>
                                    val recent = Comment.mostRecentVoter(entityId)
                                    SQL("UPDATE notification SET title_user = {title_user}, count = count - 1 WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND action_type = {action_type}")
                                        .on('entity_id -> entityId, 'entity_type -> entityType.id, 'action_type -> actionType.id, 'title_user -> recent.get).executeUpdate() > 0
                                case ActionType.Reply =>
                                    val recent = Comment.mostRecentReplier(entityId)
                                    SQL("UPDATE notification SET title_user = {title_user}, count = count - 1 WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND action_type = {action_type}")
                                        .on('entity_id -> entityId, 'entity_type -> entityType.id, 'action_type -> actionType.id, 'title_user -> recent.get).executeUpdate() > 0
                            }
                        case EntityType.Post =>
                            actionType match {
                                case ActionType.Vote =>
                                    val recent = Post.mostRecentVoter(entityId)
                                    SQL("UPDATE notification SET title_user = {title_user}, count = count - 1 WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND action_type = {action_type}")
                                        .on('entity_id -> entityId, 'entity_type -> entityType.id, 'action_type -> actionType.id, 'title_user -> recent.get).executeUpdate() > 0
                                case ActionType.Reply =>
                                    val recent = Post.mostRecentReplier(entityId)
                                    SQL("UPDATE notification SET title_user = {title_user}, count = count - 1 WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND action_type = {action_type}")
                                        .on('entity_id -> entityId, 'entity_type -> entityType.id, 'action_type -> actionType.id, 'title_user -> recent.get).executeUpdate() > 0
                            }
                    }
                } else if (exists.get.count > 0) {
                    SQL("UPDATE notification SET count = count - 1 WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND action_type = {action_type}")
                        .on('entity_id -> entityId, 'entity_type -> entityType.id, 'action_type -> actionType.id).executeUpdate() > 0
                } else {
                    SQL("DELETE FROM notification WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND action_type = {action_type}")
                        .on('entity_id -> entityId, 'entity_type -> entityType.id, 'action_type -> actionType.id).executeUpdate() > 0
                }
            } else {
                false
            }
        }
    }

    def getNotifications(userId: Int, limit: Int = 15): Seq[Notification] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM notification WHERE user_id = {user_id} ORDER BY time DESC LIMIT {limit}").on('user_id -> userId, 'limit -> limit).as(notificationParser *)
        }
    }

}
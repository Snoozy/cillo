package com.cillo.core.data.db.models

import anorm.SqlParser._
import anorm._
import com.cillo.core.data.db.models.Enum._
import com.cillo.utils.Etc
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.concurrent.Akka
import scala.concurrent.duration._
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
                Notification(notificationId, entityId, EntityType.fromInt(entityType).get, ActionType.fromInt(actionType).get, titleUser, userId, count, read != 0, time)
        }
    }

    def addListener(entityId: Int, entityType: EntityType, userId: Int): Unit = {
        Akka.system.scheduler.scheduleOnce(10.milliseconds) {
            DB.withConnection { implicit connection =>
                SQL("INSERT INTO notification_listener (entity_id, entity_type, user_id) VALUES ({entity_id}, {entity_type}, {user_id})")
                    .on('entity_id -> entityId, 'entity_type -> entityType.id, 'user_id -> userId).executeInsert()
            }
        }
    }

    def create(entityId: Int, entityType: EntityType, actionType: ActionType, titleUser: Int): Unit = {
        Akka.system.scheduler.scheduleOnce(10.milliseconds) {
            DB.withConnection { implicit connection =>
                val time = System.currentTimeMillis()
                val oneday = time - 86400000
                val exists = SQL("SELECT * FROM notification WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND action_type = {action_type} AND time > {oneday} LIMIT 1")
                    .on('entity_id -> entityId, 'entity_type -> entityType.id, 'action_type -> actionType.id, 'oneday -> oneday).as(notificationParser.singleOpt)
                if (exists.isDefined) {
                    if (exists.get.count < 101) {
                        SQL("UPDATE notification SET count = count + 1, title_user = {user}, time = {time}, `read` = 0 WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND action_type = {action_type} AND time > {oneday} LIMIT 1")
                            .on('entity_id -> entityId, 'entity_type -> entityType.id, 'action_type -> actionType.id, 'user -> titleUser, 'time -> time, 'oneday -> oneday).executeUpdate()
                    } else {
                        SQL("UPDATE notification SET title_user = {user}, time = {time}, `read` = 0 WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND action_type = {action_type} AND time > {oneday} LIMIT 1")
                            .on('entity_id -> entityId, 'entity_type -> entityType.id, 'action_type -> actionType.id, 'user -> titleUser, 'time -> time, 'oneday -> oneday).executeUpdate()
                    }
                } else {
                    val time = System.currentTimeMillis()
                    val listeners = SQL("SELECT user_id FROM notification_listener WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND user_id != {user}")
                        .on('entity_id -> entityId, 'entity_type -> entityType.id, 'user -> titleUser).as(scalar[Int] *)
                    listeners.par map { l =>
                        SQL("INSERT INTO notification (entity_id, entity_type, title_user, action_type, count, user_id, time) VALUES ({entity_id}, {entity_type}, {title_user}," +
                            " {action_type}, 0, {user_id}, {time})")
                            .on('entity_id -> entityId, 'entity_type -> entityType.id, 'title_user -> titleUser, 'action_type -> actionType.id, 'user_id -> l, 'time -> time).executeInsert()
                    }
                }
            }
        }
    }

    def delete(entityId: Int, entityType: EntityType, actionType: ActionType, userId: Int): Unit = {
        Akka.system.scheduler.scheduleOnce(10.milliseconds) {
            DB.withConnection { implicit connection =>
                val exists = SQL("SELECT * FROM notification WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND action_type = {action_type} LIMIT 1")
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
    }

    def read(userId: Int): Boolean = {
        DB.withConnection { implicit connection =>
            SQL("UPDATE notification SET `read` = 1 WHERE user_id = {user_id}").on('user_id -> userId).executeUpdate()
            true
        }
    }

    def getDetails(notification: Notification): (String, String) = {
        notification.entityType match {
            case EntityType.Post =>
                val post = Post.find(notification.entityId)
                val board = Board.find(post.get.boardId)
                val descr = {
                    if (post.get.title.isDefined)
                        post.get.title.get
                    else
                        Etc.preview(post.get.data, 50)
                }
                ("/" + board.get.name + "/posts/" + post.get.postId.get, descr)
            case EntityType.Comment =>
                val comment = Comment.find(notification.entityId)
                val board = Board.find(Post.find(comment.get.postId).get.boardId)
                val descr = Etc.preview(comment.get.data, 50)
                ("/" + board.get.name + "/comments/" + comment.get.commentId.get, descr)
        }
    }

    def getNotifications(userId: Int, limit: Int = 15): Seq[Notification] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM notification WHERE user_id = {user_id} ORDER BY time DESC LIMIT {limit}").on('user_id -> userId, 'limit -> limit).as(notificationParser *)
        }
    }

}
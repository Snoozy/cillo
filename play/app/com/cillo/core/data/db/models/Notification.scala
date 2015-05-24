package com.cillo.core.data.db.models

import anorm.SqlParser._
import anorm._
import com.cillo.core.data.Constants
import com.cillo.core.data.db.models.Enum._
import play.api.Play.current
import play.api.db._
import play.api.libs.json._


case class Notification (
    notificationId: Option[Int],
    entityId: Int,
    entityType: EntityType,
    titleUser: Int,
    actionType: ActionType,
    count: Int,
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
            get[Long]("time") map {
            case notificationId ~ entityId ~ entityType ~ titleUser ~ actionType ~ count ~ time =>
                Notification(notificationId, entityId, EntityType.fromInt(entityId).get, titleUser, ActionType.fromInt(actionType).get, count, time)
        }
    }

    def create(entityId: Int, entityType: EntityType, actionType: ActionType, titleUser: Int) = {
        DB.withConnection { implicit connection =>
            val exists = SQL("SELECT * FROM notification WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND action_type = {action_type}")
                .on('entity_id -> entityId, 'entity_type -> entityType.id, 'action_type -> actionType.id).as(notificationParser.singleOpt)
            if (exists.isDefined) {
                SQL("UPDATE notification SET count = count + 1 WHERE entity_id = {entity_id} AND entity_type = {entity_type}")
                    .on('entity_id -> entityId, 'entity_type -> entityType.id)
            } else {
                val time = System.currentTimeMillis()
                SQL("INSERT INTO notification (entity_id, entity_type, title_user, action_type, count, time) VALUES ({entity_id}, {entity_type}, {title_user}," +
                    " {action_type}, 0, {time})").on('entity_id -> entityId, 'entity_type -> entityType.id, 'title_user -> titleUser, 'action_type -> actionType.id, 'time -> time).executeInsert()
            }
        }
    }



}
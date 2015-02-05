package com.cillo.core.data.db.models

import anorm.SqlParser._
import anorm._
import com.cillo.core.data.db.models.Post.postParser
import play.api.Play.current
import play.api.db._
import play.api.libs.json._

case class Group (
    group_id: Option[Int],
    name: String,
    time: Long,
    description: String,
    creator_id: Int,
    followers: Int,
    privacy: Int,
    photo: String,
    photo_id: Int
)

object Group {

    private val DefaultPhoto = 1
    private val DefaultPhotoString = "DEFAULT GROUP PHOTO"
    private val ImageURLBase = "https://static.cillo.co/image/"

    private[models] val groupParser: RowParser[Group] = {
        get[Option[Int]]("group_id") ~
            get[String]("name") ~
            get[Long]("time") ~
            get[String]("description") ~
            get[Int]("creator_id") ~
            get[Int]("followers") ~
            get[Int]("privacy") ~
            get[Int]("photo") map {
            case group_id ~ name ~ time ~ description ~ creator_id ~ followers ~ privacy ~ photo =>
                val p = Media.find(photo)
                if (p.isDefined)
                    Group(group_id, name, time, description, creator_id, followers, privacy, ImageURLBase + p.get.media_name, photo)
                else
                    Group(group_id, name, time, description, creator_id, followers, privacy, ImageURLBase + DefaultPhotoString, photo)
        }
    }

    def find(id: Int):Option[Group] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM `group` WHERE group_id = {id}").on('id -> id).as(groupParser.singleOpt)
        }
    }

    def find(name: String): Option[Group] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM `group` WHERE name = {name} AND privacy = 0").on('name -> name).as(groupParser.singleOpt)
        }
    }

    def create(name: String, desc: Option[String], creator_id: Int, privacy: Int = 0, photo: Int = DefaultPhoto): Option[Long] = {
        val time = System.currentTimeMillis()

        DB.withConnection { implicit connection =>
            SQL("INSERT INTO `group` (name, description, creator_id, followers, privacy, time, photo) VALUES " +
                "({name}, {desc}, {creator_id}, {followers}, {privacy}, {time}, {photo})").on('name -> name,
                    'desc -> desc.getOrElse(""), 'creator_id -> creator_id, 'followers -> 0, 'privacy -> privacy,
                    'time -> time, 'photo -> photo).executeInsert()
        }
    }

    def update(group_id: Int, desc: String, pic: Int) = {
        DB.withConnection { implicit connection =>
            SQL("UPDATE `group` SET description = {desc}, photo = {pic} WHERE group_id = {group}")
                .on('desc -> desc, 'pic -> pic, 'group -> group_id).executeUpdate()
        }
    }

    def addFollower(user_id: Int, group_id: Int): Boolean = {
        val time = System.currentTimeMillis()

        DB.withConnection { implicit connection =>
            SQL("UPDATE `group` SET followers = followers + 1 WHERE group_id = {group_id}")
                .on('group_id -> group_id).executeUpdate()
            SQL("INSERT INTO user_to_group (user_id, group_id, time) VALUES ({user_id}, {group_id}, {time})")
                .on('user_id -> user_id, 'group_id -> group_id, 'time -> time).executeInsert()
        }
        true
    }

    def removeFollower(user_id: Int, group_id: Int): Boolean = {
        DB.withConnection { implicit connection =>
            SQL("DELETE FROM user_to_group WHERE user_id = {user_id} AND group_id = {group_id}")
                .on('user_id -> user_id, 'group_id -> group_id).executeUpdate()
            SQL("UPDATE `group` SET followers = followers - 1 WHERE group_id = {group_id}")
                .on('group_id -> group_id).executeUpdate()
        }
        true
    }

    def getTrendingGroups: Seq[Group] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM `group` WHERE privacy = 0 ORDER BY followers DESC LIMIT 20").as(groupParser *)
        }
    }

    def getTrendingPosts(group_id: Int, limit: Int = 200): Seq[Post] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM post WHERE group_id = {group_id} ORDER BY post_id DESC LIMIT {limit}").on('group_id -> group_id, 'limit -> limit).as(postParser *)
        }
    }

    def toJsonSeq(groups: Seq[Group], following: Boolean = false): JsValue = {
        var json = Json.arr()
        groups.foreach { group =>
            json = json.+:(toJson(group, following))
        }
        json
    }

    def toJson(group: Group, following: Boolean = false): JsValue = {
        Json.obj(
            "name" -> group.name,
            "followers" -> group.followers,
            "group_id" -> group.group_id.get,
            "creator_id" -> group.creator_id,
            "photo" -> group.photo,
            "description" -> group.description,
            "following" -> following
        )
    }

}
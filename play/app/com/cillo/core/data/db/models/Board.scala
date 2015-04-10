package com.cillo.core.data.db.models

import anorm.SqlParser._
import anorm._
import com.cillo.core.data.db.models.Post.postParser
import play.api.Play.current
import play.api.db._
import play.api.libs.json._

case class Board (
    board_id: Option[Int],
    name: String,
    time: Long,
    description: String,
    creator_id: Int,
    followers: Int,
    privacy: Int,
    photo: String,
    photo_id: Int
)

object Board {

    private val DefaultPhotoString = "default_group"
    private val ImageURLBase = "https://static.cillo.co/image/"

    private[data] val boardParser: RowParser[Board] = {
        get[Option[Int]]("board_id") ~
            get[String]("name") ~
            get[Long]("time") ~
            get[String]("description") ~
            get[Int]("creator_id") ~
            get[Int]("followers") ~
            get[Int]("privacy") ~
            get[Option[Int]]("photo") map {
            case board_id ~ name ~ time ~ description ~ creator_id ~ followers ~ privacy ~ photo =>
                if (photo.isDefined) {
                    val p = Media.find(photo.get)
                    if (p.isDefined)
                        Board(board_id, name, time, description, creator_id, followers, privacy, ImageURLBase + p.get.media_name, photo.get)
                    else
                        Board(board_id, name, time, description, creator_id, followers, privacy, ImageURLBase + DefaultPhotoString, 0)
                } else {
                    Board(board_id, name, time, description, creator_id, followers, privacy, ImageURLBase + DefaultPhotoString, 0)
                }

        }
    }

    def find(id: Int):Option[Board] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM board WHERE board_id = {id}").on('id -> id).as(boardParser.singleOpt)
        }
    }

    def find(name: String): Option[Board] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM board WHERE name = {name} AND privacy = 0").on('name -> name).as(boardParser.singleOpt)
        }
    }

    def create(name: String, desc: Option[String], creator_id: Int, privacy: Int = 0, photo: Option[Int] = None): Option[Long] = {
        val time = System.currentTimeMillis()

        DB.withConnection { implicit connection =>
            SQL("INSERT INTO `board` (name, description, creator_id, followers, privacy, time, photo) VALUES " +
                "({name}, {desc}, {creator_id}, {followers}, {privacy}, {time}, {photo})").on('name -> name,
                    'desc -> desc.getOrElse(""), 'creator_id -> creator_id, 'followers -> 0, 'privacy -> privacy,
                    'time -> time, 'photo -> photo).executeInsert()
        }
    }

    def update(board_id: Int, desc: String, pic: Int) = {
        DB.withConnection { implicit connection =>
            SQL("UPDATE `board` SET description = {desc}, photo = {pic} WHERE board_id = {board}")
                .on('desc -> desc, 'pic -> pic, 'board -> board_id).executeUpdate()
        }
    }

    def getPostsCount(board_id: Int): Int = {
        DB.withConnection { implicit connection =>
            SQL("SELECT COUNT(*) FROM post WHERE board_id = {board}").on('board -> board_id).as(scalar[Long].single).toInt
        }
    }

    def addFollower(user_id: Int, board_id: Int): Boolean = {
        val time = System.currentTimeMillis()

        DB.withConnection { implicit connection =>
            SQL("UPDATE `board` SET followers = followers + 1 WHERE board_id = {board_id}")
                .on('board_id -> board_id).executeUpdate()
            SQL("INSERT INTO user_to_board (user_id, board_id, time) VALUES ({user_id}, {board_id}, {time})")
                .on('user_id -> user_id, 'board_id -> board_id, 'time -> time).executeInsert()
        }
        true
    }

    def removeFollower(user_id: Int, board_id: Int): Boolean = {
        DB.withConnection { implicit connection =>
            SQL("DELETE FROM user_to_board WHERE user_id = {user_id} AND board_id = {board_id}")
                .on('user_id -> user_id, 'board_id -> board_id).executeUpdate()
            SQL("UPDATE `board` SET followers = followers - 1 WHERE board_id = {board_id}")
                .on('board_id -> board_id).executeUpdate()
        }
        true
    }

    def getTrendingBoards: Seq[Board] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM `board` WHERE privacy = 0 ORDER BY followers DESC LIMIT 20").as(boardParser *)
        }
    }

    def getFeed(board_id: Int, limit: Int = Post.DefaultPageSize): Seq[Post] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM post WHERE board_id = {board_id} ORDER BY post_id DESC LIMIT {limit}").on('board_id -> board_id, 'limit -> limit).as(postParser *)
        }
    }

    def getFeedPaged(board_id: Int, after: Int, limit: Int = Post.DefaultPageSize): Seq[Post] = {
        DB.withConnection { implicit connection =>
            val posts = SQL("SELECT * FROM post WHERE post_id < {after} AND board_id = {board_id} ORDER BY post_id DESC LIMIT {limit}")
                .on('board_id -> board_id, 'after -> after, 'limit -> limit).as(postParser *)
            if (posts.length < limit)
                posts
            else
                posts.takeRight(limit)
        }
    }

    def toJsonSeq(boards: Seq[Board], following: Option[Boolean] = None, user: Option[User] = None): JsValue = {
        var json = Json.arr()
        boards.foreach { board =>
            if (following.isDefined) {
                json = json.+:(toJson(board, following.get))
            } else if (user.isDefined) {
                val f = User.userIsFollowing(user.get.user_id.get, board.board_id.get)
                json = json.+:(toJson(board, f))
            } else {
                json = json.+:(toJson(board))
            }
        }
        json
    }

    def toJson(board: Board, following: Boolean = false): JsValue = {
        Json.obj(
            "name" -> board.name,
            "followers" -> board.followers,
            "board_id" -> board.board_id.get,
            "creator_id" -> board.creator_id,
            "photo" -> board.photo,
            "description" -> board.description,
            "following" -> following
        )
    }

}
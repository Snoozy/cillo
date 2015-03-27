package com.cillo.core.data.db.models

import anorm.SqlParser._
import anorm._
import com.cillo.core.data.db.models.Comment.commentParser
import com.cillo.core.data.db.models.Post.postParser
import com.cillo.utils.Etc.makeDigest
import play.api.Play.current
import play.api.db._
import com.cillo.utils.Session
import play.api.libs.json._

case class User(
    user_id: Option[Int],
    username: String,
    name: String,
    password: String,
    email: String,
    time: Long,
    reputation: Int,
    photo: String,
    photo_id: Int,
    bio: String,
    token: Option[String] = None,
    session: Option[Session] = None
)

object User {

    private val DefaultPhotoString = "0.png"
    private val ImageURLBase = "https://static.cillo.co/image/"

    private[models] val userParser: RowParser[User] = {
        get[Option[Int]]("user_id") ~
            get[String]("username") ~
            get[String]("name") ~
            get[String]("password") ~
            get[String]("email") ~
            get[Long]("time") ~
            get[Option[Int]]("reputation") ~
            get[Option[Int]]("photo") ~
            get[String]("bio") map{
            case user_id ~ username ~ name ~ password ~ email ~ time ~ reputation ~ photo ~ bio =>
                if (photo.isDefined) {
                    val p = Media.find(photo.get)
                    if (p.isDefined)
                        User(user_id, username, name, password, email, time, reputation.getOrElse(0), ImageURLBase + p.get.media_name, photo.get, bio)
                    else
                        User(user_id, username, name, password, email, time, reputation.getOrElse(0), ImageURLBase + DefaultPhotoString, 0, bio)
                } else {
                    User(user_id, username, name, password, email, time, reputation.getOrElse(0), ImageURLBase + DefaultPhotoString, 0, bio)
                }
        }
    }

    def find(id: Int): Option[User] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM user WHERE user_id = {id}").on('id -> id).as(userParser.singleOpt)
        }
    }

    def find(username: String): Option[User] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM user WHERE username = {username}").on('username -> username).as(userParser.singleOpt)
        }
    }

    def create(username: String, name: String, password: String, email: String, bio: Option[String]): Option[Long] = {
        val time = System.currentTimeMillis()

        DB.withConnection { implicit connection =>
            SQL("INSERT INTO user (username, name, password, email, bio, time, reputation) VALUES ({username}, {name}," +
                " {password}, {email}, {bio}, {time}, 0)").on('username -> username, 'name -> name,
                    'password -> makeDigest(password), 'email -> email, 'bio -> bio.getOrElse(""), 'time -> time).executeInsert()
        }
    }

    def update(user_id: Int, name: String, bio: String, pic: Int) = {
        DB.withConnection { implicit connection =>
            SQL("UPDATE user SET name = {name}, bio = {bio}, photo = {photo} WHERE user_id = {user}")
                .on('name -> name, 'photo -> pic, 'user -> user_id, 'bio -> bio).executeUpdate()
        }
    }

    def getPosts(user_id: Int, limit: Int = Post.DefaultPageSize): Seq[Post] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM post WHERE user_id = {id} ORDER BY post_id DESC LIMIT {limit}").on('id -> user_id, 'limit -> limit).as(postParser *)
        }
    }

    def getFeed(user_id: Int, limit: Int = Post.DefaultPageSize): Seq[Post] = {
        //TODO user timeline
        DB.withConnection { implicit connection =>
            val board_ids = User.getBoardIDs(user_id)
            if (board_ids.isEmpty)
                Seq()
            else {
                val posts = SQL("SELECT * FROM post WHERE board_id IN ({board_ids}) ORDER BY post_id DESC LIMIT {limit}")
                    .on('board_ids -> board_ids, 'limit -> limit).as(postParser *)
                posts
            }
        }
    }

    def getComments(user_id: Int, limit: Int = Post.DefaultPageSize): Seq[Comment] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM comment WHERE user_id = {user_id} ORDER BY comment_id DESC LIMIT {limit}").on('user_id -> user_id, 'limit -> limit).as(commentParser *)
        }
    }

    def userIsFollowing(user_id: Int, board_id: Int): Boolean = {
        DB.withConnection { implicit connection =>
            val count = SQL("SELECT COUNT(*) FROM user_to_board WHERE user_id = {user_id} AND board_id = {board_id}")
                .on('user_id -> user_id, 'board_id -> board_id).as(scalar[Long].single)
            count > 0L
        }
    }

    def toJsonByUserID(user_id: Int, self: Option[User] = None, email: Boolean = false): JsValue = {
        val userExists = User.find(user_id)
        if (!userExists.isDefined)
            Json.obj("error" -> "User does not exist.")
        else {
            val user = userExists.get
            toJson(user, self, email)
        }
    }

    def getBoardIDs(user_id: Int): Seq[Int] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT board_id FROM user_to_board WHERE user_id = {id}").on('id -> user_id).as(int("board_id") *)
        }
    }

    def getBoards(user_id: Int): Seq[Board] = {
        DB.withConnection { implicit connection =>
            val boardIDs = User.getBoardIDs(user_id)
            if (boardIDs.isEmpty)
                return List()
            else
                SQL("SELECT * FROM `board` WHERE board_id IN ({board_ids})").on('board_ids -> boardIDs).as(Board.boardParser *)
        }
    }

    def toJsonByUsername(username: String, self: Option[User] = None, email: Boolean = false): JsValue = {
        val userExists = User.find(username)
        if (!userExists.isDefined)
            Json.obj("error" -> "User does not exist.")
        else {
            val user = userExists.get
            toJson(user, self, email)
        }
    }

    def toJson(user: User, self: Option[User] = None, email: Boolean = false): JsValue = {
        var newJson = Json.obj()
        if (email)
            newJson = newJson.as[JsObject] +
                ("name" -> Json.toJson(user.name)) +
                ("username" -> Json.toJson(user.username)) +
                ("user_id" -> Json.toJson(user.user_id)) +
                ("reputation" -> Json.toJson(user.reputation)) +
                ("photo" -> Json.toJson(user.photo)) +
                ("bio" -> Json.toJson(user.bio)) +
                ("email" -> Json.toJson(user.email)) +
                ("board_count" -> Json.toJson(User.getBoardIDs(user.user_id.get).size))
        else
            newJson = newJson.as[JsObject] +
                ("name" -> Json.toJson(user.name)) +
                ("username" -> Json.toJson(user.username)) +
                ("user_id" -> Json.toJson(user.user_id)) +
                ("reputation" -> Json.toJson(user.reputation)) +
                ("photo" -> Json.toJson(user.photo)) +
                ("bio" -> Json.toJson(user.bio)) +
                ("board_count" -> Json.toJson(User.getBoardIDs(user.user_id.get).size))
        if (self.isDefined && user.user_id.get == self.get.user_id.get)
            newJson = newJson.as[JsObject] + ("self" -> Json.toJson(true))
        else
            newJson = newJson.as[JsObject] + ("self" -> Json.toJson(false))
        newJson
    }

}
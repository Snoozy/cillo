package com.cillo.core.data.db.models

import anorm.SqlParser._
import anorm._
import com.cillo.core.data.cache.Session
import com.cillo.core.data.db.models.Comment.commentParser
import com.cillo.core.data.db.models.Post.postParser
import com.cillo.utils.Etc.makeDigest
import play.api.Play.current
import com.cillo.core.data.Constants
import org.apache.commons.lang3.RandomStringUtils
import play.api.db._
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
) {
    lazy val admin: Boolean = {
        if (token.isDefined) {
            new Session(token.get).get("admin").isDefined
        } else
            false
    }
}

object User {

    private val DefaultPhotoString = "default"
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

    def findByEmail(email: String): Option[User] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM user WHERE email = {email}").on('email -> email).as(userParser.singleOpt)
        }
    }

    def checkUsername(s: String): Boolean = {
        !find(s).isDefined
    }

    def checkEmail(s: String): Boolean = {
        !findByEmail(s).isDefined
    }

    /**
     * Generates valid username from valid email
     *
     * @param s Valid email
     * @return Valid username for user.
     */
    def genUsername(s: String, backup: String = ""): String = {
        val gen = genRawUsername(s)
        if (!gen.isDefined) {
            if (backup != "") {
                val b = genRawUsername(backup)
                if (!b.isDefined) {
                    genRawUsername(RandomStringUtils.random(14)).get
                } else
                    b.get
            } else
                genRawUsername(RandomStringUtils.random(14)).get
        } else
            gen.get
    }

    private def genRawUsername(s: String): Option[String] = {
        val raw = s.replace(" ", "")
        val i = {
            val t = raw.indexOf('@')
            if (t > 0 && t < (Constants.MaxUsernameLength + 1)) {
                t
            } else {
                Constants.MaxUsernameLength
            }
        }
        val parsed = raw.substring(0, i)
        if (!checkUsername(parsed)) {
            var count = 0
            val rand = "%04d".format(scala.util.Random.nextInt(1000))
            var check = parsed
            if (check.length > (Constants.MaxUsernameLength - 1)) {
                check = check.substring(0, Constants.MaxUsernameLength - 4)
            }
            check = check + rand
            while (!checkUsername(check)) {
                if (count > 4)
                    return None
                check = check.substring(0, Constants.MaxUsernameLength - 4) + "%04d".format(scala.util.Random.nextInt(1000))
                count += 1
            }
            Some(check)
        } else Some(parsed)
    }

    def create(username: String, name: String, password: String, email: String, bio: Option[String] = None, pic: Option[Int] = None): Option[Long] = {
        val time = System.currentTimeMillis()

        val pass = {
            if (password != "")
                makeDigest(password)
            else password
        }

        if (checkEmail(email)) {
            DB.withConnection { implicit connection =>
                SQL("INSERT INTO user (username, name, password, email, bio, time, reputation, photo) VALUES ({username}, {name}," +
                    " {password}, {email}, {bio}, {time}, 0, {pic})").on('username -> username, 'name -> name,
                        'password -> pass, 'email -> email, 'bio -> bio.getOrElse(""), 'time -> time, 'pic -> pic).executeInsert()
            }
        } else
            None
    }

    def update(user_id: Int, name: String, username: String, bio: String, pic: Int) = {
        DB.withConnection { implicit connection =>
            SQL("UPDATE user SET name = {name}, username = {username}, bio = {bio}, photo = {photo} WHERE user_id = {user}")
                .on('name -> name, 'photo -> pic, 'user -> user_id, 'bio -> bio, 'username -> username).executeUpdate()
        }
    }

    def updatePassword(user_id: Int, password: String) = {
        DB.withConnection { implicit connection =>
            val pass = makeDigest(password)
            SQL("UPDATE user SET password = {pass} WHERE user_id = {user}").on('pass -> pass, 'user -> user_id).executeUpdate()
        }
    }

    def getPosts(user_id: Int, limit: Int = Post.DefaultPageSize): Seq[Post] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM post WHERE user_id = {id} ORDER BY time DESC LIMIT {limit}")
                .on('id -> user_id, 'limit -> limit).as(postParser *)
        }
    }

    def getPostsPaged(user_id: Int, after: Int, limit: Int = Post.DefaultPageSize): Seq[Post] = {
        DB.withConnection { implicit connection =>
            val afterPost = Post.find(after)
            if (afterPost.isDefined) {
                val posts = SQL("SELECT * FROM post WHERE time < {time} AND user_id = {id} ORDER BY time DESC LIMIT {limit}")
                    .on('id -> user_id, 'time -> afterPost.get.time, 'limit -> limit).as(postParser *)
                if (posts.length < limit)
                    posts
                else
                    posts.takeRight(limit)
            } else {
                Seq()
            }
        }
    }

    def getFeed(user_id: Int, limit: Int = Post.DefaultPageSize): Seq[Post] = {
        DB.withConnection { implicit connection =>
            val board_ids = User.getBoardIDs(user_id)
            if (board_ids.isEmpty)
                Seq()
            else {
                val posts = SQL("SELECT * FROM post WHERE board_id IN ({board_ids}) ORDER BY time DESC LIMIT {limit}")
                    .on('board_ids -> board_ids, 'limit -> limit).as(postParser *)
                posts
            }
        }
    }

    def getFeedPaged(user_id: Int, after: Int, limit: Int = Post.DefaultPageSize): Seq[Post] = {
        DB.withConnection { implicit connection =>
            val board_ids = User.getBoardIDs(user_id)
            if (board_ids.isEmpty)
                Seq()
            else {
                val afterPost = Post.find(after)
                if (afterPost.isDefined) {
                    val posts = SQL("SELECT * FROM post WHERE time < {time} AND board_id IN ({board_ids}) ORDER BY post_id DESC LIMIT {limit}")
                        .on('board_ids -> board_ids, 'time -> afterPost.get.time, 'limit -> limit).as(postParser *)
                    if (posts.length < limit)
                        posts
                    else
                        posts.takeRight(limit)
                } else {
                    Seq()
                }
            }
        }
    }

    def getComments(user_id: Int, limit: Int = Post.DefaultPageSize): Seq[Comment] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM comment WHERE user_id = {user_id} ORDER BY comment_id DESC LIMIT {limit}").on('user_id -> user_id, 'limit -> limit).as(commentParser *)
        }
    }

    def getCommentsPaged(user_id: Int, after: Int, limit: Int = Post.DefaultPageSize): Seq[Comment] = {
        DB.withConnection { implicit connection =>
            val afterCom = Comment.find(after)
            if (afterCom.isDefined) {
                val comments = SQL("SELECT * FROM comment WHERE time < {time} AND user_id = {id} ORDER BY comment_id DESC LIMIT {limit}")
                    .on('id -> user_id, 'time -> afterCom.get.time, 'limit -> limit).as(commentParser *)
                if (comments.length < limit)
                    comments
                else
                    comments.takeRight(limit)
            } else {
                Seq()
            }
        }
    }

    def userIsFollowing(user_id: Int, board_id: Int): Boolean = {
        DB.withConnection { implicit connection =>
            val count = SQL("SELECT COUNT(*) FROM user_to_board WHERE user_id = {user_id} AND board_id = {board_id}")
                .on('user_id -> user_id, 'board_id -> board_id).as(scalar[Long].single)
            count > 0L
        }
    }

    def getPostsCount(user_id: Int): Int = {
        DB.withConnection { implicit connection =>
            SQL("SELECT COUNT(*) FROM post WHERE user_id = {user}").on('user -> user_id).as(scalar[Long].single).toInt
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

    def toJson(user: User, self: Option[User] = None, email: Boolean = false, anon: Boolean = false): JsValue = {
        var newJson = Json.obj()
        if (!anon)
            newJson = newJson.as[JsObject] +
                ("name" -> Json.toJson(user.name)) +
                ("username" -> Json.toJson(user.username)) +
                ("user_id" -> Json.toJson(user.user_id)) +
                ("reputation" -> Json.toJson(user.reputation)) +
                ("photo" -> Json.toJson(user.photo)) +
                ("bio" -> Json.toJson(user.bio)) +
                ("board_count" -> Json.toJson(User.getBoardIDs(user.user_id.get).size))
        else {
            newJson = newJson.as[JsObject] +
                ("name" -> Json.toJson("Anonymous")) +
                ("username" -> Json.toJson("")) +
                ("user_id" -> Json.toJson("")) +
                ("reputation" -> Json.toJson("")) +
                ("photo" -> Json.toJson("https://static.cillo.co/image/anon")) +
                ("bio" -> Json.toJson("")) +
                ("board_count" -> Json.toJson(0))

        }
        if (email) {
            newJson = newJson.as[JsObject] + ("email" -> Json.toJson(user.email))
        }
        if (self.isDefined && user.user_id.get == self.get.user_id.get)
            newJson = newJson.as[JsObject] + ("self" -> Json.toJson(true))
        else
            newJson = newJson.as[JsObject] + ("self" -> Json.toJson(false))
        newJson
    }

}
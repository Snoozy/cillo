package com.cillo.core.data.db.models

import anorm.SqlParser._
import anorm._
import com.cillo.utils.EncodeDecode
import play.api.Play.current
import play.api.db._
import play.api.libs.json.{JsObject, JsValue, Json}

case class Comment (
    comment_id: Option[Int],
    post_id: Int,
    user_id: Int,
    data: String,
    time: Long,
    path: String,
    votes: Int,
    status: Int
)

object Comment {

    private[models] val commentParser: RowParser[Comment] = {
        get[Option[Int]]("comment_id") ~
            get[Int]("post_id") ~
            get[Int]("user_id") ~
            get[String]("data") ~
            get[Long]("time") ~
            get[String]("path") ~
            get[Int]("votes") ~
            get[Int]("status") map {
            case comment_id ~ post_id ~ user_id ~ data ~ time ~ path ~ votes ~ status =>
                Comment(comment_id, post_id, user_id ,data, time, path, votes, status)
        }
    }

    def find(id: Int): Option[Comment] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM comment WHERE comment_id = {id}").on('id -> id).as(commentParser.singleOpt)
        }
    }

    def delete(id: Int): Boolean = {
        DB.withConnection { implicit connection =>
            val res = SQL("UPDATE comment SET status = 1 WHERE comment_id = {id}").on('id -> id).executeUpdate()
            res > 0
        }
    }

    def create(post_id: Int, user_id: Int, data: String, parent_id: Option[Int]): Option[Long] = {
        val path = parent_id match {
            case None => ""
            case Some(_) =>
                val parent = Comment.find(parent_id.get).getOrElse(return None)
                parent.path + "/" + EncodeDecode.encodeNum(parent.comment_id.get)

        }

        val time = System.currentTimeMillis()

        DB.withConnection { implicit connection =>
            val ret = SQL("INSERT INTO comment (post_id, user_id, data, path, time, votes) VALUES ({post_id}, {user_id}, {data}," +
                "{path}, {time}, 0)").on('post_id -> post_id, 'user_id -> user_id, 'path -> path, 'data -> data, 'time -> time).executeInsert()
            SQL("UPDATE post SET comment_count = comment_count + 1 WHERE post_id = {post_id}")
                .on('post_id -> post_id).executeUpdate()
            ret
        }
    }

    def toJson(comment: Comment, user: Option[User] = None): JsValue = {
        Json.obj(
            "comment_id" -> comment.comment_id.get,
            "user" -> User.toJsonByUserID(comment.user_id, self = user),
            "content" -> (if(comment.status != 1) Json.toJson(comment.data) else Json.toJson("")),
            "time" -> Json.toJson(comment.time),
            "votes" -> Json.toJson(comment.votes),
            "deleted" -> Json.toJson(comment.status == 1)
        )
    }

    def toJsonSeqWithUser(comments: Seq[Comment], user: Option[User]): JsValue = {
        var json = Json.arr()
        comments.foreach { comment =>
           json = json.+:(toJson(comment, user).as[JsObject] + ("post" -> Post.toJsonSingle(Post.find(comment.post_id).get, user)))
        }
        json
    }

}
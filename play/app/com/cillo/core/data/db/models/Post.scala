package com.cillo.core.data.db.models

import anorm.SqlParser._
import anorm._
import com.cillo.core.data.db.models.Comment.commentParser
import com.cillo.utils.Etc.{bool2int, int2bool}
import com.cillo.core.web.views.html.components
import play.api.Play.current
import play.api.db._
import play.api.libs.json._

case class Post (
    post_id: Option[Int],
    user_id: Int,
    title: Option[String],
    data: String,
    board_id: Int,
    repost_id: Option[Int],
    votes: Int,
    comment_count: Int,
    time: Long,
    post_type: Int,
    media: Seq[Int]
)

object Post {

    val DefaultPageSize = 20

    private[models] val postParser: RowParser[Post] = {
        get[Option[Int]]("post_id") ~
            get[Int]("user_id") ~
            get[Option[String]]("title") ~
            get[String]("data") ~
            get[Int]("board_id") ~
            get[Option[Int]]("repost_id") ~
            get[Int]("votes") ~
            get[Int]("comment_count") ~
            get[Long]("time") ~
            get[Int]("post_type") ~
            get[String]("media") map {
            case post_id ~ user_id ~ title ~ data ~ board_id ~ repost_id ~ votes ~ comment_count ~ time ~ post_type ~ media =>
                val media_ids = media.split("~").filter(_ != "").map(_.toInt)
                Post(post_id, user_id, title, data, board_id, repost_id, votes, comment_count, time, post_type, media_ids)
        }
    }

    def find(id: Int): Option[Post] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM post WHERE post_id = {id}").on('id -> id).as(postParser.singleOpt)
        }
    }

    def createSimplePost(user_id: Int, title: Option[String], data: String, board_id: Int, repost_id: Option[Int] = None): Option[Long] = {
        // checking that data is a number if post is a repost
        if (repost_id.isDefined) {
            val repostExists = Post.find(repost_id.get)
            if (!repostExists.isDefined)
                return None
        }

        val titleParsed: Option[String] = {
            if (title.isDefined) {
                Some(title.get.replace("\n", ""))
            } else {
                None
            }
        }

        val time = System.currentTimeMillis()

        DB.withConnection { implicit connection =>
            SQL("INSERT INTO post (user_id, title, data, board_id, repost_id, votes, time, post_type, comment_count) values ({user_id}, {title}, {data}," +
                " {board_id}, {repost_id}, 0, {time}, 0, 0)").on('user_id -> user_id, 'title -> titleParsed, 'data -> data,
                    'board_id -> board_id, 'repost_id -> repost_id, 'time -> time).executeInsert()
        }
    }

    def createMediaPost(user_id: Int, title: Option[String], data: String, board_id: Int, media_ids: Seq[Int]): Option[Long] = {
        val time = System.currentTimeMillis()

        media_ids.foreach(id => if (!Media.find(id).isDefined) return None)

        val media_string = media_ids.mkString("~")

        val titleParsed: Option[String] = {
            if (title.isDefined) {
                Some(title.get.replace("\n", ""))
            } else {
                None
            }
        }

        DB.withConnection { implicit connection =>
            SQL("INSERT INTO post (user_id, title, data, board_id, votes, time, post_type, comment_count, media) values ({user_id}, {title}, {data}," +
                " {board_id}, 0, {time}, 0, 1, {media})").on('user_id -> user_id, 'title -> titleParsed, 'data -> data,
                    'board_id -> board_id, 'time -> time, 'media -> media_string).executeInsert()
        }
    }

    def deletePost(post_id: Int): Boolean = {
        DB.withConnection { implicit connection =>
            val commentIds: Seq[Int] = SQL("SELECT comment_id FROM comment WHERE post_id = {post_id}").on('post_id -> post_id).as(scalar[Int].*)
            if (commentIds.nonEmpty) {
                SQL("DELETE FROM comment_vote WHERE comment_id IN ({ids})").on('ids -> commentIds).executeUpdate()
            }
            SQL("DELETE FROM comment WHERE post_id = {post_id}").on('post_id -> post_id).executeUpdate()
            SQL("DELETE FROM post_vote WHERE post_id = {post_id}").on('post_id -> post_id).executeUpdate()
            SQL("DELETE FROM post WHERE post_id = {post_id}").on('post_id -> post_id).executeUpdate()
        }
    }

    def userHasReposted(user_id: Int, post_id: Int): Boolean = {
        DB.withConnection { implicit connection =>
            val post = SQL("SELECT * FROM post WHERE repost_id = {post} AND user_id = {user}")
                .on('post -> post_id, 'user -> user_id).as(postParser *)
            post.nonEmpty
        }
    }

    def getRootComments(post_id: Int, limit: Int = 5): Seq[Comment] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM comment WHERE post_id = {post} AND path = '' ORDER BY votes LIMIT {LIMIT}")
                .on('post -> post_id, 'limit -> limit).as(commentParser *)
        }
    }

    def toJson(posts: Seq[Post]): JsValue = {
        toJsonWithUser(posts, None)
    }

    def toJsonSingle(post: Post, user: Option[User]): JsValue = {
        var newPost = Json.obj(
            "post_id" -> Json.toJson(post.post_id)
        )

        if (post.repost_id.isDefined) {
            val reposted_post = Post.find(post.repost_id.get)
            if (reposted_post.isDefined) {
                val board = Board.find(reposted_post.get.board_id)
                val poster = User.find(reposted_post.get.user_id)
                val reposter = User.find(post.user_id)
                val repost_board = Board.find(post.board_id)
                if (board.isDefined && poster.isDefined && reposter.isDefined && repost_board.isDefined) {
                    val anon = repost_board.get.privacy == 1
                    newPost = newPost.as[JsObject] +
                        ("repost" -> Post.toJsonSingle(reposted_post.get, user)) +
                        ("content" -> Json.toJson(post.data)) +
                        ("title" -> Json.toJson(post.title)) +
                        ("board" -> Board.toJson(repost_board.get)) +
                        ("user" -> User.toJson(reposter.get, self = user, anon = anon)) +
                        ("time" -> Json.toJson(post.time)) +
                        ("votes" -> Json.toJson(post.votes)) +
                        ("comment_count" -> Json.toJson(post.comment_count))
                }
            }
        } else {
            val board = Board.find(post.board_id)
            val poster = User.find(post.user_id)
            if (board.isDefined && poster.isDefined) {
                val anon = board.get.privacy == 1
                newPost = newPost.as[JsObject] +
                    ("content" -> Json.toJson(post.data)) +
                    ("title" -> Json.toJson(post.title)) +
                    ("board" -> Board.toJson(board.get)) +
                    ("user" -> User.toJson(poster.get, self = user, anon = anon)) +
                    ("time" -> Json.toJson(post.time)) +
                    ("votes" -> Json.toJson(post.votes)) +
                    ("comment_count" -> Json.toJson(post.comment_count))
            }
        }
        if (user.isDefined) {
            newPost = newPost.as[JsObject] + ("vote_value" -> Json.toJson(PostVote.getPostVoteValue(post.post_id.get, user.get.user_id.get)))
        }
        if (post.media.nonEmpty) {
            var mediaArr = Json.arr()
            post.media.foreach { media_id =>
                val media = Media.find(media_id)
                if (media.isDefined) {
                    mediaArr = mediaArr.+:(Json.toJson(Media.BaseMediaURL + media.get.media_name))
                }
            }
            newPost = newPost.as[JsObject] + ("media" -> mediaArr)
        }
        newPost
    }

    def toJsonWithUser(posts: Seq[Post], user: Option[User]): JsValue = {
        var json = Json.arr()
        posts.foreach { post =>
            json = json.+:(toJsonSingle(post, user))
        }
        json
    }

    def toHTMLWIthUser(posts: Seq[Post], user: Option[User]): String = {
        posts.map { post =>
            components.post(post, user)()
        }.mkString("")
    }

}
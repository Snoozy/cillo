package com.cillo.core.data.db.models

import anorm.SqlParser._
import anorm._
import com.cillo.core.data.db.models.Comment.commentParser
import com.cillo.utils.Etc.{bool2int, int2bool}
import play.api.Play.current
import play.api.db._
import play.api.libs.json._

case class Post (
    post_id: Option[Int],
    user_id: Int,
    title: Option[String],
    data: String,
    group_id: Int,
    repost: Int,
    votes: Int,
    comment_count: Int,
    time: Long,
    post_type: Int,
    media: Array[Int]
)

object Post {

    val DefaultPageSize = 20

    private[models] val postParser: RowParser[Post] = {
        get[Option[Int]]("post_id") ~
            get[Int]("user_id") ~
            get[Option[String]]("title") ~
            get[String]("data") ~
            get[Int]("group_id") ~
            get[Int]("repost") ~
            get[Int]("votes") ~
            get[Int]("comment_count") ~
            get[Long]("time") ~
            get[Int]("post_type") ~
            get[String]("media") map {
            case post_id ~ user_id ~ title ~ data ~ group_id ~ repost ~ votes ~ comment_count ~ time ~ post_type ~ media =>
                val media_ids = media.split(",").filter(_ != "").map(_.toInt)
                Post(post_id, user_id, title, data, group_id, repost, votes, comment_count, time, post_type, media_ids)
        }
    }

    def find(id: Int): Option[Post] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM post WHERE post_id = {id}").on('id -> id).as(postParser.singleOpt)
        }
    }

    def createSimplePost(user_id: Int, title: Option[String], data: String, group_id: Int, repost: Boolean): Option[Long] = {
        // checking that data is a number if post is a repost
        if (repost && !data.forall(Character.isDigit))
            return None

        if (repost) {
            val repostExists = Post.find(data.toInt)
            if (!repostExists.isDefined)
                return None
        }

        val time = System.currentTimeMillis()

        DB.withConnection { implicit connection =>
            SQL("INSERT INTO post (user_id, title, data, group_id, repost, votes, time, post_type, comment_count) values ({user_id}, {title}, {data}," +
                " {group_id}, {repost}, 0, {time}, 0, 0)").on('user_id -> user_id, 'title -> title, 'data -> data,
                    'group_id -> group_id, 'repost -> (repost:Int), 'time -> time).executeInsert()
        }
    }

    def createMediaPost(user_id: Int, title: Option[String], data: String, group_id: Int, media_ids: Array[Int]): Option[Long] = {
        val time = System.currentTimeMillis()

        val media_string = media_ids.mkString(",")

        DB.withConnection { implicit connection =>
            SQL("INSERT INTO post (user_id, title, data, group_id, repost, votes, time, post_type, comment_count, media) values ({user_id}, {title}, {data}," +
                " {group_id}, {repost}, 0, {time}, 0, 1, {media})").on('user_id -> user_id, 'title -> title, 'data -> data,
                    'group_id -> group_id, 'repost -> false, 'time -> time, 'media -> media_string).executeInsert()
        }
    }

    def userHasReposted(user_id: Int, post_id: Int): Boolean = {
        DB.withConnection { implicit connection =>
            val post = SQL("SELECT * FROM post WHERE data = {post} AND user_id = {user}")
                .on('post -> post_id, 'user -> user_id).as(postParser.singleOpt)
            post.isDefined
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
            "post_id" -> Json.toJson(post.post_id),
            "repost" -> Json.toJson(post.repost:Boolean)
        )

        if (post.repost:Boolean) {
            val reposted_post = Post.find(post.data.toInt)
            if (reposted_post.isDefined) {
                val group = Group.find(reposted_post.get.group_id)
                val poster = User.find(reposted_post.get.user_id)
                val reposter = User.find(post.user_id)
                val repost_group = Group.find(post.group_id)
                if (group.isDefined && poster.isDefined && reposter.isDefined && repost_group.isDefined) {
                    newPost = newPost.as[JsObject] +
                        ("repost_user" -> User.toJson(reposter.get, self = user)) +
                        ("repost_group" -> Group.toJson(repost_group.get)) +
                        ("repost_id" -> Json.toJson(post.data.toInt)) +
                        ("content" -> Json.toJson(reposted_post.get.data)) +
                        ("title" -> Json.toJson(reposted_post.get.title)) +
                        ("group" -> Group.toJson(group.get)) +
                        ("user" -> User.toJson(poster.get, self = user)) +
                        ("time" -> Json.toJson(reposted_post.get.time)) +
                        ("votes" -> Json.toJson(reposted_post.get.votes)) +
                        ("comment_count" -> Json.toJson(reposted_post.get.comment_count))
                }
            }
        } else {
            val group = Group.find(post.group_id)
            val poster = User.find(post.user_id)
            if (group.isDefined && poster.isDefined) {
                newPost = newPost.as[JsObject] +
                    ("content" -> Json.toJson(post.data)) +
                    ("title" -> Json.toJson(post.title)) +
                    ("group" -> Group.toJson(group.get)) +
                    ("user" -> User.toJson(poster.get, self = user)) +
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

}
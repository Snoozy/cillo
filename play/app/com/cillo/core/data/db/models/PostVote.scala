package com.cillo.core.data.db.models

import anorm.SqlParser._
import anorm._
import play.api.Play.current
import play.api.db._

case class PostVote (
    post_vote_id: Option[Int],
    post_id: Int,
    user_id: Int,
    value: Int,
    time: BigInt
)

object PostVote {

    private[models] val postVoteParser: RowParser[PostVote] = {
        get[Option[Int]]("post_vote_id") ~
            get[Int]("post_id") ~
            get[Int]("user_id") ~
            get[Int]("value") ~
            get[java.math.BigInteger]("time") map {
            case post_vote_id ~ post_id ~ user_id ~ value ~ time =>
                PostVote(post_vote_id, post_id, user_id, value, time)
        }
    }

    def findByPostAndUser(post_id: Int, user_id: Int): Option[PostVote] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM post_vote WHERE user_id = {user} AND post_id = {post}").on('user -> user_id, 'post -> post_id)
                .as(postVoteParser.singleOpt)
        }
    }

    def votePost(post_id: Int, user_id: Int, value: Int): Boolean = {
        val post = Post.find(post_id)
        val voteExists = PostVote.findByPostAndUser(post_id, user_id)
        if (post.isDefined && (value == -1 || value == 1) && !voteExists.isDefined) {
            DB.withConnection { implicit connection =>
                if (value == 1 && (user_id != post.get.user_id)) {
                    SQL("UPDATE user SET reputation = reputation + 20 WHERE user_id = {user}")
                        .on('user -> post.get.user_id).executeUpdate()
                } else if (value == -1 && (user_id != post.get.user_id)) {
                    SQL("UPDATE user SET reputation = reputation - 20 WHERE user_id = {user}")
                        .on('user -> post.get.user_id).executeUpdate()
                }
                val time = System.currentTimeMillis()
                SQL("INSERT INTO post_vote (post_id, user_id, value, time) VALUES ({post_id}, {user_id}, {value}, {time})")
                    .on('post_id -> post_id, 'user_id -> user_id, 'value -> value, 'time -> time).executeInsert()
                SQL("UPDATE post SET votes = votes + {value} WHERE post_id = {post_id}")
                    .on('value -> value, 'post_id -> post_id).executeUpdate()
            }
            true
        } else if (voteExists.isDefined && (value == -1 || value == 1) && post.isDefined) {
            if (voteExists.get.value != value) {
                DB.withConnection { implicit connection =>
                    if (value == 1 && (user_id != post.get.user_id)) {
                        SQL("UPDATE user SET reputation = reputation + 40 WHERE user_id = {user}")
                            .on('user -> post.get.user_id).executeUpdate()
                    } else if (value == -1 && (user_id != post.get.user_id)) {
                        SQL("UPDATE user SET reputation = reputation - 40 WHERE user_id = {user}")
                            .on('user -> post.get.user_id).executeUpdate()
                    }
                    val time = System.currentTimeMillis()
                    SQL("UPDATE post_vote SET value = {value}, time = {time} WHERE post_id = {post_id} AND user_id = {user_id}")
                        .on('value -> value, 'user_id -> user_id, 'post_id -> post_id, 'time -> time).executeUpdate()
                    SQL("UPDATE post SET votes = votes + {value} WHERE post_id = {post_id}")
                        .on('value -> 2 * value, 'post_id -> post_id).executeUpdate()
                }
                true
            } else false
        } else false
    }

    def getPostVotesByUserAndPost(user_id: Int, post_ids: Seq[Int]): Seq[PostVote] = {
        DB.withConnection { implicit connection =>
            if (post_ids.isEmpty)
                return List()
            SQL("SELECT * FROM post_vote WHERE post_id IN ({post_id}) AND user_id = {user_id}")
                .on('post_id -> post_ids, 'user_id -> user_id).as(postVoteParser *)
        }
    }

    def getPostVoteValue(post_id: Int, user_id: Int): Int = {
        val postVote = PostVote.findByPostAndUser(post_id, user_id)
        if (postVote.isDefined)
            postVote.get.value
        else 0
    }

}
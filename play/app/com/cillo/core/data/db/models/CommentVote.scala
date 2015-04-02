package com.cillo.core.data.db.models

import anorm.SqlParser._
import anorm._
import play.api.Play.current
import play.api.db._

case class CommentVote (
    comment_vote_id: Option[Int],
    comment_id: Int,
    user_id: Int,
    value: Int,
    time: BigInt
)

object CommentVote {

    private[models] val commentVoteParser: RowParser[CommentVote] = {
        get[Option[Int]]("comment_vote_id") ~
            get[Int]("comment_id") ~
            get[Int]("user_id") ~
            get[Int]("value") ~
            get[java.math.BigInteger]("time") map {
            case comment_vote_id ~ comment_id ~ user_id ~ value ~ time =>
                CommentVote(comment_vote_id, comment_id, user_id, value, time)
        }
    }

    def findByCommentAndUser(comment_id: Int, user_id: Int): Option[CommentVote] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM comment_vote WHERE user_id = {user} AND comment_id = {comment}").on('user -> user_id, 'comment -> comment_id)
                .as(commentVoteParser.singleOpt)
        }
    }

    def voteComment(comment_id: Int, user_id: Int, value: Int): Boolean = {
        val comment = Comment.find(comment_id)
        val voteExists = CommentVote.findByCommentAndUser(comment_id, user_id)
        if (comment.isDefined && (value == -1 || value == 1) && !voteExists.isDefined) {
            DB.withConnection { implicit connection =>
                if (value == 1) {
                    SQL("UPDATE user SET reputation = reputation + 10 WHERE user_id = {user}")
                        .on('user -> user_id).executeUpdate()
                }
                val time = System.currentTimeMillis()
                SQL("INSERT INTO comment_vote (comment_id, user_id, value, time) VALUES ({comment_id}, {user_id}, {value}, {time})")
                    .on('comment_id -> comment_id, 'user_id -> user_id, 'value -> value, 'time -> time).executeInsert()
                SQL("UPDATE comment SET votes = votes + {value} WHERE comment_id = {comment_id}")
                    .on('value -> value, 'comment_id -> comment_id).executeUpdate()
            }
            true
        } else if (voteExists.isDefined && (value == -1 || value == 1) && comment.isDefined) {
            if (voteExists.get.value != value) {
                DB.withConnection { implicit connection =>
                    if (value == 1) {
                        SQL("UPDATE user SET reputation = reputation + 10 WHERE user_id = {user}")
                            .on('user -> user_id).executeUpdate()
                    } else if (value == -1) {
                        SQL("UPDATE user SET reputation = reputation - 10 WHERE user_id = {user}")
                            .on('user -> user_id).executeUpdate()
                    }
                    val time = System.currentTimeMillis()
                    SQL("UPDATE comment_vote SET value = {value}, time = {time} WHERE comment_id = {comment_id} AND user_id = {user_id}")
                        .on('value -> value, 'user_id -> user_id, 'comment_id -> comment_id, 'time -> time).executeUpdate()
                    SQL("UPDATE comment SET votes = votes + {value} WHERE comment_id = {comment_id}")
                        .on('value -> 2 * value, 'comment_id -> comment_id).executeUpdate()
                }
                true
            } else false
        } else false
    }

    def getCommentVotesByUserAndPost(user_id: Int, comment_ids: Seq[Int]): Seq[CommentVote] = {
        DB.withConnection { implicit connection =>
            if (comment_ids.isEmpty)
                return List()
            SQL("SELECT * FROM comment_vote WHERE post_id IN ({comment_id}) AND user_id = {user_id}")
                .on('comment -> comment_ids, 'user_id -> user_id).as(commentVoteParser *)
        }
    }

    def getCommentVoteValue(user_id: Int, comment_id: Int): Int = {
        val commentVote = CommentVote.findByCommentAndUser(comment_id, user_id)
        if (commentVote.isDefined)
            commentVote.get.value
        else 0
    }

}
package com.cillo.core.data.db.models

import anorm._
import com.cillo.core.data.db.models.Comment.commentParser
import com.cillo.utils.EncodeDecode
import play.api.Play.current
import com.cillo.core.web.views.html.components
import play.api.db._
import play.api.libs.json.{JsValue, _}

case class CommentTree(rootComments: Seq[CommentTreeNode], post_id: Int)

case class CommentTreeNode(comment: Comment, children: Seq[CommentTreeNode])

object CommentTree {

    def getPostCommentsTop(post_id: Int): CommentTree = {
        DB.withConnection { implicit connection =>
            val comments = SQL("SELECT * FROM comment WHERE post_id = {id}").on('id -> post_id).as(commentParser *)
            commentsByTop(comments)
        }
    }

    def commentTreeToJson(tree: CommentTree, user: Option[User] = None): JsValue = {
        Json.obj("post_id" -> tree.post_id, "comments" -> commentTreeJsonRecurse(tree.rootComments, user))
    }

    private def commentsByTop(comments: Seq[Comment]): CommentTree = {
        if (comments.isEmpty) {
            CommentTree(Seq(), 0)
        } else {
            val rootComments = comments.par.filter(_.path == "").toList
            CommentTree(sortTopRecurse(comments, rootComments), comments.head.post_id)
        }
    }

    private def sortTopRecurse(comments: Seq[Comment], currentLevel: Seq[Comment]): Seq[CommentTreeNode] = {
        if (currentLevel.isEmpty)
            List()
        else {
            currentLevel.sortBy(- _.votes).par.map { currentComment =>
                CommentTreeNode(currentComment, sortTopRecurse(comments, getChildren(currentComment, comments)))
            }.toList
        }
    }

    private def getChildren(comment: Comment, comments: Seq[Comment]): Seq[Comment] = {
        comments.par.filter { currentComment =>
            currentComment.path.equals(comment.path + "/" + EncodeDecode.encodeNum(comment.comment_id.get))
        }.toList
    }

    private def commentTreeJsonRecurse(nodes: Seq[CommentTreeNode], user: Option[User]): JsValue = {
        var json = Json.arr()
        nodes.foreach { node =>
            val comment = node.comment
            var newComment: JsValue = Comment.toJson(comment).as[JsObject] + ("children" -> commentTreeJsonRecurse(node.children, user))
            if (user.isDefined)
                newComment = newComment.as[JsObject] + ("vote_value" -> Json.toJson(CommentVote.getCommentVoteValue(user.get.user_id.get, comment.comment_id.get)))
            else
                newComment = newComment.as[JsObject] + ("vote_value" -> Json.toJson(0))
            json = json.+:(newComment) // Adds it to json array
        }
        json
    }

}
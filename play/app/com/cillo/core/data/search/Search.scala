package com.cillo.core.data.search

import anorm.SqlParser._
import anorm._
import com.cillo.core.data.db.models.Board
import play.api.Play.current
import play.api.db._
import com.cillo.core.data.db.models.Board.boardParser


object Search {

    def boardSearch(query: String): Seq[Board] = {
        val natSearch = naturalBoardSearch(query)
        if (natSearch.length < 5) {
            expandedBoardSearch(query)
        } else {
            natSearch
        }
    }

    def autoComplete(query: String): Seq[Board] = {
        expandedBoardSearch(query)
    }

    /*
        Expanded board search with query expansion
     */
    private def expandedBoardSearch(query: String): Seq[Board] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM board WHERE MATCH(name) AGAINST({query} IN NATURAL LANGUAGE MODE WITH QUERY EXPANSION)")
                .on('query -> query).as(boardParser *)
        }
    }

    /*
        Search with just natural language mode on
     */
    private def naturalBoardSearch(query: String): Seq[Board] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM board WHERE MATCH(name) AGAINST({query} IN NATURAL LANGUAGE MODE)")
                .on('query -> query).as(boardParser *)
        }
    }

}
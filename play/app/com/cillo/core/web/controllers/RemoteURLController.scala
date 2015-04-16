package com.cillo.core.web.controllers

import play.api.Play
import play.api.Play.current
import play.api.mvc._

object RemoteURLController extends Controller {

    private val CdnUrl = "https://static.cillo.co/"
    private lazy val staticPrefix = Play.current.configuration.getString("static.prefix").get

    def js = {
        Play.isProd match {
            case true => "<script type=\"text/javascript\" src=\"" + CdnUrl + "js/" + staticPrefix + "-bundle.js" + "\"></script>"
            case false =>
                val files = Play.getFile("public/js")
                files.list.toList.sortWith(_ < _).map { s =>
                    "<script type=\"text/javascript\" src=\"/assets/js/" + s + "\"></script>"
                }.mkString("\n")
        }
    }

    def css = {
        Play.isProd match {
            case true => "<link rel=\"stylesheet\" href=\"" + CdnUrl + "css/" + staticPrefix + "-bundle.css" + "\" />"
            case false =>
                val files = Play.getFile("public/css")
                files.list.map { s =>
                    "<link rel=\"stylesheet\" href=\"/assets/css/" + s + "\">"
                }.mkString("\n")
        }
    }

}
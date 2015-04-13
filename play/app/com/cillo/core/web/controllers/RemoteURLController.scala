package com.cillo.core.web.controllers

import play.api.Play
import play.api.Play.current
import play.api.mvc._

object RemoteURLController extends Controller {

    private val CdnUrl = "https://static.cillo.co/"
    private val staticPrefix = Play.current.configuration.getString("static.prefix")

    def url(file: String) = {
        Play.isProd match {
            case true =>
                val i = file.lastIndexOf('/')
                val path = file.substring(0, i + 1) + staticPrefix.get + "-" + file.substring(i + 1)
                CdnUrl + path
            case false => "/assets/" + file
        }
    }

}
package com.cillo.core.web.controllers

import play.api.Play
import play.api.Play.current
import play.api.mvc._

object RemoteURLController extends Controller {

    private val CdnUrl = "https://static.cillo.co/"

    def url(file: String) = {
        Play.isProd match {
            case true => CdnUrl + file
            case false => "/assets/" + file
        }
    }

}
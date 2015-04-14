package com.cillo.core.web.controllers

import play.api.Play
import play.api.Play.current
import play.api.mvc._

object RemoteURLController extends Controller {

    private val CdnUrl = "https://static.cillo.co/"
    private val staticPrefix = Play.current.configuration.getString("static.prefix")

    def jsurl(file: String) = {
        Play.isProd match {
            case true => CdnUrl + "js/" + staticPrefix.get + "-" + file
            case false => "/assets/js/" + file
        }
    }

    def cssurl(file: String) = {
        Play.isProd match {
            case true => CdnUrl + "css/" + staticPrefix.get + "-" + file
            case false => "/assets/css/" + file
        }
    }

}
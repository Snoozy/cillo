package com.cillo.core.api.controllers

import com.cillo.core.data.aws.S3
import com.cillo.utils.play.Auth._
import com.cillo.core.data.Constants
import play.api.libs.json.Json
import play.api.mvc._

/**
 * Controls the upload of media to through the API. Returns a media id that can be supplied with a post to attach
 * media.
 */

object MediaController extends Controller {

    /**
     * Uploads a media file and returns the media id for attachment to posts.
     *
     * @return Json with the media id of the media just uploaded.
     */
    def upload = ApiAuthAction { implicit user => implicit request =>
        val multiBody = request.body.asMultipartFormData
        if (multiBody.isDefined) {
            multiBody.get.file("media").map { media =>
                val mediaFile = media.ref.file
                if (mediaFile.length() > Constants.MaxMediaSize)
                    BadRequest(Json.obj("error" -> "Media upload too large. Code: 101"))
                else {
                    val id = S3.upload(mediaFile)
                    if (id.isEmpty)
                        BadRequest(Json.obj("error" -> "Upload failed. Code: 104"))
                    else {
                        Ok(Json.obj("media_id" -> Json.toJson(id.get)))
                    }
                }
            }.getOrElse(BadRequest(Json.obj("error" -> "Request format invalid.")))
        } else {
            BadRequest(Json.obj("error" -> "No file detected. Code: 103"))
        }
    }

}
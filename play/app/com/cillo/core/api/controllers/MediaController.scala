package com.cillo.core.api.controllers

import java.util.UUID

import com.cillo.core.data.aws.S3
import com.cillo.core.data.db.models.Media
import com.cillo.utils.play.Auth.AuthAction
import play.api.libs.json.Json
import play.api.mvc._
import play.api.Logger

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
    def upload = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required. Code: 10"))
            case Some(_) =>
                val multiBody = request.body.asMultipartFormData
                if (multiBody.isDefined) {
                    multiBody.get.file("media").map { media =>
                        val mediaFile = media.ref.file
                        if (mediaFile.length() > 3145728)
                            BadRequest(Json.obj("error" -> "Media upload too large. Code: 101"))
                        else {
                            val uuid = UUID.randomUUID()
                            if (!S3.upload(mediaFile, "image/" + uuid))
                                BadRequest(Json.obj("error" -> "Upload failed. Code: 104"))
                            else {
                                val DBMedia = Media.create(0, uuid.toString)
                                if (!DBMedia.isDefined)
                                    BadRequest(Json.obj("error" -> "Error uploading. Code: 102"))
                                else {
                                    Ok(Json.obj("media_id" -> Json.toJson(DBMedia.get)))
                                }
                            }
                        }
                    }.getOrElse(BadRequest(Json.obj("error" -> "Request format invalid.")))
                } else {
                    BadRequest(Json.obj("error" -> "No file detected. Code: 103"))
                }
        }
    }

}
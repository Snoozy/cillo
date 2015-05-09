package com.cillo.core.web.controllers

import java.util.UUID

import com.cillo.core.data.aws.S3
import com.cillo.core.data.db.models.Media
import com.cillo.utils.play.Auth.AuthAction
import play.api.libs.json.Json
import play.api.mvc._

/**
 * Controls the upload of media to through the API. Returns a media id that can be supplied with a post to attach
 * media.
 */

object MediaController extends Controller {

    private final val MediaIdentifier = "^media-\\S.*$".r

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
                    val resSeq = multiBody.get.files.filter(_.key.matches(MediaIdentifier.toString()))
                    if (resSeq.nonEmpty) {
                        val res = resSeq.map { media =>
                            val mediaFile = media.ref.file
                            if (mediaFile.length() > 5145728)
                                -1
                            else {
                                val id = S3.upload(mediaFile)
                                if (!id.isDefined)
                                    -1
                                else {
                                    val DBMedia = Media.create(0, id.get)
                                    if (!DBMedia.isDefined)
                                        -1
                                    else {
                                        DBMedia.get.toInt
                                    }
                                }
                            }
                        }
                        if (res.contains(-1)) {
                            BadRequest(Json.obj("error" -> "Error uploading images."))
                        } else {
                            Ok(Json.obj("media_ids" -> Json.toJson(res)))
                        }
                    } else {
                        BadRequest(Json.obj("error" -> "No images detected"))
                    }
                } else {
                    BadRequest(Json.obj("error" -> "No file detected. Code: 103"))
                }
        }
    }
}
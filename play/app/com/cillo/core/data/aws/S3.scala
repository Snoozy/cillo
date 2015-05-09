package com.cillo.core.data.aws

import java.io.{InputStream, File}
import java.util.UUID
import javax.imageio.ImageIO
import java.net.URL

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import com.amazonaws.{AmazonClientException, AmazonServiceException}
import play.api.Play
import com.sksamuel.scrimage._

object S3 {

    private final val bucketName = "cillo-static"

    def uploadImg(img: Image, profile: Boolean = false, original: Option[Image] = None, uuid: String = UUID.randomUUID().toString): Option[String] = {
        val aws_key = Play.current.configuration.getString("aws.key")
        val aws_secret = Play.current.configuration.getString("aws.secret")
        if (aws_key.isDefined && aws_secret.isDefined) {
            try {
                val key = "image/" + uuid
                val aws_creds = new BasicAWSCredentials(aws_key.get, aws_secret.get)
                val s3client = new AmazonS3Client(aws_creds)
                val metadata = new ObjectMetadata()
                metadata.setContentType("image/jpeg")
                val normal = {
                    if (original.isDefined) {
                        Image(original.get).constrain(2000, 2000).writer(Format.JPEG).toStream
                    } else {
                        Image(img).constrain(2000, 2000).writer(Format.JPEG).toStream
                    }
                }
                if (!profile) {
                    val med = Image(img).constrain(550, 550).writer(Format.JPEG).toStream
                    s3client.putObject(new PutObjectRequest(bucketName, key + "_med", med, metadata))
                } else {
                    val thumb: InputStream = Image(img).cover(50, 50).writer(Format.JPEG).toStream
                    val prof = Image(img).cover(200, 200).writer(Format.JPEG).toStream
                    s3client.putObject(new PutObjectRequest(bucketName, key + "_small", thumb, metadata))
                    s3client.putObject(new PutObjectRequest(bucketName, key + "_prof", prof, metadata))
                }
                s3client.putObject(new PutObjectRequest(bucketName, key, normal, metadata))
                Some(uuid.toString)
            } catch {
                case e: AmazonClientException => play.api.Logger.debug("Amazon Client Exception. Error: " + e.getMessage)
                    None
                case e: AmazonServiceException => play.api.Logger.debug("Amazon Service Exception. Error: " + e.getMessage)
                    None
            }
        } else None
    }

    def upload(file: File, profile: Boolean = false, x: Option[Double] = None, y: Option[Double] = None,
               height: Option[Double] = None, width: Option[Double] = None): Option[String] = {
        if (x.isDefined && y.isDefined && width.isDefined && height.isDefined) {
            val temp = Image(file)
            val imgWidth = temp.width
            val imgHeight = temp.height
            if (x.get > imgWidth || width.get > imgWidth || y.get > imgHeight || height.get > imgHeight) {
                uploadImg(Image(file), profile = profile)
            } else {
                val img = temp.trim(x.get.toInt, y.get.toInt, imgWidth - (width.get.toInt + x.get.toInt), imgHeight - (height.get.toInt + y.get.toInt))
                uploadImg(img, profile = profile, original = Some(temp))
            }
        } else {
            uploadImg(Image(file), profile = profile)
        }
    }

    def uploadURL(url: String, profile: Boolean = false): Option[String] = {
        val raw = Option(ImageIO.read(new URL(url)))
        if (raw.isDefined) {
            val image = Image(raw.get)
            uploadImg(image, profile = profile)
        } else {
            None
        }
    }

}
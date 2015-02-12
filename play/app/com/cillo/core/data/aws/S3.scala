package com.cillo.core.data.aws

import java.io.File

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import com.amazonaws.{AmazonClientException, AmazonServiceException}
import play.api.Play

object S3 {

    private final val bucketName = "cillo-static"

    def upload(file: File, key: String): Boolean = {
        val aws_key = Play.current.configuration.getString("aws.key")
        val aws_secret = Play.current.configuration.getString("aws.secret")
        if (aws_key.isDefined && aws_secret.isDefined) {
            try {
                val aws_creds = new BasicAWSCredentials(aws_key.get, aws_secret.get)
                val s3client = new AmazonS3Client(aws_creds)
                val metadata = new ObjectMetadata()
                metadata.setContentType("image/jpeg")
                s3client.putObject(new PutObjectRequest(bucketName, key, file).withMetadata(metadata))
                true
            } catch {
                case e: AmazonClientException => println("Amazon Client Exception. Error: " + e.getMessage)
                    false
                case e: AmazonServiceException => println("Amazon Service Exception. Error: " + e.getMessage)
                    false
            }
        } else false
    }

}
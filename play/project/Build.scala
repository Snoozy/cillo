import play.PlayImport._
import play.PlayScala
import sbt._
import sbt.Keys._

object Build extends Build {
    lazy val cillo = Project(id = "cillo", base = file(".")).settings(
        name := "cillo",
        version := "1.5",
        scalaVersion := "2.11.1",
        libraryDependencies ++= Seq(
            jdbc,
            anorm,
            cache,
            ws,
            filters,
            "mysql" % "mysql-connector-java" % "5.1.27",
            "org.apache.commons" % "commons-lang3" % "3.3.2",
            "com.googlecode.xmemcached" % "xmemcached" % "2.0.0",
            "com.amazonaws" % "aws-java-sdk-s3" % "1.9.0",
            "io.argonaut" %% "argonaut" % "6.0.4",
            "com.mohiva" %% "play-html-compressor" % "0.3.1",
            "com.google.code.gson" % "gson" % "2.3.1",
            "com.sksamuel.scrimage" %% "scrimage-core" % "1.4.2",
            "javax.mail" % "mail" % "1.4.7",
            "io.fastjson" % "boon" % "0.31",
            "net.debasishg" %% "redisclient" % "2.13",
            "com.sksamuel.scrimage" %% "scrimage-canvas" % "1.4.2"
        ),
        unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )
    ).enablePlugins(PlayScala)

    doc in Compile <<= target.map(_ / "none")

}

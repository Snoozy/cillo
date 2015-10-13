import play.PlayImport._
import play.PlayScala
import play.twirl.sbt.Import.TwirlKeys
import sbt._
import sbt.Keys._

object Build extends Build {
    lazy val cillo = Project(id = "cillo", base = file(".")).settings(
        name := "cillo",
        version := "0.1",
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
            "com.sksamuel.scrimage" %% "scrimage-core" % "1.4.2",
            "javax.mail" % "mail" % "1.4.7",
            "io.fastjson" % "boon" % "0.31",
            "net.debasishg" %% "redisclient" % "2.13",
            "com.mohiva" %% "play-html-compressor" % "0.3.1" exclude("rhino", "js"),
            "com.yahoo.platform.yui" % "yuicompressor" % "2.4.7" exclude("rhino", "js"),
            "com.github.jreddit" % "jreddit" % "1.0.2",
            "com.notnoop.apns" % "apns" % "1.0.0.Beta6",
            "com.sksamuel.scrimage" %% "scrimage-canvas" % "1.4.2"
        ),
        doc in Compile <<= target.map(_ / "none"),
        TwirlKeys.templateImports += "com.cillo.core.data.db.models._",
        unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )
    ).enablePlugins(PlayScala)
}
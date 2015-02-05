name := "Cillo"

version := "1.0"

lazy val `cillo` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
    jdbc,
    anorm,
    cache,
    ws,
    "mysql" % "mysql-connector-java" % "5.1.27",
    "org.apache.commons" % "commons-lang3" % "3.3.2",
    filters,
    "com.googlecode.xmemcached" % "xmemcached" % "2.0.0",
    "com.amazonaws" % "aws-java-sdk-s3" % "1.9.0",
    "io.argonaut" %% "argonaut" % "6.0.4"
)

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  
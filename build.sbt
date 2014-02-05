name := "forum"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)

libraryDependencies += "org.mindrot" % "jbcrypt" % "0.3m"

play.Project.playScalaSettings

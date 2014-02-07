name := "forum"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "org.mindrot" % "jbcrypt" % "0.3m",
  "postgresql" % "postgresql" % "9.1-901.jdbc4"

)

play.Project.playScalaSettings

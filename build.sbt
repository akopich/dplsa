name := "dplsa"

version := "1.0"

scalaVersion := "2.12.13"

libraryDependencies += "org.apache.spark" %% "spark-core" % "3.1.1"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.3"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.7" % "test"

libraryDependencies  ++= Seq(
  "org.scalanlp" %% "breeze" % "1.2",
  "org.scalanlp" %% "breeze-natives" % "1.2"
)


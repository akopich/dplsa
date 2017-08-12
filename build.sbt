name := "dplsa"

version := "1.0"

libraryDependencies += "org.apache.spark" % "spark-core_2.11" % "2.2.0" % "provided" exclude("com.typesafe.scala-logging", "scala-logging-slf4j_2.11")

resolvers += "Akka Repository" at "http://repo.akka.io/releases/"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2"

libraryDependencies ++= Seq(
    "org.scalanlp" %% "breeze" % "0.8.1",
    "org.scalanlp" %% "breeze-natives" % "0.8.1"
)

resolvers ++= Seq(
    "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
)

resolvers +=
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"


libraryDependencies += "org.scalatest" % "scalatest_2.11" % "3.0.3" % "test"

scalaVersion := "2.11.11"
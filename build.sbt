name := "dplsa"

version := "1.0"

libraryDependencies += "org.apache.spark" %% "spark-core" % "1.0.0"

resolvers += "Akka Repository" at "http://repo.akka.io/releases/"

libraryDependencies ++= Seq(
    "org.scalanlp" %% "breeze" % "0.8.1",
    "org.scalanlp" %% "breeze-natives" % "0.8.1"
)

resolvers ++= Seq(
    "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
)

resolvers +=
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"


libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.2.1" % "test"

scalaVersion := "2.10.4"
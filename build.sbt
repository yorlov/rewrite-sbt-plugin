enablePlugins(SbtPlugin)

organization := "it.orlov.sbt"
name := "rewrite-plugin"
version := "0.1.0-SNAPSHOT"

resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  "org.openrewrite" % "rewrite-core",
  "org.openrewrite" % "rewrite-java-21"
).map(_ % "8.48.1")

scriptedLaunchOpts ++= Seq("-Xmx1024M", s"-Dplugin.version=${version.value}")
scriptedBufferLog := false
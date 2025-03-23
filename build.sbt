enablePlugins(SbtPlugin)

organization := "it.orlov.sbt"
name := "rewrite-plugin"
version := "0.1.0-SNAPSHOT"

resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  "org.openrewrite" % "rewrite-java-21" % "8.48.1",
  "org.openrewrite" % "rewrite-polyglot" % "2.1.3"
)

scriptedLaunchOpts ++= Seq("-Xmx1024M", s"-Dplugin.version=${version.value}")
scriptedBufferLog := false
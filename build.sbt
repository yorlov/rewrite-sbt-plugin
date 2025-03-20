enablePlugins(RewritePlugin)

organization := "it.orlov.sbt"
name := "rewrite-plugin"
version := "0.1.0-SNAPSHOT"

javacOptions ++= Seq("--release", "21")

resolvers += Resolver.mavenLocal

recipeArtifacts ++= Seq(
  "it.orlov.refactoring" % "refactoring-examples" % "0.1.0-SNAPSHOT"
)

activeRecipes ++= Seq(
  "it.orlov.refactoring.YoSimple2"
)

crossPaths := false // drop off Scala suffix from artifact names
autoScalaLibrary := false // exclude scala-library from dependencies
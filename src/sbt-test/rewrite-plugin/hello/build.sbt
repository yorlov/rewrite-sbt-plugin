enablePlugins(RewritePlugin)

version := "0.1.0-SNAPSHOT"

resolvers += Resolver.mavenLocal

recipeArtifacts ++= Seq(
  "it.orlov.refactoring" % "refactoring-examples" % "0.1.0-SNAPSHOT"
)

activeRecipes ++= Seq(
  "it.orlov.refactoring.YoSimple2"
)
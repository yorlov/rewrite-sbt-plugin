enablePlugins(RewritePlugin)

version := "0.1.0-SNAPSHOT"

resolvers += Resolver.mavenLocal

recipeArtifacts += "org.openrewrite.recipe" % "rewrite-migrate-java" % "3.4.0"

activeRecipes ++= Seq(
  "Yo",
  "org.openrewrite.staticanalysis.UseSystemLineSeparator"
)
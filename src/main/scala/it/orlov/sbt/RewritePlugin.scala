package it.orlov.sbt

import it.orlov.sbt.util.RewriteUtil
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.internal.InMemoryLargeSourceSet
import org.openrewrite.java.JavaParser
import sbt.Keys.*
import sbt.io.syntax.*
import sbt.{AutoPlugin, Compile, IO, ModuleID, Setting, config, moduleIDConfigurable, sbtSlashSyntaxRichConfiguration, settingKey, taskKey}

import scala.jdk.CollectionConverters.seqAsJavaListConverter

object RewritePlugin extends AutoPlugin {

  object autoImport {
    val recipeArtifacts = settingKey[Seq[ModuleID]]("recipeArtifacts")
    val activeRecipes = settingKey[Seq[String]]("activeRecipes")
    val rewriteRun = taskKey[Unit]("rewriteRun")
    val rewriteConfig = taskKey[Option[File]]("rewriteConfig")
  }

  import autoImport.*

  private val RewriteConfig = config("rewrite").hide

  override lazy val projectSettings: Seq[Setting[?]] = Seq(
    recipeArtifacts := Seq(),
    activeRecipes := Seq(),
    rewriteConfig := Some(baseDirectory.value / "rewrite.yml"),

    ivyConfigurations += RewriteConfig,
    libraryDependencies ++= recipeArtifacts.value.map(_ % RewriteConfig),

    rewriteRun := {
      val environment = RewriteUtil.environment(
        update.value.configuration(RewriteConfig),
        rewriteConfig.value
      )

      val recipe = environment.activateRecipes(activeRecipes.value *)

      val executionContext = new InMemoryExecutionContext(println)

      val parser = JavaParser.fromJavaVersion.build()

      val sourcePaths = (Compile / sources).value.map(_.toPath).asJava

      val sourceFiles = parser.parse(sourcePaths, null, executionContext).toList

      val recipeRun = recipe.run(new InMemoryLargeSourceSet(sourceFiles), executionContext)

      recipeRun.getChangeset.getAllResults.forEach(result => {
        val after = result.getAfter

        IO.write(after.getSourcePath.toFile, after.printAll)
      })
    }
  )
}
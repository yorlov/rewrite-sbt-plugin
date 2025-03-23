package it.orlov.sbt

import it.orlov.sbt.util.RewriteUtil
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.internal.InMemoryLargeSourceSet
import org.openrewrite.java.JavaParser
import org.openrewrite.polyglot.OmniParser
import sbt.Keys.*
import sbt.io.syntax.*
import sbt.{AutoPlugin, Compile, IO, ModuleID, Setting, config, moduleIDConfigurable, sbtSlashSyntaxRichConfiguration, settingKey, taskKey}

import scala.jdk.CollectionConverters.seqAsJavaListConverter

object RewritePlugin extends AutoPlugin {

  object autoImport {
    val recipeArtifacts = settingKey[Seq[ModuleID]]("recipeArtifacts")
    val activeRecipes = settingKey[Seq[String]]("activeRecipes")
    val rewriteRun = taskKey[Unit]("rewriteRun")
    val rewriteConfig = settingKey[Option[File]]("rewriteConfig")
    val failOnInvalidActiveRecipes = settingKey[Boolean]("failOnInvalidActiveRecipes")
  }

  import autoImport.*

  private val RewriteConfig = config("rewrite").hide

  override lazy val projectSettings: Seq[Setting[?]] = Seq(
    recipeArtifacts := Seq(),
    activeRecipes := Seq(),
    rewriteConfig := Some(baseDirectory.value / "rewrite.yml"),
    failOnInvalidActiveRecipes := false,

    ivyConfigurations += RewriteConfig,
    libraryDependencies ++= recipeArtifacts.value.map(_ % RewriteConfig),

    rewriteRun := {
      val environment = RewriteUtil.environment(
        update.value.configuration(RewriteConfig),
        rewriteConfig.value
      )

      val recipe = environment.activateRecipes(activeRecipes.value *)

      val logger = streams.value.log

      val executionContext = new InMemoryExecutionContext(throwable => logger.error(s"Error during rewrite run $throwable"))

      val validations = recipe.validateAll(executionContext, new java.util.ArrayList)

      val failedValidations = validations.stream.flatMap(_.failures.stream()).toList
      if (!failedValidations.isEmpty) {
        failedValidations.forEach(failed => logger.error(s"Recipe validation error in ${failed.getProperty}: ${failed.getMessage}"))
        if (failOnInvalidActiveRecipes.value) {
          sys.error("Recipe validation errors detected as part of one or more activeRecipe(s). Please check error logs.")
        } else {
          logger.error("Recipe validation errors detected as part of one or more activeRecipe(s). Execution will continue regardless.")
        }
      }

      val parser = OmniParser.builder(
          OmniParser.defaultResourceParsers(),
          JavaParser.fromJavaVersion.build()
        )
        .build()

      val sourcePaths = Seq.concat(
          (Compile / sources).value,
          (Compile / resources).value
        )
        .map(_.toPath).asJava

      val sourceFiles = parser.parse(sourcePaths, null, executionContext).toList

      val recipeRun = recipe.run(new InMemoryLargeSourceSet(sourceFiles), executionContext)

      recipeRun.getChangeset.getAllResults.forEach(result => {
        (Option(result.getBefore), Option(result.getAfter)) match {
          case (None, None) => // This situation shouldn't happen / makes no sense

          case (None, Some(addedFile)) => IO.write(addedFile.getSourcePath.toFile, addedFile.printAll)

          case (Some(deletedFile), None) => IO.delete(deletedFile.getSourcePath.toFile)

          case (Some(before), Some(after)) =>
            if (before.getSourcePath != after.getSourcePath) {
              IO.move(before.getSourcePath.toFile, after.getSourcePath.toFile)
            }
            IO.write(after.getSourcePath.toFile, after.printAll)
        }
      })
    }
  )
}
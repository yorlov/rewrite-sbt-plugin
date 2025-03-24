package it.orlov.sbt

import it.orlov.sbt.util.*
import it.orlov.sbt.util.RewriteUtil.changesOf
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.internal.InMemoryLargeSourceSet
import org.openrewrite.java.JavaParser
import org.openrewrite.polyglot.OmniParser
import sbt.Keys.*
import sbt.io.syntax.*
import sbt.util.Level
import sbt.{AutoPlugin, Compile, IO, ModuleID, Setting, config, moduleIDConfigurable, sbtSlashSyntaxRichConfiguration, settingKey, taskKey}

import java.nio.file.Path
import java.time.Duration.ZERO
import scala.jdk.CollectionConverters.seqAsJavaListConverter

object RewritePlugin extends AutoPlugin {

  object autoImport {
    val recipeArtifacts = settingKey[Seq[ModuleID]]("recipeArtifacts")
    val activeRecipes = settingKey[Seq[String]]("activeRecipes")
    val rewriteRun = taskKey[Unit]("rewriteRun")
    val rewriteConfig = settingKey[Option[File]]("rewriteConfig")
    val failOnInvalidActiveRecipes = settingKey[Boolean]("failOnInvalidActiveRecipes")
    val recipeChangeLogLevel = settingKey[Level.Value]("recipeChangeLogLevel")
  }

  import autoImport.*

  private val RewriteConfig = config("rewrite").hide

  override lazy val projectSettings: Seq[Setting[?]] = Seq(
    recipeArtifacts := Seq(),
    activeRecipes := Seq(),
    rewriteConfig := Some(baseDirectory.value / "rewrite.yml"),
    failOnInvalidActiveRecipes := false,
    recipeChangeLogLevel := Level.Warn,

    ivyConfigurations += RewriteConfig,
    libraryDependencies ++= recipeArtifacts.value.map(_ % RewriteConfig),

    rewriteRun := {
      val logger = streams.value.log

      val environment = RewriteUtil.environment(
        update.value.configuration(RewriteConfig),
        rewriteConfig.value
      )

      val executionContext = new InMemoryExecutionContext(throwable => logger.error(s"Error during rewrite run $throwable"))

      val recipe = environment.activateRecipes(activeRecipes.value *)

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

      logger.log(recipeChangeLogLevel.value, s"Running active recipes: ${activeRecipes.value.mkString(", ")}")

      val recipeRun = recipe.run(new InMemoryLargeSourceSet(sourceFiles), executionContext)

      val relativize = (path: Path) => {
        val projectPath = if (path.isAbsolute) baseDirectory.value.toPath.relativize(path) else path
        projectPath.normalize
      }

      val estimateTimeSaved = changesOf(recipeRun)
        .flatMap(change => {
          val message = change match {

            case FileAdded(addedFile, _, _) =>
              IO.write(addedFile.getSourcePath.toFile, addedFile.printAll)
              s"Generated new file '${relativize(addedFile.getSourcePath)}'"

            case FileDeleted(deletedFile, _, _) =>
              IO.delete(deletedFile.toFile)
              s"Deleted file '${relativize(deletedFile)}'"

            case FileModified(before, after, _, _) =>
              IO.write(after.getSourcePath.toFile, after.printAll)
              s"Changes have been made to '${relativize(before.getSourcePath)}'"

            case FileMoved(before, after, _, _) =>
              IO.move(before.toFile, after.toFile)
              s"File has been moved from '${relativize(before)}' to '${relativize(after)}'"
          }

          logger.log(
            recipeChangeLogLevel.value,
            s"$message by: ${change.recipeDescriptors.map(_.getName).mkString("\n    ", "\n    ", "")}"
          )

          change.timeSavings
        })
        .fold(ZERO)(_.plus(_))

      logger.log(recipeChangeLogLevel.value, "Please review and commit the results.")
      logger.log(recipeChangeLogLevel.value, s"Estimate time saved: $estimateTimeSaved")
    }
  )
}
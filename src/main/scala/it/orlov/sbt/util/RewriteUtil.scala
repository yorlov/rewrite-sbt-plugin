package it.orlov.sbt.util

import org.openrewrite.RecipeRun
import org.openrewrite.config.{Environment, YamlResourceLoader}
import sbt.ConfigurationReport
import sbt.io.syntax.*

import java.net.URLClassLoader
import java.nio.file.Files.newInputStream
import java.util.Properties
import scala.jdk.CollectionConverters.collectionAsScalaIterableConverter
import scala.language.implicitConversions
import scala.util.Using.resource

object RewriteUtil {

  def environment(report: Option[ConfigurationReport], config: Option[File]): Environment = {
    val recipeClassLoader = new URLClassLoader(
      report.toSeq
        .flatMap(_.modules)
        .flatMap(_.artifacts)
        .map { case (_, file) => file.asURL }
        .toArray,
      getClass.getClassLoader
    )

    val properties = new Properties()
    val builder = Environment.builder(properties).scanClassLoader(recipeClassLoader)

    config.foreach { configFile =>
      resource(newInputStream(configFile.toPath)) { inputStream =>
        builder.load(new YamlResourceLoader(inputStream, configFile.toURI, properties, recipeClassLoader))
      }
    }

    builder.build()
  }

  def changesOf(recipeRun: RecipeRun): Iterable[Change] = {
    recipeRun.getChangeset.getAllResults.asScala.map(result => {
      val timeSavings = Option(result.getTimeSavings)
      val recipeDescriptors = result.getRecipeDescriptorsThatMadeChanges.asScala.toList

      (Option(result.getBefore), Option(result.getAfter)) match {
        case (None, None) => ??? // This situation shouldn't happen / makes no sense

        case (None, Some(addedFile)) => FileAdded(addedFile, recipeDescriptors, timeSavings)

        case (Some(deletedFile), None) => FileDeleted(deletedFile.getSourcePath, recipeDescriptors, timeSavings)

        case (Some(before), Some(after)) =>
          if (before.getSourcePath != after.getSourcePath) {
            FileMoved(before.getSourcePath, after.getSourcePath, recipeDescriptors, timeSavings)
          } else {
            FileModified(before, after, recipeDescriptors, timeSavings)
          }
      }
    })
  }

}
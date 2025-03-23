package it.orlov.sbt.util

import org.openrewrite.config.{Environment, YamlResourceLoader}
import sbt.ConfigurationReport
import sbt.io.syntax.*

import java.net.URLClassLoader
import java.nio.file.Files.newInputStream
import java.util.Properties
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

}
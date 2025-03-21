package it.orlov.sbt.util

import org.openrewrite.config.{ClasspathScanningLoader, Environment, YamlResourceLoader}
import sbt.ConfigurationReport
import sbt.io.syntax.*

import java.net.{URL, URLClassLoader}
import java.nio.file.Files.newInputStream
import java.util.Properties
import scala.language.implicitConversions
import scala.util.Using.resource

object RewriteUtil {

  def environment(report: Option[ConfigurationReport], config: Option[File]): Environment = {
    val recipeDependencies = report.fold(Seq.empty[URL])(_.modules.flatMap(_.artifacts.map(_._2.asURL)))

    val recipeClassLoader = new URLClassLoader(recipeDependencies.toArray, getClass.getClassLoader)
    val properties = new Properties()

    val resourceLoader = config
      .map { configFile =>
        resource(newInputStream(configFile.toPath)) { inputStream =>
          new YamlResourceLoader(inputStream, configFile.toURI, properties, recipeClassLoader)
        }
      }
      .getOrElse(new ClasspathScanningLoader(properties, recipeClassLoader))

    Environment.builder().load(resourceLoader).build()
  }

}
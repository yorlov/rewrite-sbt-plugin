package it.orlov.sbt

import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.config.Environment
import org.openrewrite.internal.InMemoryLargeSourceSet
import org.openrewrite.java.JavaParser
import sbt.Keys.{ivyConfigurations, libraryDependencies, sources, update}
import sbt.{AutoPlugin, Compile, IO, ModuleID, Setting, config, moduleIDConfigurable, sbtSlashSyntaxRichConfiguration, settingKey, taskKey}

import java.net.{URL, URLClassLoader}
import scala.jdk.CollectionConverters.seqAsJavaListConverter

object RewritePlugin extends AutoPlugin {

  object autoImport {
    val recipeArtifacts = settingKey[Seq[ModuleID]]("recipeArtifacts")
    val activeRecipes = settingKey[Seq[String]]("activeRecipes")
    val rewriteRun = taskKey[Unit]("rewriteRun")
  }

  import autoImport.*

  private val RewriteConfig = config("rewrite").hide

  override lazy val projectSettings: Seq[Setting[?]] = Seq(
    recipeArtifacts := Seq(),
    activeRecipes := Seq(),

    ivyConfigurations += RewriteConfig,
    libraryDependencies ++= recipeArtifacts.value.map(_ % RewriteConfig),

    rewriteRun := {
      val recipeDependencies = update.value.configuration(RewriteConfig)
        .fold(Seq.empty[URL])(_.modules.flatMap(_.artifacts.map(_._2.toURI.toURL)))

      val environment = Environment.builder()
        .scanClassLoader(new URLClassLoader(recipeDependencies.toArray, getClass.getClassLoader))
        .build()

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
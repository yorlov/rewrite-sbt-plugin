package it.orlov.sbt.util

import org.openrewrite.SourceFile
import org.openrewrite.config.RecipeDescriptor

import java.nio.file.Path
import java.time.Duration

sealed trait Change {

  def timeSavings: Option[Duration]

  def recipeDescriptors: List[RecipeDescriptor]

}

case class FileAdded(addedFile: SourceFile, recipeDescriptors: List[RecipeDescriptor], timeSavings: Option[Duration]) extends Change

case class FileDeleted(deletedFile: Path, recipeDescriptors: List[RecipeDescriptor], timeSavings: Option[Duration]) extends Change

case class FileMoved(before: Path, after: Path, recipeDescriptors: List[RecipeDescriptor], timeSavings: Option[Duration]) extends Change

case class FileModified(before: SourceFile, after: SourceFile, recipeDescriptors: List[RecipeDescriptor], timeSavings: Option[Duration]) extends Change

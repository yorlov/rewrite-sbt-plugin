import com.jsuereth.sbtpgp.PgpKeys.publishSigned
import sbt.util
import sbtrelease.ReleasePlugin.runtimeVersion
import sbtrelease.ReleaseStateTransformations.*
import xerial.sbt.Sonatype.sonatypeCentralHost

enablePlugins(SbtPlugin)

organization := "it.orlov.sbt"
name := "rewrite-plugin"

libraryDependencies ++= Seq(
  "org.openrewrite" % "rewrite-java-21" % "8.48.1",
  "org.openrewrite" % "rewrite-polyglot" % "2.1.3"
)

scriptedLaunchOpts ++= Seq("-Xmx1024M", s"-Dplugin.version=${version.value}")
scriptedBufferLog := false

releaseTagName := s"${runtimeVersion.value}"
releaseCommitMessage := s"[skip ci] prepare release '${runtimeVersion.value}'"
releaseNextCommitMessage := s"[skip ci] prepare for next development iteration '${runtimeVersion.value}'"

releaseProcess := Seq(
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepTask(publishSigned),
  releaseStepTask(sonatypeBundleRelease),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

pgpSigningKey := sys.env.get("SIGNING_KEY_ID")

sonatypeCredentialHost := sonatypeCentralHost
sbtPluginPublishLegacyMavenStyle := false
publishTo := sonatypePublishToBundle.value
versionScheme := Some("semver-spec")
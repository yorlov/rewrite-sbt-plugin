import sbt.url

publishMavenStyle := true
sonatypeProfileName := "it.orlov"

homepage := Some(url("https://github.com/yorlov/rewrite-sbt-plugin"))

licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0"))

scmInfo := Some(
  ScmInfo(
    url("https://github.com/yorlov/rewrite-sbt-plugin"),
    "scm:git@github.com:yorlov/rewrite-sbt-plugin.git"
  )
)

developers := List(
  Developer(
    id = "yorlov",
    name = "Yuri Orlov",
    email = "yuri.n.orlov@gmail.com",
    url = url("https://orlov.it")
  )
)
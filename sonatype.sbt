
// Your profile name of the sonatype account. The default is the same with the organization value
// sonatypeProfileName := "com.github.angiolep"

// To sync with Maven central, you need to supply the following information:
publishMavenStyle := true

licenses := Seq("Apache 2" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("http://angiolep.github.io/projects/akka-wamp/index.html"))

scmInfo := Some(
  ScmInfo(
    url(
      "https://github.com/angiolep/akka-wamp"),
      "scm:git:git@github.com:angiolep/akka-wamp.git"
    )
  )

developers := List(
  Developer(
    id = "angiolep",
    name = "Paolo Angioletti",
    email = "paolo.angioletti@gmail.com",
    url = url("http://angiolep.github.io"))
  )

apiURL := Some(url("http://angiolep.github.io/projects/akka-wamp/api/index.html"))

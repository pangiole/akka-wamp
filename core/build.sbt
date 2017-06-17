import Dependencies._

name := "akka-wamp"

mainClass in Compile := Some("akka.wamp.router.StandaloneRouterApp")

libraryDependencies ++= Seq (
  akkaHttp,
  akkaSlf4j,
  logback,
  jackson
) ++ Seq(
  akkaHttpTestkit,
//  akkaStreamTestkit,
  scalatest,
  scalamock,
  mockito,
  pegdown,
  junit
).map(_ % Test)

/*testOptions in Test += Tests.Setup { () =>
  System.setProperty("akka.loglevel", "off")
  System.setProperty("akka.stdout-loglevel", "off")
}*/

parallelExecution in Test := true

publishMavenStyle := true

isSnapshot := false

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

licenses := Seq("Apache 2" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("http://angiolep.github.io/projects/akka-wamp/index.html"))

pomExtra :=
  <scm>
    <url>git://github.com/angiolep/akka-wamp.git</url>
    <connection>scm:git:git@github.com:angiolep/akka-wamp.git</connection>
  </scm>
    <developers>
      <developer>
        <name>Paolo Angioletti</name>
        <email>paolo.angioletti@gmail.com</email>
        <url>http://angiolep.github.io</url>
      </developer>
    </developers>

apiURL := Some(url("http://angiolep.github.io/projects/akka-wamp/api/index.html"))

credentials += Credentials(Path.userHome / ".ivy2" / "sonatype")

enablePlugins(JavaAppPackaging)

mappings in Universal <+= (packageBin in Compile, sourceDirectory ) map {
  (_, src) =>
    // we are using the reference.conf as default application.conf
    // the user can override settings here
    val conf = src / "main" / "resources" / "reference.conf"
    conf -> "conf/application.conf"
}

bashScriptExtraDefines += """addApp "${app_home}/../conf/application.conf" """

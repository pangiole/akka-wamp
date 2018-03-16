
resolvers += "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/"

// see https://github.com/xerial/sbt-sonatype
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.3")


addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.3")


addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")
addSbtPlugin("com.codacy" % "sbt-codacy-coverage" % "1.3.11")


addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.1")
//addSbtPlugin("com.lightbend.paradox" % "sbt-paradox" % "0.3.2")


resolvers += Resolver.jcenterRepo
addSbtPlugin("com.lightbend.akka" % "sbt-paradox-akka" % "0.6")


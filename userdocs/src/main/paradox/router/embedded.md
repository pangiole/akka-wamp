
# Embedded
Akka Wamp provides you with a basic WAMP Router that can be easily embedded into your Akka application.

## Usage
Make your SBT, Gradle or Maven build depend on the latest version of the ``akka-wamp-router`` library:

sbt
:   @@snip [build.sbt](build.sbt)

gradle
:    @@snip [build.gradle](build.gradle)

mvn
:    @@snip [pom.xml](pom.xml)


## Code
Create and bind the ``router`` actor passing an actor reference factory, such as the actor system or any actor context, to the ``EmbeddedRouter.createAndBind`` method. The ``router`` actor will bind (and start listening for connection) onto all endpoints listed in the ``application.conf`` as explained further [below](#Configuration)

scala
:    @@snip [RouterScalaApp.scala](../../../../../examples/router/src/RouterScalaApp.scala)

java
:    @@snip [RouterJavaApp.java](../../../../../examples/router/src/RouterJavaApp.java)

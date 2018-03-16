
# Configuration
Either you decide to adopt the Actors or the Futures or the Streams based API, the underlying Akka Wamp ``client`` actor will be affected by the [TypeSafe Config](https://github.com/lightbend/config) fragment shown below:

@@snip[application.conf](../../../../../client/src/main/resources/reference.conf) { #client }
      
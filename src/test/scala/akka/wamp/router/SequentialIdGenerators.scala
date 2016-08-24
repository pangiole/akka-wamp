package akka.wamp.router

import akka.wamp.Scope

trait SequentialIdGenerators {
  
  val scopes = Map(
    'global -> new Scope.Session {},
    'router -> new Scope.Session {},
    'session -> new Scope.Session {}
  )
}

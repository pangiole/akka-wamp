package akka.wamp.router

import akka.wamp.Scope

trait SequentialIdGenerators {
  
  val scopes = Map(
    'global -> new Scope.SessionScope {},
    'router -> new Scope.SessionScope {},
    'session -> new Scope.SessionScope {}
  )
}

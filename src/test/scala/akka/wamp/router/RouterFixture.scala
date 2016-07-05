package akka.wamp.router

import akka.wamp.Wamp._

trait RouterFixture {
  val scopes = Map[Symbol, IdScope](
    'global  -> (_ + 1),
    'router  -> (_ + 1),
    'session -> (_ + 1)
  )
}

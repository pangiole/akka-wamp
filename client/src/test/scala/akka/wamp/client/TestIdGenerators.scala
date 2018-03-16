package akka.wamp.client

import akka.wamp.{IdGenerator, SessionScopedIdGenerator}

object TestIdGenerators {
  def newTestIdGenerators: Map[Symbol, IdGenerator] = {
    Map(
      'global  -> new SessionScopedIdGenerator,
      'router  -> new SessionScopedIdGenerator,
      'session -> new SessionScopedIdGenerator
    )
  }

}

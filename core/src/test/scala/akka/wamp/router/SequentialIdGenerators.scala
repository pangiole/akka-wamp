package akka.wamp.router

import akka.wamp.{SessionScopedIdGenerator, IdGenerator}


object SequentialIdGenerators {
  
  def testIdGenerators(): Map[Symbol, IdGenerator] = {
    Map(
      'global  -> new SessionScopedIdGenerator,
      'router  -> new SessionScopedIdGenerator,
      'session -> new SessionScopedIdGenerator
    )
  }
}

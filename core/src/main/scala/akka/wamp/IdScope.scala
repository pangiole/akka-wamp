package akka.wamp

/**
  * A scope for unique identifier
  */
private[wamp] trait IdScope {

  /**
    * Generate the next identifier within this scope
    *
    * @return the next identifier
    */
  def nextRequestId(): Id = nextRequestId(Set.empty[Id])

  /**
    * Generate the next identifier within this scope excluding the provided ones
    *
    * @param excludes is the identifiers set to be excluded
    * @return the next identifier
    */
  def nextRequestId(excludes: Set[Id]): Id

  protected def generate(oldIds: Set[Id], fn: (Id) => Id): Id = {
    var newId: Long = lastId
    do {
      newId = fn(newId)
    } while (oldIds.contains(newId))
    lastId = newId
    newId
  }

  private var lastId = 0L
}


/**
  * A scope for unique identifier
  */
private[wamp] object IdScopes {

  /**
    * Identifiers in the global scope MUST be drawn randomly from a uniform distribution
    * over the complete range. Entities whose IDs are generated randomly in the
    * global scope are:
    *
    * - Router.sessions
    * - Broker.publications
    */
  trait GlobalIdScope extends IdScope {
    override def nextRequestId(excludes: Set[Id]): Id = {
      generate(excludes, _ => Id.draw())
    }
  }

  /**
    * Identifiers in the router scope can be chosen freely by the specific router implementation.
    * Akka Wamp applies random generation. Entities whose IDs are generated randomly in the
    * router scope are:
    *
    * - Broker.subscriptions
    * - Dealer.registrations
    */
  trait RouterIdScope extends IdScope {
    override def nextRequestId(excludes: Set[Id]): Id = {
      generate(excludes, _ => Id.draw())
    }
  }

  /**
    * Identifiers in the session scope SHOULD be incremented by 1 beginning with 1 (for each
    * direction client-to-router and router-to-client). Entities whose IDs are generated
    * sequentially in the session scope are:
    *
    * - Dealer.invocations
    * - Client.*
    */
  trait SessionIdScope extends IdScope {
    override def nextRequestId(excludes: Set[Id]): Id = {
      generate(excludes, _ + 1)
    }
  }

}

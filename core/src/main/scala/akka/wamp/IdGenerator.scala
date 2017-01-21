package akka.wamp

/**
 * Generates unique identifiers within a specific scope
 */
private[wamp] abstract class IdGenerator {

  /**
    * Generates the next identifier within excluding the provided ones
    *
    * @param excludes are the identifier to exclude from generation
    * @return the next identifier
    */
  // @tailrec
  def nextId(excludes: Set[Long]): Long = {
    val id = Id.draw()
    if (!excludes.contains(id)) id
    else nextId(excludes)
  }

  /**
    * Generates the next identifier with NO exclusion
    *
    * @return the next identifier
    */
  def nextId(): Long = nextId(Set())
}


/**
  * Generates identifiers in the global scope
  *
  * They MUST be drawn randomly from a uniform distribution over t
  * he complete range. Entities whose identifiers are generated
  * randomly in global scope are:
  *
  *   - Router.sessions
  *   - Broker.publications
  *
  */
private[wamp] class GlobalScopedIdGenerator extends IdGenerator


/**
  * Generate identifiers in the router scope.
  *
  * They can be chosen freely by the specific router implementation.
  * Entities whose identifiers are generated in the router scope are:
  *
  *   - Broker.subscriptions
  *   - Dealer.registrations
  *
  */
private[wamp] class RouterScopedIdGenerator extends IdGenerator


/**
  * Generate identifiers in session scope.
  *
  * Identifiers are incremented by 1 beginning with 1 and they are used for
  *
  *   - Dealer.invocations
  *   - Client.*
  */
class SessionScopedIdGenerator extends IdGenerator {
  final override def nextId(excludes: Set[Long] = Set()): Long = {
    val newId = lastId + 1
    lastId = newId
    newId
  }
  private var lastId = 0L
}
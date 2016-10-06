package akka.wamp

trait Scope {
  protected[wamp] def nextId(excludes: Set[Id] = Set()): Id = {
    var id: Long = 0
    do {
      id = Id.draw()
    } while (excludes.contains(id))
    id
  }
}


object Scope {
  
  object GlobalScope extends Scope

  object RouterScope extends Scope

  trait SessionScope extends Scope {
    private var id: Id = 0
    protected[wamp] final override def nextId(excludes: Set[Id] = Set()): Id = {
      do {
        id = id + 1
      } while (excludes.contains(id))
      id
    }
  }

  val defaults = Map(
    'global -> GlobalScope,
    'router -> RouterScope,
    'session -> new SessionScope {}
  )
}

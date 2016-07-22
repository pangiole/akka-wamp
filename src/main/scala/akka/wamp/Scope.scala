package akka.wamp

trait Scope {
  def nextId(excludes: Set[Id]): Id = {
    var id: Long = 0
    do {
      id = Id.draw()
    } while (excludes.contains(id))
    id
  }
}


object Scope {
  
  object Global extends Scope

  object Router extends Scope

  trait Session extends Scope {
    private var id: Id = 0
    final override def nextId(excludes: Set[Id] = Set()): Id = {
      do {
        id = id + 1
      } while (excludes.contains(id))
      id
    }
  }

  val defaults = Map(
    'global -> Global,
    'router -> Router,
    'session -> new Session {}
  )
}

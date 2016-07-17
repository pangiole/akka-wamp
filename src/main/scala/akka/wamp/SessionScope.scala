package akka.wamp

trait SessionScope {
  var id = 0
  def nextId: Id = {
    id = id + 1
    id
  } 
}

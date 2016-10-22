package akka.wamp.router

import akka.actor._
import akka.wamp._

/**
  * The registration of a callee for a procedure it provides
  * 
  * @param id is this registration identifier
  * @param callee is the callee actor reference
  * @param procedure is the registered procedure identifier
  */
case class Registration(id: Id, callee: ActorRef, procedure: Uri)


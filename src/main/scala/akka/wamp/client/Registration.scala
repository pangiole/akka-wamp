package akka.wamp.client

import akka.wamp._
import akka.wamp.messages.Registered

class Registration(val procedure: Uri, val handler: InvocationHandler, val registered: Registered)
package akka.wamp

// TODO shall we model it just simple as property of Session?
/**
  * A Realm is a routing and administrative __domain__, optionally
  * protected by authentication and authorization.
  * 
  * [[Message]]s are only routed within a Realm.
  * 
  */
class Realm(uri: Uri) 


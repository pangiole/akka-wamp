package akka.wamp.router

class CustomConnectionHandlerSpec extends ConnectionHandlerBaseSpec {

  override def testConfigSource: String =
    """
      | akka {
      |   wamp {
      |     router {
      |       validate-strict-uris    = true
      |       abort-unknown-realms    = true
      |       drop-offending-messages = true
      |     }
      |   }
      | }
    """.stripMargin


  "A router.ConnectionHandler configured with custom settings" should "drop offending messages and resume" in { f =>
    // TODO --> bad messages

    // TODO --> bad ABORT : no HELLO yet
    // f.client.sendMessage("""[3,{},"wamp.error.close_realm"]""")
    // ~~~ dropped

    // --> bad GOODBYE : no session
    f.client.sendMessage("""[6,{},"wamp.error.close_realm"]""")
    // ~~~ dropped

    // --> bad SUBSCRIBE : no session
    f.client.sendMessage("""[32,1,{},"myapp.topic"]""")
    // ~~~ dropped

    // TODO  --> bad UNSUBSCRIBE : no session

    // --> bad PUBLISH : no session
    f.client.sendMessage("""[16,2,{"acknowledge":true},"myapp.topic"]""")
    // ~~~ dropped

    // --> bad REGISTER : no session
    f.client.sendMessage("""[64,3,{},"myapp.procedure1"]""")
    // ~~~ dropped

    // TODO  --> bad UNREGISTER : no session

    // --> bad CALL : no session
    f.client.sendMessage("""[48,4,{},"myapp.procedure1"]""")
    // ~~~ dropped

    // TODO  --> bad INVOCATION : no session

    // TODO  --> bad YIELD : no session

    // TODO  --> bad RESULT : no session

    // --> bad HELLO : invalid realm URI
    f.client.sendMessage("""[1,"invalid..realm",{"roles":{"subscriber":{}}}]""")
    // ~~~ dropped

    // --> bad HELLO : invalid key
    f.client.sendMessage("""[1,"myrealm",{"INVALID KEY":null}]""")
    // ~~~ dropped

    // --> bad HELLO : invalid role
    f.client.sendMessage("""[1,"myrealm",{"roles":{"invalid role":{}}}]""")
    // ~~~ dropped

    // --> bad HELLO : myrealm
    f.client.sendMessage("""[1,"myrealm",{"roles":{"publisher":{}}}]""")
    // <-- ABORT
    f.client.expectMessage("""[3,{"message":"The realm 'myrealm' does not exist."},"wamp.error.no_such_realm"]""")

    // --> HELLO
    f.client.sendMessage("""[1,"default",{"roles":{"publisher":{}}}]""")
    // <-- WELCOME
    f.client.expectMessage("""[2,1,{"agent":"akka-wamp-0.15.1","roles":{"broker":{},"dealer":{}}}]""")

    // SESSION #1 OPEN
    // \
    // --> bad HELLO : 2nd within session lifecycle
    // ~~~ dropped
    f.client.sendMessage("""[1,"default",{"roles":{"publisher":{}}}]""")

    // --> bad GOODBYE : invalid reason URI
    // ~~~ dropped
    f.client.sendMessage("""[6,{},"invalid..reason"]""")

    // --> GOODBYE
    f.client.sendMessage("""[6,{},"wamp.error.close_realm"]""")
    // <-- GOODBYE
    f.client.expectMessage("""[6,{},"wamp.error.goodbye_and_out"]""")
    // \
    // SESSION #1 CLOSED

    // --> HELLO
    f.client.sendMessage("""[1,"default",{"roles":{"subscriber":{},"publisher":{},"callee":{}}}]""")
    // <-- WELCOME
    f.client.expectMessage("""[2,2,{"agent":"akka-wamp-0.15.1","roles":{"broker":{},"dealer":{}}}]""")

    // SESSION #2 OPEN
    // \
    // --> bad SUBSCRIBE : invalid topic URI
    f.client.sendMessage("""[32,1,{},"invalid..topic'"]""")
    // ~~~ dropped

    // --> SUBSCRIBE(#1)
    f.client.sendMessage("""[32,1,{},"myapp.topic"]""")
    // <-- SUBSCRIBED
    f.client.expectMessage("""[33,1,1]""")

    // --> bad PUBLISH : invalid topic URI
    f.client.sendMessage("""[16,2,{"acknowledge":true},"invalid..topic"]""")
    // ~~~ dropped

    // --> PUBLISH(#3)
    f.client.sendMessage("""[16,3,{"acknowledge":true},"myapp.topic"]""")
    // <-- PUBLISHED
    f.client.expectMessage("""[17,3,3]""")

    // TODO PUBLISH with args and kwargs

    // --> bad REGISTER : invalid procedure URI
    f.client.sendMessage("""[64,4,{},"invalid..procedure"]""")
    // ~~~ dropped

    // --> REGISTER(#5)
    f.client.sendMessage("""[64,5,{},"myapp.procedure.itself"]""")
    // <-- REGISTERED
    f.client.expectMessage("""[65,5,2]""")

    // --> bad CALL : invalid procedure URI
    f.client.sendMessage("""[48,6,{},"invalid..procedure"]""")
    // ~~~ dropped

    // --> CALL(#7)
    f.client.sendMessage("""[48,7,{},"myapp.procedure.itself",["args0",1],{"key":"value"}]""")
    // <-- INVOCATION(#1)
    f.client.expectMessage("""[68,1,2,{},["args0",1],{"key":"value"}]""")

    // --> YIELD(#1)
    f.client.sendMessage("""[70,1,{},[{"prop","res"}]]""")
    // <-- RESULT(#7)
    f.client.expectMessage("""[50,7,{},[{"prop","res"}]]""")

    // --> GOODBYE
    f.client.sendMessage("""[6,{},"wamp.error.close_realm"]""")
    // <-- GOODBYE
    f.client.expectMessage("""[6,{},"wamp.error.goodbye_and_out"]""")
    // \
    // SESSION #2 CLOSED

    f.client.expectNoMessage()
  }
}

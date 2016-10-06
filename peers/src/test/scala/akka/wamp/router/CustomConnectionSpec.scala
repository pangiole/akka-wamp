package akka.wamp.router


/**
  * It tests the ``router.Connection`` behaviour with a 
  * 
  *   CUSTOM configuration
  *   
  * by exercising it with various JSON messages covering
  * the most common scenarios (session handling, subscriptions, 
  * publications, etc.)
  */
class CustomConnectionSpec extends ConnectionFixtureSpec {

  override def testConfigSource: String =
    """
      | akka {
      |   wamp {
      |     router {
      |       validate-strict-uris = true
      |       abort-unknown-realms = true
      |     }
      |   }
      | }
    """.stripMargin

  // TODO how to test when disconnect-offending-peers ???
  // client.sendMessage("""[1,"loose.URI..realm",{"roles":{"subscriber":{}}}]""")
  
  
  "A custom router.Connection" should "disconnect on offending messages" in { f =>
    pending
    withWsClient(f.httpRoute) { client =>
      
      // --> HELLOs
      client.sendMessage("""[1,"INVALID.REALM",{"roles":{"subscriber":{}}}]""")
      client.sendMessage("""[1,"myapp.realm",{"roles":{"subscriber":{}}}]""")

      // <-- ABORT
      client.expectMessage("""[3,{"message":"The realm myapp.realm does not exist."},"wamp.error.no_such_realm"]""")

      // --> HELLOs
      client.sendMessage("""[1,"akka.wamp.realm",{"roles":{"invalid":{}}}]""")
      client.sendMessage("""[1,"akka.wamp.realm",{"roles":{"publisher":{}}}]""")

      // <-- WELCOME
      client.expectMessage("""[2,1,{"agent":"akka-wamp-0.10.0","roles":{"broker":{},"dealer":{}}}]""")

      // SESSION #1 OPEN

      // --> GOODBYEs
      client.sendMessage("""[6,{},"invalid.REASON"]""")
      client.sendMessage("""[6,{},"wamp.error.close_realm"]""")
      
      // <-- GOODBYE
      client.expectMessage("""[6,{},"wamp.error.goodbye_and_out"]""")

      // SESSION #1 CLOSED
      // but TRANSPORT still CONNECTED :-)

      // --> HELLO
      client.sendMessage("""[1,"akka.wamp.realm",{"roles":{"subscriber":{}}}]""")

      // <-- WELCOME
      client.expectMessage("""[2,2,{"agent":"akka-wamp-0.10.0","roles":{"broker":{},"dealer":{}}}]""")

      // SESSION #2 OPEN
      
      // --> GOODBYE
      client.sendMessage("""[6,{},"wamp.error.close_realm"]""")
      
      // <-- GOODBYE
      client.expectMessage("""[6,{},"wamp.error.goodbye_and_out"]""")

      // SESSION #2 OPEN
      // but TRANSPORT still CONNECTED :-)
      
      client.expectNoMessage()
    }
  }
}
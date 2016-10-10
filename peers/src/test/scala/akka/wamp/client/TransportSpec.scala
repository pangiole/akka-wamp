package akka.wamp.client

import akka.actor.ActorSystem
import akka.wamp._
import com.typesafe.config.ConfigFactory

class TransportSpec extends ClientFixtureSpec(ActorSystem("test", ConfigFactory.parseString(
  """
    | akka {
    |   wamp {
    |     router {
    |       abort-unknown-realms = true
    |     }
    |   }
    | }
  """.stripMargin))) {


  "A client transport" should "start as connected and succeed to disconnect" in { f =>
    f.withTransport { transport =>
      transport.connected mustBe true
      whenReady(transport.disconnect()) { done =>
        transport.connected mustBe false
        assert(true)
      }
    }
  }

  
  it should "fail open session when it turns out to have been disconnected from client side" in { f =>
    f.withTransport { transport =>
      whenReady(transport.disconnect()) { done =>
        val session = transport.openSession()
        whenReady(session.failed) { e =>
          e mustBe a[TransportException]
          e.getMessage mustBe "Disconnected"
        }
      }
    }
  }

  
  it should "fail open session if it turns out to have been disconnected from router side" in { f =>
    // TODO https://github.com/angiolep/akka-wamp/issues/11
    pending
    f.withTransport { transport =>
      // TODO how to simulate router side disconnection ???
      val session = transport.openSession()
      whenReady(session.failed) { e =>
        e mustBe a[TransportException]
        e.getMessage mustBe "disconnected"
      }
    }
  }


  it should "fail open session when invalid realm is given" in { f =>
    f.withTransport { transport =>
      val session = transport.openSession("invalid..realm")
      whenReady(session.failed) { ex =>
        ex mustBe a[TransportException]
        ex.getMessage mustBe "invalid URI invalid..realm"
      }
    }
  }

  
  it should "fail open session when invalid roles are given" in { f =>
    f.withTransport { transport =>
      val session = transport.openSession("akka.wamp.realm", roles = Set("invalid"))
      whenReady(session.failed) { ex =>
        ex mustBe a[TransportException]
        ex.getMessage must startWith("invalid roles in Map(roles -> Map(invalid -> Map()))")
      }
    }
  }


  it should "fail open session when router aborts" in { f =>
    f.withTransport { transport =>
      // if unknown.realm is given and the router cannot create it
      // then the router will reply an Abort message
      val session = transport.openSession("unknown.realm")
      session.failed.futureValue match {
        case AbortException(abort) =>
          abort.reason mustBe "wamp.error.no_such_realm"
          abort.details mustBe Dict("message" -> "The realm 'unknown.realm' does not exist.")
        case e =>
          fail(s"unexpected $e")
      }
    }
  }

  
  it should "fail open multiple sessions on the same transport" in { f =>
    f.withTransport { transport =>
      whenReady(transport.openSession()) { session1 =>
        val session2 = transport.openSession()
        session2.failed.futureValue match {
          case AbortException(abort) =>
            abort.reason mustBe "akka.wamp.error.session_already_open"
            abort.details mustBe Dict()
          case e =>
            fail(s"unexpected $e")
        }
      }
    }
  }

  
  it should "succeed open multiple sessions on distinct transports" in { f =>
    f.withTransport { conn1 =>
      whenReady(conn1.openSession()) { session =>
        session.id mustBe 1
        session.details mustBe Map(
          "agent" -> "akka-wamp-0.10.0",
          "roles" -> Map("broker" -> Map(), "dealer" -> Map())
        )
      }
    }
    f.withTransport { transport2 =>
      whenReady(transport2.openSession()) { session =>
        session.id mustBe 2
      }
    }
  }
}

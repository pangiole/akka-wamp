package akka.wamp.client

import akka.actor.ActorSystem
import akka.wamp._
import com.typesafe.config.ConfigFactory

class ConnectionSpec extends ClientFixtureSpec(ActorSystem("test", ConfigFactory.parseString(
  """
    | akka {
    |   wamp {
    |     router {
    |       abort-unknown-realms = true
    |     }
    |   }
    | }
  """.stripMargin))) {

  "A client connection" should "fail open session when invalid realm is given" in { f =>
    f.withConnection { conn =>
      val session = conn.openSession("invalid..realm")
      whenReady(session.failed) { ex =>
        ex mustBe a[ConnectionException]
        ex.getMessage mustBe "invalid URI invalid..realm"
      }
    }
  }
  
  it should "fail open session when invalid roles are given" in { f =>
    f.withConnection { conn =>
      val session = conn.openSession("akka.wamp.realm", roles = Set("invalid"))
      whenReady(session.failed) { ex =>
        ex mustBe a[ConnectionException]
        ex.getMessage must startWith("invalid roles in Map(roles -> Map(invalid -> Map()))")
      }
    }
  }
  
  
  it should "fail open session when it turns out to have been disconnected from client side" in { f =>
    // TODO https://github.com/angiolep/akka-wamp/issues/11
    pending
    f.withConnection { conn =>
      // Client side disconnection 
      whenReady(conn.disconnect()) { _ =>
        val session = conn.openSession()
        whenReady(session.failed) { e =>
          e mustBe ConnectionException
          e.getMessage mustBe "disconnected"
        }
      }
    }
  }

  
  it should "fail open session if it turns out to have been disconnected from router side" in { f =>
    // TODO https://github.com/angiolep/akka-wamp/issues/11
    pending
    f.withConnection { conn =>
      // TODO how to simulate router side disconnection ???
      /*whenReady(routerRef ! Disconnect)*/ { // _ =>
        val session = conn.openSession()
        whenReady(session.failed) { e =>
          e mustBe ConnectionException
          e.getMessage mustBe "disconnected"
        }
      }
    }
  }
  
  
  it should "fail open session when router aborts" in { f =>
    f.withConnection { conn =>
      // if unknown.realm is given and the router cannot create it
      // then the router will reply an Abort message
      val session = conn.openSession("unknown.realm")
      session.failed.futureValue match {
        case AbortException(abort) =>
          abort.reason mustBe "wamp.error.no_such_realm"
          abort.details mustBe Dict("message" -> "The realm 'unknown.realm' does not exist.")
        case e =>
          fail(s"unexpected $e")
      }
    }
  }

  it should "succeed open multiple sessions on distinct connections" in { f =>
    f.withConnection { conn1 =>
      whenReady(conn1.openSession()) { session =>
        session.id mustBe 1
        session.details mustBe Map(
          "agent" -> "akka-wamp-0.8.0",
          "roles" -> Map("broker" -> Map(), "dealer" -> Map())
        )
      }
    }
    f.withConnection { conn2 =>
      whenReady(conn2.openSession()) { session =>
        session.id mustBe 2
      }
    }
  }
  
  
  it should "fail open multiple sessions on the same connection" in { f =>
    f.withConnection { conn =>
      whenReady(conn.openSession()) { session1 =>
        val session2 = conn.openSession()
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
  
  
  it should "succeed disconnect" in { f =>
    // TODO https://github.com/angiolep/akka-wamp/issues/29
    pending
    f.withConnection { conn => 
      whenReady(conn.disconnect()) { _ =>
        assert(true)
      }
    }
  }
}

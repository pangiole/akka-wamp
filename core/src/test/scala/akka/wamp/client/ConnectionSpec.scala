package akka.wamp.client

import akka.actor.ActorSystem
import akka.wamp._
import com.typesafe.config.ConfigFactory

class ConnectionSpec extends ClientBaseSpec(ActorSystem("test", ConfigFactory.parseString(
  """
    | akka {
    |   wamp {
    |     router {
    |       abort-unknown-realms = true
    |     }
    |   }
    | }
  """.stripMargin))) {


  "A client.Connection" should "succeed to be established and disconnected" in { f =>
    f.withConnection { conn =>
      conn.disconnected mustBe false
      whenReady(conn.disconnect()) { _ =>
        conn.disconnected mustBe true
      }
    }
  }

  
  it should "fail to open session when it turns out to have been disconnected from client side" in { f =>
    f.withConnection { conn =>
      whenReady(conn.disconnect()) { _ =>
        whenReady(conn.open().failed) { ex =>
          ex mustBe a[ClientException]
          ex.getMessage mustBe "Disconnected"
        }
      }
    }
  }

  
  it should "fail to open session if it turns out to have been disconnected from router side" in { f =>
    // TODO https://github.com/angiolep/akka-wamp/issues/11
    pending
    f.withConnection { conn =>
      val session = conn.open()
      whenReady(session.failed) { e =>
        e mustBe a[ClientException]
        e.getMessage mustBe "Disconnected"
      }
    }
  }


  it should "fail to open session when invalid realm URI is given" in { f =>
    f.withConnection { conn =>
      val session = conn.open("invalid..realm")
      whenReady(session.failed) { ex =>
        ex mustBe a[ClientException]
        ex.getMessage mustBe "invalid URI invalid..realm"
      }
    }
  }


  it should "fail to open session when router aborts" in { f =>
    f.withConnection { conn =>
      // if unknown.realm is given and the router cannot create it
      // then the router will reply an Abort message
      val session = conn.open("unknown.realm")
      whenReady(session.failed) { ex =>
        ex.getMessage mustBe "wamp.error.no_such_realm"
        // abort.details mustBe Dict("message" -> "The realm 'unknown.realm' does not exist.")
      }
    }
  }

  
  it should "fail to open multiple sessions on the same connection" in { f =>
    f.withConnection { conn =>
      val session1 = conn.open()
      whenReady(session1) { _ =>
        val session2 = conn.open()
        whenReady(session2.failed) { ex =>
          ex mustBe a[ClientException]
          ex.getMessage mustBe "Session already open"
        }
      }
    }
  }

  
  it should "succeed open multiple sessions on distinct connections" in { f =>
    f.withConnection { conn1 =>
      whenReady(conn1.open()) { session1 =>
        session1.id mustBe 1
        session1.realm mustBe "default"
        session1.details mustBe Map(
          "agent" -> "akka-wamp-0.13.0",
          "roles" -> Map("broker" -> Map(), "dealer" -> Map())
        )
      }
    }
    f.withConnection { conn2 =>
      whenReady(conn2.open()) { session2 =>
        session2.id mustBe 2
      }
    }
  }
}

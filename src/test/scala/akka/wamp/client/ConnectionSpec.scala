package akka.wamp.client

import akka.actor.{ActorSystem, PoisonPill}
import akka.wamp.Dict
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

  "A connection" should "fail open session when invalid realm is given" in { f =>
    val session = for {
      conn <- f.client.connect(f.url)
      ssn <- conn.openSession("invalid..realm")
    } yield ssn

    whenReady(session.failed) { e =>
      e mustBe a[ConnectionException]
      e.getMessage mustBe "invalid URI invalid..realm"
    }
  }
  
  it should "fail open session when invalid roles are given" in { f =>
    val session = for {
      conn <- f.client.connect(f.url)
      ssn <- conn.openSession("akka.wamp.realm", roles = Set("invalid"))
    } yield ssn

    whenReady(session.failed) { e =>
      e mustBe a[ConnectionException]
      e.getMessage must startWith("invalid roles in Set(invalid)")
    }
  }
  
  it should "fail open session when it turns out to be disconnected" in { f =>
    // TODO https://github.com/angiolep/akka-wamp/issues/11
    pending
    val connection = f.client.connect(f.url)
    whenReady(connection) { conn =>
      /*
      // Client side disconnection 
      whenReady(conn.disconnect()) { _ =>
        val session = conn.openSession()
        whenReady(session.failed) { e =>
          e mustBe ConnectionException
          e.getMessage mustBe "disconnected"
        }
      }
      
      // Server side disconnection
      // ???
      */
      f.router
    }
  }
  
  it should "fail open session when router aborts" in { f =>
    // if unknown.realm is given and the router cannot create it
    // then the router will reply an Abort message
    val session = for {
      conn <- f.client.connect(f.url)
      ssn <- conn.openSession("unknown.realm")
    } yield ssn

    session.failed.futureValue match {
      case AbortException(abort) =>
        abort.reason mustBe "wamp.error.no_such_realm"
        abort.details mustBe Dict("message" -> "The realm unknown.realm does not exist.")
      case other =>
        fail(s"unexpected $other")
    }
  }

  it should "succeed open sessions in optimal scenarios" in { f =>
    val session1 = for {
      conn <- f.client.connect(f.url)
      ssn <- conn.openSession()
    } yield ssn

    whenReady(session1) { s =>
      s.id mustBe 1
      s.details mustBe Map(
        "agent" -> "akka-wamp-0.7.0",
        "roles" -> Map("broker" -> Map(), "dealer" -> Map())
      )
    }

    val session2 = for {
      conn <- f.client.connect(f.url)
      ssn <- conn.openSession()
    } yield ssn
    
    whenReady(session2)(_.id mustBe 2)
  }
  
  
  it should "disconnect a connection" in { f =>
    // TODO https://github.com/angiolep/akka-wamp/issues/29
    val connection = f.client.connect(f.url)
    pending
    whenReady(connection) { conn =>
      /*whenReady(conn.disconnect()) { _ =>
        assert(true)
      }*/
    }
  }
}

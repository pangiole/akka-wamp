package akka.wamp.client

import akka.actor.{ActorSystem, PoisonPill}
import akka.wamp.Dict
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

  "A client transport" should "fail open session when invalid realm is given" in { f =>
    val session = for {
      transport <- Client().connect(f.url)
      session <- transport.open("invalid..realm")
    } yield session

    whenReady(session.failed) { e =>
      e mustBe a[TransportException]
      e.getMessage mustBe "invalid URI invalid..realm"
    }
  }
  
  it should "fail open session when invalid roles are given" in { f =>
    val session = for {
      transport <- Client().connect(f.url)
      session <- transport.open("akka.wamp.realm", roles = Set("invalid"))
    } yield session

    whenReady(session.failed) { e =>
      e mustBe a[TransportException]
      e.getMessage must startWith("requirement failed: invalid roles ")
    }
  }
  
  it should "fail open session when it turns to be disconnected" in { f =>
    pending
    val transport = Client().connect(f.url)
    whenReady(transport) { t =>
      // TODO cannot find a good way to simulate transport disconnection :-(
      f.router ! PoisonPill
      val session = t.open()
      whenReady(session.failed) { e =>
        e mustBe TransportException
        e.getMessage mustBe "disconnected"
      }
    }
  }
  
  it should "fail open session when router aborts" in { f =>
    // if unknown.realm is given and router cannot create it
    // then router will reply Abort message
    val session = for {
      transport <- Client().connect(f.url)
      session <- transport.open("unknown.realm")
    } yield session

    session.failed.futureValue match {
      case AbortException(abort) =>
        abort.reason mustBe "wamp.error.no_such_realm"
        abort.details mustBe Dict("message" -> "The realm unknown.realm does not exist.")
      case other =>
        fail(s"unexpected $other")
    }
  }

  it should "succeed open one or more sessions in optimal scenarios" in { f =>
    val session1 = for {
      transport <- Client().connect(f.url)
      session <- transport.open()
    } yield session

    whenReady(session1) { s =>
      s.id mustBe 1
      s.details mustBe Map(
        "agent" -> "akka-wamp-0.6.0",
        "roles" -> Map("broker" -> Map())
      )
    }

    val session2 = for {
      transport <- Client().connect(f.url)
      session <- transport.open()
    } yield session
    
    whenReady(session2)(_.id mustBe 2)
  }
  
  
}

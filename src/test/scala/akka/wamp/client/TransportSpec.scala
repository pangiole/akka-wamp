package akka.wamp.client

import akka.wamp.Dict
import akka.wamp.messages._

class TransportSpec extends ClientFixtureSpec {
  
  "A client transport" should "open a session" in { f =>
    val session = for {
      transport <- Client().connect(f.url)
      session <- transport.open()
    } yield session
    
    whenReady(session) { session =>
      session.id mustBe 1
      session.details mustBe Map(
        "agent" -> "akka-wamp-0.5.0", 
        "roles" -> Map("broker" -> Map())
      )
    }
  }
  
  it should "reject opening when illegal realm" in { f =>
    val session = for {
      transport <- Client().connect(f.url)
      session <- transport.open("illegal!realm")
    } yield session

    session.failed.futureValue match {
      case OpenException(message) =>
        message mustBe "requirement failed: invalid uri illegal!realm"
      case other =>
        fail(s"unexpected $other")
    } 
  }

  it should "reject opening when illegal roles" in { f =>
    val session = for {
      transport <- Client().connect(f.url)
      session <- transport.open("akka.wamp.realm", roles = Set("wrong"))
    } yield session

    session.failed.futureValue match {
      case OpenException(message) =>
        message must startWith("requirement failed: invalid roles ")
      case other =>
        fail(s"unexpected $other")
    }
  }

  
  it should "fail opening when unknown realm" in { f =>
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
}

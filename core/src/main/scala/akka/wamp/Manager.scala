package akka.wamp

import java.io.FileInputStream
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManager, TrustManager, KeyManagerFactory, TrustManagerFactory, SSLContext}

import akka.actor.SupervisorStrategy._
import akka.actor.{Actor, ActorInitializationException, ActorKilledException, ActorLogging, DeathPactException, OneForOneStrategy, Props, SupervisorStrategy}
import akka.wamp.client.ConnectionHandler
import akka.wamp.messages.{Bind, Connect}
import akka.wamp.router.ConnectionListener
import com.typesafe.config.Config

import scala.collection.JavaConverters._
import java.util.Arrays.asList

private[wamp] class Manager extends Actor with ActorLogging {
  implicit val actorSystem = context.system
  val config = actorSystem.settings.config

  def managers[T](key: String)(fn: (KeyStore, Array[Char]) => Seq[T]): Seq[T] = {
    val stores: Seq[Config] = config.getConfigList(s"ssl-config.$key.stores").asScala
    stores.flatMap { store =>
      val path = store.getString("path")
      val password = store.getString("password").toCharArray
      val tpe = store.getString("type")

      val keyStore = KeyStore.getInstance(tpe)
      val inputStream = new FileInputStream(path)
      try keyStore.load(inputStream, password)
      finally inputStream.close()

      fn(keyStore, password)
    }
  }

  val keyManagers = managers[KeyManager]("keyManager") { (keyStore, password) =>
    val kmf = KeyManagerFactory.getInstance("SunX509")
    kmf.init(keyStore, password)
    asList(kmf.getKeyManagers).asScala.flatten
  }

  val trustManagers = managers[TrustManager]("trustManager") { (keyStore, _) =>
    val tmf = TrustManagerFactory.getInstance("SunX509")
    tmf.init(keyStore)
    asList(tmf.getTrustManagers).asScala.flatten
  }


  val sslContext = SSLContext.getInstance("TLS")
  sslContext.init(keyManagers.toArray, trustManagers.toArray, new SecureRandom)

  override def receive: Receive = {
    case cmd @ Bind(router, endpoint) => {
      val binder = sender()
      context.actorOf(ConnectionListener.props(binder, router, endpoint, sslContext))
    }

    case cm @ Connect(address, format) =>
      val connector = sender()
      context.actorOf(ConnectionHandler.props(connector, address, format, sslContext))
  }


  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(loggingEnabled = true) {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Stop
    case _: DeathPactException => Stop
    case _: Exception =>
      Restart
  }
}


private[wamp] object Manager {
  def props() = Props(new Manager())
}

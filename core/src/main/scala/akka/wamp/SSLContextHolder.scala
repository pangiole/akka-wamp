package akka.wamp

import java.io.FileInputStream
import java.security.{KeyStore, SecureRandom}
import java.util.Arrays.asList
import javax.net.ssl.{KeyManager, KeyManagerFactory, SSLContext, TrustManager, TrustManagerFactory}

import akka.actor.Actor
import akka.event.Logging
import com.typesafe.config.Config

import scala.collection.JavaConverters._

private[wamp] class SSLContextHolder(logSource: Actor) {

  val config = logSource.context.system.settings.config

  val log = Logging(logSource.context.system, logSource)


  def withKeyStore[T](key: String)(fn: (KeyStore, Array[Char]) => Seq[T]): Seq[T] = {
    val stores: Seq[Config] = config.getConfigList(s"ssl-config.$key.stores").asScala
    stores.flatMap { store =>
      try {
        val path = store.getString("path")
        val password = store.getString("password").toCharArray
        val tpe = store.getString("type")

        val keyStore = KeyStore.getInstance(tpe)
        val inputStream = new FileInputStream(path)
        try keyStore.load(inputStream, password)
        finally inputStream.close()

        fn(keyStore, password)
      } catch {
        case ex: Throwable =>
          log.warning(ex.getMessage, ex)
          Seq()
      }
    }
  }

  val keyManagers = withKeyStore[KeyManager]("keyManager") { (keyStore, password) =>
    try {
      val kmf = KeyManagerFactory.getInstance("SunX509")
      kmf.init(keyStore, password)
      asList(kmf.getKeyManagers).asScala.flatten
    } catch {
      case ex: Throwable =>
        log.warning(ex.getMessage, ex)
        Seq()
    }
  }

  val trustManagers = withKeyStore[TrustManager]("trustManager") { (keyStore, _) =>
    try {
      val tmf = TrustManagerFactory.getInstance("SunX509")
      tmf.init(keyStore)
      asList(tmf.getTrustManagers).asScala.flatten
    } catch {
      case ex: Throwable =>
        log.warning(ex.getMessage, ex)
        Seq()
    }
  }

  var sslContext = SSLContext.getInstance("TLS")
  sslContext.init(keyManagers.toArray, trustManagers.toArray, new SecureRandom)
}


object SSLContextHolder {
  def apply(logSource: Actor): SSLContextHolder = new SSLContextHolder(logSource)
}
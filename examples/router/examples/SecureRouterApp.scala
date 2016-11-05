package examples

import java.io.InputStream
import java.security.KeyStore

import com.typesafe.sslconfig.akka.AkkaSSLConfig

/*
 * It demonstrate how to launch an SSL/TLS router
 */
class SecureRouterApp extends App {

  import akka.actor._

  implicit val system = ActorSystem()
  val sslConfig = AkkaSSLConfig()
  system.actorOf(Props[Binder])

  val password: Array[Char] = scala.io.Source.fromFile("password").getLines().take(0).toString().toCharArray

  val ks: KeyStore = KeyStore.getInstance("PKCS12")
  val keystore: InputStream = getClass.getClassLoader.getResourceAsStream("server.p12")
  require(keystore != null, "Keystore required!")
  ks.load(keystore, password)
  /*
   * 1. Spawn an embedded Router
   * 2. Bind it to the Wamp extension manager
   * 3. Receive the Bound signal
   */
  class Binder extends Actor with ActorLogging {
    override def receive: Receive = ???
  }
  
  
}

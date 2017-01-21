/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

import akka.Done
import akka.actor._
import akka.wamp.client._

object PoloniexScalaClient extends App {

  val actorSystem = ActorSystem()
  val client = Client(actorSystem)
  implicit val executionContext = actorSystem.dispatcher

  client.connect("wss://api.poloniex.com", "json").map ( c =>
    c.open("realm1").map { implicit s =>

      // TODO provide a subscribe macro that takes 10-arguments
      // currencyPair, last, lowestAsk, highestBid, percentChange, baseVolume, quoteVolume, isFrozen, 24hrHigh, 24hrLow

      s.subscribe("ticker", event => {
        event.args.map { args =>
          println(args)
          Done
        }
      })
    }
  )
  .failed.foreach { ex =>
    ex.printStackTrace()
    System.exit(-1)
  }

}

# akka-wamp/examples/poloniex

[Poloniex](https://www.poloniex.com) is an US-based cryptocurrency exchange offering maximum security and advanced trading features.

This examples demonstrate how to write an Akka Wamp client able to connect to the [Poloniex API](https://poloniex.com/support/api/) endpoint, open a session and subscribe some of its topics so to receive ticks and currency pairs events.


## Run

```bash
cd akka-wamp
sbt 

example-poloniex/runMain PoloniexScalaClient
```

You may also run ``PoloniexJavaClient`` if you prefer to do so.


## TLS

The Poloniex server presents itself with a certificate signed by CAs - Certification Authorities already known by your underlying JRE - Java Runtime Environment. 

Since the Poloniex server does not ask for client's certificates, there's **no** particular TLS configuration you're required to setup for your Akka Wamp client. It should just work! 

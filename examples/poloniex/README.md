# examples/poloniex

[Poloniex](https://www.poloniex.com) is an US-based cryptocurrency exchange offering maximum security and advanced trading features.

This examples demonstrate how to write an Akka Wamp client able to connect to the [Poloniex API](https://poloniex.com/support/api/) endpoint, open a session and subscribe some of its topics so to receive ticks and currency pairs events.

## Run

```bash
cd akka-wamp

sbt
project poloniex
run
```

## TLS/SSL

Poloniex server presents itself with a certificate signed by CA - Certification Authorities already known by the  Java Runtime Environment. As Poloniex server doesn't ask for any client certificate, there's no particular TLS/SSL you're required to configure for Akka Wamp. 

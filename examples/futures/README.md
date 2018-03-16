# akka-wamp/examples/futures
This example demonstrates you how to write WAMP clients using the [Akka Wamp Futures](http://angiolep.github.io/projects/akka-wamp/client/futures.html) based API. 

Please note that, as the client in this example attempts to establish local connections, you're required to run the [examples/router](../router/README.md) first.

## Run

```bash
cd akka-wamp
sbt

example-futures/runMain FuturesScalaClient default
```


## TLS
It requires you to generate the necessary TLS cryptographic material. Just call the provided ``mkcerts.sh`` script AFTER having done the same for the [examples/router](../router/README.md) project. 

```bash
cd akka-wamp
./examples/futures/mkcerts.sh
```

Run the client

```bash
sbt
example-futures/runMain FuturesScalaClient secured
```

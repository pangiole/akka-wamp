# akka-wamp/examples/futures
This example demonstrates you how to write clients using the [Futures API](http://angiolep.github.io/projects/akka-wamp/client/futures.html). As the clients in this example attempt local connections, you're required to run the [examples/router](../router/README.md) first.

## Run

```bash
cd akka-wamp
sbt -Dakka.loglevel=DEBUG

project futures
run
```


## FuturesSecuredClient
It requires you to generate the necessary TLS/SSL cryptographic material. Just call the provided ``mkcerts.sh`` AFTER doing the same for the [examples/router](../router/README.md) project. 

```bash
cd akka-wamp
./examples/futures/mkcerts.sh
```

Run the client

```bash
sbt -Dakka.loglevel=DEBUG -Djavax.net.debug=all

project futures
run
```

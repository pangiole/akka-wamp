# examples/router

This examples demonstrate how to spin an embedded Akka Wamp router on both the local unsecured endpoint and the local TLS/SSL endpoint.

### Local unsecured
It's the default configuration. Just run it and the router will start accepting connection on ``ws://localhost:8080/wamp``

```bash
cd akka-wamp
sbt -Dakka.loglevel=DEBUG

project router
runMain LocalRouterApp
```


## Local TLS/SSL

This example provides you with a realistic TLS/SSL configuration. It assumes your server DNS is ``example.com`` and makes you create the necessary cryptographic material.


### Setup
Make ``example.com`` an additional alias name for ``127.0.0.1`` (the loopback interface). Then make a TLS/SSL certificate chain for it.
  
  
```bash
sudo echo "127.0.0.1 example.com" >/etc/hosts

cd akka-wamp
./examples/router/mkcerts.sh
```

The ``mkcert.sh`` script works on any Unix-like systems able to provide the following command line tools:

* [JDK keytool](https://docs.oracle.com/javase/8/docs/technotes/tools/windows/keytool.html)
* [OpenSSL](https://github.com/openssl/openssl)


### Run

```bash
cd akka-wamp 
sbt -Dakka.loglevel=DEBUG -Djavax.net.debug=all

project router
runMain SecuredRouterApp
```

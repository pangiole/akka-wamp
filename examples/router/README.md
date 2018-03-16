# akka-wamp/examples/router

This examples demonstrate how to launch an embedded Akka Wamp router listening onto two endpoints: 

 * the ``default`` (unsecured) one whose address is ``ws://localhost:8080/wamp`` 
 * and an additional ``secured`` TLS endpoint whose address is ``wss://example.com:8433/wamp``


### TLS certificates
This example provides you with a pretty realistic TLS configuration. It assumes your server DNS name is ``example.com`` and it provides you the ``mkcerts.sh`` script to let you create the necessary cryptographic material.


First of all, make ``example.com`` an additional alias name for ``127.0.0.1`` (the loopback interface).
  
```bash
sudo echo "127.0.0.1 example.com" >/etc/hosts
```

Then make a TLS certificate chain for it.

```bash
cd akka-wamp
./examples/router/mkcerts.sh
```

The ``mkcert.sh`` script works on any Unix-like systems able to provide the following command line tools:

* [JDK keytool](https://docs.oracle.com/javase/8/docs/technotes/tools/windows/keytool.html)
* [OpenSSL](https://github.com/openssl/openssl)

MS Windows systems are not yet supported by this example.


### Run

```bash
cd akka-wamp 
sbt -Djavax.net.debug=all

example-router/runMain RouterScalaApp
```

You may also run ``RouterJavaApp`` if you prefer to do so.


### Client
Now give the [examples/futures](../futures) a better try to see if it really connects to the endpoint secured via TLS.
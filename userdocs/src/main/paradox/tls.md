# TLS
Akka Wamp supports [TLS](https://tools.ietf.org/html/rfc5246) - Transport Layer Security protocol via its simple configuration (no coding required).

## Configuration
As per following configuration excerpt, by default, Akka Wamp loads the trust store shipped with the underlying __JRE__ - Java Runtime Environment.

@@snip[application.conf](../../../../router/src/main/resources/application.conf){ #tls }

The ``cacerts`` file, having proprietary format __JKS__ - Java KeyStore, is a binary repository of TLS certificates for the most accredited CA - Certificate Authorities that any application in the world can safely trust. 

Next sections explain how to customize the above settings.




## Router
To properly configure a router that binds to a TLS endpoint, it is important to make a clear distinction: either the router asks for the client's certificate or it does not.

@@@warning { title='Limitation' }
Bear in mind that Akka Wamp does **not** support routers demanding for client's certificates yet
@@@



### Certificate chain
In either cases, the router/server must own and present itself with a certificate having its public key joined. That certificate must have been issued and signed by some CA - Certificate Authority that any client can safely trust. The CA can be an intermediate authority or a root self-certified authority. In real world scenarios, the router/server's (owner's) certificate begins a chain of certificates, maybe with one or more intermediary issuers but certainly ending to some accredited root CA.

![tls](tls.png)

### Key stores
A router which is **not** demanding for the client's certificate, needs to be configured with key stores containing its own public/private key pair, its own certificate and the certificates of all the issuers in its certificate's chain up to the root certificate authority.
  
```hocon
ssl-config {
  keyManager {
    stores = [
      { type = "JKS", path = ${router.home}/example.com.jks, password = "changeit" },
      { type = "JKS", path = ${java.home}/lib/security/cacerts, password = "changeit" }
    ]
  }
  # trustManager ... not needed!
}
```

Such a key stores can be created using the [Java KeyTool](https://docs.oracle.com/javase/8/docs/technotes/tools/windows/keytool.html), with few commands as simple as:

@@snip[mkcerts.sh](../../../../examples/router/mkcerts.sh){ #make-example-cert }

### Certificate Authorities
In real world scenarios, a certificate such as the ``example.com`` we created above, must have been issued/signed by intermediary or root CA - Certification Authorities. For that reason, whoever owns the router/server creates and submits a CSR - Certificate Signing Request to some CA chosen at its discretion.

For test purposes, the router/server owner could create a _"fake"_ self-signed root CA certificate with few commands as simple as:

@@snip[mkcerts.sh](../../../../examples/router/mkcerts.sh){ #make-ca-cert }

### Signing Request

For test purposes, the router/server owner could also create a CSR - Certificate Signing Request, so to simulate its submission and its completion, with few commands as simple as:

@@snip[mkcerts.sh](../../../../examples/router/mkcerts.sh){ #csr }





## Client
To properly configure an Akka Wamp client that connects to a WAMP router accepting connections using the TLS protocol, it is important to firstly make a clear distinction: either the router asks for the client's certificate or it does not.

@@@warning { title='Limitation' }
Akka Wamp does not support routers asking for client's certificates.
@@@


The only scenario supported by Akka Wamp is the one that the router does **not** ask for client's certificate. In this scenario, the client does not need to be configured with any Java _"keys stores"_ but rather with _"trust stores"_ only. Client's trust stores shall contain certificates of all issuers in the certificate chain of the remote router. 


### Trust stores

By default, Akka Wamp loads the trust store shipped with the underlying __JRE__ - Java Runtime Environment, which contains (almost) all the globally accredited CA - Certificate Authorities. Therefore, it's more likely you don't need to change the default configuration at all.

For test purposes, the client might connect to a router owning a certificate issued by some fake root CA, such as the one created in the above section. In this case, the client needs to be configured with a trust store containing the certificate of that fake root CA.

```hocon
ssl-config {
  trustManager {
    stores = [
      { type = "JKS", path = ${client.home}/trust-store.jks, password = "changeit" },
      { type = "JKS", path = ${java.home}/lib/security/cacerts, password = "changeit" }
    ]
  }
}
```

Such a trust store can be create running scripts like the following:


@@snip[mkcerts.sh](../../../../examples/futures/mkcerts.sh){ #make-trust-store }


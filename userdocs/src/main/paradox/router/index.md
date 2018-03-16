@@@ index
* [Config](config.md)
* [Embedded](embedded.md)
* [Standalone](standalone.md)
* [Docker](docker.md)
* [Internals](internals.md)
@@@ 


# Router
Akka Wamp provides you with a basic, and very minimalistic, WAMP Router that can be either embedded into your Akka application or launched as standalone server application. It implements core features such as:

* WAMP Basic Profile,
* Both broker and dealer roles,
* JSON serialization,
* WebSocket transport

@@@warning
Though perfectly functional, the WAMP Router provided by Akka Wamp is intended for development and experimentation purposes only. For serious applications, you're advised to adopt a more mature production ready WAMP router such as [Crossbar.IO](http://crossbar.io/) instead.
@@@




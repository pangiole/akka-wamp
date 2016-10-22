# Router configuration
Either the embedded or the standalone router can be configured by applying the following settings:
 
```bash
akka {
  wamp {
    router {
      # The local filesystem path the router will serve static
      # web resources (such as HTML docs) out of
      #
      webroot = "./webroot"
    
      # The boolean switch to validate against strict URIs 
      # rather than loose URIs
      #
      validate-strict-uris = false
   
      # The boolean switch to NOT automatically create realms 
      # if they don't exist yet.
      #
      abort-unknown-realms = false
   
      # The boolean switch to drop offending messages (e.g. 
      # not deserializable or against the protocol).
      #
      # By default, offending messages will cause session to be
      # closed and transport to be disconnected. Set this switch on 
      # if you prefer to rather drop offending messages and resume.
      #
      drop-offending-messages = false
      
      # Named transport configurations
      #
      transport {
        default {
          # Transport protocol can be:
          #
          # - tcp
          #     Raw TCP
          # - tsl
          #     Transport Secure Layer
          # - ws    
          #     WebSocket 
          # - wss
          #     WebSocket over TLS
          #
          protocol = "ws"

          # Transport subprotocol can be:
          # 
          # - wamp.2.json
          #   JSON Javascript Object Notation
          #
          # - wamp.2.msgpack
          #   Message Pack
          #
          subprotocol = "wamp.2.json"

          # The TCP interface to bind to
          #
          iface = "127.0.0.1"

          # The TCP port number (between 0 and 65536) to bind to.
          # If set to 0 the first available randome port number 
          # will be chosen
          #
          port = 8080

          # (Only for "ws" and "wss" protocols)
          # The URL path incoming HTTP Upgrade request are expected 
          # to be addressed to
          #
          wspath = "router"
        },
        #secure {
        #  protocol = "wss"
        #  iface = "127.0.0.1"
        #  port = 8443
        #  path = "router"
        #}
      }
    }
  }
}
```
      
Above default settings can be overridden

 * (for standalone router) by editing the ``conf/application.conf`` file
 * (for embedded router) by providing an ``application.conf`` file on the classpath,
 * by passing system properties to the Java interpreter (e.g. ``-Dakka.wamp.router.transport.default.port=9090``)


## Logging
```bash
akka {
  #
  # Following are already set by akka-wamp as default
  #
  loggers = ["akka.event.slf4j.Slf4jLogger"]
  loglevel = "INFO"
  logging-filter = "akka.event.slf4j.Slf4jLoggingFilter"
    
  # Just provide an additional logback.xml file on the classpath 
  # so to customize your loggers, appenders and patterns
  # 
}
```

## Security
TBD

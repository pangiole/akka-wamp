# Configuration

```bash
akka {
  wamp {
    client {
      # The boolean switch to validate against strict URIs 
      # rather than loose URIs
      #
      validate-strict-uris = false
      
      # NOTE
      # Clients will always disconnect on offending messages
      # No configuration settings is provided to change this behaviour.
      #
    }
  }
}
```
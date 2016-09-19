# Configuration

```bash
akka {
  wamp {
    client {
      # The boolean switch to validate against strict URIs 
      # rather than loose URIs
      #
      validate-strict-uris = false

      # The boolean switch to disconnect those peers that send 
      # offending messages (e.g. not deserializable or causing
      # session failures)
      #
      # By default, offending messages are just dropped and 
      # the router resumes processing next incoming messages
      #
      disconnect-offending-peers = false
    }
  }
}
```
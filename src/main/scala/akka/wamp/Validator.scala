package akka.wamp

import akka.wamp.client.Client

/**
  * It validates values against WAMP protocol types
  * 
  * @param strictUris is a boolean switch to validate against strict URIs rather than loose URIs
  */
class Validator(strictUris: Boolean) {

  private val uriRegex = 
    if (strictUris)  """^([0-9a-z_]+\.)*([0-9a-z_]+)$""".r
    else /* loose */ """^([^\s\.#]+\.)*([^\s\.#]+)$""".r

  private val dictKeyRegex = 
    """[a-z][a-z0-9_]{2,}""".r
  
  def validate[T](value: T): Unit = value match {
    case id: Id =>
      if (!(id >= Id.min && id <= Id.max))
        throw new IllegalArgumentException(s"invalid ID $id")
      
    case uri: Uri => 
      if (!uriRegex.pattern.matcher(uri).matches) 
        throw new IllegalArgumentException(s"invalid URI $uri")

    case dict: Dict =>
      if (dict == null)
        throw new IllegalArgumentException(s"invalid DICT")
      dict.keys.foreach { key =>
        if (!dictKeyRegex.pattern.matcher(key).matches)
          throw new IllegalArgumentException(s"invalid KEY $key")
      }
  }

  /**
    * Validate roles in the given details dictionary.
    * 
    * A Client must announce the roles it supports via "Hello.Details.roles|dict", 
    * with a key mapping to a "Hello.Details.roles.<role>|dict" where "<role>" can be:
    *
    * - "publisher"
    * - "subscriber"
    * - "caller"
    * - "callee"
    * 
    * @param details is the dictionary with roles to validate
    */
  def validateRoles(details: Dict): Unit = {
    if (!details.isDefinedAt("roles") || details.roles.isEmpty) 
      throw new IllegalArgumentException(s"missing roles in ${details}")
    
    if(!details.roles.forall(Roles.client.contains(_)))
      throw new IllegalArgumentException(s"invalid roles in ${details.roles}")
  }
}

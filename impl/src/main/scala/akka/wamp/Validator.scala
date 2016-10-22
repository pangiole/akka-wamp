package akka.wamp

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

    case dict: Map[_, _] =>
      if (dict == null)
        throw new IllegalArgumentException(s"invalid DICT")
      dict.keys.foreach { key =>
        if (!dictKeyRegex.pattern.matcher(key.asInstanceOf[String]).matches)
          throw new IllegalArgumentException(s"invalid KEY $key")
      }
  }

  /**
    * Validate client roles in the given details dictionary.
    * 
    * @param details is the dictionary with roles to validate
    */
  def validateClientRoles(details: Dict): Unit = {
    
    def isValidRole(role: Role) = Roles.client.contains(role)
    
    if (details.isDefinedAt("roles")) {
      details("roles") match {
        case rolesDict: Map[_, _] =>
          if (rolesDict.isEmpty) {
            /**
              * A client must announce the roles it supports via "Hello.Details.roles|dict", 
              * with a key mapping to a "Hello.Details.roles.<role>|dict" where "<role>" can be:
              *
              * - "publisher"
              * - "subscriber"
              * - "caller"
              * - "callee"
              */
            throw new IllegalArgumentException(s"missing roles in $details")
          }
          val rolesKey = rolesDict.keySet.map(_.asInstanceOf[String])
          if (!rolesKey.forall(Roles.client.contains(_))) {
            throw new IllegalArgumentException(s"invalid roles in $details")
          }
          val rolesValues = rolesDict.values.map(_.asInstanceOf[Map[String, _]])
          if (!rolesValues.forall(_.isEmpty)) {
            /**
              * The "<role>|dict" is in turn a dictionary describing features
              * supported by the peer for that role.
              *
              * This MUST be empty for WAMP Basic Profile implementations, and MUST
              * be used by implementations implementing parts of the Advanced Profile
              * to list the specific set of features they support.
              */
            throw new IllegalArgumentException(s"invalid roles in $details")
          }  
        case _ => {
          throw new IllegalArgumentException(s"invalid roles in $details")
        }
      }
    } else {
      throw new IllegalArgumentException(s"missing roles in $details")
    }
  }
}

package akka.wamp.messages

import akka.wamp.{Id, Uri}

class Validator(strictUris: Boolean) {


  private val regex = 
    if (strictUris)  """^([0-9a-z_]+\.)*([0-9a-z_]+)$""".r
    else /* loose */ """^([^\s\.#]+\.)*([^\s\.#]+)$""".r
  
  def validate[T](value: T): Unit = value match {
    case id: Id =>
      if (!(id >= Id.Min && id <= Id.Max))
        throw new IllegalArgumentException(s"invalid ID $id")
      
    case uri: Uri => 
      if (!regex.pattern.matcher(uri).matches) 
        throw new IllegalArgumentException(s"invalid URI $uri")
  }
}

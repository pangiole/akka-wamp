package akka.wamp


package object messages {

  /**
    * Uniform Resource Identifier
    */
  type Uri = String

  /**
    *  Dictionary
    */
  type Dict = Map[String, Map[_, _]]

}

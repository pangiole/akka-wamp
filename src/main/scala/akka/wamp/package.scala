package akka

import scala.collection.mutable.{Map => MutableMap}

package object wamp {

  val HELLO = 1
  val WELCOME = 2
  val ABORT = 3
  val GOODBYE = 6
  val ERROR = 8

  /**
    * Unique identifiers being used to distinguish [[Session]]s, [[Publication]]s, 
    * [[Subscription]]s, [[Registration]]s and [[Request]]s
    */
  object Id {
    val MIN = 0L
    val MAX = 9007199254740992L
    def draw = (scala.util.Random.nextDouble() * MAX).toLong
  }

  /**
    * An identifier generator
    */
  type IdGenerator = (Map[Long, Any]) => Long
  
  /**
    * Uniform Resource Identifier
    */
  type Uri = String

  /**
    *  Dictionary for options and details elements
    */
  type Dict = Map[String, Any]
  
  
  class DictBuilder private(entries: MutableMap[String, Any]) {
    def withRoles(rs: String*): DictBuilder = {
      var m = Map.empty[String, Any]
      rs.foreach(r => m += (r -> Map()))
      new DictBuilder(entries + ("roles" -> m))
    }
    def withEntry(key: String, value: Any) = {
      /* Some WAMP messages contain "Options|dict" or "Details|dict" elements.
       * This allows for future extensibility and implementations that only
       * provide subsets of functionality by ignoring unimplemented
       * attributes.  
       * TODO Keys in "Options" and "Details" MUST be of type "string" and MUST match the regular expression "[a-z][a-z0-9_]{2,}" 
       * for WAMP _predefined_ keys.  Implementations MAY use implementation-specific
       * keys that MUST match the regular expression "_[a-z0-9_]{3,}".
       * Attributes unknown to an implementation MUST be ignored.
       */
      new DictBuilder(entries + (key -> value))
    }
    def build() = entries.toMap
  }
  
  object DictBuilder {
    def apply() = new DictBuilder(MutableMap.empty[String, Any])
  }
}

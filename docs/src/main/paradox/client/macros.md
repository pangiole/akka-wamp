# Macros

@@@warning
As macros are not yet supported in Java, this section is for Scala developers only.
@@@


Akka Wamp provides you with some useful [Scala Macros](http://docs.scala-lang.org/overviews/macros/overview.html) to reduce the boilerplate code you would have to write to consume/handle arguments from incoming [Payloads](../payloads.html)

## Futures
Macros described by this section are those you can use in writing client applications with the Akka Wamp [Futures Client API](../futures.html).

### Subscribe
It allows you to subscribe a [lambda consumer](./futures.html#lambda-consumer) to a topic.

```scala
Client().open().foreach { implicit session =>  
  subscribe("mytopic", (name: String, age: Int) => {
    println("$name is $age years old")
  })
}
```

This macro expands to an [event consumer](./futures.html#event-consumer) that expects an implicit ``Session`` object in scope. The expanded consumer is be able to apply the supplied lambda with arguments carried by incoming events. Arguments are matched to parameters as described in the [deserialization](#deserialization) section below.


### Register
It allows you to subscribe a [lambda handler](./futures.html#lambda-handler) as a procedure.

```scala
Client().open().foreach { implicit session =>  
  register("myproc", (name: String, age: Int) => {
    name.length + age
  })
}
```

This macro expands to an [invocation handler](./futures.html#invocation-handler) that expects an implicit ``Session`` object in scope. The expanded handler is be able to apply the supplied lambda with arguments carried by incoming invocations. Arguments match parameters as described in the [deserialization](#deserialization) section below.


### Deserialization

Incoming arguments match lambda parameters by applying the logic described in this section. Be ``rank`` the number of supplied lambda parameters, ``args`` the incoming list of indexed arguments and ``kwargs`` the (optional) incoming dictionary of named arguments, then Akka Wamp:

* Prefers to match ``args`` as first choice and ``kwargs`` as second.
* Selects either of ``args`` or ``kwargs`` depending on their length. The arguments having length different than rank will be discarded.
* ``args`` must match data types of same position parameters. ``kwargs`` must match parameter names too.
* If neither ``args`` nor ``kwargs`` match parameters then deserialization fails with ``ClientException``


For example, any of the following incoming JSON messages:

```json
[48,1,{},"myproc",["paolo", 99]]  
[48,2,{},"myproc",["paolo", 99],{"male":true}]  
[48,3,{},"myproc",[],{"name":"paolo","age":99}]
[48,4,{},"myproc",[true],{"name":"paolo","age":99}]
```

match the following lambda parameters:

```scala
(name: String, age: Int) => /* do something with name and age */
```

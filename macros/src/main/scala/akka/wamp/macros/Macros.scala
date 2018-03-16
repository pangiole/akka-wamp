package akka.wamp.macros

import akka.wamp.client.{Registration, Subscription}

import scala.concurrent.Future
import scala.reflect.macros.blackbox.Context

/**
  * Provides implementations for macros
  *
  * == Subscribe ==
  *
  * {{{
  *   val session: Future[Session] = ....
  *   session.flatMap { implicit s =>
  *     subscribe("mytopic", (name: String, age: Int) => {
  *       println(s"$name is $age years old")
  *     })
  *   }
  * }}}
  *
  * == Register ==
  *
  * {{{
  *   val session: Future[Session] = ....
  *   session.flatMap { implicit s =>
  *     register("myprocedure", (name: String, age: Int) => {
  *       name.length + age
  *     })
  *   }
  * }}}
  */

object Macros {

  def subscribe0_impl(c: Context)(topic: c.Expr[String], lambda: c.Expr[Function0[Unit]]): c.Expr[Future[Subscription]] =
    c.Expr[Future[Subscription]](c.parse(subscribe(c)(topic, lambda.tree.asInstanceOf[c.universe.Function])))

  def subscribe1_impl[T: c.WeakTypeTag](c: Context)(topic: c.Expr[String], lambda: c.Expr[Function1[T, Unit]]): c.Expr[Future[Subscription]] =
    c.Expr[Future[Subscription]](c.parse(subscribe(c)(topic, lambda.tree.asInstanceOf[c.universe.Function])))

  def subscribe2_impl[T1: c.WeakTypeTag, T2: c.WeakTypeTag](c: Context)(topic: c.Expr[String], lambda: c.Expr[Function2[T1, T2, Unit]]): c.Expr[Future[Subscription]] =
    c.Expr[Future[Subscription]](c.parse(subscribe(c)(topic, lambda.tree.asInstanceOf[c.universe.Function])))

  def subscribe3_impl[T1: c.WeakTypeTag, T2: c.WeakTypeTag, T3: c.WeakTypeTag](c: Context)(topic: c.Expr[String], lambda: c.Expr[Function3[T1, T2, T3, Unit]]): c.Expr[Future[Subscription]] =
    c.Expr[Future[Subscription]](c.parse(subscribe(c)(topic, lambda.tree.asInstanceOf[c.universe.Function])))

  def subscribe4_impl[T1: c.WeakTypeTag, T2: c.WeakTypeTag, T3: c.WeakTypeTag, T4: c.WeakTypeTag](c: Context)(topic: c.Expr[String], lambda: c.Expr[Function4[T1, T2, T3, T4, Unit]]): c.Expr[Future[Subscription]] =
    c.Expr[Future[Subscription]](c.parse(subscribe(c)(topic, lambda.tree.asInstanceOf[c.universe.Function])))


  private def subscribe(c: Context)(topic: c.Expr[String], lambda: c.universe.Function): String = {
    val (tpc, arity, args, kwargs) = inspect(c)(topic, lambda)
    val code = s"""
       | import akka.wamp.client._
       | import scala.concurrent._
       |
       | implicitly[Session].subscribe($tpc, event => {
       |   val errmsg = "Couldn't invoke lambda consumer for ${tpc.replace('"','\'')}"
       |   val args = event.args
       |   if (args.size == $arity) {
       |     try {
       |       $lambda.apply(${args.mkString(",")})
       |     }
       |     catch {
       |       case ex: Throwable => throw new ClientException(errmsg+": "+ex.getMessage, ex)
       |     }
       |   }
       |   else {
       |     val kwargs = event.kwargs
       |     if (kwargs.size == $arity) {
       |       try {
       |         $lambda.apply(${kwargs.mkString(",")})
       |       }
       |       catch {
       |         case ex: Throwable => throw new ClientException(errmsg+": "+ex.getMessage, ex)
       |       }
       |     }
       |     else {
       |       val cause = "unexpected number of arguments"
       |       throw new ClientException(errmsg+": "+cause, new IllegalArgumentException(cause))
       |     }
       |   }
       | })
       """.stripMargin
    code
  }


  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  def register0_impl[R: c.WeakTypeTag](c: Context)(procedure: c.Expr[String], lambda: c.Expr[Function0[R]]): c.Expr[Future[Registration]] =
    c.Expr[Future[Registration]](c.parse(register(c)(procedure, lambda.tree.asInstanceOf[c.universe.Function])))

  def register1_impl[T1: c.WeakTypeTag, R: c.WeakTypeTag](c: Context)(procedure: c.Expr[String], lambda: c.Expr[Function1[T1, R]]): c.Expr[Future[Registration]] =
    c.Expr[Future[Registration]](c.parse(register(c)(procedure, lambda.tree.asInstanceOf[c.universe.Function])))

  def register2_impl[T1: c.WeakTypeTag, T2: c.WeakTypeTag, R: c.WeakTypeTag](c: Context)(procedure: c.Expr[String], lambda: c.Expr[Function2[T1, T2, R]]): c.Expr[Future[Registration]] =
    c.Expr[Future[Registration]](c.parse(register(c)(procedure, lambda.tree.asInstanceOf[c.universe.Function])))

  def register3_impl[T1: c.WeakTypeTag, T2: c.WeakTypeTag, T3: c.WeakTypeTag, R: c.WeakTypeTag](c: Context)(procedure: c.Expr[String], lambda: c.Expr[Function3[T1, T2, T3, R]]): c.Expr[Future[Registration]] =
    c.Expr[Future[Registration]](c.parse(register(c)(procedure, lambda.tree.asInstanceOf[c.universe.Function])))

  def register4_impl[T1: c.WeakTypeTag, T2: c.WeakTypeTag, T3: c.WeakTypeTag, T4: c.WeakTypeTag, R: c.WeakTypeTag](c: Context)(procedure: c.Expr[String], lambda: c.Expr[Function4[T1, T2, T3, T4, R]]): c.Expr[Future[Registration]] =
    c.Expr[Future[Registration]](c.parse(register(c)(procedure, lambda.tree.asInstanceOf[c.universe.Function])))


  private def register(c: Context)(procedure: c.Expr[String], lambda: c.universe.Function): String = {
    val (prc, arity, args, kwargs) = inspect(c)(procedure, lambda)
    val code = s"""
       | import akka.wamp.client._
       | import akka.wamp.serialization._
       | import scala.concurrent._
       |
       | implicitly[Session].register($prc, invoc => {
       |   val errmsg = "Couldn't invoke lambda consumer for ${prc.replace('"','\'')}"
       |   val args = invoc.args
       |   if (args.size == $arity) {
       |     try {
       |       $lambda.apply(${args.mkString(",")})
       |     }
       |     catch {
       |       case ex: Throwable => throw new ClientException(errmsg+": "+ex.getMessage, ex)
       |     }
       |   }
       |   else {
       |     val kwargs = invoc.kwargs
       |     if (kwargs.size == $arity) {
       |       try {
       |         $lambda.apply(${kwargs.mkString(",")})
       |       }
       |       catch {
       |         case ex: Throwable => throw new ClientException(errmsg+": "+ex.getMessage, ex)
       |       }
       |     }
       |     else {
       |       val cause = "unexpected number of arguments"
       |       throw new ClientException(errmsg+": "+cause, new IllegalArgumentException(cause))
       |     }
       |   }
       | })
       |
       """.stripMargin
    code
  }



  // ~~~~~~~~~~~~~~~~~~~~~~~~~

  private def inspect(c: Context)(subject: c.Expr[String], function: c.universe.Function):  (String, Int, List[String], List[String]) = {
    import c.universe._

    val params = function.vparams.map {
      _ match {
        case ValDef(_, name, tpt, _) =>
          (name.decodedName.toString, tpt.asInstanceOf[TypeTree].original.toString())
      }
    }

    val args = params.zipWithIndex.map { case ((_, tpt), index) =>
      s"""args($index).asInstanceOf[$tpt]"""
    }


    val kwargs = params.zipWithIndex.map { case ((name, tpt), _) =>
      s"""kwargs("$name").asInstanceOf[$tpt]"""
    }

    (subject.tree.toString, params.size, args, kwargs)
  }
}

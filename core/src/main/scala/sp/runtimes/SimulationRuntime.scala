package sp.runtimes

import akka.actor._
import sp.domain._
import sp.system.messages._
import akka.pattern.ask
import akka.util._
import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Exempel på en runtime eller service.
 *
 * Skicka en SimpleMessage med attribute till den som ser ut:
 * "name" -> "Kristofer",
 * "operation" -> {"id" -> ID(UUID)}
 */
class SimulationRuntime(about: CreateRuntime) extends Actor {
  import sp.domain.Logic._
  private implicit val to = Timeout(20 seconds)
  import context.dispatcher


  import sp.system.SPActorSystem._
  def receive = {
    case SimpleMessage(_, attr) => {
      val reply = sender


      //val attr = extractAttributes(attr)
      val modelE = either(attr.getAs[ID]("model"), "missing property model: ID")
      val stateE = either(attr.getAs[State]("state"), "missing property state: State object")

      val res = for {
        model <- modelE.right
        state <- stateE.right
      } yield {
        val opsF = (modelHandler ? GetOperations(model)).mapTo[SPIDs] map(_.items.map(_.asInstanceOf[Operation]))
        val thingsF = (modelHandler ? GetThings(model)).mapTo[SPIDs] map(_.items.map(_.asInstanceOf[Thing]))

        for {
          ops <- opsF
          things <- thingsF
        } yield {
          import sp.domain.Logic._


          val stateVars = things.map(sv => sv.id -> sv.inDomain).toMap ++ createOpsStateVars(ops)
          implicit val props = EvaluateProp(stateVars, Set(), ThreeStateDefinition)

          val newState = (for {
            id <- attr.getAs[ID]("execute")
            o <- ops.find(_.id == id)
          } yield o next state) getOrElse(state)

          val enabled = ops.filter(o => o.eval(newState))
          println(s"Enabled operations: $enabled")


          val result = SPAttributes(
            "enabled" -> (enabled.map(o => (o.id))),
            "state" -> newState
          )

          reply ! result
        }

      }

      res.left.map(e => reply ! e)
      res.right.map(f => f.onFailure{case e: Throwable => reply ! SPError(e.toString)})





//      val ops: Future[Either[SPError, List[Operation]]] = for {
//        f <- either(model.right.map(m => (modelHandler ? GetOperations(m))), "")
//        got <- f.right
//        inst <- tryWithOption(got.asInstanceOf[SPIDs].items.map(_.asInstanceOf[Operation]))
//      } yield inst


    }
    case cr @ CreateRuntime(_, m, n, attr) => {
      // load things from the model here.
      // If needed return cr after load is complete
      println(cr)
      sender ! cr
    }
    case GetRuntimes => {
      sender ! about
    }
  }

  def createOpsStateVars(ops: List[Operation]) = {
    ops.map(o => o.id -> sp.domain.logic.OperationLogic.OperationState.inDomain).toMap
  }


//  def getOperation = {
//
//  }
//
//  def extractAttributes(attr: SPAttributes) = {
//    val model = either(attr.getAsID("model"), "missing property model: ID")
//    val state = either(attr.getStateAttr("state"), "missing property state: State object")
//    val execute = either(attr.getAsID("execute"), "missing property execute: ID")
//
//    (model, state, execute)
//
//
//  }
//
//  def dig[T](f: Option[T], error: String) = {
//    f match {
//      case Some(x) => Future(x)
//      case None => Future.failed(new Throwable(error))
//    }
//  }
//
  def either[T](input: Option[T], error: String): Either[SPError, T] = {
    val e = Left[SPError, T](SPError(error))
    val temp = input.map(Right[SPError, T](_))
    val res: Either[SPError, T] = temp.getOrElse(e)
    res
  }
//  def either[T](input: Future[Any], error: String): Future[Either[SPError, Any]] = {
//    input.map{
//      case e: SPError => Left(e)
//      case x @ _ => Right(x)
//    }.recover{case t => Left(SPError(error))}
//  }
//  def either[T](input: Either[SPError, Future[Any]], error: String): Future[Either[SPError, Any]] = {
//    input match {
//      case x: Left => Future(x)
//      case x: Right => either(x, error)
//    }
//  }
//  def tryWithOption[T](t: => T): Option[T] = {
//    try {
//      Some(t)
//    } catch {
//      case e: Exception => None
//    }
//  }
}

object SimulationRuntime {
  def props(cr: CreateRuntime) = Props(classOf[SimulationRuntime], cr)
}

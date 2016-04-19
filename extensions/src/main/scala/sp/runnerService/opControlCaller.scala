package sp.runnerService

import akka.actor._
import org.json4s.JsonAST.JString
import sp.domain._
import sp.system.messages._
import sp.domain.Logic._

/**
  *
  * The operation control service must be started and connected
  *
  * Created by kristofer on 2016-03-02.
  */
class OPControlCaller(eventHandler: ActorRef, serviceHandler: ActorRef, operationControl: String) extends Actor{

  var state: Option[State] = None
  var executeMap = Map[ID, ActorRef]()



  def receive = {
    case r @ Response(ids, attr, service, _) if service == operationControl => {
      println("got resp from control: "+ r)
      attr.getAs[State]("state").map{s =>
        state = Some(s)
        for {
          kv <- executeMap
          currentState <- s.state.get(kv._1)
          str <- currentState.to[String]
        }yield {
          if (str == "completed") {
            exec(kv._1)
            kv._2 ! "done"
          } else if (str == "notReady"){
            kv._2 ! "errror"
          }
        }

      }
    }

    case ExecuteMyID(id) => {
      executeMap = executeMap + (id -> sender())
      exec(id)
    }

  }

  def exec(id: ID) = {
    serviceHandler ! Request(operationControl,
      SPAttributes(
        "command"->SPAttributes(
          "commandType"-> "execute",
          "execute" -> id
        )))
  }

}

case class ExecuteMyID(id: ID)


object OPControlCaller {
  def props(eventHandler: ActorRef, serviceHandler: ActorRef, operationControl: String) =
    Props(classOf[OPControlCaller], eventHandler, serviceHandler, operationControl)
}

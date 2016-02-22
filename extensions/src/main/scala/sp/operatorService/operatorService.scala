package sp.operatorService

import akka.actor._
import sp.domain.Logic._
import sp.domain._
import sp.system._
import sp.system.messages._
object operatorService extends SPService {

  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "calculator" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "done" -> KeyDefinition("String", List(), None)
  )

  val transformTuple = (
    TransformValue("done", _.getAs[String]("done"))
    )
  val transformation = transformToList(transformTuple.productIterator.toList)

  def props = ServiceLauncher.props(Props(classOf[operatorService]))
}
  class operatorService extends Actor with ServiceSupport {

    def receive = {
      case r@Request(service, attr, ids, reqID) => {
        val replyTo = sender()
        implicit val rnr = RequestNReply(r, replyTo)

        val done: String = transform(operatorService.transformTuple)
        //val res: String = done match {
         // case  "true" => "red";
          //case _ => "green";
        //}
        System.out.println("Hej")
        System.out.println(sender())
        System.out.println("Hej")
        val res = List("green","green","green","green","green","green","green","green")
        replyTo ! Response(List(), SPAttributes("result" -> res), rnr.req.service, rnr.req.reqID)
        self ! PoisonPill
      }
    }
  }



/*
Js kallar på scala funktionen, scala funktionen skickar tillbaka en array.
Skicka denna arrayen till javascript [röd,grön]
scala -> uppdaterar en array i javascript operatorInstGUI
 */
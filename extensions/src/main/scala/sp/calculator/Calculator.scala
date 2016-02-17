package sp.calculator

import akka.actor._
import sp.domain.logic.IDAbleLogic
import scala.concurrent._
import sp.system.messages._
import sp.system._
import sp.domain._
import sp.domain.Logic._


object Calculator extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "calculations",
      "description" -> "A simple calculation service"
    ),
    "a"-> KeyDefinition("Int", List(), Some(0)),
    "b"-> KeyDefinition("Int", List(), Some(0)),
    "sign"-> KeyDefinition("String", List("+", "-"), Some("+"))
  )

  val transformTuple  = (
    TransformValue("a", _.getAs[Int]("a")),
    TransformValue("b", _.getAs[Int]("b")),
    TransformValue("sign", _.getAs[String]("sign"))
  )
  val transformation = transformToList(transformTuple.productIterator.toList)
  def props = ServiceLauncher.props(Props(classOf[Calculator]))
}


class Calculator extends Actor with ServiceSupport {
  def receive = {
    case r @ Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      val a = transform(Calculator.transformTuple._1)
      val b = transform(Calculator.transformTuple._2)
      val sign = transform(Calculator.transformTuple._3)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get

      println(s"calc got: $r")

      val res = sign match{
        case "+"=> a+b
        case "-"=> a-b
      }
      replyTo ! Response(List(), SPAttributes("result"->res), rnr.req.service, rnr.req.reqID)
      self ! PoisonPill
    }
  }


}





package sp.operatorService

import akka.actor._
import sp.domain.Logic._
import sp.domain._
import sp.system._
import sp.system.messages._
import scala.collection.mutable.MutableList
import sp.system.SPActorSystem.eventHandler

object operatorService extends SPService {

  var q = MutableList[List[List[String]]]();
  var emptySent = true;
  var latestSent = List(List("Hej"));

  def enqueuePalette(palette: List[List[String]]): Unit = {
    System.out.println("Number of lists: " + palette.length)
    if (palette.length > 2) {
    val temp1 = MutableList(List(List(palette(0)(0), palette(1)(0), palette(2)(0), palette(3)(0)),List(palette(0)(1), palette(1)(1), palette(2)(1), palette(3)(1))))
    val temp2 = MutableList(List(List(palette(0)(2), palette(1)(2), palette(2)(2), palette(3)(2)),List(palette(0)(3), palette(1)(3), palette(2)(3), palette(3)(3))))
    q = q ++ temp1 ++ temp2
    //q = q ++ MutableList(palette.take(2)) ++ MutableList(palette.tail.tail);
  }
      else
        q = q ++ MutableList(palette)
  }
  def dequeuePalette(): Unit = {
      q = q.tail;
  }

  val yellow = "#ffff66"; val red = "#ff3333"; val green = "#5cd65c"
  val blue = "#0066ff"; val empty = "#ffffff"

  def parseColour(ls: List[List[String]]): List[List[String]] = {
    ls.map(x => x.map(x => x match {
        case "yellow" => yellow
        case "red"    => red
        case "green"  => green
        case "blue"   => blue
        case "empty"  => empty
        case "0"      => empty
        case "1"      => yellow
        case "2"      => green
        case "3"      => red
        case "4"      => blue
        case _        => empty
      })
    )
  }

  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "hide" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "getNext" -> KeyDefinition("Boolean", List(), None),
    "buildOrder" -> KeyDefinition("List[List[String]]", List(), None)
  )
  val transformTuple =(
    TransformValue("getNext", _.getAs[Boolean]("getNext")),
    TransformValue("buildOrder", _.getAs[List[List[String]]]("buildOrder"))
    )

  val transformation = transformToList(transformTuple.productIterator.toList)
  def props = ServiceLauncher.props(Props(classOf[operatorService]))

}
  class operatorService extends Actor with ServiceSupport {

    def receive = {
      case r@Request(service, attr, ids, reqID) => {
        val replyTo = sender()
        implicit val rnr = RequestNReply(r, replyTo)
        val getNext: Boolean = transform(operatorService.transformTuple._1)
        var que = List.fill(2)(List.fill(4)("empty"))
        
        if (getNext && operatorService.q.isEmpty) {
          operatorService.emptySent = true
        }

        else if (getNext && operatorService.q.nonEmpty) {
          que = operatorService.q.head
          operatorService.latestSent = que
          operatorService.dequeuePalette()
          operatorService.emptySent = false
        }
        else if (!getNext && operatorService.q.isEmpty) {
          operatorService.enqueuePalette(transform(operatorService.transformTuple._2))
          if (operatorService.emptySent) {
              que = operatorService.q.head
              operatorService.latestSent = que
              operatorService.dequeuePalette()
          }
          else {
            que = operatorService.latestSent
          }


        }
        else if (!getNext && operatorService.q.nonEmpty) {
          operatorService.enqueuePalette(transform(operatorService.transformTuple._2))
          que = operatorService.latestSent
        }
        que = operatorService.parseColour(que)
        val pruttlol = List(1,2,3,4,5,6)
        replyTo ! Response(List(), SPAttributes("result" -> que, "hej" -> pruttlol), rnr.req.service, rnr.req.reqID)
        self ! PoisonPill
      }
    }
  }
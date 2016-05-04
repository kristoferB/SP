package sp.psl

import akka.actor._
import sp.domain.Logic._
import sp.domain._
import sp.system._
import sp.system.messages._

import scala.collection.mutable.MutableList

object OperatorService extends SPService {

  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "hide" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "getNext" -> KeyDefinition("Boolean", List(), None),
    "buildOrder" -> KeyDefinition("List[List[String]]", List(), None),
    "fixturePositions" -> KeyDefinition("Int", List(1,2), Some(0))
  )
  val transformTuple = (
    TransformValue("getNext", _.getAs[Boolean]("getNext")),
    TransformValue("buildOrder", _.getAs[List[List[String]]]("buildOrder")),
    TransformValue("fixturePositions", _.getAs[Int]("fixturePositions"))
    )

  val transformation = transformToList(transformTuple.productIterator.toList)
  def props(serviceHandler: ActorRef) = Props(classOf[OperatorService])

}
  class OperatorService extends Actor with ServiceSupport with TowerBuilder {

    var fixturePosition = 1

    def receive = {
      case r@Request(service, attr, ids, reqID) => {
        val replyTo = sender()
        implicit val rnr = RequestNReply(r, replyTo)

        val rawTower = transform(OperatorService.transformTuple._2)
        val suggestedPos = transform(OperatorService.transformTuple._3)

        val tower = makeTower(rawTower)
        if (verifyTower())

        val getNext: Boolean = transform(OperatorService.transformTuple._1)
        var que = List.fill(2)(List.fill(4)("empty"))

        if (getNext && OperatorService.q.isEmpty) {
          OperatorService.emptySent = true
        }

        else if (getNext && OperatorService.q.nonEmpty) {
          que = OperatorService.q.head
          OperatorService.latestSent = que
          OperatorService.dequeuePalette()
          OperatorService.emptySent = false
        }
        else if (!getNext && OperatorService.q.isEmpty) {
          OperatorService.enqueuePalette(transform(OperatorService.transformTuple._2))
          if (OperatorService.emptySent) {
              que = OperatorService.q.head
              OperatorService.latestSent = que
              OperatorService.dequeuePalette()
          }
          else {
            que = OperatorService.latestSent
          }


        }
        else if (!getNext && OperatorService.q.nonEmpty) {
          OperatorService.enqueuePalette(transform(OperatorService.transformTuple._2))
          que = OperatorService.latestSent
        }
        que = OperatorService.parseColour(que)
        val pruttlol = List(1,2,3,4,5,6)
        replyTo ! Response(List(), SPAttributes("result" -> que, "hej" -> pruttlol), rnr.req.service, rnr.req.reqID)
        self ! PoisonPill
      }
    }
  }


trait TowerBuilder {
  def makeTower(xs: List[List[String]]) = {
    for {
      c <- xs.map(_.zipWithIndex).zipWithIndex
      r <- c._1
      color <- matchColor(r._1)
    } yield {Brick(r._2, c._2, color)}
  }

  def verifyTower(t: List[Brick]) = {
    val cols = t.groupBy(_.col)
    val ok = cols.foldLeft(Map[Int, Boolean]()){(a, b) =>
      val sorted = b._2.sortWith(_.row > _.row)
      val res = sorted.isEmpty || sorted.head.row == sorted.size
      a + (b._1 -> res)
    }
    ok.foreach(b => if (!b._2) println(s"Column ${b._1} is not ok: ${cols(b._1)}"))
    ok.values.fold(true)(_ && _)
  }

  def splitTower

  def matchColor(color: String): Option[BrickColor] = {
    color match {
      case "yellow" => Some(Yellow)
      case "red"    => Some(Red)
      case "green"  => Some(Green)
      case "blue"   => Some(Blue)
      case "1"      => Some(Yellow)
      case "2"      => Some(Green)
      case "3"      => Some(Red)
      case "4"      => Some(Blue)
      case _        => None[BrickColor]
    }
  }
}


case class Brick(row: Int, col: Int, color: BrickColor)

case class BrickTower(columns: List[BrickColumn])
case class BrickColumn(Bricks: List[BrickColor])
sealed trait BrickColor
case object Yellow extends BrickColor
case object Red extends BrickColor
case object Green extends BrickColor
case object Blue extends BrickColor


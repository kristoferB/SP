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
  def props(serviceHandler: ActorRef) = Props(classOf[OperatorService], serviceHandler)

}
class OperatorService(sh: ActorRef) extends Actor with ServiceSupport with TowerBuilder {
  var fixturePosition = 2

  def receive = {
    case r@Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      val suggestedPos = transform(OperatorService.transformTuple._3)
      if (suggestedPos > 0) fixturePosition = suggestedPos
      else if (fixturePosition == 1) fixturePosition = 2
      else fixturePosition = 1

      val rawTower = transform(OperatorService.transformTuple._2)
      val tower = makeTower(rawTower)

      tower.foreach{t =>
        val (f1, f2) = divideTowerOnRobots(t, fixturePosition)
        val ops = ids.collect{case o: Operation if o.attributes.getAs[Behavior]("behavior").nonEmpty => o}

        val f1Ops = getBrickOperations(f1, ops)
        val f2Ops = getBrickOperations(f2, ops)

        // add R2 operations!
        val seqF1 = Sequence(f1Ops.map(o => Hierarchy(o.id)):_*)
        val seqF2 = Sequence(f2Ops.map(o => Hierarchy(o.id)):_*)

        val paraSOP = Parallel(seqF1, seqF2)

        //sh ! Request("OrderHandler")

      }

      if (tower.isEmpty){
        //return error
      }


      //replyTo ! Response(List(), SPAttributes("result" -> que, "hej" -> pruttlol), rnr.req.service, rnr.req.reqID)
      //self ! PoisonPill
    }
  }
}


trait TowerBuilder {
  def makeTower(xs: List[List[String]]) = {
    val t = for {
      r <- xs.map(_.zipWithIndex).zipWithIndex
      c <- r._1
      color <- matchColor(c._1)
    } yield {Brick(r._2+1, c._2+1, color)}
    verifyTower(t)
  }

  def verifyTower(t: List[Brick]) = {
    val cols = t.groupBy(_.col)
    val ok = cols.foldLeft(Map[Int, Boolean]()){(a, b) =>
      val sorted = b._2.sortWith(_.row > _.row)
      val res = sorted.isEmpty || sorted.head.row == sorted.size
      a + (b._1 -> res)
    }
    ok.foreach(b => if (!b._2) println(s"Column ${b._1} is not ok: ${cols(b._1)}"))
    if (ok.values.fold(true)(_ && _)) Some(t) else None
  }

  def sortBricks(xs: List[Brick], takeLeft: Boolean = true) = {
    xs.sortWith((b1, b2) => b1.row < b2.row ||
      (b1.row == b2.row && ((takeLeft && b1.col > b2.col) || (!takeLeft && b1.col < b2.col))))
  }

  def updateFixturePosition(xs: List[Brick], fixture: Int) = xs.zipWithIndex.map(z => z._1.copy(fixturePos = z._2+1, fixture = fixture))

  def divideTowerOnRobots(t: List[Brick], fixturePosition: Int) = {
    val (r1Bricks, r2Bricks) = t.partition(_.col <=2)

    val R1Fix = updateFixturePosition(sortBricks(r1Bricks), fixturePosition)
    val R2Fix = updateFixturePosition(sortBricks(r2Bricks, takeLeft = false), fixturePosition+2)

    (R1Fix, R2Fix)
  }

  def getBrickOperations(bricks: List[Brick], ops: List[Operation]) = {
    val opsNB = ops.flatMap{case o => o.attributes.getAs[Behavior]("behavior").map(o->_)}
    bricks.flatMap { b =>
      val pick = opsNB.find(_._2.pick.getOrElse(List()).contains(b.fixture))
      val place = opsNB.find(_._2.place.getOrElse(List()).contains(b.col))

      val updPick = pick.map{o =>
        val no = b.fixture*10 + b.fixturePos
        val ab = AbilityStructure(o._2.ability, List(AbilityParameter(o._2.parameter, no)))
        o._1.copy(name = o._1.name +"_"+ no, id = ID.newID,
          attributes = o._1.attributes + ("ability"->ab) + ("brick"->b) )
      }
      val updPlace = place.map{o =>
        val no = b.row*10 + b.col
        val ab = AbilityStructure(o._2.ability, List(AbilityParameter(o._2.parameter, no)))
        o._1.copy(name = o._1.name +"_"+ no,id = ID.newID,
          attributes = o._1.attributes + ("ability"->ab) + ("brick"->b))
      }

      List(updPick, updPlace).flatten
    }
  }

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
      case _        => None
    }
  }
}


case class Brick(row: Int, col: Int, color: BrickColor, fixture: Int = -1, fixturePos: Int = -1)
case class Behavior(op: String, ability: ID, parameter: ID, pick: Option[List[Int]], place: Option[List[Int]])


sealed trait BrickColor
case object Yellow extends BrickColor
case object Red extends BrickColor
case object Green extends BrickColor
case object Blue extends BrickColor


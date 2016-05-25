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
        val paraSOP = towerToSOP(t, fixturePosition, ids)
        val sopSpec = SOPSpec("tower", List(paraSOP._1))
        val sopSpecLoad = SOPSpec("load", List(paraSOP._2))
        val updIds = sopSpec :: sopSpecLoad :: paraSOP._3 ++ ids

        val stations = Map("tower" -> sopSpec.id, "load" -> sopSpecLoad.id)

        println("The tower: ")
        paraSOP._4.map(println)


        sh ! Request("OrderHandler", SPAttributes(
          "order" -> SPAttributes(
            "id"->ID.newID,
            "name"-> towerName(t),
            "stations"-> stations
          )
        ), updIds)
      }

      if (tower.isEmpty){
        println("tower could not be parsed: "+ rawTower)
      }


      replyTo ! Response(List(), SPAttributes("tower" -> tower), rnr.req.service, rnr.req.reqID)
      //self ! PoisonPill
    }
    case error: SPError => println(s"Operator Service got an error: $error")
  }
}




trait TowerBuilder extends TowerOperationTypes {

  def towerToSOP(t: List[Brick],fixturePosition: Int, ids: List[IDAble]) = {
    val (f1, f2) = divideTowerOnRobots(t, fixturePosition)
    val nameMap = ids.map(x => x.name -> x).toMap

    val f1Ops = getBrickOperations(f1, nameMap)
    val f2Ops = getBrickOperations(f2, nameMap)
    val loadOps = getLoadOps(f1, f2, nameMap)
    val unLoadOps = getUnloadOps(f1, f2, nameMap)

    val allOps = f1Ops ++ f2Ops ++ loadOps ++ unLoadOps._1 ++ unLoadOps._2 ++ unLoadOps._3

    val seqF1 = Sequence((f1Ops ++ unLoadOps._1).map(o => Hierarchy(o.id)):_*)
    val seqF2 = Sequence((f2Ops ++ unLoadOps._2).map(o => Hierarchy(o.id)):_*)
    val seqLoad = Sequence(loadOps.map(o => Hierarchy(o.id)):_*)
    val seqUnloadTower = unLoadOps._3.map(o=>Hierarchy(o.id))

    val brickSeq: List[SOP] = Parallel(seqF1, seqF2) :: seqUnloadTower

    (Sequence(brickSeq:_*), seqLoad, allOps, f1 ++ f2)
  }

  def makeTower(xs: List[List[String]]) = {
    val t = for {
      r <- xs.map(_.reverse.zipWithIndex).zipWithIndex
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
    val (r1Bricks, r2Bricks) = t.partition(_.col >=3)

    val R1Fix = updateFixturePosition(sortBricks(r1Bricks), fixturePosition)
    val R2Fix = updateFixturePosition(sortBricks(r2Bricks, takeLeft = false), fixturePosition+2)

    (R1Fix, R2Fix)
  }

  def getBrickOperations(bricks: List[Brick], nameMap: Map[String, IDAble]) = {
    val res = for {
      b <- bricks
    } yield {
      val piPos = b.fixture*10 + b.fixturePos
      val plPos = b.row*10 + b.col
      val robot = b.col match {
        case c if c >= 3 => "R4"
        case _ => "R5"
      }
      val pick = makeOperationWithParameter(robot, "pickBlock", "pos", piPos, nameMap)
      val place = makeOperationWithParameter(robot, "placeBlock", "pos", plPos, nameMap)

      List(pick, place)
    }
    res.flatten
  }

  def getLoadOps(f1: List[Brick], f2: List[Brick], nameMap: Map[String, IDAble]) = {
    val feedFix = makeOperationWithParameter("Flexlink", "fixtureToOperator", "no", {if (f1.nonEmpty) 1 else 0} +  {if (f2.nonEmpty) 1 else 0}, nameMap)

    def fixToRMake(bO: Option[Brick]) = {
      bO.map(b => List(makeOperationWithParameter("Flexlink", "fixtureToRobot", "pos", b.fixture, nameMap))).getOrElse(List())
    }
    val f1ToR = fixToRMake(f1.headOption)
    val f2ToR = fixToRMake(f2.headOption)

    def instrToOpMake(f: List[Brick]) = makeOperationWithParameter("Operator", "loadFixture", "brickPositions", SPValue(f) , nameMap)
    val fBuild = (instrToOpMake(f1), instrToOpMake(f2))

    List(feedFix, fBuild._1) ++ (f1ToR :+ fBuild._2) ++ f2ToR
  }

  def getUnloadOps(f1: List[Brick], f2: List[Brick], nameMap: Map[String, IDAble]) = {
    def fixOutMake(bO: Option[Brick]) = {
      bO.map(b => List(makeOperationWithParameter("R2", "pickAtPos", "pos", b.fixture, nameMap))).getOrElse(List())
    }
    val f1Out = fixOutMake(f1.headOption)
    val f2Out = fixOutMake(f2.headOption)

    val placeElevatorF1 = makeOperation("R2", "homeTableToElevatorStn3", nameMap)
    val placeElevatorF2 = placeElevatorF1.copy(id = ID.newID)

    val pickBaseOut = makeOperationWithParameter("R2", "pickAtPos", "pos", 5, nameMap)
    val placetable = makeOperation("R2", "deliverTower", nameMap)


    val outSeqF1 = f1Out ++ {if (f1Out.nonEmpty) List(placeElevatorF1) else List()}
    val outSeqF2 = f2Out ++ {if (f2Out.nonEmpty) List(placeElevatorF2) else List()}
    val outSeqTower = List(pickBaseOut, placetable)

    (outSeqF1, outSeqF2, outSeqTower)
  }

  def towerName(xs: List[Brick]) = xs.map(_.color).mkString("_")

  def matchColor(color: String): Option[String] = {
    color.toLowerCase match {
      case "yellow" => Some("Yellow")
      case "red"    => Some("Red")
      case "green"  => Some("Green")
      case "blue"   => Some("Blue")
      case "1"      => Some("Yellow")
      case "2"      => Some("Green")
      case "3"      => Some("Red")
      case "4"      => Some("Blue")
      case _        => None
    }
  }
}


case class Brick(row: Int, col: Int, color: String, fixture: Int = -1, fixturePos: Int = -1)
case class Behavior(op: String, ability: ID, parameter: ID, pick: Option[List[Int]], place: Option[List[Int]])

trait TowerOperationTypes {
  def makeOperationWithParameter(resource: String, ability: String, parameter: String, value: SPValue, nameMap: Map[String, IDAble]) = {
    val ab = nameMap(s"$resource.$ability")
    val p = nameMap(s"$resource.$ability.$parameter")
    val valueJson = if (value.isInstanceOf[SPAttributes]) "" else "_"+value.toJson
    Operation(s"O_$ability${resource}$valueJson", List(), attributes = SPAttributes("ability" -> AbilityStructure(ab.id, List(AbilityParameter(p.id, value)))))
  }

  def makeOperation(resource: String, ability: String, nameMap: Map[String, IDAble]) = {
    val ab = nameMap(s"$resource.$ability")
    Operation(s"O_$ability${resource}", List(), attributes = SPAttributes("ability" -> AbilityStructure(ab.id, List())))
  }


}


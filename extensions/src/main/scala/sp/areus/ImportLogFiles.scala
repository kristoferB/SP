package sp.areus

import akka.actor._
import scala.concurrent._
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._
import sp.system._
import scala.util.{Success, Try}


case class Pose(time: Double, joints: List[Double])


object ImportLogFiles extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "import"
    ),
    "file" -> KeyDefinition("File", List(), Some("")),
    "resource1" -> KeyDefinition("String", List(), Some("R1")),
    "operation1" -> KeyDefinition("String", List(), Some("O1")),
    "resource2" -> KeyDefinition("String", List(), Some("R2")),
    "operation2" -> KeyDefinition("String", List(), Some("O2")),
    "hierarchyName" -> KeyDefinition("String", List(), Some("Trajectory"))
  )
  val transformTuple  = (
    TransformValue("file", _.getAs[String]("file")),
    TransformValue("resource1", _.getAs[String]("resource1")),
    TransformValue("operation1", _.getAs[String]("operation1")),
    TransformValue("resource2", _.getAs[String]("resource2")),
    TransformValue("operation2", _.getAs[String]("operation2")),
    TransformValue("hierarchyName", _.getAs[String]("hierarchyName"))
  )
  val transformation = transformToList(transformTuple.productIterator.toList)
  def props = ServiceLauncher.props(Props(classOf[ImportLogFiles]))
}

class ImportLogFiles extends Actor with ServiceSupport {
  import context.dispatcher

  def receive = {
    case r@Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val progress = context.actorOf(progressHandler)

      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get
      val file = transform(ImportLogFiles.transformTuple._1)
      val resource1 = transform(ImportLogFiles.transformTuple._2)
      val operation1 = transform(ImportLogFiles.transformTuple._3)
      val resource2 = transform(ImportLogFiles.transformTuple._4)
      val operation2 = transform(ImportLogFiles.transformTuple._5)
      val hierarchyName = transform(ImportLogFiles.transformTuple._6)

      println(s"I got: $file")

      val robot1 = Thing(resource1)
      val robot2 = Thing(resource2)
      val poses = dummyData.dummyParse
      val reversePoses = poses.zip(poses.reverse).map(zip =>
        Pose(zip._1.time, zip._2.joints)
      )


      val op = Operation(operation1, List(), SPAttributes("poses"->poses))
      val op2 = Operation(operation2, List(), SPAttributes("poses"->reversePoses))

      val root = findName(hierarchyName, ids, HierarchyRoot(hierarchyName))

      val node1 = HierarchyNode(op.id)
      val robotNode1 = HierarchyNode(robot1.id, List(node1))
      val node2 = HierarchyNode(op2.id)
      val robotNode2 = HierarchyNode(robot2.id, List(node2))

      val newSpec = root.copy(children = root.children ++ List(robotNode1, robotNode2))

      // example ops
//      val o11 = Operation("o11", List(), SPAttributes("time"-> 2))
//      val o12 = Operation("o12", List(), SPAttributes("time"-> 5))
//      val o13 = Operation("o13", List(), SPAttributes("time"-> 4))
//      val o21 = Operation("o21", List(), SPAttributes("time"-> 3))
//      val o22 = Operation("o22", List(), SPAttributes("time"-> 2))
//      val o23 = Operation("o23", List(), SPAttributes("time"-> 3))
//      val ops = List(o11, o12, o13, o21, o22, o23)
//
//      val sopSeq = SOP(Sequence(o11, o12, o13), Sequence(o21, o22, o23))
//      val sopArbi = SOP(Arbitrary(o12, o22))
//
//      val conditions = sp.domain.logic.SOPLogic.extractOperationConditions(List(sopSeq, sopArbi), "traj")
//      val opsUpd = ops.map{o=>
//        val cond = conditions.get(o.id).map(List(_)).getOrElse(List())
//        o.copy(conditions = cond)
//      }


      replyTo ! Response(List(robot1, robot2, op, op2, newSpec), SPAttributes(), service, reqID)

    }


  }

  def findName[T](name: String, ids: List[IDAble], orElse: T) = {
    ids.find(x => x.name == name && x.isInstanceOf[T]).
      map(_.asInstanceOf[T]).
      getOrElse(orElse)
  }
}

case class Mark(robot: Int, entersAtSample: Int, exitsAtSample: Int)

case object dummyData {
  import scala.io.Source

  def getPosesFromLog(file: List[String]) = {
    file.flatMap{ l =>
      val xs = l.trim.split("""\s+""").toList.map(x => Try(x.toDouble))
      xs match {
        case  Success(time) ::
          Success(j1) ::
          Success(j2) ::
          Success(j3) ::
          Success(j4) ::
          Success(j5) ::
          Success(j6) :: Nil => {
          Some(Pose(time, List(j1, j2, j3, j4, j5, j6)))
        }
        case x => None
      }
    }
  }

  def createSarmadJson = {
    val poses = dummyParse
    //println(s"poses ${poses.size}")
    val times = poses.map(_.time)
    val trajectory = poses.map(_.joints)

    val robot = SPAttributes(
      "makespan" -> 8.0,
      "samplingRate" -> 0.012,
      "timeToleranceMax" -> 0.1,
      "timeToleranceMin" -> 0.001,
      "epsilonT" -> 0.001,
      "costScaleFactor" -> 100,
      "velocityLimit" -> (1 to 6).map(x => 200),
      "accelerationLimit" -> (1 to 6).map(x => 2000),
      "jerkLimit" -> (1 to 6).map(x => 15000),
      "weights" -> List(List(20, 20, 20, 10, 7, 5)),
      "time" -> times,
      "trajectory" -> trajectory
    )

    val r1Z = Mark(0, 100, 300)
    val r2Z = Mark(1, 301, 400)

    val request = SPAttributes(
      "robots" -> List(robot, robot),
      "sharedZones" -> List(List(r1Z, r2Z))
    )

    request.toJson


  }


  def dummyParse = getPosesFromLog(dummyData.file.getLines().toList)

  val file = Source.fromFile("./testFiles/longRunOriginal.emi")
}


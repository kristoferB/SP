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
    "resource" -> KeyDefinition("String", List(), Some("R1")),
    "operation" -> KeyDefinition("String", List(), Some("O1")),
    "hierarchyName" -> KeyDefinition("String", List(), Some("Trajectory"))
  )
  val transformTuple  = (
    TransformValue("file", _.getAs[String]("file")),
    TransformValue("resource", _.getAs[String]("resource")),
    TransformValue("operation", _.getAs[String]("operation")),
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
      val resource = transform(ImportLogFiles.transformTuple._2)
      val operation = transform(ImportLogFiles.transformTuple._3)
      val hierarchyName = transform(ImportLogFiles.transformTuple._4)

      println(s"I got: $file")

      val robot = findName(resource, ids, Thing(resource))
      val op = Operation(operation, List(), SPAttributes("poses"->dummyData.dummyParse))

      val root = findName(hierarchyName, ids, HierarchyRoot(hierarchyName))

      val node = HierarchyNode(op.id)
      val robotNode = root.children.find(_.item == robot.id).getOrElse(HierarchyNode(robot.id, List()))
      val newRobotNode = robotNode.copy(children = robotNode.children :+ node)

      val newSpec = root.copy(children = root.children :+ newRobotNode)



      replyTo ! Response(List(robot, op, newSpec), SPAttributes(), service, reqID)

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
      "jerkLimit" -> (1 to 6).map(x => 6000),
      "time" -> times,
      "trajectory" -> trajectory
    )

    val r1Z = Mark(1, 100, 300)
    val r2Z = Mark(1, 200, 400)

    val request = SPAttributes(
      "robots" -> List(robot, robot),
      "sharedZones" -> List(List(r1Z, r2Z))
    )

    request.pretty

  }

  def dummyParse = getPosesFromLog(dummyData.file.getLines().toList)

  val file = Source.fromFile("./testFiles/longRunOriginal.emi")
}


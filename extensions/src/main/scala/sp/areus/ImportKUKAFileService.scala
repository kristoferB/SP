package sp.areus

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import sp.domain._
import sp.system.messages._

import scala.concurrent.duration._


/**
 * Created by Kristofer on 2014-06-27.
 */
class ImportKUKAFileService(modelHandler: ActorRef) extends Actor {
  implicit val timeout = Timeout(1 seconds)
  import context.dispatcher



  def receive = {
    case Request(_, attr) => {
      val reply = sender
//      extract(attr) match {
//        case Some((file, name, model)) => {
//          println(s"I got the file in importKUKA")
//
//          val trajectory = extractTrajectory(file)
//
//          val operationName = name.flatMap(_.asString).getOrElse("robotOP").replaceAll("\\.[^.]*$", "")
//
//          val energy = {
//            val joints = List(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
//            val sum = trajectory.foldLeft(joints)((a, b) => {
//              val current = b.asMap.flatMap(_.get("current")).flatMap(_.asList).map(_.flatMap(_.asDouble)).getOrElse(joints)
//              current.zip(a).foldLeft(List[Double]())((res, z)=> res :+ ((z._1^2) + z._2^2))
//            })
//          }
//          val attrib = Attr("trajectory"-> MapPrimitive(Map("samples" -> ListPrimitive(trajectory))))
//
//          val op = Operation(operationName, List(), attrib)
//          val res = modelHandler ? UpdateIDs(model, -1, List(op))
//
//          reply ! "yes"
//
//        }
//        case None => reply ! errorMessage(attr)
//      }
    }
  }

//  def extract(attr: SPAttributes) = {
//    for {
//      file <- attr.getAsString("file")
//      model <- attr.getAsID("model")
//    } yield (file, attr.get("name"), model)
//  }
//
//  def errorMessage(attr: SPAttributes) = {
//    SPError("The request is missing parameters: \n" +
//      s"file: ${attr.getAsString("file")}" + "\n" +
//      s"Request: ${attr}" )
//  }
//
//  def extractTrajectory(file: String): List[SPAttributeValue] = {
//    val lines = file.lines.toList.map(_.trim.split("""\s+""").toList )
//    val numbs = lines.map(_.flatMap(parseDouble)).filterNot(_.isEmpty)
//    val joints = numbs.map(_.slice(6, 12))
//    val energy = numbs.map(_.slice(0, 6))
//
//    val zip = joints zip energy zip (1 to joints.size)
//
//    zip.map {
//      case ((position, energy), sample) => {
//        MapPrimitive(Map(
//          "sample"-> IntPrimitive(sample),
//          "position"-> ListPrimitive(position.map(DoublePrimitive.apply)),
//          "current"-> ListPrimitive(energy.map(DoublePrimitive.apply))
//        ))
//      }
//    }
//  }

  def parseDouble(s: String) = try { Some(s.toDouble) } catch { case _:Throwable => None }

  implicit class ExtraDouble(d: Double) {
    def ^(n: Int) = scala.math.pow(d, n)
  }


}


object ImportKUKAFileService{
  def props(modelHandler: ActorRef) = Props(classOf[ImportKUKAFileService], modelHandler)
}

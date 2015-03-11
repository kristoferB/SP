package sp.jsonImporter

import akka.actor._
import sp.system.messages._
import sp.domain._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

/**
 * To import operations and things from json
 */
class ImportJSONService(modelHandler: ActorRef) extends Actor {
  implicit val timeout = Timeout(1 seconds)

  def receive = {
    case Request(_, attr) => {
      val reply = sender
      extract(attr) match {
        case Some((file, name)) => {

          println(s"I got the file in importJSON: $file")

          println(name)

          /*
          Add the operations and thins from json
           */

          val items: List[IDAble] = List()

          val id = ID.newID
          val n = name.flatMap(_.asString).getOrElse("noName")
          for {
            model <- (modelHandler ? CreateModel(id, n, Attr("attributeTags" -> MapPrimitive(Map()), "conditionGroups" -> ListPrimitive(List())))).mapTo[ModelInfo]
            items <- modelHandler ? UpdateIDs(id, model.version, items.toList)
          } yield {
            println(s"MADE IT: $model")
            reply ! model.model.toString
          }

        }
        case None => sender ! errorMessage(attr)
      }
    }
  }

  def extract(attr: SPAttributes) = {
    for {
      file <- attr.getAsString("file")
    } yield (file, attr.get("name"))
  }

  def errorMessage(attr: SPAttributes) = {
    SPError("The request is missing parameters: \n" +
      s"file: ${attr.getAsString("file")}" + "\n" +
      s"Request: ${attr}")
  }

}


object ImportJSONService {
  def props(modelHandler: ActorRef) = Props(classOf[ImportJSONService], modelHandler)
}

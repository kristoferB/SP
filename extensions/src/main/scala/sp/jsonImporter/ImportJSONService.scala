package sp.jsonImporter

import akka.actor._
import sp.domain.logic.PropositionParser
import sp.system.ServiceHandler
import sp.system.messages._
import sp.domain._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import spray.json._
import sp.json._
import sp.json.SPJson._

/**
 * To import operations and things from json
 */
class ImportJSONService(modelHandler: ActorRef) extends Actor {
  implicit val timeout = Timeout(1 seconds)

  import context.dispatcher

  def receive = {
    case Request(_, attr) => {
      val reply = sender
      extract(attr) match {
        case Some((file, name)) => {

          println(s"Name: $name")

          val items: List[IDAble] = JsonParser(s"$file").convertTo[List[IDAble]]

          val id = ID.newID
          val n = name.flatMap(_.asString).getOrElse("noName")
          for {
            model <- (modelHandler ? CreateModel(id, n, Attr("attributeTags" -> MapPrimitive(Map()), "conditionGroups" -> ListPrimitive(List())))).mapTo[ModelInfo]
            items <- (modelHandler ? UpdateIDs(id, model.version, items.toList)).mapTo[SPIDs]
            ops <- (modelHandler ? GetOperations(id)).mapTo[SPIDs]
            newOps = ops.items.map(_.asInstanceOf[Operation]).map(parseGuardActionToPropositionCondition)
            items2 <- (modelHandler ? (UpdateIDs(id, model.version + 1, newOps))).mapTo[SPIDs]
          } yield {
            println(s"MADE IT: $model")

            reply ! model.model.toString
          }

        }
        case None => sender ! errorMessage(attr)
      }
    }
  }

  def parseGuardActionToPropositionCondition(op: Operation): Operation = {
    def getGuard(str: String) = for {
      guard <- op.attributes.get(str)
      guardAsString <- guard.asString
    } yield PropositionParser.parseStr(guardAsString) match {
        case Right(p) => p
        case _ => AlwaysFalse
      }
    val preGuard = getGuard("preGuard").get
    val postGuard = getGuard("postGuard").get
    Operation(name = op.name, conditions = List(PropositionCondition(preGuard, List()), PropositionCondition(postGuard, List())), attributes = op.attributes)
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

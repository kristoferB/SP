package sp.jsonImporter

import akka.actor._
import sp.domain.logic.{PropositionConditionLogic, ActionParser, PropositionParser}
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._
import org.json4s.native.Serialization._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

/**
 * To import operations and things from json
 */
class ImportJSONService(modelHandler: ActorRef) extends Actor with ServiceSupportTrait {
  implicit val timeout = Timeout(1 seconds)

  import context.dispatcher

  def receive = {
    case Request(_, attr, _) => {
      val reply = sender
      extract(attr) match {
        case Some((file, name)) => {

          println(s"Name: $name")

          lazy val idables = read[List[IDAble]](file)

          for {
          //Creates a model and updates the model with "idables" parsed from the given json file
            modelInfo <- futureWithErrorSupport[ModelInfo](modelHandler ? CreateModel(
              id = ID.newID,
              name = name.getOrElse("noName")))
            _ <- futureWithErrorSupport[Any](modelHandler ? UpdateIDs(model = modelInfo.id, items = idables))

            //Update the operations in the model with "conditions" connected to the parsed "idables"
<<<<<<< HEAD
            SPIDs(opsToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetOperations(model = modelInfo.model))
            ops = opsToBe.map(_.asInstanceOf[Operation])
            _ <- futureWithErrorSupport[Any](modelHandler ? UpdateIDs(model = modelInfo.model, modelVersion = modelInfo.version, items = ops))
=======
            SPIDs(opsToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetOperations(model = modelInfo.id))
            opsWithConditionsAdded = opsToBe.map(_.asInstanceOf[Operation]).flatMap(op => PropositionConditionLogic.parseAttributesToPropositionCondition(op, idables))
            _ <- futureWithErrorSupport[Any](modelHandler ? UpdateIDs(model = modelInfo.id,  items = opsWithConditionsAdded))
>>>>>>> 2951d9606fc9637de1f9ea358be9a223dc5295e0

          } yield {
            println(s"MADE IT: $modelInfo")
            //            println(opsWithConditionsAdded.map(_.name).mkString("\n"))
            reply ! modelInfo.id.toString
          }

        }
        case None => sender ! errorMessage(attr)
      }
    }
  }

  def extract(attr: SPAttributes) = {
    for {
      file <- attr.getAs[String]("file")
    } yield (file, attr.getAs[String]("name"))
  }

  def errorMessage(attr: SPAttributes) = {
    SPError("The request is missing parameters: \n" +
      s"file: ${attr.getAs[String]("file")}" + "\n" +
      s"Request: ${attr}")
  }

}

object ImportJSONService {
  def props(modelHandler: ActorRef) = Props(classOf[ImportJSONService], modelHandler)
}
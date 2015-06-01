package sp.jsonImporter

import akka.actor._
import sp.domain.logic.{ActionParser, PropositionParser}
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
    case Request(_, attr) => {
      val reply = sender
      extract(attr) match {
        case Some((file, name)) => {

          println(s"Name: $name")

          val idables = read[List[IDAble]](file)

          for {
          //Creates a model and updates the model with "idables" parsed from the given json file
            modelInfo <- futureWithErrorSupport[ModelInfo](modelHandler ? CreateModel(
              name = name.getOrElse("noName")))
            _ <- futureWithErrorSupport[Any](modelHandler ? UpdateIDs(model = modelInfo.model, modelVersion = modelInfo.version, items = idables))

            //Update the operations in the model with "conditions" connected to the parsed "idables"
            SPIDs(opsToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetOperations(model = modelInfo.model))
            opsWithConditionsAdded = opsToBe.map(_.asInstanceOf[Operation]).flatMap(op => parseAttributesToPropositionCondition(op, idables))
            _ <- futureWithErrorSupport[Any](modelHandler ? UpdateIDs(model = modelInfo.model, modelVersion = modelInfo.version, items = opsWithConditionsAdded))

          } yield {
            println(s"MADE IT: $modelInfo")
            //            println(opsWithConditionsAdded.map(_.name).mkString("\n"))
            reply ! modelInfo.model.toString
          }

        }
        case None => sender ! errorMessage(attr)
      }
    }
  }

  def parseAttributesToPropositionCondition(op: Operation, idablesToParseFromString: List[IDAble]): Option[Operation] = {
    def getGuard(str: String) = (for {
      guard <- op.attributes.get(str)
      guardAsString <- guard.getAs[String]
    } yield PropositionParser(idablesToParseFromString).parseStr(guardAsString) match {
        case Right(p) => Some(p)
        case Left(f) => {
          println(s"PropositionParser failed for operation ${op.name} on guard: $guardAsString. Failure message: $f")
          None
        }
      }).flatten
    def getAction(str: String) = for {
      actions <- op.attributes.get(str)
      actionsAsString <- actions.getAs[List[String]]
    } yield actionsAsString.map { action => ActionParser(idablesToParseFromString).parseStr(action) match {
        case Right(a) => Some(a)
        case Left(f) => {
          println(s"ActionParser failed for operation ${op.name} on action: $action. Failure message: $f")
          None
        }
      }
      }.flatten

    return for {
      preGuard <- getGuard("preGuard")
      preAction <- getAction("preAction")
      postGuard <- getGuard("postGuard")
      postAction <- getAction("postAction")
    } yield {
        op.copy(conditions = List(PropositionCondition(preGuard, preAction, SPAttributes("kind" -> "precondition")),
          PropositionCondition(postGuard, postAction, SPAttributes("kind" -> "postcondition"))))
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
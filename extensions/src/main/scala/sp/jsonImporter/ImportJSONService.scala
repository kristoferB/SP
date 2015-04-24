package sp.jsonImporter

import akka.actor._
import sp.domain.logic.{PropositionConditionLogic, ActionParser, PropositionParser}
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

          val idables: List[IDAble] = JsonParser(s"$file").convertTo[List[IDAble]]

          val id = ID.newID
          val n = name.flatMap(_.asString).getOrElse("noName")
          for {
            model <- (modelHandler ? CreateModel(id, n, Attr("attributeTags" -> MapPrimitive(Map()), "conditionGroups" -> ListPrimitive(List())))).mapTo[ModelInfo]
            _ <- modelHandler ? UpdateIDs(id, model.version, idables)
            SPIDs(opsToBe) <- (modelHandler ? GetOperations(id)).mapTo[SPIDs]
            opsWithConditionsAdded = opsToBe.map(_.asInstanceOf[Operation]).flatMap(op => parseGuardActionToPropositionCondition(op,idables))
            _ <-  modelHandler ? (UpdateIDs(id, model.version, opsWithConditionsAdded))
            SPIDs(thingsToBe) <- (modelHandler ? GetThings(id)).mapTo[SPIDs]
            things = thingsToBe.map(_.asInstanceOf[Thing])
            initState = getInitState(things)

          } yield {
            println(s"MADE IT: $model")
            println(opsWithConditionsAdded.map(o => s"${o.name} ${
              import PropositionConditionLogic._

              val preGuard = o.conditions.head.asInstanceOf[PropositionCondition].guard

              preGuard.eval(initState.get)
            }").mkString("\n"))

            reply ! model.model.toString
          }

        }
        case None => sender ! errorMessage(attr)
      }
    }
  }

  def parseGuardActionToPropositionCondition(op: Operation, idablesToParseFromString: List[IDAble]): Option[Operation] = {
    def getGuard(str: String) = (for {
      guard <- op.attributes.get(str)
      guardAsString <- guard.asString
    } yield PropositionParser(idablesToParseFromString).parseStr(guardAsString) match {
        case Right(p) => Some(p)
        case Left(f) => {
          println(s"PropositionParser failed on guard: $guardAsString. Failure message: $f");
          None
        }
      }).flatten
    def getAction(str: String) = for {
      actions <- op.attributes.get(str)
      actionsList <- actions.asList
      actionsAsString = actionsList.flatMap(_.asString)
    } yield actionsAsString.map { action => ActionParser(idablesToParseFromString).parseStr(action) match {
        case Right(a) => Some(a)
        case Left(f) => {
          println(s"ActionParser failed on action: $action. Failure message: $f");
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
        op.copy(conditions = List(PropositionCondition(preGuard, preAction, SPAttributes(Map("kind" -> "preconditon"))),
          PropositionCondition(postGuard, postAction, SPAttributes(Map("kind" -> "postconditon")))))
      }
  }

  def getInitState(things: List[Thing]): Option[State] = {
    val idOptionInitValueList = things.map { v => v.id -> v.attributes.get("init") }
    if (idOptionInitValueList.contains(None)) {
      println(s"Init values could not be extracted from the things." +
        s"At least the thing ${idOptionInitValueList.filter(kv => kv._2.isEmpty).head} lacks attribute 'init'")
      None
    }
    else Some(State(idOptionInitValueList.map { case (k, v) => k -> v.get }.toMap))
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

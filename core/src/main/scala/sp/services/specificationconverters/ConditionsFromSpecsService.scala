package sp.services.specificationconverters

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import sp.domain._
import sp.domain.Logic._
import sp.system.messages._

import scala.concurrent.duration._
import scala.util._


case class ConditionsFromSpecs(map: Map[ID, List[Condition]])

/**
 * Created by Kristofer on 2014-09-04.
 */
class ConditionsFromSpecsService(modelHandler: ActorRef) extends Actor {
  private implicit val to = Timeout(1 seconds)

  import context.dispatcher

  def receive = {
    case None => "hej"
    case Request(_, attr) => {
      val reply = sender
      extract(attr) match {
        case Some((model, opsID)) => {
          val specsF = modelHandler ? GetSpecs(model)

          specsF map {
            case SPIDs(specs) =>
              // handle on SOPSpec for now
              val sopSpecs = specs filter(_.isInstanceOf[SOPSpec]) map(_.asInstanceOf[SOPSpec])
              import sp.domain.logic.SOPLogic._
              val conds = sopSpecs map{spec =>
                val sop = spec.sop
                val group = spec.name
                extractOperationConditions(sop, group)
              }
              val fold = mergeConditionMaps(conds)
              reply ! ConditionsFromSpecs(fold)

            case error: SPErrorString => reply !  error
          }

          specsF.recover { case e: Exception => {
            println("Resultfuture Fail: " + e.toString)
            reply ! SPError(e.toString)
          }}
        }


        case None => reply ! errorMessage(attr)
      }

    }
  }

  def extract(attr: SPAttributes) = {
    for {
      model <- attr.getAs[ID]("model")
    } yield {
      val ops = (attr.getAs[List[ID]]("operations").getOrElse(List[ID]()))
      (model, ops)
    }
  }

  def errorMessage(attr: SPAttributes) = {
    SPError("The request is missing parameters: \n" +
      s"model: ${attr.getAs[ID]("model")}" + "\n" +
      s"ops: ${attr.getAs[List[ID]]("operations")}" )
  }
}


object ConditionsFromSpecsService{
  def props(modelHandler: ActorRef) = Props(classOf[ConditionsFromSpecsService], modelHandler)
}
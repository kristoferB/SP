package sp.services.specificationconverters

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import sp.domain._
import sp.system.messages._

import scala.concurrent.duration._
import scala.util._

/**
 * Created by Kristofer on 2014-09-04.
 */
class ConditionsFromSpecsService(modelHandler: ActorRef) extends Actor {
  private implicit val to = Timeout(1 seconds)

  import context.dispatcher

  def receive = {
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
                extractOperationCondition(sop, group)
              }
              val fold = mergeConditionMaps(conds)

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
      model <- attr.getAsString("model")
    } yield {
      val ops = (attr.getAsList("operations") map( _.flatMap(_.asID))).getOrElse(List[ID]())
      (model, ops)
    }
  }

  def errorMessage(attr: SPAttributes) = {
    SPError("The request is missing parameters: \n" +
      s"model: ${attr.getAsString("model")}" + "\n" +
      s"ops: ${attr.getAsList("operations") map (_.flatMap(_.asID))}" )
  }
}


object ConditionsFromSpecsService{
  def props(modelHandler: ActorRef) = Props(classOf[ConditionsFromSpecsService], modelHandler)
}
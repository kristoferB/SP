package sp.virtcom

import akka.actor.{Props, Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import sp.domain.{State, Thing, Operation, ID}
import sp.jsonImporter.ServiceSupportTrait
import sp.system.messages._
import scala.concurrent.duration._
import sp.domain.Logic._

/**
 * Created by patrik on 2015-06-03.
 */
class CreateInstancesFromTypeModelService(modelHandler: ActorRef) extends Actor with ServiceSupportTrait {
  implicit val timeout = Timeout(1 seconds)

  import context.dispatcher

  def receive = {
    case Request(service, attr) => {

      println(s"service: $service")

      val id = attr.getAs[ID]("activeModelID").getOrElse(ID.newID)



            val result = for {
              modelInfo <- futureWithErrorSupport[ModelInfo](modelHandler ? GetModelInfo(id))
              SPIDs(opsToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetOperations(model = modelInfo.model))
              ops = opsToBe.map(_.asInstanceOf[Operation])
              SPIDs(varsToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetThings(model = modelInfo.model))
              vars = varsToBe.map(_.asInstanceOf[Thing])

//              _ <- futureWithErrorSupport[Any](modelHandler ? UpdateIDs(model = id, modelVersion = modelInfo.version, items = newOps))

            } yield {
                println(ops)
                println(vars)
                "ok"
              }

      sender ! result

    }
  }

  private case class OpSeqResult(finalState : State, opSeq : Seq[Operation])
//  def findOpSeq(freshStates : Map[State,Set[Operation]],visitedStates : Map[State,Seq[Operation]] = Map()) : OpSeqResult = {
//
//  }


}

object CreateInstancesFromTypeModelService {
  def props(modelHandler: ActorRef) = Props(classOf[CreateInstancesFromTypeModelService], modelHandler)
}

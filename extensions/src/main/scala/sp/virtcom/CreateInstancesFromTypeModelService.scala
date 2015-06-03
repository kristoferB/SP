package sp.virtcom

import akka.actor.{Props, Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import sp.domain._
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
          //          println(ops)
          //          println(vars)
          val initState = getInitState(vars.toSet)
          val wfs = WrapperForSearch(ops.toSet)
          val result = for {
            r1 <- wfs.findOpSeq("gripRoof_ABB", Set(initState), Map(initState -> Seq()))
            r2 <- wfs.findOpSeq("fixateRoof_ABB", r1)
            r3 <- wfs.findOpSeq("fixateRoof_ABB", r2)
          } yield {
              (r3.opSeq ++ r2.opSeq ++ r1.opSeq).reverse
            }
          println("start")
          result match {
            case Some(r) => println(r.map(_.name).mkString("\n"))
            case _ =>
          }
          println("end")

        }

      sender ! result

    }
  }

  def getInitState(vars: Set[Thing]) = {
    val state = vars.foldLeft(Map(): Map[ID, SPAttributeValue]) { case (acc, v) =>
      v.attributes.getAsMap("stateVariable") match {
        case Some(map) => map.get("init") match {
          case Some(value) => acc + (v.id -> value)
          case _ => acc
        }
        case _ => acc
      }
    }
    State(state)
  }

  case class WrapperForSearch(ops: Set[Operation]) {

    case class OpSeqResult(finalState: State, opSeq: Seq[Operation])

    def findOpSeq(soughtOp: String, r: OpSeqResult) : Option[OpSeqResult]= findOpSeq(soughtOp, Set(r.finalState), Map(r.finalState -> Seq()))
    def findOpSeq(soughtOp: String, freshStates: Set[State], visitedStates: Map[State, Seq[Operation]]): Option[OpSeqResult] = {
      import sp.domain.logic.PropositionConditionLogic._

      def isOpPresent(opsToLookAt: Seq[(State, Set[Operation])]): Option[OpSeqResult] = {
        def isOpPresentInner(ops: Seq[Operation]): Option[Operation] = ops match {
          case o +: os => if (o.name.equals(soughtOp)) Some(o) else isOpPresentInner(os)
          case _ => None
        }
        opsToLookAt match {
          case (s, ops) +: rest => isOpPresentInner(ops.toSeq) match {
            case Some(o) => Some(OpSeqResult(o.conditions(1).next(o.conditions(0).next(s)), o +: visitedStates(s)))
            case _ => isOpPresent(rest)
          }
          case _ => None
        }

      }
      //Method starts
      if (freshStates.isEmpty) None
      else {
        val enabledOps = freshStates.map(s => s -> ops.filter(_.conditions(0).eval(s)))
        case class LocalResult(fStates: Set[State], vStates: Map[State, Seq[Operation]])
        isOpPresent(enabledOps.toSeq) match {
          case Some(r) => Some(r)
          case _ => {
            val newStates = enabledOps.foldLeft(new LocalResult(Set(), visitedStates): LocalResult) { case (acc, (s, os)) =>
              val newAcc = os.foldLeft(acc: LocalResult) { case (accInner, o) => {
                val newState = o.conditions(1).next(o.conditions(0).next(s))
                if (accInner.vStates.contains(newState))
                  accInner.copy()
                else
                  LocalResult(accInner.fStates + newState, Map(newState -> (o +: accInner.vStates(s))) ++ accInner.vStates)
              }
              }
              newAcc
            }
            findOpSeq(soughtOp, newStates.fStates, newStates.vStates)
          }
        }
      }
    }

  }

}

object CreateInstancesFromTypeModelService {
  def props(modelHandler: ActorRef) = Props(classOf[CreateInstancesFromTypeModelService], modelHandler)
}

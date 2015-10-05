package sp.areus

import akka.actor._
import sp.domain._
import sp.system._
import sp.system.{ServiceLauncher, SPService}
import sp.system.messages._
import sp.virtcom.modeledCases.{PSLFloorRoofCase, VolvoWeldConveyerCase}
import sp.domain.Logic._

/**
 * Created by patrik on 2015-10-05.
 */

object SearchOpSeqTimeService extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "description" -> "Creates SOP for operations with shortast* execution time"
    ),
    "setup" -> SPAttributes(
      "ops" -> KeyDefinition("List[ID]", List(), Some(SPValue(List())))
    )
  )

  val transformTuple = (
    TransformValue("setup", _.getAs[SearchOpSeqTimeSetup]("setup"))
    )

  val transformation = transformToList(transformTuple.productIterator.toList)

  def props = ServiceLauncher.props(Props(classOf[SearchOpSeqTimeService]))

}

case class SearchOpSeqTimeSetup(model: String)

class SearchOpSeqTimeService extends Actor with ServiceSupport {

  def receive = {
    case r@Request(service, attr, ids, reqID) =>

      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      // include this if you want to send progress messages. Send attributes to it during calculations
      val progress = context.actorOf(progressHandler)
      println(s"service: $service")

      val o11 = Operation("o11", List(), SPAttributes("time" -> 2))
      val o12 = Operation("o12", List(), SPAttributes("time" -> 5))
      val o13 = Operation("o13", List(), SPAttributes("time" -> 4))
      val o21 = Operation("o21", List(), SPAttributes("time" -> 3))
      val o22 = Operation("o22", List(), SPAttributes("time" -> 2))
      val o23 = Operation("o23", List(), SPAttributes("time" -> 3))
      val ops = List(o11, o12, o13, o21, o22, o23)

      val sopSeq = SOP(Sequence(o11, o12, o13), Sequence(o21, o22, o23))
      val sopArbi = SOP(Arbitrary(o12, o22))

      val conditions = sp.domain.logic.SOPLogic.extractOperationConditions(List(sopSeq, sopArbi), "traj")
      val opsUpd = ops.map { o =>
        val cond = conditions.get(o.id).map(List(_)).getOrElse(List())
        o.copy(conditions = cond)
      }

      lazy val initState = State(ops.map(o => o.id -> OperationState.init).toMap)

      println(s"initState: $initState")

      def f1() = {
        implicit val evalSetup2 = EvaluateProp(ops.map(o => o.id -> ((_: SPValue) => true)).toMap, Set(), TwoStateDefinition)

        println(s"enabled: ${opsUpd.filter(o => o.eval(initState))}")

        println(s"update2: ${opsUpd.filter(o => o.eval(initState)).map(o => o.next(initState)).mkString("\n")}")
      }
      f1

      implicit val evalSetup3 = EvaluateProp(ops.map(o => o.id -> ((_ : SPValue) => true)).toMap,Set(),ThreeStateDefinition)

      println(s"update3: ${opsUpd.filter(o => o.eval(initState)).map(o => o.next(initState)).mkString("\n")}")

      rnr.reply ! Response(List(), SPAttributes(), service, reqID)
      progress ! PoisonPill
      self ! PoisonPill

    case (r: Response, reply: ActorRef) => {
      reply ! r
    }
    case x => {
      sender() ! SPError("What do you whant me to do? " + x)
      self ! PoisonPill
    }

  }

}
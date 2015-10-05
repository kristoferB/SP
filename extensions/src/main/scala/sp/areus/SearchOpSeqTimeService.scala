package sp.areus

import akka.actor._
import sp.domain._
import sp.system._
import sp.system.{ServiceLauncher, SPService}
import sp.system.messages._
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

      def mapOps[T](t: T) = ops.map(o => o.id -> t).toMap

      lazy val initState = State(mapOps(OperationState.init))
      lazy val goalState = State(mapOps(OperationState.finished))

      lazy val evalualteProp2 = EvaluateProp(mapOps((_: SPValue) => true), Set(), TwoStateDefinition)
      lazy val evalualteProp3 = EvaluateProp(mapOps((_: SPValue) => true), Set(), ThreeStateDefinition)


      def findStraightSeq(ops: List[Operation], initState: State, goalState: State, evalSetup: EvaluateProp) = {
        implicit val es = evalSetup

        println(s"enabled: ${ops.filter(o => o.eval(initState))}")

        println(s"update2: ${ops.filter(o => o.eval(initState)).map(o => o.next(initState)).mkString("\n")}")
      }

      implicit val evalSetup3 = evalualteProp3

      println(s"update3: ${opsUpd.filter(o => o.eval(initState)).map(o => o.next(initState)).mkString("\n")}")

      rnr.reply ! Response(List(), SPAttributes(), service, reqID)
      progress ! PoisonPill
      self ! PoisonPill

    case (r: Response, reply: ActorRef) =>
      reply ! r

    case x =>
      sender() ! SPError("What do you whant me to do? " + x)
      self ! PoisonPill

  }

}
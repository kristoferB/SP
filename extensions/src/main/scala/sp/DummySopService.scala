package sp.extensions.DummySopService

import sp.system._
import akka.actor._
import sp.domain.logic.IDAbleLogic
import scala.concurrent._
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._

object DummySopService extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "description" -> "Create a Dummy SOP"
    ),
    "setup" -> SPAttributes(
      "sopname" -> KeyDefinition("String", List(), Some("DummySopName")),
      "stringList" -> KeyDefinition("List[String]")
    )
  )

  val transformTuple = (
    TransformValue("setup", _.getAs[DummySopSetup]("setup"))
    )

  val transformation = transformToList(transformTuple.productIterator.toList)

  // important to incl ServiceLauncher if you want unique actors per request
  def props = ServiceLauncher.props(Props(classOf[DummySopService]))

  // Alla får även "core" -> ServiceHandlerAttributes

  //  case class ServiceHandlerAttributes(model: Option[ID],
  //                                      responseToModel: Boolean,
  //                                      onlyResponse: Boolean,
  //                                      includeIDAbles: List[ID])

}

case class DummySopSetup(sopname: String, stringList: List[String])

// Add constructor parameters if you need access to modelHandler and ServiceHandler etc
class DummySopService extends Actor with ServiceSupport {

  import context.dispatcher

  def receive = {
    case r@Request(service, attr, ids, reqID) =>

//      //Example-------------------------------------------------------------
//            val o11 = Operation("o11", List(), SPAttributes("time" -> 2))
//            val o12 = Operation("o12", List(), SPAttributes("time" -> 5))
//            val o13 = Operation("o13", List(), SPAttributes("time" -> 4))
//            val o21 = Operation("o21", List(), SPAttributes("time" -> 1.5))
//            val o22 = Operation("o22", List(), SPAttributes("time" -> 2))
//            val o23 = Operation("o23", List(), SPAttributes("time" -> 4.23))
//            val opsEx = List(o11, o12, o13, o21, o22, o23)
//
//            val sopSeq = SOP(Sequence(o11, o12, o13), Sequence(o21, o22, o23))
//            val sopArbi = SOP(Arbitrary(o12, o22))
//
//            val conditions = sp.domain.logic.SOPLogic.extractOperationConditions(List(sopSeq, sopArbi), "traj")
//            val opsUpd = opsEx.map { o =>
//              val cond = conditions.get(o.id).map(List(_)).getOrElse(List())
//              o.copy(conditions = cond)
//            }

      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      // include this if you want to send progress messages. Send attributes to it during calculations
      val progress = context.actorOf(progressHandler)

      println(s"I got: $r")

      val setup = transform(DummySopService.transformTuple)

      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get

      val ops = ids.filter(item => item.isInstanceOf[Operation])
      println(s"selected ops: ${ops.map(_.name)}")

            println(s"core är även med: $core")

      println(s"stringList ${setup.stringList}")

      val hs = ops.map(x => Hierarchy(x.id))
      val seq = Sequence(hs: _*)
      val items = List(SOPSpec(name = setup.sopname, sop = List(seq), attributes = SPAttributes().addTimeStamp))

      sendResp(Response(items, SPAttributes("info" -> "Dummy SOP created"), service, reqID), progress)

    case (r: Response, reply: ActorRef) =>
      reply ! r
    case x =>
      sender() ! SPError("What do you whant me to do? " + x)
      self ! PoisonPill
  }

  import scala.concurrent._
  import scala.concurrent.duration._

  def sendResp(r: Response, progress: ActorRef)(implicit rnr: RequestNReply) = {
    rnr.reply ! r
    progress ! PoisonPill
    self ! PoisonPill
  }

}




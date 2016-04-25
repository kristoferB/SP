package sp.runnerService

import akka.actor._
import sp.domain.Logic._
import sp.domain.SOP._
import sp.domain._
import sp.system.messages._
import sp.system.{SPService, ServiceLauncher, _}
import sp.system.SPActorSystem.eventHandler


/**
  * Created by Susan on 2016-03-11.
  */

object AutoTest extends SPService{
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "runner",
      "description" -> "A service to run SOP's in SP"
    ),
    "SOP" -> KeyDefinition("Option[ID]", List(), Some("")
    )
  )

  val transformTuple  = TransformValue("SOP", _.getAs[ID]("SOP"))

  val transformation = List(transformTuple)

  def props(eventHandler: ActorRef, runnerService: ActorRef) = Props(classOf[AutoTest], eventHandler,runnerService)
  //def props(eventHandler: ActorRef, operationController: String) =
//    ServiceLauncher.props(Props(classOf[AutoTest], eventHandler, RunnerService))

}

class AutoTest(eventHandler: ActorRef, runnerService: ActorRef) extends Actor with ServiceSupport {
  val o1 = Operation("o1", List(), SPAttributes(), ID.makeID("a0f565e2-e44b-4017-a24e-c7d01e970dec").get)
  val o2 = Operation("o2", List(), SPAttributes(), ID.makeID("b0f565e2-e44b-4017-a24e-c7d01e970dec").get)
  val o3 = Operation("o3", List(), SPAttributes(), ID.makeID("c0f565e2-e44b-4017-a24e-c7d01e970dec").get)
  val o4 = Operation("o4", List(), SPAttributes(), ID.makeID("d0f565e2-e44b-4017-a24e-c7d01e970dec").get)
  val o5 = Operation("o5", List(), SPAttributes(), ID.makeID("e0f565e2-e44b-4017-a24e-c7d01e970dec").get)
  val sop = Parallel(Sequence(o1, Parallel(Sequence(o2, o3), o4), o5))

  val sopSpec = SOPSpec("theSOPSpec", List(sop), SPAttributes())

  val longList: List[IDAble] = List(o1, o2, o3, o4, o5, sopSpec)

  def receive = {
    case r@Request(service, attr, ids, reqID) => {
      val r = Request("RunnerService",
        SPAttributes(
          "SOP" -> sopSpec.id
        ),
        longList
      )
      runnerService ! r
    }
  }
}

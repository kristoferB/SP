package sp.patientcardsservice

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import scala.util.{ Try, Success }
import scala.util.Random.nextInt

sealed trait API_PatientCardsService
object API_PatientCardsService {
  case class NewPatient() extends API_PatientCardsService
  case class DiffPatient() extends API_PatientCardsService
  case class RemovedPatient() extends API_PatientCardsService
  case class Patient( careContactId: String, patientData: Map[String,Any]) extends API_PatientCardsService
  case class elvisEvent( eventType: String, patient: Patient) extends API_PatientCardsService
  case class State(state: State) extends API_PatientCardsService

  val service = "patientCardsService"
}
class PatientCardsService extends Actor with ActorLogging {

  // conneting to the pubsub bus using the mediator actor
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe(PatientCardsService.service, self) // "self" is an actor, goes to actors "recieve()"

  // A "ticker" that sends a "tick" string to self every 2 second
  import scala.concurrent.duration._
  import context.dispatcher

  var serviceOn: Boolean = false
  context.system.scheduler.schedule(0.5 seconds, 0.5 seconds, self, "tick")

  def parseCommand(x: String): Try[API_PatientCardsService] =
    SPMessage fromJson(x) flatMap (_.getBodyAs[API_PatientCardsService])

    def handleCommand: API_PatientCardsService => Unit = {
      case API_PatientCardsService.Start() => serviceOn = true
      case API_PatientCardsService.Stop() => serviceOn = false
    }

    def dataMsg() = SPMessage.make(
      SPAttributes("from" -> PatientCardsService.service).addTimeStamp,
      API_PatientCardsService.D3Data(List.fill(7)(nextInt(50)))
    ).get.toJson

    def receive = {
      case "tick" => if(serviceOn) mediator ! Publish("d3ExampleAnswers", dataMsg())
      case x: String => parseCommand(x) foreach handleCommand
      case z => println(s"PatientCardsService didn't recognize $z")
    }


    def handleRequests(x: String): Unit = {
      val mess = SPMessage.fromJson(x)

      matchRequests(mess)
      matchVDMessages(mess)
      matchServiceRequests(mess)


    }

    def matchRequests(mess: Try[SPMessage]) = {
      PatientCardsComm.extractRequest(mess, handlerID, name) map { case (h, b) =>
        val updH = h.copy(from = h.to, to = "")

        // Message was to me so i send an SPACK
        mediator ! Publish("answers", AbilityComm.makeMess(updH, APISP.SPACK()))

        b match {
          case api.StartAbility(id, params, attr) =>
          abilities.get(id) match {
            case Some(a) =>
            a.actor ! StartAbility(state, h.reqID, params, attr)
            case None =>
            mediator ! Publish("answers", AbilityComm.makeMess(updH, APISP.SPError(s"ability $id does not exists in this handler")))
            mediator ! Publish("answers", AbilityComm.makeMess(updH, APISP.SPDone()))

          }

          case api.ForceResetAbility(id) =>
          abilities.get(id) match {
            case Some(a) =>
            a.actor ! ResetAbility(state)
            case None =>
            mediator ! Publish("answers", AbilityComm.makeMess(updH, APISP.SPError(s"ability $id does not exists in this handler")))
          }
          mediator ! Publish("answers", AbilityComm.makeMess(updH, APISP.SPDone()))

          case x: api.ForceResetAllAbilities =>
          val r = ResetAbility(state)
          abilities.foreach(kv => kv._2.actor ! r)
          mediator ! Publish("answers", AbilityComm.makeMess(updH, APISP.SPDone()))

          case api.ExecuteCmd(cmd) =>
          // to be implemented

          case x: api.GetAbilities =>
          val xs = abilities.map(_._2.ability).toList

          val abs = abilities.map(a=>(a._2.ability.id,a._2.ability.name)).toList

          println("got getabilitiies request")
          mediator ! Publish("answers", AbilityComm.makeMess(updH, api.Abilities(xs)))
          mediator ! Publish("answers", AbilityComm.makeMess(updH, api.Abs(abs)))

          mediator ! Publish("answers", AbilityComm.makeMess(updH, APISP.SPDone()))

          abilities.foreach(a => a._2.actor ! GetState)


          case api.SetUpAbility(ab, hand) =>
          val ids = idsFromAbility(ab)
          val act = context.actorOf(AbilityActor.props(ab))
          abilities += ab.id -> AbilityStorage(ab, act, ids)
          act ! NewState(filterState(ids, state))
          mediator ! Publish("answers", AbilityComm.makeMess(updH, APISP.SPDone()))
        }
      }
    }

  }

  object PatientCardsService {
    def props = Props(classOf[PatientCardsService])
    val service = "patientCardsService"
  }

package sp.patientcardsservice

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import scala.util.{ Try, Success }
import scala.util.Random.nextInt

// sealed trait API_PatientEvent
// object API_PatientEvent {
//   case class NewPatient() extends API_PatientEvent
//   case class DiffPatient() extends API_PatientEvent
//   case class RemovedPatient() extends API_PatientEvent
//   case class Patient( careContactId: String, patientData: Map[String,Any]) extends API_PatientEvent
//   case class elvisEvent( eventType: String, patient: Patient) extends API_PatientEvent
//   case class State(state: State) extends API_PatientEvent
//
//   val service = "patientCardsDevice"
// }

package API_PatientEvent {
  sealed trait PatientEvent
  case class NewPatient( careContactId: String, patientData: Map[String,Any]) extends PatientEvent
  case class DiffPatient( careContactId: String, patientData: Map[String,Any]) extends PatientEvent
  case class RemovedPatient( careContactId: String) extends PatientEvent

  // sealed trait API_PatientCardsDevice
  // case class NewPatient() extends API_PatientCardsDevice
  // case class DiffPatient() extends API_PatientCardsDevice
  // case class RemovedPatient() extends API_PatientCardsDevice
  // case class Patient( careContactId: String, patientData: Map[String,Any]) extends API_PatientCardsDevice
  // case class elvisEvent( eventType: String, patient: Patient) extends API_PatientCardsDevice
  // case class State(state: State) extends API_PatientCardsDevice

  object attributes {
    val service = "patientCardsService"
}
}

import sp.patientcardsservice.{API_PatientEvent => api}

class PatientCardsDevice extends Actor with ActorLogging {

  // conneting to the pubsub bus using the mediator actor
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe(PatientCardsDevice.service, self) // "self" is an actor, goes to actors "recieve()"


  var serviceOn: Boolean = false


  def parseCommand(x: String): Try[API_PatientEvent] =
    SPMessage fromJson(x) flatMap (_.getBodyAs[API_PatientEvent])

    def handleCommand: API_PatientEvent => Unit = {
      case api.Start() => serviceOn = true
      case api.Stop() => serviceOn = false
    }

    def dataMsg() = SPMessage.make(
      SPAttributes("from" -> PatientCardsDevice.service).addTimeStamp,
      api.D3Data(List.fill(7)(nextInt(50)))
    ).get.toJson

    def receive = {
      case "tick" => if(serviceOn) mediator ! Publish("d3ExampleAnswers", dataMsg())
      case x: String => parseCommand(x) foreach handleCommand
      case z => println(s"PatientCardsDevice didn't recognize $z")
    }


    def handleRequests(x: String): Unit = {
      val mess = SPMessage.fromJson(x)

      matchRequests(mess)

    }

    def matchRequests(mess: Try[SPMessage]) = {
      PatientCardsComm.extractRequest(mess) map { case (h, b) =>
        val updH = h.copy(from = h.to, to = "")


        b match {
          case api.NewPatient(careContactId, patientData) =>


        }
      }
    }

  }

  object PatientCardsDevice {
    def props = Props(classOf[PatientCardsDevice])
    // val service = "patientCardsService"
  }

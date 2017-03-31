package sp.example

import akka.actor._

import scala.util.{Failure, Random, Success, Try}
import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._

import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

// The messages that this service can send and receive is
// is defined using this API structure

package API_PatientEvent {

  // Messages I can receive
  sealed trait PatientEvent
  case class NewPatient(careContactId: String, patientData: Map[String, String]) extends PatientEvent
  case class DiffPatient(careContactId: String, patientData: Map[String, String]) extends PatientEvent
  case class RemovePatient(careContactId: String) extends PatientEvent

  object attributes {
    val service = "exampleService"
  }
}

import sp.example.{API_PatientEvent => api}


/**
  *  This is the actor (the service) that listens for messages on the bus
  *  It keeps track of a set of Pie diagrams that is updated every second
  */
class ExampleService extends Actor with ActorLogging {

  // connecting to the pubsub bus using the mediator actor
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe("patient-event-topic", self)


  // The metod that receve messages. Add service logic in a trait so you can test it. Here the focus in on parsing
  // and on the messages on the bus
  def receive = {
    case mess @ _ if {log.debug(s"ExampleService MESSAGE: $mess from $sender"); false} => Unit

    case x: String => handleRequests(x)

  }

 def handleRequests(x: String): Unit = {
    val mess = SPMessage.fromJson(x)
    matchRequests(mess)
 }

 def matchRequests(mess: Try[SPMessage]) = {
  extractPatientEvent(mess) map { case (h, b) =>
    b match {
      case api.NewPatient(careContactId, patientData) => println("new patient: " + careContactId + " -- patient data: " + patientData)
      case api.DiffPatient(careContactId, patientData) => println("updated patient: " + careContactId + " -- patient data: " + patientData)
      case api.RemovePatient(careContactId) => println("removed patient: " + careContactId)
    }
  }
}

 def extractPatientEvent(mess: Try[SPMessage]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader]
    b <- m.getBodyAs[api.PatientEvent]
  } yield (h, b)

  val statusResponse = SPAttributes(
    "service" -> api.attributes.service,
    "api" -> "to be added with macros later",
    "groups" -> List("examples"),
    "attributes" -> api.attributes
  )

  // Sends a status response when the actor is started so service handlers finds it
  override def preStart() = {
    val mess = SPMessage.makeJson(SPHeader(api.attributes.service, "serviceHandler"), statusResponse)
    mess.map(m => mediator ! Publish("spevents", m))
  }

  // A "ticker" that sends a "tick" string to self every 2 second
  import scala.concurrent.duration._
  import context.dispatcher
  val ticker = context.system.scheduler.schedule(2 seconds, 2 seconds, self, "tick")

}

object ExampleService {
  def props = Props(classOf[ExampleService])
}

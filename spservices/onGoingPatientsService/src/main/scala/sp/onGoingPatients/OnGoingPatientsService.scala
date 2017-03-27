package sp.onGoingPatients

import akka.actor._

import scala.util.{Failure, Random, Success, Try}
import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._

import java.io.File
import java.io.IOException
import java.util

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.collection.immutable.ListMap

import com.codemettle.reactivemq._
import com.codemettle.reactivemq.ReActiveMQMessages._
import com.codemettle.reactivemq.model._

import elastic.PatientsToElastic
import elastic.GetFromElastic

// The messages that this service can send and receive is
// is defined using this API structure

package API_OnGoingPatientsService {
  sealed trait API_OnGoingPatientsService

  // included here for simplicity
  case object StartThePLC extends API_OnGoingPatientsService


  object attributes {
    val service = "exampleService"
    val version = 1.0
    val api = "to be fixed by macros"
  }
}
import sp.onGoingPatients.{API_OnGoingPatientsService => api}


/**
  *  This is the actor (the service) that listens for messages on the bus
  *  It keeps track of a set of Pie diagrams that is updated every second
  */
class OnGoingPatientsService extends Actor with ActorLogging {
  // From Patient.scala
  implicit val formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all // for json serialization

  val patientsToElastic = new PatientsToElastic
  val getFromElastic = new GetFromElastic

  // connecting to the pubsub bus using the mediator actor
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe("felvis-data", self)
  mediator ! Subscribe("new-patient-topic", self)


  // The metod that receve messages. Add service logic in a trait so you can test it. Here the focus in on parsing
  // and on the messages on the bus
  def receive = {
    case mess @ _ if {log.debug(s"OnGoingPatientsService MESSAGE: $mess from $sender"); false} => Unit

    case "ping" =>
      println("---> Got patients:  ")
      println(getFromElastic.getAllPatients())
      //println(getFromElastic.getAllFinishedPatients())
      //println(getFromElastic.getPatientsOfTeam("NAKME"))

    case x: String =>
      patientsToElastic.messageReceived(x)
      mediator ! Publish("new-patient-topic", "ping") // Should not publish here. Publish based on what was updated.

  }


}

object OnGoingPatientsService {
  def props = Props(classOf[OnGoingPatientsService])
}

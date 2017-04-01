package sp.gPubSub

import akka.actor._

import scala.util.{Failure, Random, Success, Try}
import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._

import java.io.File
import java.io.IOException
import java.util

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.collection.immutable.ListMap

import sp.gPubSub.{API_PatientEvent => api}

/**
  *  This is the actor (the service) that listens for messages on the bus
  *  It keeps track of a set of Pie diagrams that is updated every second
  */
class GPubSubDevice extends Actor with ActorLogging {

  // connecting to the pubsub bus using the mediator actor
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)


  // The metod that receve messages. Add service logic in a trait so you can test it. Here the focus in on parsing
  // and on the messages on the bus
  def receive = {
    case mess @ _ if {log.debug(s"GPubSubService MESSAGE: $mess from $sender"); false} => Unit

    case "tick" =>
      val messList = getMessages
      if (!messList.isEmpty) {
        val header = SPHeader(from = "gPubSubService", to = "elvisDataHandlerService")
        val body = api.ElvisData(messList(0))
        val elvisDataSPMessage = GPubSubComm.makeMess(header, body)
        elvisDataSPMessage match {
          case Success(v) =>
            println(s"Publishing elvis message: $v")
            mediator ! Publish("elvis-data-topic", v)
          case Failure(e) =>
            println("Failed")
      }
    }
  }

  def getMessages() = {
    val inst = new GPubSubClient("Intelligentaakuten","intelligentaakuten-158811")
    inst.main() // returns list of received messages
  }

  // A "ticker" that sends a "tick" string to self every 2 second
  import scala.concurrent.duration._
  import context.dispatcher
  val ticker = context.system.scheduler.schedule(0 seconds, 0.1 seconds, self, "tick")

}

object GPubSubDevice {
  def props = Props(classOf[GPubSubDevice])
}

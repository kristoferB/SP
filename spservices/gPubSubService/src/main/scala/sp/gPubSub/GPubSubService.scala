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

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.services.pubsub.model.PublishResponse
import com.google.api.services.pubsub.model._
import com.google.api.services.pubsub.{Pubsub, PubsubScopes}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.collection.immutable.ListMap

trait PubSubEndpoints {
  self =>
  val projectName: String

  def getFQTopicName(topicName: String)(implicit projectName: String): String = s"projects/$projectName/topics/$topicName"
  def getFQSubscriptionName(subName: String)(implicit projectName: String) = s"projects/$projectName/subscriptions/$subName"
}

class GPubSub(applicationName: String, implicit val projectName: String) extends PubSubEndpoints with LazyLogging {

  val Transport = GoogleNetHttpTransport.newTrustedTransport()
  val JsonFactory = JacksonFactory.getDefaultInstance()


  //Ellen: changed the application default credentials file (for gcloud)
  var creds = GoogleCredential.getApplicationDefault()

  def main(): List[String] = {
    val sub = getSubscription("arto-topic", "arto-sub")
    var listOfMessages = new ListBuffer[String]()
    for (x <- sub.pullMessages()) {
      x.foreach { m =>
        val mDecoded = new String(m.getMessage.decodeData())
        listOfMessages += mDecoded
        sub.ackMessage(Seq(m.getAckId))
      }
    }
    listOfMessages.toList
  }


  class TopicSubscription(val underlying: Subscription)(implicit val ps: Pubsub, implicit val projectName: String)
      extends RichSubscription

  implicit lazy val pubsubClient: Pubsub = getPubsubClient()

  def getPubsubClient(): Pubsub = {
    if (creds.createScopedRequired()) {
      creds = creds.createScoped(PubsubScopes.all())
    }
    val requestFactory = Transport.createRequestFactory(creds)
    val initializer = requestFactory.getInitializer()
    new Pubsub.Builder(Transport, JsonFactory, initializer).setApplicationName(applicationName).build()
  }

  def getSubscription(topicName: String, subName: String): TopicSubscription = {
    new TopicSubscription(pubsubClient.projects().subscriptions().get(getFQSubscriptionName(subName)).execute())
  }


  trait RichSubscription extends PubSubEndpoints {
    val underlying: Subscription
    val SubscriptionName: String = underlying.getName

    implicit val ps: Pubsub

    def pullMessages(batchSize: Int = 5, block: Boolean = false): Option[util.List[ReceivedMessage]] = {
      val pullRequest = new PullRequest()
        .setReturnImmediately(!block)
        .setMaxMessages(batchSize)

      val pullResponse = ps.projects()
        .subscriptions()
        .pull(SubscriptionName, pullRequest).execute()

      val receivedMessages: Option[util.List[ReceivedMessage]] = Option(pullResponse.getReceivedMessages())
      receivedMessages
    }

    def ackMessage(acks: Seq[String]) : Unit = {
      val ackRequest = new AcknowledgeRequest().setAckIds(acks)
      ps.projects().subscriptions().acknowledge(SubscriptionName, ackRequest).execute()
    }
  }
}



// The messages that this service can send and receive is
// is defined using this API structure

package API_GPubSubService {
  sealed trait API_GPubSubService

  // included here for simplicity
  case object StartThePLC extends API_GPubSubService

  object attributes {
    val service = "exampleService"
    val version = 1.0
    val api = "to be fixed by macros"
  }
}
import sp.gPubSub.{API_GPubSubService => api}


/**
  *  This is the actor (the service) that listens for messages on the bus
  *  It keeps track of a set of Pie diagrams that is updated every second
  */
class GPubSubService extends Actor with ActorLogging {

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
        mediator ! Publish("elvis-topic", messList(0))
        //was: mediator ! Publish("elvis-topic", """{"new":{"timestamp":"2017-01-31T20:07:00Z","patient":{"CareContactId":4499370,"CareContactRegistrationTime":"2017-01-31T20:07:00Z","DepartmentComment":"","Events":[{"CareEventId":4499370,"Category":"Q","End":"2017-01-31T20:07:00Z","Start":"2017-01-31T20:07:00Z","Title":"Ej k\u00f6lapp","Type":"EJK\u00d6LAPP","Value":"EJK\u00d6LAPP","VisitId":4580377},{"CareEventId":4499370,"Category":"U","End":"2017-01-31T22:38:00Z","Start":"2017-01-31T22:25:00Z","Title":"R\u00f6/klin","Type":"R\u00d6NT/KLIN","Value":"R\u00f6ntgen","VisitId":4580377},{"CareEventId":4499370,"Category":"T","End":"2017-01-31T20:07:00Z","Start":"2017-01-31T20:07:00Z","Title":"Triage","Type":"TRIAGE","Value":"TRIAGE","VisitId":4580377},{"CareEventId":4499370,"Category":"P","End":"2017-02-01T01:50:00Z","Start":"2017-01-31T20:07:00Z","Title":"Gul","Type":"PRIO3","Value":"Gul","VisitId":4580377},{"CareEventId":4499370,"Category":"T","End":"2017-02-01T00:58:00Z","Start":"2017-02-01T00:58:00Z","Title":"Klar","Type":"KLAR","Value":"Klar","VisitId":4580377},{"CareEventId":4499370,"Category":"T","End":"2017-01-31T21:11:00Z","Start":"2017-01-31T21:11:00Z","Title":"L\u00e4kare","Type":"L\u00c4KARE","Value":"ISMRO1","VisitId":4580377},{"CareEventId":4499370,"Category":"T","End":"2017-02-01T08:59:00Z","Start":"2017-02-01T08:59:00Z","Title":"OmsKoord","Type":"OMSKOORD","Value":"OMSKOORD","VisitId":4580924}],"Location":"53","PatientId":4038,"ReasonForVisit":"B","Team":"NAK23T","VisitId":4580924,"VisitRegistrationTime":"2017-02-01T01:50:00Z"}}}""")
      }

  }

  def getMessages() = {
    val inst: GPubSub = new GPubSub("Intelligentaakuten","intelligentaakuten-158811")
    inst.main() // returns list of received messages
  }

  // A "ticker" that sends a "tick" string to self every 2 second
  import scala.concurrent.duration._
  import context.dispatcher
  val ticker = context.system.scheduler.schedule(0 seconds, 0.1 seconds, self, "tick")

}

object GPubSubService {
  def props = Props(classOf[GPubSubService])
}

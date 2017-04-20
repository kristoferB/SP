package sp.gPubSub

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

class GPubSubClient(applicationName: String, implicit val projectName: String) extends PubSubEndpoints with LazyLogging {

  val Transport = GoogleNetHttpTransport.newTrustedTransport()
  val JsonFactory = JacksonFactory.getDefaultInstance()


  //Ellen: changed the application default credentials file (for gcloud)
  var creds = GoogleCredential.getApplicationDefault()

  def main(): List[String] = {
    val sub = getSubscription("elvis-snap", "elvis-snap-sub")
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

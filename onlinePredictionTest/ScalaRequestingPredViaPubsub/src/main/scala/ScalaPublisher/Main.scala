import com.google.cloud.ServiceOptions
import com.google.pubsub.v1._
import com.google.cloud.pubsub.v1._
import com.google.protobuf.ByteString
import org.json4s.{ JObject, JValue }

import util.Try
import org.json4s.native.JsonMethods

object Main extends App {

  def jsonExampleForEdvardsTestV1 = {
    import org.json4s.JsonDSL._
    import org.json4s.native.JsonMethods.{ render, compact }
    val json =
      ("type" -> "request") ~
      ("tfrequest" ->
        ("instances" -> List( // this is the input json for the tensorflow model
          ("input_a" -> 17) ~
          ("input_b" -> -3)
        ))
      )
    compact(render(json))
  }

  val projectId = ServiceOptions.getDefaultProjectId
  val topicId = "prediction-test"
  val topicName = TopicName.create(projectId, topicId)
  val subscriptionId = "output-listener"
  val subscriptionName = SubscriptionName.create(projectId, subscriptionId)

  val messageReceiver = new MessageReceiver() {
    override def receiveMessage(message: PubsubMessage, consumer: AckReplyConsumer): Unit = {
      consumer.ack()
      val msgToStr = (msg: PubsubMessage) => Try(msg.getData().toString("utf-8"))
      val strToJson = (str: String) => Try(JsonMethods.parse(str))
      val jsonToTFOutput = (json: JValue) => Try(json \ "tfoutput")

      // this (wrapped) object is the json returned by the served tensorflow model
      val tfoutputTry = msgToStr(message) flatMap strToJson flatMap jsonToTFOutput

      val printJVal = (json: JValue) => println(JsonMethods.compact(JsonMethods.render(json)))
      tfoutputTry.foreach(printJVal)
    }
  }
  val subscription = Subscriber.defaultBuilder(subscriptionName, messageReceiver).build().startAsync()

  val client = TopicAdminClient.create()
  val topic = client.getTopic(topicName)
  val publisher = Publisher.defaultBuilder(topicName).build
  //val message = PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8("Hi from scala")).build
  val message = PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8(jsonExampleForEdvardsTestV1)).build
  println("published sampleJsonPredRequest to GCloud bus")
  publisher.publish(message)

  scala.io.StdIn.readLine("Press ENTER to exit.\n")
  subscription.stopAsync()
}

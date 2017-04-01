package sp.fakeElvis

import akka.actor._

import scala.util.{Failure, Random, Success, Try}
import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._

import sp.fakeElvis.{API_PatientEvent => api}

class FakeElvisDevice extends Actor {
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  // activate the extension
  val mediator = DistributedPubSub(context.system).mediator

  def receive = {
    case mess: String => {
<<<<<<< HEAD:spservices/fakeElvisService/src/main/scala/sp/fakeelvisservice/FakeElvisService.scala
      println(s"Publishing felvis message: $mess")
      mediator ! Publish("felvis-topic", mess)
=======
      val header = SPHeader(from = "fakeElvisService", to = "elvisDataHandlerService")
      val body = api.ElvisData(mess)
      val elvisDataSPMessage = FakeElvisComm.makeMess(header, body)
      elvisDataSPMessage match {
        case Success(v) =>
          println(s"Publishing felvis message: $v")
          mediator ! Publish("felvis-data-topic", v)
        case Failure(e) =>
          println("Failed")
      }
>>>>>>> 5227ad676106e412421dba0d379a39f7b4ef2dfc:spservices/fakeElvisService/src/main/scala/sp/fakeElvis/FakeElvisDevice.scala
    }
  }

  val statusResponse = SPAttributes(
    "service" -> api.attributes.service,
    "api" -> "to be added with macros later",
    "groups" -> List("examples"),
    "attributes" -> api.attributes
  )
  // Sends a status response when the actor is started so service handlers finds it
}

object FakeElvisDevice {
  def props = Props(classOf[FakeElvisDevice])
  implicit val system = ActorSystem("SP")
  val felvisPublisher = system.actorOf(Props[FakeElvisDevice], "felvis-publisher")

  val felvisdataPath = getClass.getResource("/output170201-0217.txt") //("/test.txt") //

  def readFromFile : collection.Iterator[String] = {
    val source = scala.io.Source.fromFile(felvisdataPath.getPath, "utf-8")
    val lines = try source.getLines mkString "\n" finally source.close() // getLines returns an Iterator[String] which mkString turns into a String
    //println("lines loaded")
    lines.split(",\n").iterator // splits read results to an Array which is made into a collection.Iterator[String]
  }

  while (true) {
    var li = readFromFile // the file is only read once. Probably compiler optimising.
    while (li.hasNext) {
      Thread.sleep(1000)
      felvisPublisher ! li.next()
    }
  }
}

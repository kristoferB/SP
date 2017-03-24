package sp.fakeelvisservice

import akka.actor._

import scala.util.{Failure, Random, Success, Try}
import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._

package API_FakeElvisService {
  sealed trait API_FakeElvisService
  object attributes {
    val service = "fakeElvisService"
    val version = 1.0
    val api = "to be fixed by macros"
  }
}
import sp.fakeelvisservice.{API_FakeElvisService => api}

class FakeElvisService extends Actor {
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  // activate the extension
  val mediator = DistributedPubSub(context.system).mediator

  def receive = {
    case mess: String => {
      println(s"Publishing felvis message: $mess")
      mediator ! Publish("felvis-data", mess)
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

object FakeElvisService {
  def props = Props(classOf[FakeElvisService])
  implicit val system = ActorSystem("SP")
  val felvisPublisher = system.actorOf(Props[FakeElvisService], "felvis-publisher")

  val felvisdataPath = getClass.getResource("/output170201-0217.txt") // ("/test.txt")

  def readFromFile : collection.Iterator[String] = {
    val source = scala.io.Source.fromFile(felvisdataPath.getPath, "utf-8")
    val lines = try source.getLines mkString "\n" finally source.close() // getLines returns an Iterator[String] which mkString turns into a String
    //println("lines loaded")
    lines.split(",\n").iterator // splits read results to an Array which is made into a collection.Iterator[String]
  }

  while (true) {
    var li = readFromFile
    while (li.hasNext) {
      Thread.sleep(2000)
      felvisPublisher ! li.next()
    }
  }
}

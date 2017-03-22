package sp.d3exampleservice

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import scala.util.{ Try, Success }
import scala.util.Random.nextInt

sealed trait API_D3ExampleService
object API_D3ExampleService {
  case class Start() extends API_D3ExampleService
  case class Stop() extends API_D3ExampleService
  case class D3Data(barHeights: List[Int]) extends API_D3ExampleService
}

class D3ExampleService extends Actor with ActorLogging {

  println("INSIDE D3 SERVICE")

  // conneting to the pubsub bus using the mediator actor
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe(D3ExampleService.service, self)

  // A "ticker" that sends a "tick" string to self every 2 second
  import scala.concurrent.duration._
  import context.dispatcher

  var serviceOn: Boolean = false
  context.system.scheduler.schedule(0.5 seconds, 0.5 seconds, self, "tick")

  def parseCommand(x: String): Try[API_D3ExampleService] =
    SPMessage fromJson(x) flatMap (_.getBodyAs[API_D3ExampleService])

  def handleCommand: API_D3ExampleService => Unit = {
    case API_D3ExampleService.Start() => serviceOn = true
    case API_D3ExampleService.Stop() => serviceOn = false
  }

  def dataMsg() = SPMessage.make(
    SPAttributes("from" -> D3ExampleService.service).addTimeStamp,
    API_D3ExampleService.D3Data(List.fill(7)(nextInt(50)))
  ).get.toJson

  def receive = {
    case "tick" => if(serviceOn) mediator ! Publish("d3ExampleAnswers", dataMsg())
    case x: String => parseCommand(x) foreach handleCommand
    case z => println(s"D3Exampleservice didn't recognize $z")
  }
}

object D3ExampleService {
  def props = Props(classOf[D3ExampleService])
  val service = "d3ExampleService"
}

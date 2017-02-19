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
  case class D3Data(barHeights: List[Int]) extends API_D3ExampleService
}

class D3ExampleService extends Actor with ActorLogging {

  // conneting to the pubsub bus using the mediator actor
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe(D3ExampleService.service, self)

  // A "ticker" that sends a "tick" string to self every 2 second
  import scala.concurrent.duration._
  import context.dispatcher

  def receive = {
    case "tick" =>
      val barHeights = List.fill(7)(nextInt(50))
      val spm = SPMessage.make(
        SPAttributes("from" -> D3ExampleService.service).addTimeStamp,
        API_D3ExampleService.D3Data(barHeights)
      ).map(_.toJson)
      spm foreach (mediator ! Publish("d3ExampleAnswers", _))
    case x: String =>
      SPMessage.fromJson(x) match {
        case Success(spm: SPMessage) =>
          if(Try(spm.getBodyAs[API_D3ExampleService.Start]).isSuccess)
            context.system.scheduler.schedule(0.5 seconds, 0.5 seconds, self, "tick")
        case x => println(s"D3Exampleservice didn't recognize $x")

      }
    case z =>
      println(s"D3Exampleservice didn't recognize $z")
  }

}

object D3ExampleService {
  def props = Props(classOf[D3ExampleService])
  val service = "d3ExampleService"
}

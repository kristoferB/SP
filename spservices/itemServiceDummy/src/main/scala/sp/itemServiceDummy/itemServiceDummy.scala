package sp.itemServiceDummy

import sp.domain._
import sp.domain.Logic._
//import sp.messages._
import sp.messages.Pickles._

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import scala.util.{ Try, Success }
import scala.util.Random.nextInt

sealed trait API_ItemServiceDummy
object API_ItemServiceDummy {
  case class Hello() extends API_ItemServiceDummy
  case class RequestSampleItem() extends API_ItemServiceDummy
  case class SampleItem(operationSPV: SPValue = SampleItemAsSPV()) extends API_ItemServiceDummy
  case class Item(item: SPValue) extends API_ItemServiceDummy
}

object SampleItemAsSPV {
  def apply() = SPValue[Operation](
    Operation("sample operation")
  )
}

//
// This is a dummy service to see to that itemeditor can retrieve an item from the backend
//
// What is done here might be moved into a modelservice or something later
//
//
class ItemServiceDummy extends Actor {

  // conneting to the pubsub bus using the mediator actor
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe(ItemServiceDummy.service, self)

  def parseCommand(x: String): Try[API_ItemServiceDummy] =
    SPMessage fromJson(x) flatMap (_.getBodyAs[API_ItemServiceDummy])

  def helloAns() = SPMessage.makeJson(
    SPAttributes("from" -> "itemEditorService").addTimeStamp,
    API_ItemServiceDummy.Hello()
  )

  def sampleItemAns() = SPMessage.makeJson(
    SPAttributes("from" -> "itemEditorService").addTimeStamp,
    API_ItemServiceDummy.SampleItem()
  )

  def handleCommand: API_ItemServiceDummy => Unit = {
    case API_ItemServiceDummy.Hello() =>
      mediator ! Publish("itemEditorAnswers", helloAns())
    case API_ItemServiceDummy.RequestSampleItem() =>
      mediator ! Publish("itemEditorAnswers", sampleItemAns())
    case API_ItemServiceDummy.Item(item) =>
      println("ItemServiceDummy: received " + item)
  }

  def receive = {
    case x: String => parseCommand(x) foreach handleCommand
    case z => println(s"itemEditorService didn't recognize $z")
  }
}

object ItemServiceDummy {
  def props = Props(classOf[ItemServiceDummy])
  val service = "itemEditorService"
}

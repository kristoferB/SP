package sp.itemEditorService

import sp.domain._
import sp.domain.Logic._
//import sp.messages._
import sp.messages.Pickles._

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import scala.util.{ Try, Success }
import scala.util.Random.nextInt

sealed trait API_ItemEditorService
object API_ItemEditorService {
  case class Hello() extends API_ItemEditorService
  case class RequestSampleItem() extends API_ItemEditorService
  case class SampleItem(operationSPV: SPValue = SampleItemAsSPV()) extends API_ItemEditorService
  case class Item(item: SPValue) extends API_ItemEditorService
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
class ItemEditorService extends Actor {

  // conneting to the pubsub bus using the mediator actor
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe(ItemEditorService.service, self)

  def parseCommand(x: String): Try[API_ItemEditorService] =
    SPMessage fromJson(x) flatMap (_.getBodyAs[API_ItemEditorService])

  def helloAns() = SPMessage.makeJson(
    SPAttributes("from" -> "itemEditorService").addTimeStamp,
    API_ItemEditorService.Hello()
  )

  def sampleItemAns() = SPMessage.makeJson(
    SPAttributes("from" -> "itemEditorService").addTimeStamp,
    API_ItemEditorService.SampleItem()
  )

  def handleCommand: API_ItemEditorService => Unit = {
    case API_ItemEditorService.Hello() =>
      mediator ! Publish("itemEditorAnswers", helloAns())
    case API_ItemEditorService.RequestSampleItem() =>
      mediator ! Publish("itemEditorAnswers", sampleItemAns())
    case API_ItemEditorService.Item(item) =>
      println("ItemEditorService: received " + item)
  }

  def receive = {
    case x: String => parseCommand(x) foreach handleCommand
    case z => println(s"itemEditorService didn't recognize $z")
  }
}

object ItemEditorService {
  def props = Props(classOf[ItemEditorService])
  val service = "itemEditorService"
}

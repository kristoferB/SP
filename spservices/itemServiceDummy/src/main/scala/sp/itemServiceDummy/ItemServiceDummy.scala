package sp.itemServiceDummy

import sp.domain._
import sp.domain.Logic._

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import scala.util.{ Try, Success }
import scala.util.Random.nextInt


object GetSampleItem {
  def apply() = Operation("sample operation hejje")
}

//
// This is a dummy service for proof of concept that itemeditor and itemexplorer can work with the same items
// and talk via backend
//
// What is done here should be handled by modelService probably, sometime!
//
//
class ItemServiceDummy extends Actor {

  // connecting to the pubsub bus using the mediator actor
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)

  val sampleItems = SampleSPItems()

  def parseCommand(x: String): Option[API_ItemServiceDummy] =
    API_ItemServiceDummy.extract(SPMessage.fromJson(x)).map(_._2)

  def publishToEditor(cmd: API_ItemServiceDummy): Unit = {
    val sph = SPHeader(from = "itemEditorService", to = "itemEditorWidget")
    val mess = API_ItemServiceDummy.makeMess(sph, cmd)
    mediator ! Publish("itemEditorAnswers", mess)
  }

  def publishToExplorer(cmd: API_ItemServiceDummy): Unit = {
    val sph = SPHeader(from = "itemEditorService", to = "itemExplorerWidget")
    val mess = API_ItemServiceDummy.makeMess(sph, cmd)
    mediator ! Publish("itemExplorerAnswers", mess)
  }

  def handleCommand: API_ItemServiceDummy => Unit = {
    case API_ItemServiceDummy.Hello() =>
      publishToEditor(API_ItemServiceDummy.Hello())
    case API_ItemServiceDummy.RequestItem(id) =>
      sampleItems.find(_.id == id).foreach(idAble => publishToEditor(API_ItemServiceDummy.Item(idAble)))
    case API_ItemServiceDummy.RequestSampleItem() =>
      publishToEditor(API_ItemServiceDummy.SampleItem())
    case API_ItemServiceDummy.RequestSampleItems() =>
      publishToExplorer(API_ItemServiceDummy.SampleItemList(sampleItems))
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

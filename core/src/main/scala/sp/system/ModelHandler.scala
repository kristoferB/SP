package sp.system

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import sp.domain.SPAttributes
import scala.concurrent.Future
import akka.pattern.pipe
import scala.concurrent.duration._
import sp.system.messages._
import akka.persistence._

import sp.domain._

class ModelHandler extends PersistentActor {
  override def persistenceId = "modelhandler"
  implicit val timeout = Timeout(1 seconds)
  import context.dispatcher

  private var modelMap: Map[ID, ActorRef] = Map()
  private var viewMap: Map[String, ActorRef] = Map()

  def receiveCommand = {
    case cm @ CreateModel(id, name, attr)=> {
      val reply = sender
      if (!modelMap.contains(id)){
        persist(cm){n =>
          addModel(n)
          modelMap(id).tell(GetModels, reply)
        }
      } else modelMap(id).tell(GetModels, reply)
    }

    case m: ModelMessage => {
      if (modelMap.contains(m.model)) modelMap(m.model) forward m
      else sender ! SPError(s"Model ${m.model} does not exist.")
    }

    case (m: ModelMessage, v: Long) => {
      val viewName = viewNameMaker(m.model, v)
      if (!viewMap.contains(viewName)) {
        println(s"The modelService creates a new view for ${m.model} version: ${v}")
        val view = context.actorOf(sp.models.ModelView.props(m.model, v, viewName))
        viewMap += viewName -> view
      }
      viewMap(viewName).tell(m, sender)
    }


    case GetModels =>
      val reply = sender
      if (!modelMap.isEmpty){
        val fList = Future.traverse(modelMap.values)(x => (x ? GetModels).mapTo[ModelInfo]) map(_ toList)
        fList map ModelInfos pipeTo reply
      } else reply ! ModelInfos(List[ModelInfo]())
  }

  def addModel(cm: CreateModel) = {
    println(s"The modelService creates a new model called ${cm.name} id: ${cm.model}")
    val newModelH = context.actorOf(sp.models.ModelActor.props(cm.model))
    newModelH ! ModelInfo(cm.model, cm.name, 0, cm.attributes)
    modelMap += cm.model -> newModelH
  }

  def viewNameMaker(id: ID, v: Long) = id.toString() + " - Version: " + v

  def receiveRecover = {
    case cm: CreateModel  => addModel(cm)
  }

}

object ModelHandler {
  def props = Props(classOf[ModelHandler])
}
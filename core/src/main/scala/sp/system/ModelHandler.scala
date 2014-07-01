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


class ModelHandler extends EventsourcedProcessor {
  private var modelMap: Map[String, ActorRef] = Map()
  implicit val timeout = Timeout(1 seconds)
  import context.dispatcher
  
  def receiveCommand = {
    case cm @ CreateModel(name, attr)=> {
      val reply = sender
      if (!modelMap.contains(name)){
        persist(cm){n =>
          addModel(n)
          modelMap(name).tell(GetModels, reply)
        }
      } else modelMap(name).tell(GetModels, reply)
    }

    case m: ModelMessage => {
      if (modelMap.contains(m.model)) modelMap(m.model) forward m
      else sender ! SPError(s"Model ${m.model} does not exist.")
    }

    case GetModels =>
      val reply = sender
      if (!modelMap.isEmpty){
        val fList = Future.traverse(modelMap.values)(x => (x ? GetModels).mapTo[ModelInfo]) map(_ toList)
        fList map ModelInfos pipeTo reply
      } else reply ! ModelInfos(List[ModelInfo]())
  }

  def addModel(cm: CreateModel) = {
    println(s"The modelService creates a new model called $cm.name")
    val newModelH = context.actorOf(sp.models.ModelActor.props(cm.model, cm.attributes), cm.model)
    modelMap += cm.model -> newModelH
  }

  def receiveRecover = {
    case cm: CreateModel  => addModel(cm)
  }

}

object ModelHandler {
  def props = Props(classOf[ModelHandler])
}
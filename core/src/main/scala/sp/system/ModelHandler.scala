package sp.system

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
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
    case CreateModel(name)=> {
      val reply = sender
      if (!modelMap.contains(name)){
        persist(name){n =>
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

  def addModel(name: String) = {
    println(s"The modelService creates a new model called $name")
    val newModelH = context.actorOf(sp.models.ModelActor.props(name), name)
    modelMap += name -> newModelH
  }

  def receiveRecover = {
    case name: String  => addModel(name)
    case SnapshotOffer(_, snapshot: Any) => snapshot
  }

}

object ModelHandler {
  def props = Props(classOf[ModelHandler])
}
package sp.system

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import akka.event.Logging
import sp.system.messages._
import scala.concurrent.Future
import akka.pattern.pipe
import scala.concurrent.duration._

//TODO: make persistent soon

class RunTimeHandler extends Actor {
  var kindMap: Map[String, RegisterRuntimeKind] = Map()
  private var runMap: Map[String, ActorRef] = Map()
  implicit val timeout = Timeout(5 seconds)
  import context.dispatcher
  val log = Logging(context.system, this)

  def receive = {
    case kind @ RegisterRuntimeKind(name, _, _) => kindMap += name -> kind; sender ! kindMap(name)
    case GetRuntimeKinds => {
      val xs = kindMap map{case (name, kind) => RuntimeKindInfo(name, kind.attributes)}
      sender ! RuntimeKindInfos(xs toList)
    }
    case cr @ CreateRuntime(kind, name, _, _) => {
      val reply = sender
      if (kindMap.contains(kind)){
        if (!runMap.contains(name)){
          val a = context.actorOf(kindMap(kind).props, name)
          runMap += name -> a
          a.tell(cr, reply)
        } else reply ! SPError(s"runtime $name already exists")

      } else {
        reply ! SPError(s"$kind is not available as a runtime kind")
      }
    }
    case GetRuntimes => {
      val reply = sender
      if (!runMap.isEmpty) {
        val fList = Future.traverse(runMap.values)(x => (x ? GetRuntimes).mapTo[CreateRuntime]) map (_ toList)
        fList map RuntimeInfos pipeTo reply
      } else reply ! RuntimeInfos(List[CreateRuntime]())
    }
    case m: RuntimeMessage => {
      if (runMap.contains(m.runtime)) runMap(m.runtime) forward m
      else sender ! SPError(s"Runtime ${m.runtime} does not exist.")
    }

//    case CreateRuntime(name, rtype, model, tempid) => {
//      val id = if (tempid == ID.empty) ID.newID else tempid
//      if (!runMap.contains(id)) {
//        log.info("Runtime service createds a new runtime named " + name + " of type " + rtype)
//        // add lookup here for various runtime types
//        rtype match {
//          case "LISAES" => {
//            val actor = context.actorOf(se.sekvensa.sp.runtime.elasticsearch.ElasticSearchRuntimev2.props(id, name))
//            val send = sender
//            runMap = runMap + (id -> actor)
//            (actor ? Created(id)) pipeTo send
//          }
//          case "OPSIM" => {
//            val actor = context.actorOf(se.sekvensa.sp.runtime.supremica.SupremicaRuntime.props(id, name))
//            val send = sender
//            runMap = runMap + (id -> actor)
//            (actor ? Created(id)) pipeTo send
//          }
//          case s:String => sender ! ServiceErrorString("Runtimetype with name " + s + ", is not registered")
//        }
//      } else {
//        sender ! ServiceErrorString("Runtime with id " + id + ", already created")
//      }
//
//    }
//    case GetRuntimes => {
//      val replyto = sender
//      if (runMap.isEmpty)
//        replyto ! List[RuntimeInfo]()
//      else {
//        val listf = Future.sequence(runMap.values map (_ ? GetRuntimes))
//        listf map println _
//        listf map (replyto ! _.toList)
//      }
//    }
//
//    case m @ RuntimeMessage(runtimeID, mess, replyTo) => {
//      println("RuntimeMessage " + m)
//      if (runMap.contains(runtimeID)){
//        runMap(runtimeID) ! {if (replyTo == null) m.copy(replyTo=sender) else m}
//      } else sender ! ServiceErrorMissingID(runtimeID.toString, "That runtime is not availible")
//    }
//
//    //    case CreateNewRunTime(r) =>
//    //      if (!runMap.contains(m.id.toString())) {
//    //        println("The modelService creates a new model called " + m)
//    //        val newModelH = context.actorOf(Props(new ModelHandler(m)))
//    //        modelMap = modelMap + (m.id.toString() -> newModelH)
//    //        sender ! ModelUpdated
//    //    } else sender ! ServiceErrorString("Model with that id already added. Use update messages instead")

    case _ => println("not impl yet")
  }

}

object RunTimeHandler {
  def props = Props(classOf[ModelHandler])
}

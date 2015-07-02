package sp.system

import akka.actor._
import sp.domain.logic.IDAbleLogic
import scala.concurrent.duration._
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._

/**
 * launch new worker for every request if the service is long running.
 * If it is fast, you can skip that
 */
class ServiceExample extends Actor {
  def receive = {
    case r: RegisterService => println("Service is registered")
    case r: RemoveService => println("Service is removed")
    case Request(_, attr, ids) => {
      val setup = attr.getAs[ExampleSetup]("setup")
      val id = attr.getAs[ID]("findID")
      for (s <- setup; x <- id) yield {
        context.actorOf(Props(classOf[runner], ids, s, x, sender())) ! "go"
      }
      if (setup.isEmpty && id.isEmpty) sender() ! SPError("couldn't understand the attributes: $attr")
      println(s"in serviceExample: $setup, $id, $attr")
    }
  }
}

private case class ExampleSetup(onlyOperations: Option[Boolean], searchMethod: Option[String])
private case class ExampleStatus(checked: Int)

private class runner(ids: List[IDAble], s: ExampleSetup, id: ID, replyTo: ActorRef) extends Actor {
  import context.dispatcher
  var status = ExampleStatus(0)

  // Should be around 0.5 sec - 2 sec. This is just for testing
  def startProgress = {context.system.scheduler.schedule(
    1 milliseconds, 1 milliseconds){
      replyTo ! Progress(SPAttributes("status"-> status))
    }
  }

  def receive = {
    case "go" if s.searchMethod == Some("theBad") => {
      println("HEJ")
      val progress = startProgress
      val filter = ids.filter { x =>
        status = ExampleStatus(status.checked + 1)
        if (s.onlyOperations.get && !x.isInstanceOf[Operation] || x.id == id) false
        else {
          val jsonID = SPValue(id)
          SPValue(x).find(_ == jsonID).isDefined
        }
      }

      progress.cancel()
      replyTo ! Response(filter, SPAttributes("setup"->s))
    }
    case "go" => {
      val progress = startProgress
      val f2 = IDAbleLogic.removeID(Set(id), ids).map(_.id)
      val filter = ids.filter { x =>
        status = ExampleStatus(status.checked + 1)
        f2.contains(x.id)
      }
      progress.cancel()
      replyTo ! Response(filter, SPAttributes("setup"->s))
    }
  }
}


object ServiceExample {
  val specification = SPAttributes(
    "setup" -> Map(
      "onlyOperations" -> KeyDefinition("Boolean", List(), Some(false)),
      "searchMethod" -> KeyDefinition("String", List("theGood", "theBad"), Some("theGood"))
    ),
    "findID" -> KeyDefinition("ID", List(), None)
  )

  def props = Props(classOf[ServiceExample])
}

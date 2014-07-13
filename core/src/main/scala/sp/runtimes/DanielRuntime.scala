package sp.runtimes

import akka.actor._
import sp.domain._
import sp.system.messages._

/**
 * Exempel på en runtime eller service.
 *
 * Skicka en SimpleMessage med attribute till den som ser ut:
 * "name" -> "Kristofer",
 * "operation" -> {"id" -> ID(UUID)}
 */
class DanielRuntime(about: CreateRuntime) extends Actor {
  var noOfReq = 0  // state som var


  def receive = {
    case SimpleMessage(_, attr) => {
      extractThings(attr) match {
        case Some((name, id)) => {
          // nedan så används implicit för konverting till SPAttributeValues
          sender ! SPAttributes(Map("message"-> s"hej $name", "id"-> id, "req"-> noOfReq))
          noOfReq += 1
        }
        case _ => sender ! SPError("Fel struktur")
      }
    }
    case cr @ CreateRuntime(_, m, n, attr) => {
      // load things from the model here.
      // If needed return cr after load is complete
      println(cr)
      sender ! cr
    }
    case GetRuntimes => {
      sender ! about
    }
  }


  def extractThings(attr: SPAttributes) = {
    for {
      name <- attr.getAsString("name") // hämta på översta nivån
      nested <- attr.getAttribute(List("operation", "id")) // hämtar i nestade object
      opID <- nested.asID // konverterar till ID
    } yield (name, opID)   // returnerar en tuple
  }
}

object DanielRuntime {
  def props(cr: CreateRuntime) = Props(classOf[DanielRuntime], cr)
}

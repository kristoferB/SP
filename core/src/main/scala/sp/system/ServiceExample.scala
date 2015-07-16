package sp.system

import akka.actor._
import sp.domain.logic.IDAbleLogic
import scala.concurrent._
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._



object ServiceExample {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "aMenuGroup" // to organize in gui. use "hide" to hide service in gui
    ),
    "setup" -> Map(
      "onlyOperations" -> KeyDefinition("Boolean", List(), Some(false)),
      "searchMethod" -> KeyDefinition("String", List("theGood", "theBad"), Some("theGood"))
    ),
    "findID" -> KeyDefinition("ID", List(), None)
  )

  // important to incl ServiceLauncher if you want unique actors per request
  def props = ServiceLauncher.props(Props(classOf[ServiceExample]))
}


case class ExampleSetup(onlyOperations: Option[Boolean], searchMethod: Option[String])

// Add constructor parameters if you need access to modelHandler and ServiceHandler etc
class ServiceExample extends ServiceRunner {
  import context.dispatcher
  type ServiceInput = (ExampleSetup, ID)

  def extractServiceInput(attr: SPAttributes): Option[ServiceInput] = {
    val setup = attr.getAs[ExampleSetup]("setup")
    val id = attr.getAs[ID]("findID")
    for (s <- setup; x <- id) yield {(s, x)}
  }

  def request(attr: ServiceInput, ids: List[IDAble]): Response = {
    val s = attr._1
    val id = attr._2
    s.searchMethod match {
      case Some("theBad") => {
        println("HEJ")
        var iterations = 0
        val filter = ids.filter { x =>
          updateProgress(SPAttributes("iterations" -> iterations))
          iterations += 1
          if (s.onlyOperations.get && !x.isInstanceOf[Operation] || x.id == id) false
          else {
            val jsonID = SPValue(id)
            SPValue(x).find(_ == jsonID).isDefined
          }
        }
        Response(filter, SPAttributes("setup" -> s))
      }
      case Some("theGood") => {
        println("HEJ den bra")
        var iterations = 0
        val f2 = IDAbleLogic.removeID(Set(id), ids).map(_.id)
        val filter = ids.filter { x =>
          updateProgress(SPAttributes("iterations" -> iterations))
          iterations += 1
          f2.contains(x.id)
        }
        Response(filter, SPAttributes("setup" -> s))
      }
    }
  }

}




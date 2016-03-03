package sp.operatorService

import akka.actor._
import sp.domain.Logic._
import sp.domain._
import sp.system._
import sp.system.messages._
import scala.collection.mutable.Queue
object operatorService extends SPService {
  //var q = new Queue[List[String]]
  val empty = "#ffffff"
  var q = List(empty, empty, empty, empty, empty, empty, empty, empty, "Hejhej")

  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "hide" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "getNext" -> KeyDefinition("Boolean", List(), None),
    "buildOrder" -> KeyDefinition("List[String]", List(), None)
  )

  val transformTuple =(
    TransformValue("getNext", _.getAs[Boolean]("getNext")),
    TransformValue("buildOrder", _.getAs[List[String]]("buildOrder"))
    )

  val transformation = transformToList(transformTuple.productIterator.toList)

  def props = ServiceLauncher.props(Props(classOf[operatorService]))

}
  class operatorService extends Actor with ServiceSupport {

    def receive = {
      case r@Request(service, attr, ids, reqID) => {
        val replyTo = sender()
        implicit val rnr = RequestNReply(r, replyTo)
        val getNext: Boolean = transform(operatorService.transformTuple._1)
        val buildOrder: List[String] = transform(operatorService.transformTuple._2)

        //var res = List("green","green","green","green","green","green","green","green")
        //var hejsan = operatorService.parseColour(res)

        /*
        Här saknas anrop till en funktion som tar in getNext och buildOrder,
        Funktion skall anropa schemaläggaren och lägga till listorna i någonslags kö.
        Funktion skall retunera en lista av 8 färger i hexkod, och felmeddelande i slutet.
        Problem:
        Scala verkar inte gilla att ta in listor i en kö, ev lösning: skicka in object som innehåller listor
        Lägga på meddelandet i slutet av listan.
         */

        var res = operatorService.q
        replyTo ! Response(List(), SPAttributes("result" -> res), rnr.req.service, rnr.req.reqID)
        self ! PoisonPill
      }
    }
  }
/*

  /*
  * f ("text to append": String, "filename": String)
  * A function that puts a string in a new file or appends the string with the file
  * if it already exists, statically coded to the path where the operatorInstGUI.js
  * files are as for now
  * works even if root is changed.
  * TODO: change the path to elsewhere..
  */
  def writeToFile(textToFile: String, filenameOrPath: String): Unit = {
  scala.tools.nsc.io.Path(System.getProperty("user.dir") + "/gui/web/src/app/operatorInstGUI/" + filenameOrPath).createFile().appendAll(textToFile)
  }

  /*
  * takes something and returns  but will do something more when dine. WIP
  */

  def operMessage(ls: String): String = {
    // Some sideeffect
    return "#ffffff"
  }

  /*
  * Takes a list of Strings, and parses it to another list of strings
  * Takes what the human name for a colour is and returns the hexcode
  *
  */
  val yellow = "#ffff66"; val red = "#ff3333"; val green = "#5cd65c"
  val blue = "#0066ff"; val empty = "#ffffff"

  def parseColour(ls: List[String]): List[String] = {
    ls.map(x => x match {
      case "yellow" => yellow
      case "red"    => red
      case "green"  => green
      case "blue"   => blue
      case "empty"  => empty
      case "0"      => empty
      case "1"      => blue
      case "2"      => green
      case "3"      => red
      case "4"      => yellow
      case _        => operMessage(x)

      }
    )
  }
}
 */
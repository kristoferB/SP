package sp.operatorService

import akka.actor._
import sp.domain.Logic._
import sp.domain._
import sp.system._
import sp.system.messages._
object operatorService extends SPService {

  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "hide" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "done" -> KeyDefinition("Boolean", List(), Some(false))
  )

  val transformTuple = (
    TransformValue("done", _.getAs[Boolean]("done"))
    )
  val transformation = transformToList(transformTuple.productIterator.toList)

  def props = ServiceLauncher.props(Props(classOf[operatorService]))

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
      case _        => operMessage(x)
      })



    /*ls.map(x => if ("yellow") yellow 
           else if ("red")    red
           else if ("green")  green
           else if ("blue")   blue
           else if ("empty")  empty
           else               operMessage(x))*/
  }
}


  class operatorService extends Actor with ServiceSupport {

    def receive = {
      case r@Request(service, attr, ids, reqID) => {
        val replyTo = sender()
        implicit val rnr = RequestNReply(r, replyTo)
        System.out.println("Hej")
        val done: Boolean = transform(operatorService.transformTuple)

        System.out.println(sender())
        System.out.println("Hej")
        var res = List("green","green","green","green","green","green","green","green")
        var hejsan = operatorService.parseColour(res)
        replyTo ! Response(List(), SPAttributes("result" -> hejsan), rnr.req.service, rnr.req.reqID)
        self ! PoisonPill
      }
    }
  }



/*
Js kallar på scala funktionen, scala funktionen skickar tillbaka en array.
Skicka denna arrayen till javascript [röd,grön]
scala -> uppdaterar en array i javascript operatorInstGUI
 */
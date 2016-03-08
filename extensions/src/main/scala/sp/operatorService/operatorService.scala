package sp.operatorService

import akka.actor._
import sp.domain.Logic._
import sp.domain._
import sp.system._
import sp.system.messages._
import scala.collection.mutable.MutableList

object operatorService extends SPService {

  var q = MutableList[List[String]]();
  //var k = ;

  def enqueue(ls: List[String]): Unit = {
    q += ls
  }
  def dequeue() = {
    if(!q.isEmpty){
      val temp = q.head
      q = q.tail
      temp
    } else {
     throw new NoSuchElementException
    }
  }

  def dummyQueueFunction(ls : List[String]): Unit = {
    val temp = ls.splitAt(8)
    enqueue(temp._1)
    enqueue(temp._2)
  }
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
      case "1"      => yellow
      case "2"      => green
      case "3"      => red
      case "4"      => blue
      case _        => x

    }
    )
  }

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
        /*
        Funkar inte riktigt än behöver logiken vart meddelandet kommer ifrån
        Och då agera på rätt sätt och skicka svars meddelande.
        Kommer meddelandet ifrån operatorInstGUI  -> Skicka tillbaka head på kön och ejtom
                                                  -> Om kön är tom skicka tillbaka lista med empty och tom
        Kommer meddelandet ifrån Tobbe  -> Om q.isEmpty == true, lägg in i kön och skicka tillbaka head.
                                        -> Annars lägg in i kön och skicka tillbaka tomt

         */
        var message = List[String]()
        // En ide här är att skriva om allt som en matcher då de funakr dåligt att skriva över variabler.
        if(getNext){
          if(operatorService.q.nonEmpty) {
            System.out.println("Hej1")
            message = (operatorService.dequeue() ::: List("ejtom"))

          } else {
            System.out.println("Hej2")
            message = List("empty","empty","empty","empty","empty","empty","empty","empty","tom")
          }
        } else {
          if(operatorService.q.isEmpty){
            System.out.println("Hej3")
            operatorService.dummyQueueFunction(transform(operatorService.transformTuple._2))
            //Lägger in i kön, funkar även att köa elementet har 8 i längd
            message = (operatorService.dequeue() ::: (List("TillTobbe")))

          } else {
            System.out.println("Hej4")
            operatorService.dummyQueueFunction(transform(operatorService.transformTuple._2))
            //k = [["Hej"],["Hej2"], ["Hej3"]];
            //System.out.println(k)
            message = List("empty","empty","empty","empty","empty","empty","empty","empty","TillTobbe")
          }
        }
        System.out.println("ElementsInQueue " + operatorService.q.length )
        val res= operatorService.parseColour(message)
        System.out.println(res(2))
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

}
 */
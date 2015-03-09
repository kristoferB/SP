package sp.areus

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import sp.domain._
import sp.system.messages._

import scala.concurrent.duration._


/**
 * Created by Kristofer on 2014-06-27.
 */
class ImportKUKAFileService(modelHandler: ActorRef) extends Actor {
  implicit val timeout = Timeout(1 seconds)
  import context.dispatcher



  def receive = {
    case Request(_, attr) => {
      val reply = sender
      extract(attr) match {
        case Some((file, name)) => {

          println(s"I got the file in importKUKA: $file")





        }
        case None => sender ! errorMessage(attr)
      }
    }
  }

  def extract(attr: SPAttributes) = {
    for {
      file <- attr.getAsString("file")
    } yield (file, attr.get("name"))
  }

  def errorMessage(attr: SPAttributes) = {
    SPError("The request is missing parameters: \n" +
      s"file: ${attr.getAsString("file")}" + "\n" +
      s"Request: ${attr}" )
  }


  import scala.xml._

  def toAttr(n : Node): SPAttributeValue = {
    val attr = n.attributes.asAttrMap.map{case (k, v) => k -> StringPrimitive(v)}
    val value: SPAttributeValue = {
      if (n.child.count(_.isInstanceOf[Text]) == 1) {
        val value = StringPrimitive(n.text)
        if (attr.isEmpty) value
        else MapPrimitive(attr + ("value"->value))
      }
      else {
        val children = n.child //.filter(n => n.isInstanceOf[Text] || n.isInstanceOf[Elem])
        val fold = children.foldLeft(Map[String,List[SPAttributeValue]]()){
            case (aggr, e: Elem) => {
              val newAttr = toAttr(e)
              val prev = aggr.getOrElse(e.label, List())
              val xs = if (newAttr != MapPrimitive(Map())) newAttr :: prev else prev
              aggr + (e.label -> xs)
            }
            case (aggr, t: Text) => aggr
          }

        val map = fold collect {
          case (k, x :: Nil)  =>  k -> x
          case (k, x :: xs) => k -> ListPrimitive(x :: xs)
        }
        MapPrimitive(map ++ attr)
      }

    }
    value
  }


}


object ImportKUKAFileService{
  def props(modelHandler: ActorRef) = Props(classOf[ImportKUKAFileService], modelHandler)
}

package sp.areus

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import sp.domain._
import sp.system.messages._

import scala.concurrent.duration._
import scala.xml._


/**
 * Created by Kristofer on 2014-06-27.
 */
class VCImportService(modelHandler: ActorRef) extends Actor {
  implicit val timeout = Timeout(1 seconds)
  import context.dispatcher



  def receive = {
    case Request(_, attr) => {
      val reply = sender
      extract(attr) match {
        case Some((xmlString, model, name)) => {

          val areus = scala.xml.XML.loadString(xmlString)

          val robots = (areus \\ "SimulationResults").head.child

          val temp = robots.map{r =>
            r.child.map{o =>
              val tss = o \\ "TimeStamp"
              tss.map {ts =>
                val time = ts.attribute("time")
                val joints = ts \ "Joints" head
                val positions = ts \ "Position" head
                val speed = ts \ "Speed" head
                val collisions = ts \ "Collisions" head

                val jointValues = parseJointString(joints)

                //println(s"time: $time, joints: $joints")



              }
            }


          }









          reply ! "yes"

        }
        case None => sender ! errorMessage(attr)
      }
    }
  }

  def parseJointString(n: Node) = {
    val value = n.attribute("values").map(_.toString)
    value.map{s =>
      val js = s.trim.split("""\s+""").toList
      val test = js.map(_.replaceAll("""^[^=]*=""", ""))
      println(test)
    }

  }

  def extract(attr: SPAttributes) = {
    for {
      xml <- attr.getAsString("file")
      model <- attr.getAsID("model")
    } yield (xml, model, attr.get("name"))
  }

  def errorMessage(attr: SPAttributes) = {
    SPError("The request is missing parameters: \n" +
      s"file: ${attr.getAsString("file")}" + "\n" +
      s"Request: ${attr}" )
  }



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


object VCImportService{
  def props(modelHandler: ActorRef) = Props(classOf[VCImportService], modelHandler)
}

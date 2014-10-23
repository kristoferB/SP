package sp.areus

import akka.actor._
import sp.system.messages._
import sp.domain._
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout


/**
 * Created by Kristofer on 2014-06-27.
 */
class DelmiaV5Service(modelHandler: ActorRef) extends Actor {
  implicit val timeout = Timeout(1 seconds)
  import context.dispatcher



  def receive = {
    case Request(_, attr) => {
      val reply = sender
      extract(attr) match {
        case Some((xmlString, name)) => {

          val areus = scala.xml.XML.loadString(xmlString)

          val items: Seq[IDAble] = areus \\ "Resource" flatMap { resource =>

            val als = resource \\ "ActivityList"

            val opsXML = als filter {a =>
              val acts = a \ "Activity"
              acts.nonEmpty && acts.head.attribute("ActivityType") == Some(xml.Text("DNBRobotMotionActivity"))
            }
            //println(opsXML.size)

            import sp.domain._

            val ops = opsXML flatMap {n =>
              val name = n.attribute("Task").map(_.toString).getOrElse("noName")
              val nOP = Operation(name)
              val opChildren = n.child.collect {
                case c if c.attribute("Operation") != None => {
                  val cName = c.attribute("Operation").map(_.text).getOrElse("noName")
                  val t = toAttr(c)
                  Operation(cName, List(), SPAttributes(t.asMap.get))
                }
              }.toList
              val chIDs = ListPrimitive(opChildren.map(SPAttributeValue.apply))
              val upd = nOP.copy(attributes = nOP.attributes + ("children" -> chIDs))
              upd :: opChildren
            }

            //println(ops.head)


            val sopsXML = als filter {a =>
              val acts = a \ "Activity"
              acts.nonEmpty && acts.head.attribute("ActivityType") == Some(xml.Text("DNBIgpCallRobotTask"))
            }

            val sopspecs = (sopsXML map { n =>
              val name = n.attribute("Task").map(_.toString).getOrElse("noName")
              val opChildren = n.child.collect {
                case c if {
                  val cn = (c \ "CallName")
                  cn.nonEmpty && ops.exists(_.name == cn.text)
                } => {
                  val cName = c \ "CallName" text;
                  ops.find(_.name == cName).get

                }
              }.toList
              val sop = Sequence(opChildren.map(SOP.apply):_*)
              SOPSpec(name, List(sop))
            }).toList



            val items: List[IDAble] = ops.toList ++ sopspecs
            val resourceChildren = ListPrimitive(items.filter(x => x.isInstanceOf[Operation] && x.attributes.attrs.contains("children")).map(x => IDPrimitive(x.id)))

            val resXML = resource \ "GeneralInfo"

            val res = resXML.map { r =>
              val name = (r \ "ResourceName") text
              val attr = toAttr(r) match {
                case m: MapPrimitive => m.asSPAttributes + ("kind" -> "resource")
                case x @ _ => Attr("value" -> x)
              }
              Thing(name, attr + ("children" -> resourceChildren))
            }

            val childrenIds = sopspecs.map(x => IDPrimitive(x.id)) ++ List(IDPrimitive(res.head.id))

            val resSpec = SPSpec(res.head.name, Attr("children" -> ListPrimitive(childrenIds)))
            resSpec :: items ++ res

          }



          println(name)

          val id = ID.newID
          val n = name.flatMap(_.asString).getOrElse("noName")
          for {
            model <- (modelHandler ? CreateModel(id, n, Attr("attributeTags"-> MapPrimitive(Map()), "conditionGroups"-> ListPrimitive(List())))).mapTo[ModelInfo]
            items <- modelHandler ? UpdateIDs(id, model.version, items.toList)
          } yield {
            println(s"MADE IT: $model")
            reply ! model.model.toString


          }



        }
        case None => sender ! errorMessage(attr)
      }
    }
  }

  def extract(attr: SPAttributes) = {
    for {
      xml <- attr.getAsString("file")
    } yield (xml, attr.get("name"))
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


object DelmiaV5Service{
  def props(modelHandler: ActorRef) = Props(classOf[DelmiaV5Service], modelHandler)
}

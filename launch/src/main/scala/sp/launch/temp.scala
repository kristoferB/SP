//package sp.launch
//
///**
// * Created by kristofer on 04/10/14.
// */
//import spray.routing.SimpleRoutingApp
//import akka.actor._
//
//object Main extends App {
//  //implicit val system = ActorSystem("my-system")
//
//  val areus = scala.xml.XML.loadFile("./simResult.xml")
//
//  val als = areus \\ "ActivityList"
//
//  val opsXML = als filter {a =>
//    val acts = a \ "Activity"
//    acts.nonEmpty && acts.head.attribute("ActivityType") == Some(xml.Text("DNBRobotMotionActivity"))
//  }
//  //println(opsXML.size)
//
//  import sp.domain._
//
//  val ops = opsXML flatMap {n =>
//    val name = n.attribute("Task").map(_.toString).getOrElse("noName")
//    val nOP = Operation(name)
//    val opChildren = n.child.collect {
//      case c if c.attribute("Operation") != None => {
//        val cName = c.attribute("Operation").map(_.text).getOrElse("noName")
//        val t = toAttr(c)
//        Operation(cName, List(), SPAttributes(t.asMap.get))
//      }
//    }.toList
//    val chIDs = ListPrimitive(opChildren.map(SPAttributeValue.apply))
//    val upd = nOP.copy(attributes = nOP.attributes + ("children" -> chIDs))
//    upd :: opChildren
//  }
//
//  //println(ops.head)
//
//
//  val sopsXML = als filter {a =>
//    val acts = a \ "Activity"
//    acts.nonEmpty && acts.head.attribute("ActivityType") == Some(xml.Text("DNBIgpCallRobotTask"))
//  }
//
//  val sopspecs = sopsXML map { n =>
//    val name = n.attribute("Task").map(_.toString).getOrElse("noName")
//    val opChildren = n.child.collect {
//      case c if {
//        val cn = (c \ "CallName")
//        cn.nonEmpty && ops.exists(_.name == cn.text)
//      } => {
//        val cName = c \ "CallName" text;
//        ops.find(_.name == cName).get
//
//      }
//    }.toList
//    val sop = Sequence(opChildren.map(SOP.apply):_*)
//    SOPSpec(name, List(sop))
//  }
//
//  println(sopspecs.head)
//
//
//
//  val resXML = areus \\ "GeneralInfo"
//
//  val res = resXML.map { r =>
//    val name = (r \ "ResourceName") text
//    val attr = toAttr(r) match {
//      case m: MapPrimitive => m.asSPAttributes + ("kind" -> "resource")
//      case x @ _ => Attr("value" -> x)
//    }
//    Thing(name, attr)
//  }
//
//  println(res)
//
//
//
//
//
//
//  import scala.xml._
//
//  def toAttr(n : Node): SPAttributeValue = {
//    val attr = n.attributes.asAttrMap.map{case (k, v) => k -> StringPrimitive(v)}
//    val value: SPAttributeValue = {
//      if (n.child.count(_.isInstanceOf[Text]) == 1) {
//        val value = StringPrimitive(n.text)
//        if (attr.isEmpty) value
//        else MapPrimitive(attr + ("value"->value))
//      }
//      else {
//        val children = n.child //.filter(n => n.isInstanceOf[Text] || n.isInstanceOf[Elem])
//        val fold = children.foldLeft(Map[String,List[SPAttributeValue]]()){
//          case (aggr, e: Elem) => {
//            val newAttr = toAttr(e)
//            val prev = aggr.getOrElse(e.label, List())
//            val xs = if (newAttr != MapPrimitive(Map())) newAttr :: prev else prev
//            aggr + (e.label -> xs)
//          }
//          case (aggr, t: Text) => aggr
//        }
//
//        val map = fold collect {
//          case (k, x :: Nil)  =>  k -> x
//          case (k, x :: xs) => k -> ListPrimitive(x :: xs)
//        }
//        MapPrimitive(map ++ attr)
//      }
//
//    }
//    value
//  }
//
//
//}

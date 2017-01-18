package sp.domain

import org.json4s._
import org.scalatest._
import sp.domain.Logic._
import upickle.Js

import scala.reflect.ClassTag


object UPickleAPI {
  sealed trait API
  case class Test1(p1: String, p2: String) extends API
  case class Test4(p1: String, p2: SPAttributes) extends API
  case class Test2(p1: Int, p2: Int) extends API
  case class Test3(p1: Double, p2: Tom) extends API

  sealed trait SUB
  case class Tom(str: String) extends SUB

  lazy val apiClasses: List[Class[_]] =   sp.macros.MacroMagic.values[API]
  lazy val apiJson: List[String] = sp.macros.MacroMagic.info[API, SUB]

}

object FixedType extends upickle.AttributeTagged {
  override val tagName = "isa"

  override def annotate[V: ClassTag](rw: Reader[V], n: String) = Reader[V]{
    case Js.Obj(x@_*) if x.contains((tagName, Js.Str(n.split('.').takeRight(2).mkString(".")))) =>
      rw.read(Js.Obj(x.filter(_._1 != tagName):_*))
  }

  override def annotate[V: ClassTag](rw: Writer[V], n: String) = Writer[V]{ case x: V =>
    val filter = n.split('.').takeRight(2).mkString(".")
    Js.Obj((tagName, Js.Str(filter)) +: rw.write(x).asInstanceOf[Js.Obj].value:_*)
  }

}


/**
 * Created by kristofer on 15-05-27.
 */
class AttributesUpickleTest extends FlatSpec with Matchers  {


  val o = Operation("hej")
  val state = State(Map(ID.newID -> false))
  val d = org.joda.time.DateTime.now
  val attr = SPAttributes(
    "model" -> ID.newID,
    ("int" -> 1),
    ("hej" -> "hej"),
    ("string", "Det gick"),
    ("boolean", false),
    ("double", 1.0) ,
    ("op", o) ,
    "testar" -> List(o.id, o.id, o.id),
    ("date", d) ,
    ("id", o.id) ,
    ("state", state) ,
    ("under", List(
      ("state", state) ,
      ("op", o) ,
      ("string", "bra")
    )))

  //import org.json4s.native.Serialization._
  //println(writePretty(attr))


  "An SPAttribute" should "be convert when in an uPickel API" in {
    //val t = UPickleAPI.Test4("hej", SPAttributes("cola"->"bananer"))
    //val t2 = Test1("hej", "då")
    val json = upickle.default.write("hej")
    println(json)
    //attr.getAs[String]("string") shouldEqual Some("Det gick")
  }








}


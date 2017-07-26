package sp.domain

import org.scalatest.FreeSpec
import scala.util.Try
import Logic._



trait TestClasses {
  case class Test1(p1: String, p2: Boolean = true)
  object Test1 { implicit val actionFormat: JSFormat[Test1] = deriveFormatSimple }

  object API {
    sealed trait Comm
    case class In1(a1: String) extends Comm
    case class In2(a1: String, a2: Comm) extends Comm
    object Comm {
      implicit lazy val commF: JSFormat[Comm] = deriveFormatISA[Comm]
    }
  }

  val anInt = 1
  val aBool = false
  val aString = "foo"
  val anO = Operation("o")

}



class SpAttributesTest extends FreeSpec with TestClasses{

  "Creating simple SPAttributes and SPValues" - {


    "Creating SPValues" in {
      SPValue(1)
      SPValue(anInt)
      SPValue(aBool)
      SPValue(aString)
      SPValue(anO)
      assert(true) // nothing crashed
    }

    "Creating SPAttributes" in {
      SPAttributes("p1" -> 1, "p2"->"bar")
      SPAttributes("p1" -> anInt, "p2"->"bar")
      SPAttributes("p1" -> aBool, "p2"->"bar")
      SPAttributes("p1" -> aString, "p2"->"bar")
      SPAttributes("p1" -> anO, "p2"->"bar")
      assert(true) // nothing crashed
    }
  }

  "Converting case classes" - {

    "Converting to SPAttributes and SPVAlues" in {
      val t1 = SPValue(Test1("foo"))
      val attrT1 = SPAttributes("p1"->"foo", "p2"->true)
      assert(t1 == attrT1)

      val res = SPAttributes(
        "foo"->API.In1("foo"),
        "bar"->API.In2("foo", API.In1("foo"))
      )
      val comp = SPAttributes(
        "foo"->SPAttributes("isa"->"In1", "a1"->"foo"),
        "bar"->SPAttributes("isa"->"In2", "a1"->"foo","a2"->
          SPAttributes("isa"->"In1", "a1"->"foo"))
      )
      println(res)
      assert(res == comp)
    }

    "getting back case classes" in {
      val t1 = SPValue(Test1("foo"))
      val t2 = SPAttributes(
        "foo"->API.In1("foo"),
        "bar"->API.In2("foo", API.In1("foo"))
      )

      val get1 = t1.as[Test1]
      assert(get1 == Test1("foo"))

      val get2 = t2.getAs[API.Comm]("bar")
      assert(get2.contains(API.In2("foo", API.In1("foo"))))

    }

    "Working with the SP domain" in {
      val o = Operation("o1")
      val s = SPState("s1", Map(ID.newID ->SPValue("hej")))

      val oV = SPValue(o)
      val oS = SPValue(s)
      val attr = SPAttributes("xs"->List(o, s))

      assert(oV.as[Operation] == o)
      assert(attr.getAs[List[IDAble]]("xs").contains(List(o, s)))
      assert(attr.getAs[List[Operation]]("xs").isEmpty)


    }

  }


}







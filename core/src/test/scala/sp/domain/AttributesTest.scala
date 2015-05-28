package sp.domain

import org.scalatest._
import sp.domain.Logic._
import org.json4s._


/**
 * Created by kristofer on 15-05-27.
 */
class AttributesTest extends FlatSpec with Matchers  {

  implicit val f = jsonFormats

  val o = Operation("hej")
  val state = State(Map(ID.newID -> false))
  val d = org.joda.time.DateTime.now
  val attr = "model" -> ID.newID +
    ("int" -> 1) +
    ("hej" -> "hej") +
    ("string", "Det gick") +
    ("boolean", false) +
    ("double", 1.0) +
    ("op", o) +
    ("date", d) +
    ("id", o.id) +
    ("state", state) +
    ("under", List(
      ("state", state) +
      ("op", o) +
      ("string", "bra")
    ))

  //import org.json4s.native.Serialization._
  //println(writePretty(attr))


  "An SPAttribute" should "get as string" in {
    attr.getAs[String]("string") shouldEqual Some("Det gick")
  }
  "An SPAttribute" should "return none if no key" in {
    attr.getAs[String]("nejNej") shouldEqual None
  }
  "An SPAttribute" should "return none if not correct type" in {
    attr.getAs[String]("boolean") shouldEqual None
  }
  "An SPAttribute" should "get as int" in {
    attr.getAs[Int]("int") shouldEqual Some(1)
  }
  "An SPAttribute" should "get as boolean" in {
    attr.getAs[Boolean]("boolean") shouldEqual Some(false)
  }
  "An SPAttribute" should "get as double" in {
    attr.getAs[Double]("double") shouldEqual Some(1.0)
  }
  "An SPAttribute" should "get as datetime" in {
    attr.getAs[org.joda.time.DateTime]("date") shouldEqual Some(d)
  }
  "An SPAttribute" should "get as ID" in {
    attr.getAs[ID]("id") shouldEqual Some(o.id)
  }
  "An SPAttribute" should "get as Operation" in {
    attr.getAs[Operation]("op") shouldEqual Some(o)
  }
  "An SPAttribute" should "get as State" in {
    attr.getAs[State]("state") shouldEqual Some(state)
  }
  "An SPAttribute" should "find all values" in {
    val res = attr.find("string")
    res shouldEqual List(JString("Det gick"), JString("bra"))
  }
  "An SPAttribute" should "find all objects as" in {
    val res = attr.findAs[Operation]("op")
    res shouldEqual List(o, o)
  }
  "An SPAttribute" should "find simple primitive as" in {
    val res = attr.findAs[String]("string")
    res shouldEqual List("Det gick", "bra")
  }
  "An SPAttribute" should "find objects with keys" in {
    val res = attr.findObjectsWithKeys(List("name", "conditions"))
    res shouldEqual List(
      ("op", Extraction.decompose(o)),
      ("op",Extraction.decompose(o)))
  }

  "An SPAttribute" should "find objects with keys as" in {
    val res = attr.findObjectsWithKeysAs[Operation](List("name", "conditions"))
    res shouldEqual List(
      ("op", o),
      ("op", o))
  }
  "An SPAttribute" should "find no objects when no match on keys" in {
    val res = attr.findObjectsWithKeysAs[Operation](List("name", "key100"))
    res shouldEqual List()
  }
  "An SPAttribute" should "find objects with fields" in {
    val res = attr.findObjectsWithField(List(("isa" -> "Operation")))
    res shouldEqual List(
      ("op", Extraction.decompose(o)),
      ("op",Extraction.decompose(o)))
  }
  "An SPAttribute" should "find objects with fields as" in {
    val res = attr.findObjectsWithFieldAs[Operation](List(("isa" -> "Operation")))
    res shouldEqual  List(
      ("op", o),
      ("op",o))
  }
  // Also all json4s commands are availible and map, filter, etc
  "An SPAttribute" should "also have json4s methods" in {
    val res = attr.filterField(_._2 == JDouble(1.0)).map{
      case (k, JDouble(v)) => v + 3.5
      case _ => 1.2
    }
    println(s"TEST: $res")
    res.head shouldEqual 4.5
  }







}

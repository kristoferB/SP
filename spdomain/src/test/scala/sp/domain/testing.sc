import java.util.UUID

import play.api.libs.json._
import scala.util.Try
import org.threeten.bp._






import julienrf.json.derived


object API {
  sealed trait Kalle
  case class Testing(a1: String, b2: Int = 5) extends Kalle
  case class Testing2(a3: String, b4: Int = 5) extends Kalle
  case class Testing3(k: Kalle) extends Kalle

  object Kalle {
    //implicit val f: Reads[Kalle] = derived.flat.reads((__ \ "isa").read[String])
    //implicit val f2: OWrites[Kalle] = derived.flat.owrites((__ \ "isa").write[String])
    implicit val f3: OFormat[Kalle] = derived.flat.oformat[Kalle]((__ \ "isa").format[String])

  }
}


//implicit val residentFormat = Json.format[Testing]

import sp.domain._
import sp.domain.Logic._

val k = API.Testing("hej", 4)
SPValue(k).toJson

val res = SPAttributes("isa"->"Testing")

res.as[API.Kalle]
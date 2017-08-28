package sp.domain.testing {

  import org.scalatest.FreeSpec

  import scala.util.Try
  import sp.domain._
  import Logic._
  import shapeless.Lazy
  import play.api.libs.json._
  import julienrf.json.derived._


  trait TestingDerived {
    val derive: Json.type = Json

    def deriveCaseObject[A](implicit derivedReads: Lazy[DerivedReads[A]], derivedOWrites: Lazy[DerivedOWrites[A]]): OFormat[A] =
      OFormat(derivedReads.value.reads(TypeTagReads.nested, NameAdapter.identity), derivedOWrites.value.owrites(TypeTagOWrites.nested, NameAdapter.identity))

  }


  class TestingAPITest extends FreeSpec {

    "testing" in {

      val x1 = sp.kalle.testing.TestingAPI.X1("hej", 1, false)
      println(SPValue(x1))
      println(SPValue(sp.kalle.testing.TestingAPI.X6))
      println(SPValue(sp.kalle.testing.TestingAPI.X6).as[sp.kalle.testing.TestingAPI.API])

      println(SPValue(x1).as[sp.kalle.testing.TestingAPI.API])
      println(SPAttributes.fromJson("""{"a":"hej","b":1,"c":false,"_type":"sp.kalle.testing.TestingAPI.X1"}""").flatMap(_.to[sp.kalle.testing.TestingAPI.API]))


      import sp.kalle.testing.TestingAPI.SUB._
      println(SPAttributes.fromJson("""{"a":"hej","b":1,"c":false, "d": 3}""").flatMap(_.to[sp.kalle.testing.TestingAPI.X3]))

      println(SPValue(x1))


    }


  }


}

package sp.test {

}
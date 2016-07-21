package sp.macros

import org.scalatest.{FreeSpec, Matchers}



sealed trait Test
case class Kalle(name: String = "hej", attr: Kalle2) extends Test {
  def domain: List[String] = List()
}
case class Kalle2(tomte: Int) extends Test
case class Kalle3(ko: Option[String]) extends Test

class MacroTest extends FreeSpec with Matchers {

  "Macro" - {
    "should work" in {
      val test = MacroMagic.values[Test]
      val test2 = MacroMagic.info[Test]

      println("macro test:")
      test.foreach(println)

      println("macro test 2:")
      println(test2)
    }
  }

}





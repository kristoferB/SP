package sp.macros

import org.scalatest.{FreeSpec, Matchers}


object NuSÅ {
  sealed trait Test
  case class Kalle(name: String = "hej", attr: Kalle2) extends Test
  case class Kalle2(tomte: Int = 5) extends Test
  case class Kalle3(ko: Option[String]) extends Test
}


class MacroTest extends FreeSpec with Matchers {

  "Macro" - {
    "should work" in {
      val test = MacroMagic.values[NuSÅ.Test]
      val test2 = MacroMagic.info[NuSÅ.Test, NuSÅ.Test]

      println("macro test:")
      test.foreach(println)

      println("macro test 2:")
      println(test2)
    }
  }

}





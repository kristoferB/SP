package sp.virtcom

import sp.supremicaStuff.base.{Exporters, SimpleModuleFactory}
import org.scalatest._

/**
 * Created by patrik on 2015-05-28.
 */
class SPSupremicaTest extends FreeSpec {

  "when creating a module" - {
    "the supremica parts should be right" in {
      case class Module() {
        val psl = PSLFloorRoofCase()

//        import CollectorModelImplicits._
//        psl.createModel("./testFiles/gitIgnore/")

      }
      Module()
    }
  }

}

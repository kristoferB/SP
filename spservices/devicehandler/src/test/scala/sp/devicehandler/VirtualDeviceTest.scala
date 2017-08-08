package sp.devicehandler

import org.scalatest._
import sp.domain.Logic._
import sp.domain._

/**
  * Created by kristofer on 2017-03-06.
  */
class VirtualDeviceTest extends FreeSpec with Matchers{
  import sp.abilityhandler.{APIAbilityHandler => api}

  "Methods tests" - {

    "api schema testing" in {
      println(VirtualDeviceInfo.apischema)
      assert(VirtualDeviceInfo.apischema != SPAttributes.empty)
    }


  }
}

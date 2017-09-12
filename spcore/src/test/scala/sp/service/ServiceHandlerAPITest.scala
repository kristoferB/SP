package sp.service

import org.scalatest.FreeSpec
import scala.util.Try
import sp.domain._
import Logic._







class SpAttributesTest extends FreeSpec {

  "ServiceHandlerComm " - {

    "correct request" in {
      val x = SPValue(APIServiceHandler.GetServices)
      println(x)
      println(x.as[APIServiceHandler.Request])
    }

    "correct response" in {
      val resp = APISP.StatusResponse("hej")
      //val a =
    }

  }


}







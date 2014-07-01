package sp.server


import akka.actor.ActorSystem

import scala.concurrent.future
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FreeSpec
import org.scalatest.Matchers
import spray.http.StatusCodes._
import spray.testkit.ScalatestRouteTest

class RestAPITest extends FreeSpec with SPRoute with ScalatestRouteTest with Matchers {
  def actorRefFactory = system

  "The Timezone Service" - {
    "when calling GET /api/TimezoneService/39/-119/1331161200" - {
      "should return 'hi message'" in {
        Get("/api/") ~> api ~> check {
          status should equal(OK)
          responseAs[String] == ("Seqeunce Planner REST API")
        }
      }
    }
  }
}


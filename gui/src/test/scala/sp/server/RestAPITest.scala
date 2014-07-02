//package sp.server
//
//
//import akka.actor._
//import sp.system.messages
//
//import scala.concurrent.future
//import org.scalatest.BeforeAndAfterAll
//import org.scalatest.FreeSpec
//import org.scalatest.Matchers
//import spray.http.StatusCodes._
//import spray.testkit.ScalatestRouteTest
//import sp.domain._
//import sp.system.messages._
//
//class RestAPITest extends FreeSpec with SPRoute with ScalatestRouteTest with Matchers {
//  def actorRefFactory = system
//  val modelHandler = system.actorOf(Props(classOf(MockModel), "mock"))
//
//  "The api " - {
//    "when calling GET /api/" - {
//      "should return 'hi message'" in {
//        Get("/api/") ~> api ~> check {
//          status should equal(OK)
//          responseAs[String] == ("Seqeunce Planner REST API")
//        }
//      }
//    }
//
//    "when calling GET /api/models" ~ {
//      "should return ModelInfo" in {
//        Get("/api/models") ~> api ~> check {
//          responseAs[String] == "hej"
//        }
//      }
//    }
//  }
//}
//
//class MockModel extends Actor {
//  val mi = ModelInfo("model", 1, SPAttributes(Map()))
//  val op1 = new Operation("kalle"){
//    override lazy val id = ID(java.util.UUID.fromString("3a0bad18-a4c2-4764-9d5a-b57d6579da24"))
//    override lazy val version = 2
//  }
//  val op2 = new Operation("kalle2"){
//    override lazy val id = ID(java.util.UUID.fromString("3a0bad18-a4c2-4764-9d5a-b57d6579da25"))
//    override lazy val version = 5
//  }
//  val idMap: Map[ID, IDAble] = Map(op1.id -> op1, op2.id -> op2)
//
//  override def receive = {
//    case CreateModel(name, attr) => if (mi.model == name) sender ! ModelInfo(name, 1, attr) else SPError(s"Model ${name} does not exist.")
//    case GetModels => sender ! ModelInfos(List(mi))
//    case m: ModelMessage => if (m.model != mi.model) SPError(s"Model ${m.model} does not exist.") else m match {
//      case UpdateIDs(m,v,ids) => {
//        createDiff(m,v,ids) match {
//          case Left(diff) => {
//            sender ! SPIDs(diff.ids)
//          }
//          case Right(error) => sender ! error
//        }
//      }
//      case GetIds(ids, m) => {
//        ids foreach(id=> if (!idMap.contains(id)) sender ! MissingID(id, m))
//        val res = for {
//              i <- ids
//              x <- idMap.get(i)
//        } yield x
//        sender ! SPIDs(res)
//      }
//    }
//  }
//
//  def createDiff(model: String, modelVersion: Long, ids: List[UpdateID]): Either[ModelDiff, UpdateError] = {
//    // Check if any item could not be updated and divide them
//    val updateMe = ids partition {case UpdateID(id,v, item) => {
//        val current = idMap.getOrElse(id, null)
//        // TODO: also need to check so that the classes match. Impl when everything is working. 140627
//        current == null || current.version <= v
//      }
//    }
//    if (updateMe._2.isEmpty) {
//      val upd = updateMe._1 map (uid=> uid.updated.update(uid.id, uid.version))
//      Left(ModelDiff(upd, model, modelVersion, 2))
//    } else {
//      Right(UpdateError(modelVersion, 2, updateMe._2 map(_.id)))
//    }
//  }
//}
//
//
//

package sp.server

import sp.domain._
import spray.routing._
import sp.system.messages._
import reflect.ClassTag
import scala.concurrent.ExecutionContext.Implicits.global
import akka.util.Timeout
import akka.actor._
import akka.pattern.ask
import scala.concurrent.duration._



// API classes
case class IDSaver(isa: String,
                   name: String,
                   attributes: Option[SPAttributes],
                   id: Option[ID],
                   version: Option[Long],

                   conditions: Option[List[Condition]],
                   stateVariables: Option[List[StateVariable]],
                   sop: Option[SOP])


trait SPRoute extends HttpService {
  val modelHandler: ActorRef

  implicit val timeout = Timeout(3 seconds)

  import spray.httpx.SprayJsonSupport._
  import spray.json._
  import sp.json.SPJson._
  import spray.httpx.encoding._

  val api = pathPrefix("api") {
    / {complete("Seqeunce Planner REST API")} ~
    encodeResponse(Gzip) {
      path("models") {
        get {
          toSP(GetModels, {
            case ModelInfos(list) => complete(list)
          })
        } ~
          post {
            entity(as[CreateModel]) { cmd =>
              toSP(cmd, {
                case x: ModelInfo => complete(x)
              })
            }
          }
      } ~
        pathPrefix("models" / Segment) { model =>
          / {
            toSP(GetModelInfo(model), {
              case x: ModelInfo => complete(x)
            })
          } ~
            pathPrefix("operations") {
              IDHandler(model) ~
                getSPIDS(GetOperations(model))
            } ~
            pathPrefix("things") {
              IDHandler(model) ~
                getSPIDS(GetThings(model))
            } ~
            pathPrefix("specs") {
              IDHandler(model) ~
                getSPIDS(GetSpecs(model))
            } ~
            pathPrefix("items") {
              IDHandler(model) ~
                getSPIDS(GetIds(List(), model))
            }
        }
    }
  }

  def IDHandler(model: String) = {
    path(JavaUUID){id =>
      /{ get {toSP(GetIds(List(ID(id)), model), {
        case SPIDs(x) => if (!x.isEmpty) complete(x.head) else complete(x)})}
      }~
      post {
        entity(as[IDSaver]) { x =>
          val upids = createUPIDs(List(x), Some(id))
          // Maybe req modelversion in the future
          toSP(UpdateIDs(model, -1, upids),  {
              case SPIDs(x) => if (!x.isEmpty) complete(x.head) else complete(x)
            })
        }
      }
    } ~
    / {
      post {
        entity(as[List[IDSaver]]) { xs =>
          val upids = createUPIDs(xs, None)
          toSP(UpdateIDs(model, -1, upids), {
            case SPIDs(x) => complete(x)
          })
        }
      }
    }
  }

  def getSPIDS(mess: ModelQuery) = {
    /{ get {toSP(mess, { case SPIDs(x) => complete(x)})}}
  }


  def toSP(mess: Any, matchReply: PartialFunction[Any, Route]) = {
    onSuccess(modelHandler ? mess){evalReply{matchReply}}
  }

  // to cleanup the routing
  def / = pathEndOrSingleSlash


  def replyMatcher: PartialFunction[Any, Route] = {
    case e: SPErrorString => complete(e.error)
    case e: UpdateError => complete(e)
    case a: Any  => complete("reply from application is not converted: " +a.toString)
  }

  def evalReply(pf: PartialFunction[Any, Route]) = {
    pf orElse replyMatcher
  }


  def createUPIDs(ids: List[IDSaver], maybeID: Option[ID]) = {
    ids map{ x =>
      val addID = x.copy(id = Some(x.id.getOrElse(maybeID.getOrElse(ID.newID))))
      val o = addID.toJson.convertTo[IDAble]
      UpdateID(x.id.getOrElse(o.id), x.version.getOrElse(o.version), o)
    }
  }
}
//  def getBasedOn(x: IDAble) = {
//    println(s"getbasedon: $x")
//    for {
//      bo <- x.attributes.getAsMap("basedOn")
//      id <- bo.get("id").flatMap(_.asID)
//      v <- bo.get("version").flatMap(_.asLong)
//    } yield {
//      (id, v)
//    }
//  }

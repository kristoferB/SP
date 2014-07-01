package sp.server

import sp.domain._
import spray.routing._
import sp.system.messages._
import sp.system.SPActorSystem._
import reflect.ClassTag
import scala.concurrent.ExecutionContext.Implicits.global
import akka.util.Timeout
import akka.actor._
import akka.pattern.ask
import scala.concurrent.duration._



// API classes
case class IDSaver(isa: String,
                   id: ID,
                   version: Long,
                   name: String,
                   attributes: SPAttributes,
                   conditions: Option[List[Condition]],
                   stateVariables: Option[List[StateVariable]],
                   sop: Option[SOP])


trait SPRoute extends HttpService {

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
      getSPIDS(GetIds(List(ID(id)), model))~
      post {
        entity(as[IDSaver]) { x =>
          val upids = createUPIDs(List(x))
          // Maybe req modelversion in the future
          toSP(UpdateIDs(model, -1, upids),  {
              case SPIDs(x) => complete(x)
            })
        }
      }
    } ~
    / {
      post {
        complete("soon supported")
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


  def createUPIDs(ids: List[IDSaver]) = {
    ids map{ x =>
      val o = x.toJson.convertTo[IDAble]
      UpdateID(x.id, x.version, o)
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

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



  import spray.httpx.SprayJsonSupport._
  import spray.json._
  import sp.json.SPJson._
  import spray.httpx.encoding._


trait SPRoute extends SPApiHelpers with ModelAPI with RuntimeAPI {
  val modelHandler: ActorRef
  val runtimeHandler: ActorRef

  val api = pathPrefix("api") {
    / {complete("Seqeunce Planner REST API")} ~
    encodeResponse(Gzip) {
      pathPrefix("models"){
        modelapi
      }~
      pathPrefix("runtimes"){
        runtimeapi
      } ~
      path("services") {
        //TODO: Fix service API
        complete("services")
      }
    }
  }
}



trait ModelAPI extends SPApiHelpers {
  val modelHandler: ActorRef
  private implicit val to = timeout

  def modelapi =
    /{
      get {
        callSP(GetModels, {
          case ModelInfos(list) => complete(list)
        })
      } ~
        post {
          entity(as[CreateModel]) { cmd =>
            callSP(cmd, {
              case x: ModelInfo => complete(x)
            })
          }
        }
    } ~
    pathPrefix(Segment) { model =>
      / {
        callSP(GetModelInfo(model), {
          case x: ModelInfo => complete(x)
        })
      } ~
      pathPrefix(Segment){ typeOfItems =>
        IDHandler(model) ~
        {typeOfItems match {
          case "operations" => getSPIDS(GetOperations(model))
          case "things" => getSPIDS(GetThings(model))
          case "specs" => getSPIDS(GetSpecs(model))
          case "items" => getSPIDS(GetIds(List(), model))
          case "statevariables" => path(JavaUUID){ id =>
            /{ get {callSP(GetStateVariable(id, model), {
              case SPSVs(x) => if (!x.isEmpty) complete(x.head) else complete(x)})}
            }
          }
        }}
      }
  }



  private def getSPIDS(mess: ModelQuery) = {
    /{ get {callSP(mess, { case SPIDs(x) => complete(x)})}}
  }


  private def callSP(mess: Any, matchReply: PartialFunction[Any, Route]) = {
    onSuccess(modelHandler ? mess){evalReply{matchReply}}
  }

  private def IDHandler(model: String) = {
    path(JavaUUID){id =>
      /{ get {callSP(GetIds(List(ID(id)), model), {
        case SPIDs(x) => if (!x.isEmpty) complete(x.head) else complete(x)})}
      }~
      post {
        entity(as[IDSaver]) { x =>
          val upids = createUPIDs(List(x), Some(id))
          // Maybe req modelversion in the future
          callSP(UpdateIDs(model, upids),  {
              case SPIDs(x) => if (!x.isEmpty) complete(x.head) else complete(x)
            })
        }
      }
    } ~
    / {
      post {
        entity(as[List[IDSaver]]) { xs =>
          val upids = createUPIDs(xs, None)
          callSP(UpdateIDs(model, upids), {
            case SPIDs(x) => complete(x)
          })
        }
      }
    }
  }

}





trait RuntimeAPI extends SPApiHelpers {
  val runtimeHandler: ActorRef
  private implicit val to = timeout

  def runtimeapi =
        /{get{callSP(GetRuntimes, {
            case RuntimeInfos(xs) => complete(xs) })
        } ~
          post { entity(as[CreateRuntime]) { cr =>
            callSP(cr, {
              case xs: CreateRuntime => complete(xs)
            })}
          }
        } ~
        pathPrefix(Segment){ rt =>
          /{
            post{ entity(as[SPAttributes]) { attr =>
            callSP(SimpleMessage(rt, attr), {
              case xs: SPAttributes => complete(xs)
            })}

            }
          }
        } ~
        pathPrefix("kinds"){
          /{get{callSP(GetRuntimeKinds, {
            case RuntimeKindInfos(xs) => complete(xs)
          })}}
        }

  private def callSP(mess: Any, matchReply: PartialFunction[Any, Route]) = {
    onSuccess(runtimeHandler ? mess){evalReply{matchReply}}
  }

}

trait SPApiHelpers extends HttpService {

  val timeout = Timeout(3 seconds)
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

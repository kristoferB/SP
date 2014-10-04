package sp.server

import sp.domain._
import spray.http.{AllOrigins, StatusCodes, HttpHeaders}
import spray.routing._
import spray.routing.authentication._
import sp.system.messages._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
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
                   sop: Option[List[SOP]])



  import spray.httpx.SprayJsonSupport._
  import spray.json._
  import sp.json.SPJson._
  import spray.httpx.encoding._





trait SPRoute extends SPApiHelpers with ModelAPI with RuntimeAPI with ServiceAPI {
  val modelHandler: ActorRef
  val runtimeHandler: ActorRef
  val serviceHandler: ActorRef
  val userHandler: ActorRef
  private implicit val to = timeout

  private def callSP(mess: Any, matchReply: PartialFunction[Any, Route]) = {
    onSuccess(userHandler ? mess){evalReply{matchReply}}
  }

  def myUserPassAuthenticator(userPass: Option[UserPass]): Future[Option[String]] =
    Future {
      if (userPass.exists(up => up.user == "admin" && up.pass == "pass")) Some("John")
      else None
    }

  // Handles AuthenticationRequiredRejection to omit the WWW-Authenticate header.
  // The omit prevents the browser login dialog to open when the Basic HTTP Authentication repsonds with code "401: Unauthorized".
  implicit val myRejectionHandler = RejectionHandler {
    case AuthenticationFailedRejection(cause, challengeHeaders) :: _ =>
      complete(StatusCodes.Unauthorized, "Wrong username and/or password.")
  }

  def returnUser(userName: String): UserDetails = {
    return UserDetails(22, userName)
  }

  val api = pathPrefix("api") {
    / {complete("Sequence Planner REST API")} ~
    encodeResponse(Gzip) {
      pathPrefix("models"){
          modelapi
      } ~
      pathPrefix("runtimes"){
          runtimeapi
      } ~
      pathPrefix("services") {
        serviceapi
      } ~
      path("users") {
        get {
          callSP(GetUsers, {
            case userMap: Map[String, User] => complete(userMap)
          })
        }~
        post {
          entity(as[AddUser]) { cmd =>
            callSP(cmd, {
              case createdUser: User => complete(createdUser)
            })
          }
        }
      }~
      // For tests during implementation of authentication and authorization
      authenticate(BasicAuth(myUserPassAuthenticator _, realm = "secured API")) { userName =>
        path("login") {
          complete(returnUser(userName))
        }
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
        entity(as[CreateModelNewID]) { cmd =>
          callSP(CreateModel(ID.newID, cmd.name, cmd.attributes), {
            case x: ModelInfo => complete(x)
          })
        } ~
          entity(as[ModelInfo]) { mi =>
            callSP(UpdateModelInfo(mi.model, mi.name, mi.attributes), {
              case x: ModelInfo => complete(x)
            })
          } 
      }
    } ~
    pathPrefix(JavaUUID) { model =>
      parameter('version.as[Long]){ version =>
        pathPrefix(Segment){ typeOfItems =>
          typeOfItems match {
            case "diff" => {
              /{ get {callSP(GetDiff(model, version), {
                case x: ModelDiff => complete(x)})}
              }
            }
            case "diffFrom" => {
              /{ get {callSP(GetDiffFrom(model, version), {
                case x: ModelDiff => complete(x)})}
              }
            }

            case "revert" => {
              /{ get {callSP(Revert(model, version), {
                case x: ModelInfo => complete(x)})}
              }
            }
            case _ => {
              path(JavaUUID){ id =>
                /{ get {callSP((GetIds(model, List(ID(id))), version), {
                  case SPIDs(x) => if (x.size == 1) complete(x.head) else complete(x)})}
                }
              } ~
                /{ get {callSP((GetIds(model, List()), version), { case SPIDs(x) => complete(x)})}}
            }
          }

        } ~
        / {
          get {
            callSP((GetModelInfo(model), version), {
              case x: ModelInfo => complete(x)
            })
          }
        }
      } ~
      / {
        get {
          callSP(GetModelInfo(model), {
            case x: ModelInfo => complete(x)
          })
        } ~
        post {
          entity(as[ModelInfo]) { mi =>
            callSP(UpdateModelInfo(model, mi.name, mi.attributes), {
              case x: ModelInfo => complete(x)
            })
          }
        }
      } ~
      pathPrefix(Segment){ typeOfItems =>
        IDHandler(model) ~
        {typeOfItems match {
          case "operations" => getSPIDS(GetOperations(model))
          case "things" => getSPIDS(GetThings(model))
          case "specs" => getSPIDS(GetSpecs(model))
          case "items" => getSPIDS(GetIds(model, List()))
          case "statevariables" => path(JavaUUID){ id =>
            /{ get {callSP(GetStateVariable(model, id), {
              case SPSVs(x) => if (x.size == 1) complete(x.head) else complete(x)})}
            }
          }
          case _ => reject
        }}
      }
  }


  private def getSPIDS(mess: ModelQuery) = {
    /{ get {callSP(mess, { case SPIDs(x) => complete(x)})}}
  }


  private def callSP(mess: Any, matchReply: PartialFunction[Any, Route]) = {
    onSuccess(modelHandler ? mess){evalReply{matchReply}}
  }

  private def IDHandler(model: ID) = {
    path(JavaUUID){id =>
      /{ get {callSP(GetIds(model, List(ID(id))), {
        case SPIDs(x) => if (x.size == 1) complete(x.head) else complete(x)})}
      } ~
      post {
        entity(as[IDSaver]) { x =>
          val upids = createUPIDs(List(x), Some(id))
          // Maybe req modelversion in the future
          callSP(UpdateIDs(model, upids),  {
              case SPIDs(x) => if (x.size == 1) complete(x.head) else complete(x)
            })
        }
      } ~
      delete {
        val delMe = DeleteIDs(model, List(id))
        callSP(delMe,  {
          case SPIDs(x) => if (x.size == 1) complete(x.head) else complete(x)
        })
      }
    } ~
    / {
      post {
        entity(as[List[IDSaver]]) { xs =>
          val upids = createUPIDs(xs, None)
          callSP(UpdateIDs(model, upids), {
            case SPIDs(x) => complete(x)
          })
        } ~
        entity(as[IDSaver]) { xs =>
          val upids = createUPIDs(List(xs), None)
          callSP(UpdateIDs(model, upids), {
            case SPIDs(x) => if (x.size == 1) complete(x.head) else complete(x)
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



trait ServiceAPI extends SPApiHelpers {
  val serviceHandler: ActorRef
  private implicit val to = timeout

  def serviceapi =
    /{ get { complete{
      (serviceHandler ? GetServices).mapTo[List[String]]
    }}} ~
    pathPrefix(Segment){ service =>
      post { entity(as[SPAttributes]) { attr =>
        callSP(Request(service, attr), {
          case p: Proposition => complete(p)
          case a: SPAttributes => complete(a)
          case r: Result => complete(r)
          case SPIDs(x) => complete(x)
          case SPSVs(x) => complete(x)
          case item: IDAble => complete(item)
        })
      }}
    }

  private def callSP(mess: Any, matchReply: PartialFunction[Any, Route]) = {
    onSuccess(serviceHandler ? mess){evalReply{matchReply}}
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

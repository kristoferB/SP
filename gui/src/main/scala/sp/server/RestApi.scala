package sp.server

import sp.domain._
import sp.opc.ServerSideEventsDirectives
import ServerSideEventsDirectives._
import sp.opc.simpleJsonMessToWeb.RuntimeState
import spray.http.HttpHeaders.RawHeader
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
import spray.httpx.encoding._



//// API classes
//case class IDSaver(isa: String,
//                   name: String,
//                   attributes: Option[SPAttributes],
//                   id: Option[ID],
//                   version: Option[Long],
//                   conditions: Option[List[Condition]],
//                   sop: Option[List[SOP]])









trait SPRoute extends SPApiHelpers with ModelAPI with RuntimeAPI with ServiceAPI {
  val modelHandler: ActorRef
  val runtimeHandler: ActorRef
  val serviceHandler: ActorRef
  val userHandler: ActorRef
  private implicit val to = timeout

  private def callSP(mess: Any, matchReply: PartialFunction[Any, Route]) = {
    onSuccess(userHandler ? mess){evalReply{matchReply}}
  }

  /*def myUserPassAuthenticator(userPass: Option[UserPass]): Future[Option[String]] =
    Future {
      if (userPass.exists(up => up.user == "admin" && up.pass == "pass")) Some("John")
      else None
    }*/

  // Handles AuthenticationRequiredRejection to omit the WWW-Authenticate header.
  // The omit prevents the browser login dialog to open when the Basic HTTP Authentication repsonds with code "401: Unauthorized".
  /*implicit val myRejectionHandler = RejectionHandler {
    case AuthenticationFailedRejection(cause, challengeHeaders) :: _ =>
      complete(StatusCodes.Unauthorized, "Wrong username and/or password.")
  }*/

  def returnUser(userName: String): UserDetails = {
    return UserDetails(22, userName)
  }

  val api = pathPrefix("api") {
    / {complete("Sequence Planner REST API")} ~
    encodeResponse(Gzip) {
      pathPrefix("models"){ modelapi } ~
      pathPrefix("runtimes"){ runtimeapi } ~
      pathPrefix("services") { serviceapi }  //~
//      path("users") {
//        get {
//          callSP(GetUsers, {
//            case userMap: Map[String, User] => complete(userMap)
//          })
//        }~
//        post {
//          entity(as[AddUser]) { cmd =>
//            callSP(cmd, {
//              case createdUser: User => complete(createdUser)
//            })
//          }
//        }
//      } ~
//      // For tests during implementation of authentication and authorization
//      authenticate(BasicAuth(myUserPassAuthenticator _, realm = "secured API")) { userName =>
//        path("login") {
//          complete(returnUser(userName))
//        }
//      }
    }
  }
}


import spray.httpx.SprayJsonSupport._
import spray.json._
import sp.json.SPJson._


trait ModelAPI extends SPApiHelpers {
  val modelHandler: ActorRef
  private implicit val to = timeout

  def modelapi =
    / {
      get {callSP(GetModels) } ~
      post {
        updateModelInfo ~
        entity(as[CreateModelNewID]) { cmd => callSP(CreateModel(ID.newID, cmd.name, cmd.attributes))}
      }
    } ~
    pathPrefix(JavaUUID) { model =>
      / {
        get { callSP(GetModelInfo(model)) } ~
        post { updateModelInfo }
      } ~
      pathPrefix(Segment){ typeOfItems =>
        IDHandler(model) ~
          {typeOfItems match {
            case "operations" => getSPIDS(GetOperations(model))
            case "things" => getSPIDS(GetThings(model))
            case "specs" => getSPIDS(GetSpecs(model))
            case "items" => getSPIDS(GetIds(model, List()))
            case "results" => getSPIDS(GetResults(model))
            case _ => reject
          }}
      } ~
      pathPrefix("history"){
        parameter('version.as[Long]) { version =>
          pathPrefix(Segment) { typeOfItems =>
            typeOfItems match {
              case "diff" => { / { callSP(GetDiff(model, version)) }}
              case "diffFrom" => { / { callSP(GetDiffFrom(model, version)) }}
              case "revert" => { / {callSP(Revert(model, version)) } }
              case _ => {
                / { get { callSP((GetIds(model, List()), version)) } } ~
                path(JavaUUID) { id =>
                  / { get {callSP((GetIds(model, List(ID(id))), version))}}
                }
              }
            }
          } ~
          / { get { callSP((GetModelInfo(model), version))} }
        }
      }
    }


  private def getSPIDS(mess: ModelQuery) = {
    /{ get {callSP(mess, { case SPIDs(x) => complete(x)})}}
  }

  def updateModelInfo = entity(as[ModelInfo]) {info => callSP(UpdateModelInfo(info.model, info))}


  private def callSP(mess: Any, matchReply: PartialFunction[Any, Route] = {PartialFunction.empty}) = {
    //println(s"Sending from route: $mess")
    onSuccess(modelHandler ? mess){evalReply{matchReply}}
  }

  private def IDHandler(model: ID) = {
    path(JavaUUID){id =>
      /{ get {callSP(GetIds(model, List(ID(id))))} ~
        delete { callSP(DeleteIDs(model, List(id))) }
      }
    } ~
    post {
      entity(as[IDAble]) { xs => callSP(UpdateIDs(model, -1, List(xs))) } ~
      entity(as[List[IDAble]]) { xs =>
        callSP(UpdateIDs(model, -1, xs), { case SPIDs(x) => complete(x) })
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
          } ~
          path("stop") {
            callSP(StopRuntime(rt), {
              case xs: SPAttributes => complete(xs)
            })
          } ~
          path("sse") {
            respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
                sse { (channel, lastEventID) =>

                // Register a closed event handler
                channel ! RegisterClosedHandler( () => println("Connection closed !!!") )

                // Use the channel
                runtimeHandler ! SubscribeToSSE(rt, channel, lastEventID)
              }
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
    path(Segment / "import") { service =>
      post {
        entity(as[spray.http.MultipartFormData]){ value =>
          val file = value.get("file") map { f =>
            val fileAsString = f.entity.asString
            val nameAsString = f.filename
            val attr = Attr("file" -> fileAsString, "name"-> SPAttributeValue(nameAsString))
            callSP(Request(service, attr), {
              case s: String => complete(s)
            })
          }
          file match {
            case Some(r) => r
            case None => complete(SPErrorString("Could not find a file in the body: " + value))
          }
        }
      }
    } ~
    pathPrefix(Segment){ service =>
      post { entity(as[SPAttributes]) { attr =>
        callSP(Request(service, attr), {
          case p: Proposition => complete(p)
          case a: SPAttributes => complete(a)
          case r: Result => complete(r)
          case SPIDs(x) => complete(x)
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
    case x: ModelInfo => complete(x)
    case ModelInfos(list) => complete(list)
    case x: ModelDiff => complete(x)
    case SPIDs(x) => if (x.size == 1) complete(x.head) else complete(x)
    case p: Proposition => complete(p)
    case a: SPAttributes => complete(a)
    case r: Result => complete(r)
    case item: IDAble => complete(item)
    case e: SPErrorString => complete(StatusCodes.InternalServerError, e.error)
    case e: SPErrorCodeAndString => complete(e.code,e.error)
    case e: UpdateError => complete(e)
    case MissingID(id, model, mess) => complete(StatusCodes.NotFound, s"id: $id $mess")
    case r: RuntimeState => complete(r)
    case a: Any  => complete("reply from application is not converted: " +a.toString)
  }

  def evalReply(pf: PartialFunction[Any, Route]) = {
    pf orElse replyMatcher
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

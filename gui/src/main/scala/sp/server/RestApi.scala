package sp.server

import sp.domain._
import spray.http.HttpHeaders.RawHeader
import spray.http._
import spray.routing.PathMatchers.Segment
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

import sp.opc.ServerSideEventsDirectives
import ServerSideEventsDirectives._




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

  private def callSP(mess: Any, matchReply: PartialFunction[Any, Route] = {PartialFunction.empty}) = {
    onSuccess(userHandler ? mess) {
      evalReply {
        matchReply
      }
    }
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
    / {
      complete("Sequence Planner REST API")
    } ~
      encodeResponse(Gzip) {
        pathPrefix("models") {
          modelapi
        } ~
          pathPrefix("runtimes") {
            runtimeapi
          } ~
          pathPrefix("services") {
            serviceapi
          } //~
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


trait ModelAPI extends SPApiHelpers {
  val modelHandler: ActorRef
  private implicit val to = timeout

  def modelapi =
    / {
      get {callSP(GetModels) } ~
        post {
          implicit def ju[T: Manifest] =  json4sUnmarshaller[T]
          updateModelInfo ~
            entity(as[CreateModelNewID]) { cmd => callSP(CreateModel(ID.newID, cmd.name, cmd.attributes))}
        }
    } ~
      pathPrefix(JavaUUID) { model =>
        / {
          get { callSP(GetModelInfo(model)) } ~
            post { updateModelInfo } ~
            delete { callSP(DeleteModel(model)) }
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
    implicit def jm[T <: AnyRef] =  json4sMarshaller[T]
    /{ get {callSP(mess,{case SPIDs(xs)=> complete(xs)})}}
  }

  def updateModelInfo = {
    implicit def ju[T: Manifest] =  json4sUnmarshaller[T]
    entity(as[ModelInfo]) {info => callSP(UpdateModelInfo(info.id, info))}
  }


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
        implicit def ju[T: Manifest] =  json4sUnmarshaller[T]
        entity(as[IDAble]) { xs => callSP(UpdateIDs(model, -1, List(xs))) } ~
          entity(as[List[IDAble]]) { xs =>
            callSP(UpdateIDs(model, -1, xs))
          }
      }

  }

}





trait RuntimeAPI extends SPApiHelpers {
  val runtimeHandler: ActorRef
  private implicit val to = timeout

  def runtimeapi =
    /{get{callSP(GetRuntimes)
    } ~
      post {
        implicit def ju[T: Manifest] =  json4sUnmarshaller[T]
        entity(as[CreateRuntime]) { cr =>
        callSP(cr)}
      }
    } ~
      pathPrefix("kinds"){
        /{get{callSP(GetRuntimeKinds)}}
      } ~
      pathPrefix(Segment){ rt =>
        /{
          ID.makeID(rt) match {
            case Some(runtime: ID) =>
              implicit def ju[T: Manifest] =  json4sUnmarshaller[T]
              post{ entity(as[SPAttributes]) { attr =>
                callSP(SimpleMessage(runtime, attr))}
              }
            case None => complete(SPError("The supplied runtime identifier is not a valid ID."))
          }
        } ~
        path("stop") {
          ID.makeID(rt) match {
            case Some(runtime: ID) =>
              callSP(StopRuntime(runtime), {
                case xs: SPAttributes => complete(xs)
              })
            case None => complete(SPError("The supplied runtime identifier is not a valid ID."))
          }
        } ~
        path("sse") {
          ID.makeID(rt) match {
            case Some(runtime: ID) =>
              respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
                sse { (channel, lastEventID) =>
                  // Register a closed event handler
                  channel ! RegisterClosedHandler( () => println("Connection closed !!!") )
                  // Use the channel
                  runtimeHandler ! SubscribeToSSE(runtime, channel, lastEventID)
                }
              }
            case None => complete(SPError("The supplied runtime identifier is not a valid ID."))
          }
        }
      }

  private def callSP(mess: Any, matchReply: PartialFunction[Any, Route] = {PartialFunction.empty}) = {
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
            import sp.domain.Logic._
            import org.json4s._
            val file = value.get("file") map { f =>
              val fileAsString: SPValue = f.entity.asString
              val nameAsString = f.filename.getOrElse("")
              val model = value.get("model").map(_.entity.asString).getOrElse("")
              val modelKV: (String, SPValue) = ID.makeID(model).map(id =>"model"->JString(id.toString)).getOrElse("model"->JString(""))
              val attr = SPAttributes("file" -> fileAsString, "name"-> nameAsString, modelKV)

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
        implicit def ju[T: Manifest] =  json4sUnmarshaller[T]
        post {
          entity(as[SPAttributes]) { attr =>
          callSP(Request(service, attr))
        }}
      }

  private def callSP(mess: Any, matchReply: PartialFunction[Any, Route]={PartialFunction.empty}) = {
    onSuccess(serviceHandler ? mess){evalReply{matchReply}}
  }

}




trait SPApiHelpers extends HttpService with Json4SSP {

  implicit def jm[T <: AnyRef] =  json4sMarshaller[T]

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
    case ri: RuntimeInfo => complete(ri)
    case RuntimeInfos(xs) => complete(xs)
    case RuntimeKindInfos(xs) => complete(xs)
    case e: SPErrorString => complete(StatusCodes.InternalServerError, e.error)
    case e: SPErrors => complete(StatusCodes.InternalServerError, e.errors)
    case e: UpdateError => complete(e)
    case MissingID(id, model, mess) => complete(StatusCodes.NotFound, s"id: $id $mess")
    case r: sp.opc.RuntimeState => complete(r)
    case a: Any  => complete("reply from application is not converted: " +a.toString)
  }

  def evalReply(pf: PartialFunction[Any, Route]) = {
    pf orElse replyMatcher
  }


  def intercept(info: String) = {
      mapRequest(json => {println(s"I GOT in $info: $json"); json}) {reject}
  }






}

trait Json4SSP {
  import java.lang.reflect.InvocationTargetException

  import spray.http.{ ContentTypes, HttpCharsets, HttpEntity, MediaTypes }
  import spray.httpx.unmarshalling.Unmarshaller
  import spray.httpx.marshalling.Marshaller

  import org.json4s.native.Serialization._
  import org.json4s.{ Formats, MappingException }

  import sp.domain.Logic._

  def json4sUnmarshaller[T: Manifest] =
    Unmarshaller[T](MediaTypes.`application/json`) {
      case x: HttpEntity.NonEmpty ⇒
        try read[T](x.asString(defaultCharset = HttpCharsets.`UTF-8`))
        catch {
          case MappingException("unknown error", ite: InvocationTargetException) ⇒ throw ite.getCause
        }
    }

  def json4sMarshaller[T <: AnyRef] =
    Marshaller.delegate[T, String](ContentTypes.`application/json`)(write(_))
}


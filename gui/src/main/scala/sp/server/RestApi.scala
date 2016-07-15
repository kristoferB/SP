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
import ServerSideEventsDirectives._



/**
 * Started working on cleaning up the API 150605
 * TODO: Move API to this trait
 * TODO: Make some tests using spray testkit for routings
 */
trait RestAPI extends HttpService {
  val modelHandler: ActorRef
  val runtimeHandler: ActorRef
  val serviceHandler: ActorRef
  val userHandler: ActorRef
  val eventHandler: ActorRef

  implicit val to: Timeout

  // work in progress to change the structure
  def api = {
    initial{
      get {
        path("models"){ askModel(GetModels) } ~
        path("models" / JavaUUID){ modelID => askModel(GetModelInfo(modelID))} ~
        path("models" / JavaUUID / "items"){ modelID => askModel(GetIds(modelID, List()))} ~
        path("models" / JavaUUID / "operations"){ modelID => askModel(GetOperations(modelID))} ~
        path("models" / JavaUUID / "things"){ modelID => askModel(GetThings(modelID))} ~
        path("models" / JavaUUID / "specs"){ modelID => askModel(GetSpecs(modelID))} ~
        path("models" / JavaUUID / "results"){ modelID => askModel(GetResults(modelID))}
//        path("models" / JavaUUID / Segment / JavaUUID ){ (modelID, id) =>
//          askModel(GetIds(modelID, List(id)))
//        }
      }
    }

  }


  def askModel(mess: SPCommand) = {
    val f = modelHandler ? mess
    complete("")
  }


  def initial(r: Route) = {
    pathPrefix("api"){
      /{complete("SP API")} ~ encodeResponse(Gzip) { r }
    }
  }

  def / = pathEndOrSingleSlash
}

trait SPRoute extends SPApiHelpers with EventAPI with ModelAPI with RuntimeAPI with ServiceAPI {
  val modelHandler: ActorRef
  val runtimeHandler: ActorSelection
  val serviceHandler: ActorSelection
  val userHandler: ActorSelection
  val eventHandler: ActorSelection

  val mediator: ActorRef


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
  // The omit prevents the browser login dialog to open when the Basic HTTP Authentication responds with code "401: Unauthorized".
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
        } ~
        path("events") {
          eventAPI
        }
        //~
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

trait EventAPI extends SPApiHelpers {
  val eventHandler: ActorSelection
  private implicit val to = timeout

  def eventAPI =
    / {
      respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
        // TODO: Send in as dependency
        dynamic {
          sse("", eventHandler)
        }
      }
    }

  private def callSP(mess: Any, matchReply: PartialFunction[Any, Route] = {PartialFunction.empty}) = {
    //println(s"Sending from route: $mess")
    onSuccess(eventHandler ? mess){evalReply{matchReply}}
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
      path("import"){
        post {
          implicit def ju[T: Manifest] =  json4sUnmarshaller[T]
          entity(as[ImportModel]) {im =>
            callSP(im)
          }
        }
      } ~
      pathPrefix(JavaUUID) { model =>
        / {
          get { callSP(GetModelInfo(model)) } ~
          post { updateModelInfo } ~
          delete { callSP(DeleteModel(model)) }
        } ~
        path("export"){
          get { callSP(ExportModel(model)) }
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
                  case "diff" => / { callSP(GetDiff(model, version)) }
                  case "diffFrom" => / { callSP(GetDiffFrom(model, version)) }
                  case "revert" => / {callSP(Revert(model, version)) }
                  case _ =>
                    / { get { callSP((GetIds(model, List()), version)) } } ~
                      path(JavaUUID) { id =>
                        / { get {callSP((GetIds(model, List(ID(id))), version))}}
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
        entity(as[IDAble]) { xs => callSP(UpdateIDs(model, List(xs))) } ~
          entity(as[List[IDAble]]) { xs =>
            callSP(UpdateIDs(model, xs))
          }
      }
  }
}


trait RuntimeAPI extends SPApiHelpers {
  val runtimeHandler: ActorSelection
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
      path(JavaUUID){ rt =>
        /{
          post {
            implicit def ju[T: Manifest] =  json4sUnmarshaller[T]
            entity(as[SPAttributes]) { attr =>
            callSP(SimpleMessage(rt, attr))}
          } ~
          delete {
            callSP(StopRuntime(rt), {
              case xs: SPAttributes => complete(xs)
            })
          }
        }
      }
  private def callSP(mess: Any, matchReply: PartialFunction[Any, Route] = {PartialFunction.empty}) = {
    onSuccess(runtimeHandler ? mess){evalReply{matchReply}}
  }
}


trait ServiceAPI extends SPApiHelpers {
  val serviceHandler: ActorSelection
  private implicit val to = timeout

  def serviceapi =
    /{ get { complete{
      (serviceHandler ? GetServices).mapTo[List[ServiceInfo]]
    }}} ~
    path("newid"){
      dynamic{complete(ID.newID)}
    } ~
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
        import sp.domain.Logic._
        implicit def ju[T: Manifest] =  json4sUnmarshaller[T]
        post {
          entity(as[SPAttributes]) { attr =>
            val id = attr.getAs[ID]("reqID").getOrElse(ID.newID)
            callSP(Request(service, attr, List(), id))
        }}
      }

  private def callSP(mess: Any, matchReply: PartialFunction[Any, Route]={PartialFunction.empty}) = {
    onSuccess(serviceHandler ? mess){evalReply{matchReply}}
  }

}




trait SPApiHelpers extends HttpService with Json4SSP {

  implicit def jm[T <: AnyRef] =  json4sMarshaller[T]

  val timeout = Timeout(5 seconds)
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
    case r: Response => complete(r)
    case r: Progress => complete(r)
    case item: IDAble => complete(item)
    case ri: RuntimeInfo => complete(ri)
    case im: ImportModel => complete(im)
    case RuntimeInfos(xs) => complete(xs)
    case RuntimeKindInfos(xs) => complete(xs)
    case e: SPError => complete(StatusCodes.InternalServerError, e)
//    case e: SPErrorString => complete(StatusCodes.InternalServerError, e.error)
//    case e: SPErrors => complete(StatusCodes.InternalServerError, e.errors)
//    case e: ServiceError => complete(StatusCodes.InternalServerError, e)
//    case e: UpdateError => complete(e)
//    case MissingID(id, mess) => complete(StatusCodes.NotFound, s"id: $id $mess")
    case r: sp.runtimes.opc.RuntimeState => complete(r)
    case SPOK => complete("OK")
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

  import sp.system.messages.JsonFormatsMessage._


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


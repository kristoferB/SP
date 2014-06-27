package sp.server

import akka.actor._
import akka.pattern.ask
import akka.util._
import scala.concurrent.duration._
import spray.routing._


/**
 * Created by Kristofer on 2014-06-19.
 */
class SPWebServer extends Actor with SPRoute {
  def actorRefFactory = context
  def receive = runRoute(api ~ staticRoute)

  def staticRoute: Route = {
    //path("")(getFromResource("webapp/index.html")) ~ getFromResourceDirectory("webapp")
    path("")(getFromFile("src/main/webapp/index.html")) ~ getFromDirectory("src/main/webapp")
  }
}

trait SPRoute extends HttpService {
  import sp.domain._
  import sp.system.messages._
  import sp.system.SPActorSystem._
  import spray.json._
  import sp.json.SPJson._


  val api = pathPrefix("api") {
    pathPrefix("models") {
      pathEnd {
        get {
          onSuccess(modelHandler ? GetModels){ evalReply {
            case ModelInfos(list) => {
              complete(list)
            }
          }}
        }
        post{
          entity(as[CreateModel]){cmd =>
            onSuccess(modelHandler ? cmd){ evalReply {
              case x: ModelInfo => complete(x)
            }}
          }
        }
      }

    }
  }



  def replyMatcher: PartialFunction[Any, Route] = {
    case e: SPErrorString => complete(e.error)
    case e: UpdateError => complete(e)
    case a: Any  => complete("reply from application is not converted: " +a.toString)
  }

  def evalReply(pf: PartialFunction[Any, Route]) = {
    pf orElse replyMatcher
  }
}

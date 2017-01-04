package sp.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.pattern.ask

import scala.concurrent.duration._
import akka.util.Timeout
import sp.system.PubActor

import scala.reflect.ClassTag






object APITEST {
  sealed trait API
  case class Test1(p1: String, p2: String) extends API
  case class Test2(p1: Int, p2: Int) extends API
  case class Test3(p1: Double, p2: Tom) extends API

  sealed trait SUB
  case class Tom(str: String) extends SUB

  lazy val apiClasses: List[Class[_]] =   sp.macros.MacroMagic.values[API]
  lazy val apiJson: List[String] = sp.macros.MacroMagic.info[API, SUB]

}


/**
 * Used by the SP launcher file
 * Created by Kristofer on 2014-06-19.
 */
object LaunchGUI  {//extends MySslConfiguration {

  def launch = {
    implicit val system = sp.system.SPActorSystem.system
    implicit val materializer = ActorMaterializer()

    val widgets = system.actorOf(PubActor.props("widgets"))

    val interface = system.settings.config getString "sp.interface"
    val port = system.settings.config getInt "sp.port"
    val webFolder: String = sp.system.SPActorSystem.settings.webFolder
    val srcFolder: String = if(sp.system.SPActorSystem.settings.devMode)
      sp.system.SPActorSystem.settings.devFolder else sp.system.SPActorSystem.settings.buildFolder

    //import upickle.default._

    def api =
      pathPrefix("test"){
        post{
          entity(as[String]){t =>
            val res = FixedType.read[APITEST.API](t)
            complete("JA, det funderar: "+ t)
          }
        } ~
        get {
          val t = APITEST.Test1("hej", "då")
          complete(HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`application/json`), FixedType.write(t))))
        }
      } ~
      pathPrefix("operation"){
        get {
          import sp.domain._
          import sp.domain.Logic._
          val t = Operation("hej", List(), SPAttributes("test"->APITEST.Test1("hej", "kalle")))
          val json = SPValue(t).toJson
          complete(HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`application/json`), json)))
        }
      } ~
      pathPrefix("api") {
        get {
          pathEndOrSingleSlash{complete("yes")} ~
            path("widget" / Remaining){ file =>
              implicit val timeout: Timeout = 2.seconds
              println("ho ho widget: "+file)

              //getFromFile(s"./gui/sp-example-widget/$file")

              val res = (widgets ? file).mapTo[java.io.File]
              //complete(res)


              import akka.http.scaladsl.model.HttpEntity
              import akka.http.scaladsl.model.MediaTypes.`application/javascript`
              onSuccess(res){r =>
                getFromFile(r)
              }


            }
        }
      }

    val route =
      api ~
      pathEndOrSingleSlash {
        getFromFile(srcFolder + "/index.html")
      } ~
        getFromDirectory(srcFolder) ~
        getFromDirectory(webFolder) ~
        getFromFile(srcFolder + "/index.html")
        getFromFile(webFolder + "/index.html")


    val bindingFuture = Http().bindAndHandle(route, interface, port)

    println(s"Server started ${system.name}, $interface:$port")

    bindingFuture

//  scala.io.StdIn.readLine("Press ENTER to exit application.\n") match {
//    case x => system.terminate()
//  }



  }
}







import upickle._
object FixedType extends upickle.AttributeTagged {
  override val tagName = "isa"

  override def annotate[V: ClassTag](rw: Reader[V], n: String) = Reader[V]{
    case Js.Obj(x@_*) if x.contains((tagName, Js.Str(n.split('.').takeRight(2).mkString(".")))) =>
      rw.read(Js.Obj(x.filter(_._1 != tagName):_*))
  }

  override def annotate[V: ClassTag](rw: Writer[V], n: String) = Writer[V]{ case x: V =>
    val filter = n.split('.').takeRight(2).mkString(".")
    Js.Obj((tagName, Js.Str(filter)) +: rw.write(x).asInstanceOf[Js.Obj].value:_*)
  }

}
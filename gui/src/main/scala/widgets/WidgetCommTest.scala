package spgui.widgets


import communication.APIWebSocket
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.concurrent.Future
import scala.concurrent.Promise
import fr.hmil.roshttp.HttpRequest
import monix.execution.Scheduler.Implicits.global

import scala.util.{Failure, Success, Try}
import fr.hmil.roshttp.response.SimpleHttpResponse

import scala.reflect.ClassTag

object APITEST {
  sealed trait API
  case class Test1(p1: String, p2: String) extends API
  case class Test2(p1: Int, p2: Int) extends API
  case class Test3(p1: Double, p2: Tom) extends API

  sealed trait SUB
  case class Tom(str: String) extends SUB

}


case class Hej(p1: String)
case class Hej2(p1: Int)
case class Cme(c: Int)


case class Operation(name: String, conditions: List[String], attributes: upickle.Js.Obj)

import rx._

object WidgetCommTest {

  //type Comp = ReactComponentU[Unit, Unit, Unit, Element]
  case class State(str: String)

  private class Backend($: BackendScope[Unit, State]) {
    import communication._

    val messObs = Comm.getMessageObserver { mess =>
      val c = Try{upickle.default.readJs[Cme](mess.header)}
      c.map(kalle => changeState(kalle.c.toString).runNow())
    }


    def changeState(str: String): Callback = $.setState(State(str))

    def render(s: State) =
      <.div(
        <.input(
          ^.value := s.str,
          ^.onChange ==> updateMe
        ),
        <.div(s.str),
        <.br(),
        <.button(
          ^.className := "btn btn-default",
          ^.onClick --> Callback.future(send), "SEND"
        )
      )

    def onUnmount() = {
      println("Unmounting")
      messObs.kill()
      Callback.empty
    }

    def updateMe(e: ReactEventI): Callback = {
      changeState(e.target.value)
    }

    def send(): Future[Callback] = {
      HoHo
    }

//    //implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
//    val a = Var("Init")
//    val b = a.trigger(changeState(a.now).runNow())
//    val c = a.foreach(x => println(x))


    def HoHo = {
      import fr.hmil.roshttp.body._

      val url = org.scalajs.dom.window.location.href

      // Testing som RX stuff
      import org.scalajs._
      import dom._

      def getWebsocketUri: String = {
        val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"

        s"$wsProtocol://${dom.document.location.host}/socket"
      }



      val header = Hej("hej")
      val body = Hej("hej")

      //Comm.initWS


      Comm.publishMessage("services", UPickleMessage(APIParser.writeJs(header), APIParser.writeJs(body)))

      val p = Promise[Callback]()
      //Comm.getWebSocketNotifications(str => p.success(Callback.alert(str)))
      p.future




      //val b = a.foreach(x => changeState(x))
      //val c = b

//      val ws = new WebSocket(getWebsocketUri)
//      ws.onopen = { (kalle: Event) =>
//        println("connected" + kalle)
//        ws.send("TESTAR ATT SKICKA")
//      }
//
//      var c = 0;
//      ws.onmessage = {
//        (e: MessageEvent) =>
//          //println(e.data.toString)
//          a() = e.data.toString + c.toString
//          c = c+1
//      }




//      val request = HttpRequest(url).withPath("/test")
//
//      val fGet = request.get()
//      val mess = FixedType.write(APITEST.Test1("ja", "JA"))
//      val fPost = request.post(PlainTextBody(mess))
//
//      val request2 = HttpRequest(url).withPath("/operation")
//      val fGet2 = request2.get()

//      for {
//        res <- fGet
//        p <- fPost
//        get2 <- fGet2
//      } yield {
//
//
//
//
//
//
//
//
////        val test1 = FixedType.read[APITEST.API](res.body)
////        val test1_js = upickle.json.read(res.body)
////
////        val tt = FixedType.readJs[APITEST.API](test1_js)
////
////        val test2 = FixedType.read[Hej](res.body)
////        val test3 = Try{FixedType.read[Hej2](res.body)}
////
////        val test4 = scalajs.js.JSON.parse(res.body)
////
////        val kalle = Try{upickle.default.readJs[Hej](upickle.json.readJs(test4))}
////
////        val op = FixedType.read[Operation](get2.body)
////
////        val t5 = Try{upickle.default.readJs[Hej](op.attributes.obj("test"))}
////
////        val opD = scalajs.js.JSON.parse(get2.body)
////
////
//        Callback.log("tjo")
////          Callback.alert(test1.toString) >>
////          Callback.alert(test2.toString) >>
////          Callback.alert(test3.toString) >>
////          Callback.alert(op.toString) >>
////          Callback.alert(t5.toString) >>
////          Callback.alert(kalle.toString) >>
////          Callback.alert(opD.name.toString) >>
////          Callback.alert(s"dynamic: ${test4.p1}") >>
////          Callback.alert(s"dynamic try: ${Try{test4.p5}}") >>
////          Callback.alert(p.body)
//      }


    }

  }


  private val component = ReactComponentB[Unit]("WidgetInjectionTest")
    .initialState(State("HEJ"))
    .renderBackend[Backend]
      .componentWillUnmount(_.backend.onUnmount())
    .build

  def apply(): ReactElement = component()
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
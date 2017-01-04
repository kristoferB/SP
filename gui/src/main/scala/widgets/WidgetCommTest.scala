package spgui.widgets

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.concurrent.Future
import scala.concurrent.Promise
import fr.hmil.roshttp.HttpRequest
import monix.execution.Scheduler.Implicits.global

import scala.util.{Failure, Success}
import fr.hmil.roshttp.response.SimpleHttpResponse

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


object WidgetCommTest {

  //type Comp = ReactComponentU[Unit, Unit, Unit, Element]
  case class State(str: String)

  private class Backend($: BackendScope[Unit, State]) {
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

    def updateMe(e: ReactEventI): Callback = {
      changeState(e.target.value)
    }

    def send(): Future[Callback] = {
      HoHo
    }

    def HoHo = {
      import fr.hmil.roshttp.body._
      val request = HttpRequest("http://localhost:8080/test")
      val fGet = request.get()
      val mess = FixedType.write(APITEST.Test1("ja", "JA"))
      val fPost = request.post(PlainTextBody(mess))

      for {
        res <- fGet
        p <- fPost
      } yield {
        val test1 = FixedType.read[APITEST.API](res.body)
        Callback.log("tjo") >>
          Callback.alert(test1.toString) >>
          Callback.alert(p.body)
      }


    }

  }


  private val component = ReactComponentB[Unit]("WidgetInjectionTest")
      .initialState(State("HEJ"))
    .renderBackend[Backend]
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
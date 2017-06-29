package spgui.widgets.injection

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import spgui.SPWidget

object WidgetInjectionTest {

  case class State(component: VdomElement)

  class Backend($: BackendScope[Unit, State]) {
    def changeState(element: VdomElement): Callback = $.setState(State(element))
    def render(s: State) =
      <.div(
        s.component,
        <.button(
          "fetch sp-example-widget.js",
          ^.onClick --> fetchAndSwitchComp()
        )
      )

    // Removed this for now
    def fetchAndSwitchComp(): Callback = {
      // this should maybe actually be a singleton
      Callback.empty
//      val componentLoaderService = new ComponentLoaderService()
//
//      // trying out the loader, for now using a hard link
//      componentLoaderService.fetchFactory(
//        "http://localhost:8080/api/widget/sp-example-widget.js", "TestWidget")
//        .map(CompFactory => {
//              val widget = CompFactory(WrapObj(Unit))
//              //val widgetWrapper = new WidgetWrapper(widget)
//              changeState(widget)
//            }
//        )
      }
    }

  private val placeholderText: String = "This is a placeholder component. " +
    "To replace it with sp-simple-widget, a component unknown to this app so far, " +
    "serve it with http-server -p 8080 --cors sp-example-widget/ " +
    "(having installed it with npm install http-server -g), then press fetch."
  private val PlaceholderComp = ScalaComponent.builder[Unit]("PlaceholderComp")
    .render(_ => <.h4(placeholderText))
    .build

  private val component = ScalaComponent.builder[Unit]("WidgetInjectionTest")
    .initialState(State(PlaceholderComp()))
    .renderBackend[Backend]
    .build

  def apply() = SPWidget(spwb => component())
}

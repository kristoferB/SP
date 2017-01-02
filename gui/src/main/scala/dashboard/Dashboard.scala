package spgui.dashboard

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import diode.react.ModelProxy

//import spgui.Grid.ReactGridLayoutFacade

object Dashboard {
  case class Props(proxy: ModelProxy[List[ReactElement]])

  class Backend($: BackendScope[Props, Unit]) {
    def render(p: Props) =
      <.div(
        "dashboard (gridstuff commented out)"
        /*
        ReactGridLayoutFacade(width=1920,
                              onLayoutChange = _ => println("hej")
        ).apply(
          for((w,i) <- p.proxy().zipWithIndex) yield <.div(^.key := i, w)
        )
         */
    )
  }

  private val component = ReactComponentB[Props]("Dashboard")
    .renderBackend[Backend]
    .build

  def apply(proxy: ModelProxy[List[ReactElement]]) = component(Props(proxy))
}


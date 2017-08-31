//package spgui.widgets.Kandidat
//
//  import java.util.UUID
//
//  import japgolly.scalajs.react._
//  import japgolly.scalajs.react.vdom.prefix_<^._
//  import sp.domain.Logic._
//  import spgui.SPWidget
//  import org.scalajs.dom.raw
//  import org.singlespaced.d3js.d3
//  import org.singlespaced.d3js.Ops._
//  import sp.domain._
//  import spgui.communication._
//
//package API_LOS{
//  sealed trait API_LOS
//  case class sendThings(things: List[String], things2: List[String]) extends API_LOS
//  object attributes {
//    val service = "LOS"
//  }
//}
//object ErrorHandler{
//  case class State(things: List[String], things2: List[String])
//  private class Backend($: BackendScope[Unit, State]) {
//
//    val messObs = BackendCommunication.getMessageObserver(
//      mess => {
//        //println(s"The widget example got: $mess" +s"parsing: ${mess.getBodyAs[api.API_ExampleService]}")
//        mess.getBodyAs[API_LOS.sendThings].map { x =>
//         // println("000000000000000000000000000000000000000000")
//          //println(x)
//          //$.modState(s => s.copy(things = x.things)).runNow()
//          //$.modState(s => s.copy(things2 = x.things2)).runNow()
//          $.modState(s => State(things = x.things, things2 = x.things2)).runNow()
//         // println("000000000000000000000000000000000000000000")
//        }
//
//      },
//      "error" // the topic you want to listen to. Soon we will also add some kind of backend filter,  but for now you get all answers
//    )
//
//    def render(s: State) = {
//      <.div(
//        <.h2("Error Handler"),
//        <.br(),
//        <.table(
//          ^.width:="800px",
//          <.caption("States & Values"),
//          <.thead(
//            <.tr(
//              <.th(^.width:="200px","Name PLC"),
//              <.th(^.width:="200px","State PLC"),
//              <.th(^.width:="200px","Name Model"),
//              <.th(^.width:="200px","State Model")
//            )
//        ),
//        <.tbody(
//          <.tr(
//            <.td(s.things(0)),
//            <.td(s.things(1)),
//            <.td(s.things2(0)),
//            <.td(s.things2(1))
//          ),
//          <.tr(
//            <.td(s.things(2)),
//            <.td(s.things(3)),
//            <.td(s.things2(2)),
//            <.td(s.things2(3))
//          ),
//          <.tr(
//            <.td(s.things(4)),
//            <.td(s.things(5)),
//            <.td(s.things2(4)),
//            <.td(s.things2(5))
//          )
//        )
//      )
//      )
//      //println(s)
//    }
//    def onUnmount() = {
//      println("Unmounting")
//      messObs.kill()
//      Callback.empty
//    }
//
//  }
//
//  private val component = ReactComponentB[Unit]("ErrorHandler")
//    .initialState(State(things = List("a", "a", "a", "a", "a", "a"),things2 = List("", "", "", "", "", "")))
//    .renderBackend[Backend]
//    .componentWillUnmount(_.backend.onUnmount())
//    .build
//
//  def apply() = spgui.SPWidget(spwb => component())
//
//  }
///*
//  def sendToAB(mess: abapi.Request): Callback = {
//    val h = SPHeader(from = "AbilityHandlerWidget", to = abapi.attributes.service,
//    reply = SPValue("AbilityHandlerWidget"), reqID = java.util.UUID.randomUUID())
//    val json = SPMessage(*(h), *(mess))
//    BackendCommunication.publish(json, "services")
//    Callback.empty
//  }
//
//object D3BarsComponent {
//  def apply(data: List[String]) = component(data)
//
//  private val component = ReactComponentB[List[String]]("d3DivComponent")
//    .render(_ => <.div())
//    .componentDidUpdate(dcb => Callback(addTheD3(ReactDOM.findDOMNode(dcb.component), dcb.currentProps)))
//    .build
//
//
//  val colorMap = List(
//    "#00ff00",
//    "#00ff00",
//    "#00ff00"
//  )
//  val fault1 = List(
//    "#00ff00",
//    "#ff0000",
//    "#00ff00"
//  )
//  val fault2 = List(
//    "#ff0000",
//    "#00ff00"
//  )
//  private def addTheD3(element: raw.Element, list: List[String]): Unit = {
//    val graphHeight = 440
//    val barWidth = 45
//    val frameHeight = 100
//    val horizontalBarDistance = barWidth + 100
//    val barHeightMultiplier = graphHeight / frameHeight
//
//    // clear all before rendering new data
//    // plain-js d3-examples online don't have this line, adding it is a way
//    // to let react take care of the rerendering, rather than d3 itself
//    d3.select(element).selectAll("*").remove()
//    val svg = d3.select(element).append("svg")
//      .attr("width", horizontalBarDistance)
//      .attr("height", graphHeight)
//    val g = svg.append("g")
//
//    g.append("rect")
//      .attr("x", 5)
//      .attr("y", 5)
//      .attr("width", barWidth)
//      .attr("height", barWidth*1.5)
//      .attr("fill", list(0))
//
//    svg.append("text")
//      .attr("x", 5)
//      .attr("y",5)
//      .attr("font-size",10)
//      .text("P1")
//
//
//    g.append("rect")
//      .attr("x", 5)
//      .attr("y", 75)
//      .attr("width", barWidth)
//      .attr("height", barWidth*1.5)
//      .attr("fill", list(1))
//
//    g.append("rect")
//      .attr("x", 5)
//      .attr("y", 145)
//      .attr("width", barWidth)
//      .attr("height", barWidth*1.5)
//      .attr("fill", list(2))
//  }
//
//  private val component = ReactComponentB[Unit]("ErrorHandler")
//    .initialState(colorMap)
//    .render{dcb =>
//      <.div(
//        SPButton("mod state", Seq(^.onClick --> dcb.modState(_ => colorMap))),
//        SPButton("mod state", Seq(^.onClick --> dcb.modState(_ => fault1))),
//        D3BarsComponent(dcb.state)
//      )
//    }
//    .build
//  */
//

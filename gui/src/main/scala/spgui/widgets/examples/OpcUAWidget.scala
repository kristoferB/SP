package spgui.widgets.examples

import java.util.UUID
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import spgui.communication._
import sp.domain._

// Copy paste the APIs you want to communicate with here
sealed trait API_OpcUARuntime
object API_OpcUARuntime {
  // requests
  case class Connect(url: String) extends API_OpcUARuntime
  case class Disconnect() extends API_OpcUARuntime
  case class GetNodes() extends API_OpcUARuntime
  case class Subscribe(nodeIDs: List[String]) extends API_OpcUARuntime
  case class Write(node: String, value: SPValue) extends API_OpcUARuntime

  // answers
  case class ConnectionStatus(connected: Boolean) extends API_OpcUARuntime
  case class AvailableNodes(nodes: Map[String, String]) extends API_OpcUARuntime
  case class StateUpdate(state: Map[String, SPValue], timeStamp: String) extends API_OpcUARuntime

  val service = "OpcUARuntime"
}



object OpcUAWidget {
  implicit val readWriter: ReadWriter[API_OpcUARuntime] =
    macroRW[API_OpcUARuntime.Connect] merge macroRW[API_OpcUARuntime.Disconnect] merge
      macroRW[API_OpcUARuntime.GetNodes] merge macroRW[API_OpcUARuntime.Subscribe] merge
        macroRW[API_OpcUARuntime.Write] merge macroRW[API_OpcUARuntime.ConnectionStatus] merge
          macroRW[API_OpcUARuntime.AvailableNodes] merge macroRW[API_OpcUARuntime.StateUpdate]

  case class Node(name: String, datatype: String)
  case class State(url: String, connected: Boolean, nodes: List[Node], opcState: Map[String, SPValue],
    internalValues: Map[String, String])

  private class Backend($: BackendScope[Unit, State]) {
    val messObs = BackendCommunication.getMessageObserver(
      mess => {
        val testing = fromSPValue[API_OpcUARuntime](mess.body).map{
          case API_OpcUARuntime.ConnectionStatus(connected) =>
            $.modState(s => s.copy(connected = connected, nodes = (if(!connected) List() else s.nodes))).runNow()
          case API_OpcUARuntime.AvailableNodes(nodes) =>
            val nn = nodes.map{case (n,dt) => Node(n, dt)}.toList
            $.modState(s => s.copy(nodes = nn)).runNow()
          case API_OpcUARuntime.StateUpdate(state, timestamp) =>
            $.modState(s => s.copy(opcState = state)).runNow()
          case x =>
            println(s"OpcUAWidget - TODO: $x")
        }
      },
      "answers"   // the topic you want to listen to. Soon we will also add some kind of backend filter,  but for now you get all answers
    )

    def urlChange(s: State)(e: ReactEventI) = {
      $.modState(s=>s.copy(url = e.target.value)).runNow()
      Callback.empty
    }

    def render(s: State) = {
      <.div(
        <.h2("Connected? " + (if(s.connected) "yes" else "no")),
        <.br(),
        <.input(
          ^.value     := s.url,
          ^.onChange ==> urlChange(s)
        ),
        <.button(
          ^.className := "btn btn-default",
          ^.onClick --> send(API_OpcUARuntime.Connect(s.url)), "Connect"
        ),
        <.button(
          ^.className := "btn btn-default",
          ^.onClick --> send(API_OpcUARuntime.Disconnect()), "Disconnect"
        ),
        <.button(
          ^.className := "btn btn-default",
          ^.onClick --> send(API_OpcUARuntime.GetNodes()), "Get nodes"
        ),
        <.button(
          ^.className := "btn btn-default",
          ^.onClick --> {println("SUB"); println(s.nodes.map(n=>n.name).toList); send(API_OpcUARuntime.Subscribe(s.nodes.map(n=>n.name).toList)) }, "Subscribe to all"
        ),
        renderNodes(s)
      )
    }

    def updateInternalValue(s: State, k: String)(e: ReactEventI) = {
      val newInternalValues = s.internalValues + (k -> e.target.value)
      $.modState(s=>s.copy(internalValues = newInternalValues)).runNow()
      Callback.empty
    }

    def renderNodes(s: State) = {
      <.table(
        ^.width:="900px",
        <.caption("OPCUA Nodes"),
        <.thead(
          <.tr(
            <.th(^.width:="400px","Node Identifier"),
            <.th(^.width:="50px","Datatype"),
            <.th(^.width:="200px","Value"),
            <.th(^.width:="150px","Write")
          )
        ),
        <.tbody(
          s.nodes.map(n=> {
            val value = s.opcState.get(n.name).getOrElse(*(0))
            val internalValue = s.internalValues.get(n.name).getOrElse("")
            <.tr(
              <.td(n.name),
              <.td(n.datatype),
              <.td(value.toString),
              <.td(
                <.input(
                  ^.width := "80px",
                  ^.value     := internalValue,
                  ^.onChange ==> updateInternalValue(s, n.name)
                ),
                <.button(
                  ^.width := "70px",
                  ^.className := "btn btn-small",
                  ^.onClick --> send(API_OpcUARuntime.Write(n.name, changeType(n.datatype, internalValue))), "write"
                )
              ))
          })
        )
      )
    }

    def changeType(dt: String, v: String) = {
      dt match {
        case "String" => toSPValue[String](v)
        case "Integer" => toSPValue[Int](v.toInt)
        case "Boolean" =>
          val b = v.toLowerCase == "true" || v == "1"
          toSPValue[Boolean](b)
        case "Long" => toSPValue[Long](v.toLong)
        case "Short" => toSPValue[Short](v.toShort)
        case "Double" => toSPValue[Double](v.toDouble)
      }
    }

    def onUnmount() = {
      println("Unmounting")
      messObs.kill()
      send(API_OpcUARuntime.Disconnect())
      Callback.empty
    }

    def send(mess: API_OpcUARuntime): Callback = {
      val h = SPHeader("OpcUAWidget", API_OpcUARuntime.service, "OpcUAWidget", java.util.UUID.randomUUID())
      val json = SPMessage(*(h), *(mess)) // *(...) is a shorthand for toSpValue(...)
      // BackendCommunication.publishMessage("services", json)
      BackendCommunication.publish(json, "services")
      Callback.empty
    }
  }

  private val component = ReactComponentB[Unit]("OpcUAWidget")
    .initialState(State(url="opc.tcp://localhost:12686", connected=false, nodes = List(), opcState=Map(),
      internalValues=Map()))
    .renderBackend[Backend]
    .componentWillUnmount(_.backend.onUnmount())
    .build

  def apply() = spgui.SPWidget(spwb => component())
}

package spgui.widgets.examples

import java.util.UUID
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import spgui.communication._
import sp.domain._
import sp.domain.Logic._

// Copy paste the APIs you want to communicate with here
object APIOpcUARuntime {
  sealed trait Request
  sealed trait Response
  val service = "OpcUARuntime"

  // requests
  case class Connect(url: String) extends Request
  case object Disconnect extends Request
  case object GetNodes extends Request
  case class Subscribe(nodeIDs: List[String]) extends Request
  case class Write(node: String, value: SPValue) extends Request

  // answers
  case class ConnectionStatus(connected: Boolean) extends Response
  case class AvailableNodes(nodes: Map[String, String]) extends Response
  case class StateUpdate(state: Map[String, SPValue], timeStamp: String) extends Response


  object Request {
    implicit lazy val fOPCUARuntimeRequest: JSFormat[Request] = deriveFormatISA[Request]
  }
  object Response {
    implicit lazy val fOPCUARuntimeResponse: JSFormat[Response] = deriveFormatISA[Response]
  }
}
import spgui.widgets.examples.{APIOpcUARuntime => api}

object OpcUAWidget {
  case class Node(name: String, datatype: String)
  case class State(url: String, connected: Boolean, nodes: List[Node], opcState: Map[String, SPValue],
    internalValues: Map[String, String])

  private class Backend($: BackendScope[Unit, State]) {
    val messObs = BackendCommunication.getMessageObserver(
      mess => {
        val testing = mess.body.to[api.Response].foreach{
          case api.ConnectionStatus(connected) =>
            $.modState(s => s.copy(connected = connected, nodes = (if(!connected) List() else s.nodes))).runNow()
          case api.AvailableNodes(nodes) =>
            val nn = nodes.map{case (n,dt) => Node(n, dt)}.toList
            $.modState(s => s.copy(nodes = nn)).runNow()
          case api.StateUpdate(state, timestamp) =>
            $.modState(s => s.copy(opcState = state)).runNow()
          case x =>
            println(s"OpcUAWidget - TODO: $x")
        }
      },
      "answers"   // the topic you want to listen to. Soon we will also add some kind of backend filter,  but for now you get all answers
    )

    def urlChange(s: State)(e: ReactEventFromInput) = {
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
          ^.onClick --> send(api.Connect(s.url)), "Connect"
        ),
        <.button(
          ^.className := "btn btn-default",
          ^.onClick --> send(api.Disconnect), "Disconnect"
        ),
        <.button(
          ^.className := "btn btn-default",
          ^.onClick --> send(api.GetNodes), "Get nodes"
        ),
        <.button(
          ^.className := "btn btn-default",
          ^.onClick --> {println("SUB"); println(s.nodes.map(n=>n.name).toList); send(api.Subscribe(s.nodes.map(n=>n.name).toList)) }, "Subscribe to all"
        ),
        renderNodes(s)
      )
    }

    def updateInternalValue(s: State, k: String)(e: ReactEventFromInput) = {
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
            val value = s.opcState.get(n.name).getOrElse(SPValue(0))
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
                  ^.onClick --> send(api.Write(n.name, changeType(n.datatype, internalValue))), "write"
                )
              ))
          }).toTagMod
        )
      )
    }

    def changeType(dt: String, v: String) = {
      dt match {
        case "String" => SPValue[String](v)
        case "Integer" => SPValue[Int](v.toInt)
        case "Boolean" =>
          val b = v.toLowerCase == "true" || v == "1"
          SPValue[Boolean](b)
        case "Long" => SPValue[Long](v.toLong)
        case "Short" => SPValue[Short](v.toShort)
        case "Double" => SPValue[Double](v.toDouble)
      }
    }

    def onUnmount() = {
      println("Unmounting")
      messObs.kill()
      send(api.Disconnect)
      Callback.empty
    }

    def send(mess: api.Request): Callback = {
      val h = SPHeader(from = "OpcUAWidget", to = api.service, reply = SPValue("OpcUAWidget"), reqID = java.util.UUID.randomUUID())
      val json = SPMessage.make(h, mess)
      // BackendCommunication.publishMessage("services", json)
      BackendCommunication.publish(json, "services")
      Callback.empty
    }
  }

  private val component = ScalaComponent.builder[Unit]("OpcUAWidget")
    .initialState(State(url="opc.tcp://localhost:12686", connected=false, nodes = List(), opcState=Map(),
      internalValues=Map()))
    .renderBackend[Backend]
    .componentWillUnmount(_.backend.onUnmount())
    .build

  def apply() = spgui.SPWidget(spwb => component())
}

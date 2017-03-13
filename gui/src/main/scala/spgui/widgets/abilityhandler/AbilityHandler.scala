package spgui.widgets.abilityhandler

import java.util.UUID
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import spgui.communication._
import sp.domain._
import sp.messages._
import sp.messages.Pickles._

package APIVirtualDevice {
  sealed trait Requests
  // requests setup
  case class SetUpDeviceDriver(driver: Driver) extends Requests
  case class SetUpResource(resource: Resource) extends Requests
  case class GetResources() extends Requests

  sealed trait DriverStateMapper
  case class OneToOneMapper(thing: UUID, driverID: UUID, driverIdentifier: String) extends DriverStateMapper

  // requests command (gets a SPACK and when applied, SPDone (and indirectly a StateEvent))
  case class ResourceCommand(resource: UUID, stateRequest: Map[UUID, SPValue], timeout: Int = 0) extends Requests

  // requests from driver
  case class DriverStateChange(name: String, id: UUID, state: Map[String, SPValue], diff: Boolean = false) extends Requests
  case class DriverCommand(name: String, id: UUID, state: Map[String, SPValue]) extends Requests
  case class DriverCommandDone(requestID: UUID, result: Boolean) extends Requests

  // answers
  sealed trait Replies
  case class StateEvent(resource: String, id: UUID, state: Map[UUID, SPValue], diff: Boolean = false) extends Replies

  case class Resources(xs: List[Resource]) extends Replies
  case class Drivers(xs: List[Driver]) extends Replies
  case class NewResource(x: Resource) extends Replies
  case class RemovedResource(x: Resource) extends Replies
  case class NewDriver(x: Driver) extends Replies
  case class RemovedDriver(x: Driver) extends Replies

  case class Resource(name: String, id: UUID, things: Set[UUID], stateMap: List[DriverStateMapper], setup: SPAttributes, sendOnlyDiffs: Boolean = false)
  case class Driver(name: String, id: UUID, driverType: String, setup: SPAttributes)


  object  attributes {
    val service = "virtualDevice"
  }
}
import spgui.widgets.abilityhandler.{APIVirtualDevice => vdapi}

object AbilityHandlerWidget {
  case class State(resources: List[vdapi.Resource])

  private class Backend($: BackendScope[Unit, State]) {
    val messObs = BackendCommunication.getMessageObserver(
      mess => {
        val testing = fromSPValue[vdapi.Replies](mess.body).map{
          case vdapi.Resources(r) =>
            $.modState(s => s.copy(resources = r)).runNow()
          case x =>
            println(s"AbilityHandlerWidget - TODO: $x")
        }
      },
      "answers"   // the topic you want to listen to. Soon we will also add some kind of backend filter,  but for now you get all answers
    )

    def render(s: State) = {
      <.div(
        <.h2("Hej hopp"),
        <.br(),
        <.button(
          ^.className := "btn btn-default",
          ^.onClick --> send(vdapi.GetResources()), "Get resources"
        ),
        renderResources(s)
      )
    }

    def renderResources(s: State) = {
      <.table(
        ^.width:="400px",
        <.caption("Resources"),
        <.thead(
          <.tr(
            <.th(^.width:="100px","Name")
          )
        ),
        <.tbody(
          s.resources.map(r=> {
            <.tr(
              <.td(r.name)
            )
          })
        )
      )
    }

    def onUnmount() = {
      println("Unmounting")
      messObs.kill()
      Callback.empty
    }

    def send(mess: vdapi.Requests): Callback = {
      val h = SPHeader(from = "AbilityHandlerWidget", to = vdapi.attributes.service,
        reply = SPValue("AbilityHandlerWidget"), reqID = java.util.UUID.randomUUID())
      val json = SPMessage(*(h), *(mess))
      BackendCommunication.publish(json, "services")
      Callback.empty
    }
  }

  private val component = ReactComponentB[Unit]("AbilityHandlerWidget")
    .initialState(State(resources = List()))
    .renderBackend[Backend]
    .componentWillUnmount(_.backend.onUnmount())
    .build

  def apply() = spgui.SPWidget(spwb => component())
}

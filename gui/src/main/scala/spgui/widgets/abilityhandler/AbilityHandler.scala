package spgui.widgets.abilityhandler

import java.util.UUID
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import spgui.communication._
import sp.domain._
import sp.messages._
import sp.messages.Pickles._

import spgui.widgets.abilityhandler.{APIVirtualDevice => vdapi}
import spgui.widgets.abilityhandler.{APIAbilityHandler => api}

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

package spgui.widgets.abilityhandler

import java.util.UUID
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import spgui.communication._
import sp.domain._
import sp.messages._
import sp.messages.Pickles._

import spgui.widgets.abilityhandler.{APIVirtualDevice => vdapi}
import spgui.widgets.abilityhandler.{APIAbilityHandler => abapi}

object AbilityHandlerWidget {
  case class State(resources: List[vdapi.Resource], abilities: List[abapi.Ability])

  private class Backend($: BackendScope[Unit, State]) {
    val messObs = BackendCommunication.getMessageObserver(
      mess => {
        val testing = fromSPValue[vdapi.Replies](mess.body).map{
          case vdapi.Resources(r) =>
            $.modState(s => s.copy(resources = r)).runNow()
          case x =>
            println(s"AbilityHandlerWidget - TODO: $x")
        }
        val testing2 = fromSPValue[abapi.Response](mess.body).map{
          case abapi.Abilities(a) =>
            $.modState(s => s.copy(abilities = a)).runNow()
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
          ^.onClick --> sendToVD(vdapi.GetResources()), "Get resources"
        ),
        <.button(
          ^.className := "btn btn-default",
          ^.onClick --> sendToAB(abapi.GetAbilities()), "Get abilities"
        ),
        renderResources(s),
        renderAbilities(s)
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

    def renderAbilities(s: State) = {
      <.table(
        ^.width:="400px",
        <.caption("Abilties"),
        <.thead(
          <.tr(
            <.th(^.width:="100px","Name")
          )
        ),
        <.tbody(
          s.abilities.map(a=> {
            <.tr(
              <.td(a.name)
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

    def sendToVD(mess: vdapi.Requests): Callback = {
      val h = SPHeader(from = "AbilityHandlerWidget", to = vdapi.attributes.service,
        reply = SPValue("AbilityHandlerWidget"), reqID = java.util.UUID.randomUUID())
      val json = SPMessage(*(h), *(mess))
      BackendCommunication.publish(json, "services")
      Callback.empty
    }

    def sendToAB(mess: abapi.Request): Callback = {
      val h = SPHeader(from = "AbilityHandlerWidget", to = abapi.attributes.service,
        reply = SPValue("AbilityHandlerWidget"), reqID = java.util.UUID.randomUUID())
      val json = SPMessage(*(h), *(mess))
      BackendCommunication.publish(json, "services")
      Callback.empty
    }
  }

  private val component = ReactComponentB[Unit]("AbilityHandlerWidget")
    .initialState(State(resources = List(), abilities = List()))
    .renderBackend[Backend]
    .componentWillUnmount(_.backend.onUnmount())
    .build

  def apply() = spgui.SPWidget(spwb => component())
}

package spgui.widgets.abilityhandler

import java.util.UUID
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import spgui.communication._
import sp.domain._
import Logic._

object AbilityHandlerWidget {
  import sp.devicehandler.{APIVirtualDevice => vdapi}
  import sp.abilityhandler.{APIAbilityHandler => abapi}

  case class State(resources: List[vdapi.Resource], abilities: List[abapi.Ability], abilityState: Map[ID, SPValue])

  private class Backend($: BackendScope[Unit, State]) {

    val abObs = BackendCommunication.getWebSocketStatusObserver(  mess => {
      if (mess) sendToAB(abapi.GetAbilities)
    }, abapi.topicResponse)
    val vdObs = BackendCommunication.getWebSocketStatusObserver(  mess => {
      if (mess) sendToVD(vdapi.GetResources)
    }, vdapi.topicResponse)

    val vdapiHandler = BackendCommunication.getMessageObserver(handleVDMess, vdapi.topicResponse)
    val abapiHandler = BackendCommunication.getMessageObserver(handleABMess, abapi.topicResponse)

    def handleVDMess(mess: SPMessage): Unit = {
      mess.body.to[vdapi.Response].map{
        case vdapi.Resources(r) =>
          $.modState(s => s.copy(resources = r)).runNow()
        case x =>
          println(s"AbilityHandlerWidget - TODO: $x")
      }
    }

    def handleABMess(mess: SPMessage): Unit = {
      mess.body.to[abapi.Response].map{
        case abapi.Abilities(a) =>
          $.modState(s => s.copy(abilities = a)).runNow()
        case abapi.AbilityState(id, state) =>
          $.modState{s =>
            val ns = s.abilityState ++ state
            s.copy(abilityState = ns)}.runNow()
        case x =>
          println(s"AbilityHandlerWidget - answers - TODO: $x")
      }
    }

    def render(s: State) = {
      <.div(
        <.h2("Ability Handler"),
        <.br(),
        <.button(
          ^.className := "btn btn-default",
          ^.onClick --> sendToVD(vdapi.GetResources), "Get resources"
        ),
        <.button(
          ^.className := "btn btn-default",
          ^.onClick --> sendToAB(abapi.GetAbilities), "Get abilities"
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
          }).toTagMod
        )
      )
    }

    def getAbilityState(s: SPValue): String = {
      s.getAs[String]("state").getOrElse("")
    }

    def getAbilityCount(s: SPValue): Int = {
      s.getAs[Int]("count").getOrElse(0)
    }

    def renderAbilities(s: State) = {
      <.table(
        ^.width:="550px",
        <.caption("Abilties"),
        <.thead(
          <.tr(
            <.th(^.width:="200px","Name"),
            <.th(^.width:="100px","State"),
            <.th(^.width:="50px","Count"),
            <.th(^.width:="100px","Start"),
            <.th(^.width:="100px","Reset")
          )
        ),
        <.tbody(
          s.abilities.sortBy(a=>a.name).map(a=> {
            <.tr(
              <.td(a.name),
              <.td(getAbilityState(s.abilityState.getOrElse(a.id, SPValue.empty))),
              <.td(getAbilityCount(s.abilityState.getOrElse(a.id, SPValue.empty))),
              <.td(<.button(
                ^.className := "btn btn-sm",
                ^.onClick --> sendToAB(abapi.StartAbility(a.id)), "Start"
              )),
              <.td(<.button(
                ^.className := "btn btn-sm",
                ^.onClick --> sendToAB(abapi.ForceResetAbility(a.id)), "Reset"
              ))
            )
          }).toTagMod
        )
      )
    }

    def onUnmount() = {
      println("Unmounting")
      vdapiHandler.kill()
      abapiHandler.kill()
      Callback.empty
    }

    def sendToVD(mess: vdapi.Request): Callback = {
      val h = SPHeader(from = "AbilityHandlerWidget", to = vdapi.service,
        reply = SPValue("AbilityHandlerWidget"), reqID = java.util.UUID.randomUUID())
      val json = SPMessage.make(SPValue(h), SPValue(mess))
      BackendCommunication.publish(json, vdapi.topicRequest)
      Callback.empty
    }

    def sendToAB(mess: abapi.Request): Callback = {
      val h = SPHeader(from = "AbilityHandlerWidget", to = abapi.service,
        reply = SPValue("AbilityHandlerWidget"), reqID = java.util.UUID.randomUUID())
      val json = SPMessage.make(SPValue(h), SPValue(mess))
      BackendCommunication.publish(json, abapi.topicRequest)
      Callback.empty
    }
  }

  private val component = ScalaComponent.builder[Unit]("AbilityHandlerWidget")
    .initialState(State(resources = List(), abilities = List(), abilityState = Map()))
    .renderBackend[Backend]
    .componentWillUnmount(_.backend.onUnmount())
    .build

  def apply() = spgui.SPWidget(spwb => component())
}

package spgui.widgets.Kandidat

/**
  * Created by christianjern on 2017-05-19.
  */

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import sp.domain.SPValue
import sp.messages.Pickles.{*, SPHeader, SPMessage}
import spgui._
import spgui.communication.BackendCommunication
import spgui.widgets.abilityhandler.APIVirtualDevice

case class prod (id: String, completed: Boolean, process1: Boolean, process2: Boolean, process3: Boolean)

object TestOrder {


  val OrderProd = ReactComponentB[Unit]("Orders")
    //.initialState(getList())
    .render(dcb =>
      <.div(
        <.h1("Order a cylinder"),
        <.ul(<.h4("Cylinder " + getList.toString),
          <.ul(<.input.checkbox(), <.span(" Process 1 ")),
          <.ul(<.input.checkbox(), <.span(" Process 2 ")),
          <.ul(<.input.checkbox(), <.span(" Process 3 "))),
        //^.checked := prod.process1, ^.onChange --> p.stateChange(item.copy(completed = !item.completed))),
        <.br,

        <.button("Make cylinder",
          ^.onClick --> sendToVD(//vdapi.GetResources()//dcb.modState(_ => getList()))
          )
        )
      )
    )
    .build

  def apply() = SPWidget(spwb => <.div(OrderProd("Test")))


  def getList () = {
    val x = 1
    var nr = x + 1
  }
/*
  def sendToVD(mess: vdapi.Requests): Callback = {
    val h = SPHeader(from = "AbilityHandlerWidget", to = vdapi.attributes.service,
      reply = SPValue("AbilityHandlerWidget"), reqID = java.util.UUID.randomUUID())
    val json = SPMessage(*(h), *(mess))
    BackendCommunication.publish(json, "services")
    Callback.empty
  }*/
}
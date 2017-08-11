//package spgui.widgets.Kandidat
//
///**
//  * Created by christianjern on 2017-05-19.
//  */
//
//import japgolly.scalajs.react._
//import japgolly.scalajs.react.vdom.prefix_<^._
//import sp.domain.SPValue
//import spgui._
//import spgui.communication.BackendCommunication
//import scala.concurrent.duration._
////import spgui.widgets.Kandidat.{API_START => api}
//import spgui.widgets.Kandidat.{APILabkitControl => api}
//import sp.domain.Logic._
//import sp.domain._
//import spgui.communication._
//
//case class prod (id: String, completed: Boolean, process1: Boolean, process2: Boolean, process3: Boolean)
//
//package APILabkitControl {
//  sealed trait Request
//  sealed trait Response
//
//  case class SomeNice(string: String) extends Request
//  case class SomeResp() extends Response
//  //case class Start() extends Response
//
//  object attributes {
//    val service = "LabKitControl"
//  }
//}
//object TestOrder {
//  /*var temp = prod("default", true, false, false, false)
//  println(temp)*/
//
//  val OrderProd = ReactComponentB[Unit]("Orders")
//    //.initialState(getList())
//    .render(dcb =>
//    <.div(
//      <.h1("Order a cylinder"),
//      //<.ul(<.h4("Cylinder " + getList.toString),
//        //<.ul(<.input.checkbox(), <.span(" Process 1 ")),
//        //<.ul(<.input.checkbox(), <.span(" Process 2 ")),
//        //<.ul(<.input.checkbox(), <.span(" Process 3 "))),
//      // ^.checked := temp.process1 = true, ^.onChange --> p.stateChange(item.copy(completed = !item.completed))),
//      <.br,
//
//      <.button("Make cylinder",
//        ^.onClick --> sendToStart(api.SomeNice("Starta jävla skit"))
//      )
//    )
//  )
//    .build
//
//  def apply() = SPWidget(spwb => <.div(OrderProd("Test")))
//
//
//  def getList () = {
//    val x = 1
//    var nr = x + 1
//  }
//
//  def sendToStart(mess: api.SomeNice): Callback = {
//    val h = SPHeader(from = "OrderTestWidget", to = api.attributes.service,
//      reply = SPValue("LabKitControl"), reqID = java.util.UUID.randomUUID())
//    val json = SPMessage(*(h), *(mess))
//    BackendCommunication.publish(json, "services")
//    Callback.empty
//  }
//}

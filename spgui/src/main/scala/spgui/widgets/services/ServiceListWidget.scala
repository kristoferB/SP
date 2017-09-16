package spgui.widgets.services

import java.util.UUID
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import spgui.communication._
import sp.domain._
import sp.domain.Logic._

import sp.service.{APIServiceHandler => api}

object ServiceListWidget {
  case class State(services: List[APISP.StatusResponse])

  private class Backend($: BackendScope[Unit, State]) {

    val wsObs = BackendCommunication.getWebSocketStatusObserver(connected => {
      if (connected) sendToHandler(api.GetServices)
    }, api.topicResponse)

    def handelMess(mess: SPMessage): Unit = {
      for {
        h <- mess.getHeaderAs[SPHeader]
        b <- mess.getBodyAs[api.Response]
      } yield {
        val res = b match {
          case api.Services(xs) => $.setState(State(xs))
          case api.ServiceAdded(s) => $.modState(state => State(s :: state.services))
          case api.ServiceRemoved(s) => $.modState(state => State(state.services.filter(x => x.instanceID != s.instanceID)))
        }
        res.runNow()
      }
    }

    val answerHandler = BackendCommunication.getMessageObserver(handelMess, api.topicResponse)

    def render(s: State) = {
      <.div(
        renderServices(s),
        <.div("todo: Add search and sorting. Add possibility to send messaged based on api")
      )
    }

    def renderServices(s: State) = {
      <.table(
        ^.className := "table table-striped",
        <.caption("Services"),
        <.thead(
            <.th("service"),
            <.th("name"),
            <.th("id"),
            <.th("tags"),
            <.th("version")
        ),
        <.tbody(
          s.services.map(s=> {
            <.tr(
              <.td(s.service),
              <.td(s.instanceName),
              <.td(s.instanceID.toString),
              <.td(s.tags.toString),
              <.td(s.version)
            )
          }).toTagMod
        )
      )
    }



    def onUnmount() = {
      answerHandler.kill()
      wsObs.kill()
      Callback.empty
    }

    def onMount() = {
      sendToHandler(api.GetServices)
    }

    def sendToHandler(mess: api.Request): Callback = {
      val h = SPHeader(from = "ServiceListWidget", to = api.service, reply = SPValue("ServiceListWidget"))
      val json = SPMessage.make[SPHeader, api.Request](h, api.GetServices)
      BackendCommunication.publish(json, api.topicRequest)
      Callback.empty
    }

  }

  private val component = ScalaComponent.builder[Unit]("AbilityHandlerWidget")
    .initialState(State(services = List()))
    .renderBackend[Backend]
    .componentDidMount(_.backend.onMount())
    .componentWillUnmount(_.backend.onUnmount())
    .build

  def apply() = spgui.SPWidget(spwb => component())
}

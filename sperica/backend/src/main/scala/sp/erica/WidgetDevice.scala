package sp.erica

import akka.actor._
import sp.domain._
import sp.erica.{API_Patient => apiPatient, API_PatientEvent => api}

import scala.util.Try

class WidgetDevice extends Actor with ActorLogging with sp.service.ServiceSupport {
  subscribe("state-event-topic")
  subscribe("widget-event")

  var localState: Map[String, apiPatient.Patient] = Map()
  var widgetStarted: Boolean = false

  val id = ID.newID
  val getStatus = APISP.StatusResponse(
    service = "WidgetDevice",
    instanceID = Some(id),
    tags = List("erica", "frontend"),
    topicRequest = "widget-event",
    topicResponse = "patient-cards-widget-topic"
  )
  triggerServiceRequestComm(getStatus)

  /**
  Receives incoming messages on the AKKA-bus
  */
  def receive = {
    case x: String =>
      for {
        m <- SPMessage.fromJson(x)
        b <- m.getBodyAs[api.Event]
      } yield {
        val header = SPHeader(from = "widgetService")
        b match {
          case api.State(state) => {
            if (localState != state){
              localState = state
              if (widgetStarted) {
                sendEvent(header, api.State(state))
              }
            }
          }
          case api.GetState => {
            widgetStarted = true
            sendEvent(header, api.State(localState))
          }
          case _ => log.warning("Unexpected SP-message in matchRequests: " + b)
        }
      }
  }

  /**
  Publishes a SPMessage on bus with header and body.
  */
  def sendEvent(header: SPHeader, body: api.Event) {
    val toSend = SPMessage.makeJson(header, body)
    publish("patient-cards-widget-topic", toSend)
  }

  }

  object WidgetDevice {
    def props = Props(classOf[WidgetDevice])
  }

package spgui.widgets.models

import java.util.UUID
import japgolly.scalajs.react._

import japgolly.scalajs.react.vdom.all.{ a, h1, h2, href, div, className, onClick, br, key }
import japgolly.scalajs.react.vdom.html_<^._

import sp.domain._
import spgui.communication._
import sp.domain.Logic._

object TestModel {
  def getTestModel: List[IDAble] = {
    List(
      Operation("TestOp1"),
      Operation("TestOp2"),
      Thing("TestThing1"),
      Thing("TestThing2")
    )
  }
}

object ModelsWidget {
  case class State(models: List[ID])

  private class Backend($: BackendScope[Unit, State]) {
    import sp.models.{APIModelMaker => mmapi}
    import sp.models.{APIModel => mapi}

    def extractMMResponse(m: SPMessage) = for {
      h <- m.getHeaderAs[SPHeader]
      b <- m.getBodyAs[mmapi.Response]
    } yield (h, b)

    def extractMResponse(m: SPMessage) = for {
      h <- m.getHeaderAs[SPHeader]
      b <- m.getBodyAs[mapi.Response]
    } yield (h, b)


    def makeMess(h: SPHeader, b: mmapi.Request) = SPMessage.make[SPHeader, mmapi.Request](h, b)
    def makeMess(h: SPHeader, b: mapi.Request) = SPMessage.make[SPHeader, mapi.Request](h, b)
    def makeMess(h: SPHeader, b: APISP) = SPMessage.make[SPHeader, APISP](h, b)

    def handleMess(mess: SPMessage): Unit = {
      println("handlemess: " + mess)
      extractMMResponse(mess).map{ case (h, b) =>
        val res = b match {
          case mmapi.ModelList(models) =>
            println("Got model list")
            $.setState(State(models))
          case mmapi.ModelCreated(name, attr, modelid) =>
            println("Model created")
            sendToHandler(mapi.PutItems(TestModel.getTestModel))
            $.modState(s => State(modelid :: s.models))
          case mmapi.ModelDeleted(modelid) =>
            $.modState(s => State(s.models.filterNot(_ == modelid)))
        }
        res.runNow()
      }
      extractMResponse(mess).map{ case (h, b) =>
        // val res = b match {
        //   case mapi.ModelList(models) => $.setState(State(models))
        // }
        // res.runNow()
      }
    }

    val wsObs = BackendCommunication.getWebSocketStatusObserver(  mess => {
      if (mess) sendToHandler(mmapi.GetModels)
    }, "services")




    val answerHandler = BackendCommunication.getMessageObserver(handleMess, "answers")
    val speventHandler = BackendCommunication.getMessageObserver(handleMess, "spevents")

    def renderModels(models: List[ID]) = {
      <.table(
        ^.className := "table table-striped",
        <.caption("Models"),
        <.thead(
          <.th("id"),
          <.th("delete")
        ),
        <.tbody(
          models.map(m=> {
            <.tr(
              <.td(m.toString),
              <.td(
                <.button(
                  ^.className := "btn btn-sm",
                  ^.onClick --> sendToHandler(mmapi.DeleteModel(m)), "X"
                )
              )
            )
          }).toTagMod
        )
      )
    }

    def render(state: State) = {
      <.div(
        <.h2("Models : " + state.models.size),
        renderModels(state.models),
        <.button(
          ^.className := "btn btn-default",
          ^.onClick --> sendToHandler(mmapi.CreateModel("testmodel")), "Create test model"
        )
      )
    }

    def sendToHandler(mess: mmapi.Request): Callback = {
      val h = SPHeader(from = "ModelWidget", to = mmapi.service,
        reply = SPValue("ModelWidget"), reqID = java.util.UUID.randomUUID())
      val json = makeMess(h, mess)
      BackendCommunication.publish(json, "services")
      Callback.empty
    }

    def sendToHandler(mess: mapi.Request): Callback = {
      val h = SPHeader(from = "ModelWidget", to = mapi.service,
        reply = SPValue("ModelWidget"), reqID = java.util.UUID.randomUUID())
      val json = makeMess(h, mess)
      BackendCommunication.publish(json, "services")
      Callback.empty
    }

    def onUnmount() = {
      answerHandler.kill()
      speventHandler.kill()
      Callback.empty
    }

  }

  private val component = ScalaComponent.builder[Unit]("ModelsWidget")
    .initialState(State(models = List()))
    .renderBackend[Backend]
//    .componentDidMount(_.backend.onMount())
    .componentWillUnmount(_.backend.onUnmount())
    .build

  def apply() = spgui.SPWidget(spwb => component())
}

package spgui.widgets.models

import java.util.UUID
import japgolly.scalajs.react._

import japgolly.scalajs.react.vdom.all.{ a, h1, h2, href, div, className, onClick, br, key }
import japgolly.scalajs.react.vdom.html_<^._

import sp.domain._
import spgui.communication._
import sp.domain.Logic._

import monocle.macros._
import monocle.Lens


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

  @Lenses case class UIState(historyExpanded: Set[ID], shownIdables: List[IDAble])
  @Lenses case class ModelState(models: List[ID], modelInfo: Map[ID,mapi.ModelInformation], modelHistory: Map[ID, mapi.ModelHistory])
  @Lenses case class State(modelState: ModelState, uiState: UIState)

  private class Backend($: BackendScope[Unit, State]) {

    def mL[T](lens: Lens[ModelState, T]): Lens[State, T] = (State.modelState composeLens lens)
    def uiL[T](lens: Lens[UIState, T]): Lens[State, T] = (State.uiState composeLens lens)

    def handleMess(mess: SPMessage): Unit = {
      println("handlemess: " + mess)
      extractMMResponse(mess).map{ case (h, b) =>
        val res = b match {
          case mmapi.ModelList(models) =>
            println("Got model list")
            models.foreach { m => sendToModel(m, mapi.GetModelInfo) }
            models.foreach { m => sendToModel(m, mapi.GetModelHistory) }
            $.modState(s => mL(ModelState.models).set(models)(s))
          case mmapi.ModelCreated(name, attr, modelid) =>
            println("Model created")
            sendToModel(modelid, mapi.PutItems(TestModel.getTestModel))
            $.modState(s => mL(ModelState.models).modify(modelid :: _)(s))
          case mmapi.ModelDeleted(modelid) =>
            $.modState(s => mL(ModelState.models).modify(_.filterNot(_ == modelid))(s))
          case x => Callback.empty
        }
        res.runNow()
      }
      extractMResponse(mess).map{ case (h, b) =>
        val res = b match {
          case mi@mapi.ModelInformation(name, id, version, noitems, attributes) =>
            $.modState(s=>mL(ModelState.modelInfo).modify(_ + (id -> mi))(s))
          case mh@mapi.ModelHistory(id, history) =>
            $.modState(s=>mL(ModelState.modelHistory).modify(_ + (id -> mh))(s))
          case mapi.ModelUpdate(modelid, version, noitems, updatedItems, deletedItems, info) =>
            // fetch new version history
            sendToModel(modelid, mapi.GetModelHistory)
            $.modState{ s =>
              mL(ModelState.modelInfo).modify(modelinfo => {
                val info = modelinfo.get(modelid)
                modelinfo ++ info.map(info => (modelid -> mapi.ModelInformation(info.name, info.id, version, noitems, info.attributes)))
              })(s)
            }
          case tm@mapi.SPItems(items) =>
            $.modState(s=>uiL(UIState.shownIdables).set(items)(s))
          case x => Callback.empty
        }
        res.runNow()
      }
    }

    val wsObs = BackendCommunication.getWebSocketStatusObserver(  mess => {
      if (mess) sendToHandler(mmapi.GetModels)
    }, "services")

    val answerHandler = BackendCommunication.getMessageObserver(handleMess, "answers")
    val speventHandler = BackendCommunication.getMessageObserver(handleMess, "spevents")

    def renderModels(s: State) = {
      <.table(
        ^.className := "table table-striped",
        <.caption("Models"),
        <.thead(
          <.th("id"),
          <.th("name"),
          <.th("version"),
          <.th("number of items"),
          <.th("put dummy items"),
          <.th("preview"),
          <.th("delete")
        ),
        <.tbody(
          s.modelState.models.map(m=> {
            List(
              <.tr(
                <.td(
                  (if(s.uiState.historyExpanded.contains(m))
                    <.button(^.className := "btn btn-sm",
                      ^.onClick --> $.modState(s=>uiL(UIState.historyExpanded).modify(_ - m)(s)), "-")
                  else
                    <.button(^.className := "btn btn-sm",
                      ^.onClick --> $.modState(s=>uiL(UIState.historyExpanded).modify(_ + m)(s)), "+")
                  ),
                  m.toString
                ),
                <.td(s.modelState.modelInfo.get(m).map(_.name).getOrElse("").toString),
                <.td(s.modelState.modelInfo.get(m).map(_.version).getOrElse(-1).toString),
                <.td(s.modelState.modelInfo.get(m).map(_.noOfItems).getOrElse(-1).toString),
                <.td(
                  <.button(
                    ^.className := "btn btn-sm",
                    ^.onClick --> sendToModel(m, mapi.PutItems(TestModel.getTestModel)), "+"
                  )
                ),
                <.td(
                  <.button(
                    ^.className := "btn btn-sm",
                    ^.onClick --> sendToModel(m, mapi.GetItemList()), "P"
                  )
                ),
                <.td(
                  <.button(
                    ^.className := "btn btn-sm",
                    ^.onClick --> sendToHandler(mmapi.DeleteModel(m)), "X"
                  )
                )
              ),
              if(s.uiState.historyExpanded.contains(m))
                <.tr(<.td(^.colSpan := 42, renderHistoryTable(s,m)))
              else ""
            ) : List[TagMod]
          }).flatten.toTagMod
        )
      )
    }

    def renderHistoryTable(s: State, m: ID) = {
      val hist = s.modelState.modelHistory.get(m).map(_.history).getOrElse(List())
      <.table(
        ^.className := "table table-striped",
        <.caption("History"),
        <.thead(
          <.th("Version"),
          <.th("Info"),
          <.th("Revert")
        ),
        <.tbody(
          hist.map(h =>
            <.tr(
              <.td(h._1),
              <.td(h._2.getAs[String]("info").getOrElse("no info").toString),
              <.td(
                <.button(
                    ^.className := "btn btn-sm",
                    ^.onClick --> sendToModel(m,mapi.RevertModel(h._1)), "<<"
                  )
              ))).toTagMod
        ))
    }

    def renderModelPreview(s: State): TagMod = {
      if(s.uiState.shownIdables.nonEmpty)
        <.table(
          ^.className := "table table-striped",
          <.caption("Model Preview"),
          <.thead(
            <.th("Type"),
            <.th("Name"),
            <.th("ID")
          ),
          <.tbody(
            s.uiState.shownIdables.map(i =>
              <.tr(
                <.td(i.getClass.getSimpleName),
                <.td(i.name),
                <.td(i.id.toString)
              )).toTagMod
          ))
      else
        ""
    }

    def render(state: State) = {
      <.div(
        <.h2("Models : " + state.modelState.models.size),
        renderModels(state),
        <.button(
          ^.className := "btn btn-default",
          ^.onClick --> sendToHandler(mmapi.CreateModel("testmodel")), "Create test model"
        ),
        renderModelPreview(state)
      )
    }

    def sendToHandler(mess: mmapi.Request): Callback = {
      val h = SPHeader(from = "ModelWidget", to = mmapi.service,
        reply = SPValue("ModelWidget"), reqID = java.util.UUID.randomUUID())
      val json = makeMess(h, mess)
      BackendCommunication.publish(json, "services")
      Callback.empty
    }

    def sendToModel(model: ID, mess: mapi.Request): Callback = {
      val h = SPHeader(from = "ModelWidget", to = model.toString,
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

    def onMount() = {
      Callback.empty
    }

  }

  val initialUIState = UIState(Set(), shownIdables = List())
  val initialModelState = ModelState(models = List(), modelInfo = Map(), modelHistory = Map())
  val initialState = State(initialModelState, initialUIState)
  private val component = ScalaComponent.builder[Unit]("ModelsWidget")
    .initialState(initialState)
    .renderBackend[Backend]
    .componentDidMount(_.backend.onMount())
    .componentWillUnmount(_.backend.onUnmount())
    .build

  def apply() = spgui.SPWidget(spwb => component())
}

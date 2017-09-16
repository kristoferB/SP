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
    val ops = List(Operation("TestOp1"), Operation("TestOp2"), Operation("TestOp3"))
    val things = List(Thing("TestThing1"), Thing("TestThing2"))
    val sops = List(
      SOPSpec("TestSop1", List(Sequence(List(SOP(ops(0)), SOP(ops(1)), SOP(ops(2)))))),
      SOPSpec("TestSop2", List(Parallel(List(SOP(ops(0)), SOP(ops(1)), SOP(ops(2))))))
    )
    ops ++ things ++ sops
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
  @Lenses case class ModelState(models: Set[ID], modelInfo: Map[ID,mapi.ModelInformation], modelHistory: Map[ID, mapi.ModelHistory])
  @Lenses case class State(modelState: ModelState, uiState: UIState)

  private class Backend($: BackendScope[Unit, State]) {

    def mL[T](lens: Lens[ModelState, T]): Lens[State, T] = (State.modelState composeLens lens)
    def uiL[T](lens: Lens[UIState, T]): Lens[State, T] = (State.uiState composeLens lens)

    def handleMess(mess: SPMessage): Unit = {
      println("handlemess: " + mess)
      extractMMResponse(mess).map{ case (h, b) =>
        val res = b match {
          case mmapi.ModelList(models) =>
            models.foreach { m => sendToModel(m, mapi.GetModelInfo) }
            models.foreach { m => sendToModel(m, mapi.GetModelHistory) }
            $.modState(s => mL(ModelState.models).set(models.toSet)(s))
          case mmapi.ModelCreated(name, attr, modelid) =>
            sendToModel(modelid, mapi.PutItems(TestModel.getTestModel))
            $.modState(s => mL(ModelState.models).modify(_ + modelid)(s))
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

    val topic = mmapi.topicResponse
    val wsObs = BackendCommunication.getWebSocketStatusObserver(  mess => {
      if (mess) sendToHandler(mmapi.GetModels)
    }, topic)
    val topicHandler = BackendCommunication.getMessageObserver(handleMess, topic)

    def renderModels(s: State) = {
      <.table(
        ^.className := "table table-striped",
        <.caption("Models"),
        <.thead(<.tr(
          <.th("id"),
          <.th("name"),
          <.th("version"),
          <.th("number of items"),
          <.th("put dummy items"),
          <.th("preview"),
          <.th("delete")
        )),
        <.tbody(
          s.modelState.models.toList.sorted.map(m=> {
            List(
              <.tr(
                <.td(
                  (if(s.uiState.historyExpanded.contains(m))
                    <.button(^.className := "btn btn-sm",
                      ^.onClick --> $.modState(s=>uiL(UIState.historyExpanded).modify(_ - m)(s)),
                      <.i(^.className := "fa fa-chevron-up")
                    )
                  else
                    <.button(^.className := "btn btn-sm",
                      ^.onClick --> $.modState(s=>uiL(UIState.historyExpanded).modify(_ + m)(s)),
                      <.i(^.className := "fa fa-chevron-down")
                    )
                  ),
                  " " + m.toString
                ),
                <.td(s.modelState.modelInfo.get(m).map(_.name).getOrElse("").toString),
                <.td(s.modelState.modelInfo.get(m).map(_.version).getOrElse(-1).toString),
                <.td(s.modelState.modelInfo.get(m).map(_.noOfItems).getOrElse(-1).toString),
                <.td(
                  <.button(
                    ^.className := "btn btn-sm",
                    ^.title := "Add some dummy items",
                    ^.onClick --> sendToModel(m, mapi.PutItems(TestModel.getTestModel)),
                    <.i(^.className := "fa fa-bolt")
                  )
                ),
                <.td(
                  <.button(
                    ^.className := "btn btn-sm",
                    ^.title := "Preview model",
                    ^.onClick --> sendToModel(m, mapi.GetItemList()),
                    <.i(^.className := "fa fa-eye")
                  )
                ),
                <.td(
                  <.button(
                    ^.className := "btn btn-sm",
                    ^.title := "Delete model",
                    ^.onClick --> sendToHandler(mmapi.DeleteModel(m)),
                    <.i(^.className := "fa fa-trash")
                  )
                )
              ),
              <.tr(<.td(^.colSpan := 42, renderHistoryTable(s,m))).when(s.uiState.historyExpanded.contains(m))
            )}).flatten.toTagMod
        )
      )
    }

    def renderHistoryTable(s: State, m: ID) = {
      val hist = s.modelState.modelHistory.get(m).map(_.history).getOrElse(List())
      <.table(
        ^.className := "table table-striped",
        <.caption("History"),
        <.thead(<.tr(
          <.th("Version"),
          <.th("Info"),
          <.th("Revert")
        )),
        <.tbody(
          hist.map(h =>
            <.tr(
              <.td(h._1),
              <.td(h._2.getAs[String]("info").getOrElse("no info").toString),
              <.td(
                <.button(
                  ^.className := "btn btn-sm",
                  ^.title := ("Revert to version " + h._1),
                  ^.onClick --> sendToModel(m,mapi.RevertModel(h._1)),
                  <.i(^.className := "fa fa-undo")
                )
              ))).toTagMod
        ))
    }

    def renderModelPreview(s: State): TagMod = {
      <.table(
        ^.className := "table table-striped",
        <.caption("Model Preview"),
        <.thead(<.tr(
          <.th("Type"),
          <.th("Name"),
          <.th("ID")
        )),
        <.tbody(
          s.uiState.shownIdables.map(i =>
            <.tr(
              <.td(i.getClass.getSimpleName),
              <.td(i.name),
              <.td(i.id.toString)
            )).toTagMod
        )).when(s.uiState.shownIdables.nonEmpty)
    }

    def render(state: State) = {
      <.div(
        <.button(
          ^.className := "btn btn-default",
          ^.onClick --> sendToHandler(mmapi.CreateModel("testmodel")), <.i(^.className := "fa fa-bolt"), " Create test model"
        ),
        <.button(
          ^.className := "btn btn-default",
          ^.onClick --> sendToHandler(mmapi.GetModels), <.i(^.className := "fa fa-refresh"), " Refresh models"
        ),
        renderModels(state),
        renderModelPreview(state)
      )
    }

    def sendToHandler(mess: mmapi.Request): Callback = {
      val h = SPHeader(from = "ModelWidget", to = mmapi.service,
        reply = SPValue("ModelWidget"))
      val json = makeMess(h, mess)
      BackendCommunication.publish(json, mapi.topicRequest)
      Callback.empty
    }

    def sendToModel(model: ID, mess: mapi.Request): Callback = {
      val h = SPHeader(from = "ModelWidget", to = model.toString,
        reply = SPValue("ModelWidget"))
      val json = makeMess(h, mess)
      BackendCommunication.publish(json, mapi.topicRequest)
      Callback.empty
    }

    def onUnmount() = {
      topicHandler.kill()
      Callback.empty
    }

    def onMount() = {
      Callback.empty
    }

  }

  val initialUIState = UIState(Set(), shownIdables = List())
  val initialModelState = ModelState(models = Set(), modelInfo = Map(), modelHistory = Map())
  val initialState = State(initialModelState, initialUIState)
  private val component = ScalaComponent.builder[Unit]("ModelsWidget")
    .initialState(initialState)
    .renderBackend[Backend]
    .componentDidMount(_.backend.onMount())
    .componentWillUnmount(_.backend.onUnmount())
    .build

  def apply() = spgui.SPWidget(spwb => component())
}

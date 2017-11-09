package spgui.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.all.aria

object SPWidgetElements{
  def button(text: String, onClick: Callback): VdomNode =
    <.span(
      text,
      ^.onClick --> onClick,
      ^.className := "btn",
      ^.className := SPWidgetElementsCSS.defaultMargin.htmlClass,
      ^.className := SPWidgetElementsCSS.clickable.htmlClass,
      ^.className := SPWidgetElementsCSS.button.htmlClass
    )
  
  def button(text:String, icon:VdomNode, onClick: Callback): VdomNode =
    <.span(
      <.span(text, ^.className:= SPWidgetElementsCSS.textIconClearance.htmlClass),
      icon,
      ^.onClick --> onClick,
      ^.className := "btn",
      ^.className := SPWidgetElementsCSS.defaultMargin.htmlClass,
      ^.className := SPWidgetElementsCSS.clickable.htmlClass,
      ^.className := SPWidgetElementsCSS.button.htmlClass
    )
  
  def button(icon: VdomNode, onClick: Callback): VdomNode =
    <.span(icon,
      ^.onClick --> onClick,
      ^.className := "btn",
      ^.className := SPWidgetElementsCSS.defaultMargin.htmlClass,
      ^.className := SPWidgetElementsCSS.clickable.htmlClass,
      ^.className := SPWidgetElementsCSS.button.htmlClass
    )

  def dropdown(text: String, contents: Seq[TagMod]): VdomElement =
    <.span(
      ^.className:= SPWidgetElementsCSS.dropdownRoot.htmlClass,
      <.span(
        ^.className:= SPWidgetElementsCSS.dropdownOuter.htmlClass,
        ^.className := SPWidgetElementsCSS.defaultMargin.htmlClass,
        ^.className:= "dropdown",
        <.span(
          <.span(text, ^.className:= SPWidgetElementsCSS.textIconClearance.htmlClass),
          Icon.caretDown,
          VdomAttr("data-toggle") := "dropdown",
          ^.id:="something",
          ^.className := "nav-link dropdown-toggle",
          aria.hasPopup := "true",
          aria.expanded := "false",
          ^.className := "btn",
          ^.className := SPWidgetElementsCSS.button.htmlClass,
          ^.className := SPWidgetElementsCSS.clickable.htmlClass
        ),
        <.ul(
          contents.collect{
            case e => <.div(
              ^.className := SPWidgetElementsCSS.dropdownElement.htmlClass,
              e
            )
          }.toTagMod,
          ^.className := SPWidgetElementsCSS.dropDownList.htmlClass,
          ^.className := "dropdown-menu",
          aria.labelledBy := "something"
        )
      )
    )

  def dropdownElement(text: String, icon: VdomNode, onClick: Callback): VdomNode =
    <.li(
      ^.className := SPWidgetElementsCSS.dropdownElement.htmlClass,
      <.span(icon, ^.className := SPWidgetElementsCSS.textIconClearance.htmlClass),
      text,
      ^.onClick --> onClick
    )

  def dropdownElement(text: String, onClick: Callback): VdomNode =
    <.li(
      ^.className := SPWidgetElementsCSS.dropdownElement.htmlClass,
      text,
      ^.onClick --> onClick
    )

  def buttonGroup(contents: Seq[TagMod]): VdomElement =
    <.div(
      ^.className:= "form-inline",
      contents.toTagMod
    )

  object TextBox {
    case class Props( defaultText: String, onChange: String => Callback )
    case class State( text: String )

    class Backend($: BackendScope[Props, State]) {
      def render(p:Props,s: State) =
        <.span(
          ^.className := SPWidgetElementsCSS.defaultMargin.htmlClass,
          ^.className := "input-group",
          <.input(
            ^.className := SPWidgetElementsCSS.textBox.htmlClass,
            ^.className := "form-control",
            ^.placeholder := p.defaultText,
            ^.aria.describedBy := "basic-addon1",
            ^.onChange ==> onFilterTextChange(p)
          )
        )
      def onFilterTextChange(p:Props)(e: ReactEventFromInput): Callback =
        e.extract(_.target.value)(v => (p.onChange(v))) // TODO check if this works
    }

    private val component = ScalaComponent.builder[Props]("SPTextBox")
      .initialState(State("test"))
      .renderBackend[Backend]
      .build

    def apply(defaultText: String, onChange: String => Callback) =
      component(Props(defaultText, onChange))
  }

  import java.util.UUID
  import spgui.circuit._
  import scala.scalajs.js
  import spgui.dragging._
  import org.scalajs.dom.window
  import org.scalajs.dom.MouseEvent
  import org.scalajs.dom.document
  import spgui.dragging._
  import diode.react.ModelProxy
  import diode.ModelRO

  object DragoverZone {
    trait Rectangle extends js.Object {
      var left: Float = js.native
      var top: Float = js.native
      var width: Float = js.native
      var height: Float = js.native
    }

    case class Props(id: UUID, x: Float, y: Float, w: Float, h: Float)

    case class State(hovering: Boolean = false)

    class Backend($: BackendScope[Props, State]) {

      def setHovering(hovering: Boolean) =
        $.modState(s => s.copy(hovering = hovering)).runNow()

      Dragging.dropzoneSubscribe($.props.runNow().id, setHovering)

      def render(p:Props, s:State) = {
        <.span(
          <.span(
            ^.id := p.id.toString,
            ^.style := {
              var rect =  (js.Object()).asInstanceOf[Rectangle]
              rect.left = p.x
              rect.top = p.y
              rect.height = p.h
              rect.width = p.w
              rect
            },
            ^.className := SPWidgetElementsCSS.dropZone.htmlClass,
            {if(s.hovering)
              ^.className := SPWidgetElementsCSS.blue.htmlClass
            else ""},
            ^.onMouseOver --> Callback(Dragging.setDraggingTarget(p.id))
          )
        )
      }
    }

    private val component = ScalaComponent.builder[Props]("SPDragZone")
      .initialState(State())
      .renderBackend[Backend]
      .componentWillUnmount(c => Dragging.dropzoneUnsubscribe(c.props.id))
      .build

    def apply(id: UUID, x: Float, y: Float, w: Float, h: Float) =
      component(Props(id, x, y, w, h))
  }

  def draggable(label:String, id: UUID, typ: String): TagMod = {
    Seq(
      (^.onTouchStart ==> handleTouchDragStart(label, id, typ)),
      (^.onTouchMoveCapture ==> {
        (e: ReactTouchEvent) => Callback ({
          val x = e.touches.item(0).pageX.toFloat
          val y = e.touches.item(0).pageY.toFloat
          Dragging.onDragMove(x, y)
        })
      }),
      (^.onTouchEnd ==> {
        (e: ReactTouchEvent) => Callback (Dragging.onDragStop())
      }),
      (^.onMouseDown ==> handleDragStart(label, id, typ))
    ).toTagMod
  }

  /*
   This is used to generate mouse events when dragging on a touch screen, which will trigger
   the ^.onMouseOver on any element targeted by the touch event. Mobile browsers do not support
   mouse-hover related events (and why should they) so this is a way to deal with that.
   */
  def handleTouchDrag(e: ReactTouchEvent) = Callback {
    spgui.dragging.Dragging.onDragMove(
      e.touches.item(0).pageX.toFloat,
      e.touches.item(0).pageY.toFloat
    )
  }

  def handleTouchDragStart(label: String, id: UUID, typ: String)(e: ReactTouchEvent): Callback = {
    Callback(
      Dragging.onDragStart(
        label = label,
        id = id,
        typ = typ,
        x = e.touches.item(0).pageX.toFloat,
        y = e.touches.item(0).pageY.toFloat
      )
    )
  }

  def handleDragStart(label: String, id: UUID, typ: String)(e: ReactMouseEvent): Callback = {
    Callback(
      Dragging.onDragStart(
        label = label,
        id = id,
        typ = typ,
        x = e.pageX.toFloat,
        y = e.pageY.toFloat
      )
    )
  }
}

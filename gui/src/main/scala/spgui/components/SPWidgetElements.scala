package spgui.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.all.aria

object SPWidgetElements{
  def button(text: String, onClick: Callback): VdomNode =
    <.button(text,
      ^.onClick --> onClick,
      ^.className := "btn btn-default",
      ^.className := SPWidgetElementsCSS.defaultMargin.htmlClass,
      ^.className := SPWidgetElementsCSS.clickable.htmlClass,
      ^.className := SPWidgetElementsCSS.button.htmlClass
    )
  
  def button(text:String, icon:VdomNode, onClick: Callback): VdomNode =
    <.button(
      <.span(text, ^.className:= SPWidgetElementsCSS.textIconClearance.htmlClass),
      icon,
      ^.onClick --> onClick,
      ^.className := "btn btn-default",
      ^.className := SPWidgetElementsCSS.defaultMargin.htmlClass,
      ^.className := SPWidgetElementsCSS.clickable.htmlClass,
      ^.className := SPWidgetElementsCSS.button.htmlClass
    )
  
  def button(icon: VdomNode, onClick: Callback): VdomNode =
    <.button(icon,
      ^.onClick --> onClick,
      ^.className := "btn btn-default",
      ^.className := SPWidgetElementsCSS.defaultMargin.htmlClass,
      ^.className := SPWidgetElementsCSS.clickable.htmlClass,
      ^.className := SPWidgetElementsCSS.button.htmlClass
    )
  
  def dropdown(text: String, contents: Seq[TagMod]): VdomNode =
    <.span(
      ^.className:= SPWidgetElementsCSS.dropdownOuter.htmlClass,
      ^.className := SPWidgetElementsCSS.defaultMargin.htmlClass,
      ^.className:= "dropdown",
      <.button(
        <.span(text, ^.className:= SPWidgetElementsCSS.textIconClearance.htmlClass),
        Icon.caretDown,
        VdomAttr("data-toggle") := "dropdown",
        ^.id:="something",
        ^.className := "nav-link dropdown-toggle",
        aria.hasPopup := "true",
        aria.expanded := "false",
        ^.className := "btn btn-default",
        ^.className := SPWidgetElementsCSS.button.htmlClass,
        ^.className := SPWidgetElementsCSS.clickable.htmlClass
      ),
      <.ul(
        contents.toTagMod,
        ^.className := SPWidgetElementsCSS.dropDownList.htmlClass,
        ^.className := "dropdown-menu",
        aria.labelledBy := "something"
      )
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
}

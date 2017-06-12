package spgui.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.vdom.all.aria

object SPButton {
  case class Props(
    text:String,
    content: Option[ReactNode] = None,
    tags: Option[Seq[TagMod]] = None
  )

  private val component = ReactComponentB[Props]("SPButton")
    .render_P(p =>
    <.a(p.text, ^.className:= "btn btn-default "+ComponentCSS.buttonStyle.htmlClass , p.tags, p.content)
  ).build

  def apply(t:String,content:ReactNode,tags:Seq[TagMod])
  = component(Props(t,Some(content),Some(tags)))

  def apply(t:String,content:ReactNode)
  = component(Props(t,Some(content),None))

  def apply(t:String,tags:Seq[TagMod])
  = component(Props(t,None,Some(tags)))

  def apply(t:String)
  = component(Props(t))

}

package spgui.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object SPButtonElements{
  def navButton(text:String):Seq[TagMod] = Seq(
    <.a(text),
    ^.className := SPButtonElementsCSS.clickable.htmlClass,
    ^.className := SPButtonElementsCSS.navButton.htmlClass
  )
  
  def navButton(text:String, icon:ReactNode): Seq[TagMod] = Seq(
    <.a(text, icon),
    ^.className := SPButtonElementsCSS.clickable.htmlClass,
    ^.className := SPButtonElementsCSS.navButton.htmlClass
  )

  def navButton(icon:ReactNode): Seq[TagMod] = Seq(
    <.a(icon),
    ^.className := SPButtonElementsCSS.clickable.htmlClass,
    ^.className := SPButtonElementsCSS.navButton.htmlClass
  )

  def widgetButton(text:String): Seq[TagMod] = Seq(
    <.a(text),
    ^.className := SPButtonElementsCSS.clickable.htmlClass,
    ^.className := "btn btn-default",
    ^.className := SPButtonElementsCSS.widgetButton.htmlClass
  )

  def widgetButton(text:String, icon:ReactNode): Seq[TagMod] = Seq(
    <.a(text, icon),
    ^.className := "btn btn-default",
    ^.className := SPButtonElementsCSS.clickable.htmlClass,
    ^.className := SPButtonElementsCSS.widgetButton.htmlClass
  )

  def widgetButton(icon: ReactNode): Seq[TagMod] = Seq(
    <.a(icon),
    ^.className := "btn btn-default",
    ^.className := SPButtonElementsCSS.clickable.htmlClass,
    ^.className := SPButtonElementsCSS.widgetButton.htmlClass
  )
}



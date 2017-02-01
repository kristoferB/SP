package spgui

import scalacss.Defaults._
import japgolly.univeq.UnivEq


object SPButtonCSS extends StyleSheet.Inline {
  import dsl._


  val testStyle = style("buttonTest")(
    //addClassName("testlabel")
    borderRadius(1.em))


  this.addToDocument()
}


/* BootstrapSyles.scala from tutorial..
import japgolly.univeq.UnivEq
import spatutorial.client.components.Bootstrap.CommonStyle

import scalacss.Defaults._
import scalacss.internal.mutable
import spatutorial.client.components.Bootstrap.CommonStyle._

class BootstrapStyles(implicit r: mutable.Register) extends StyleSheet.Inline()(r) {

  import dsl._

  implicit val styleUnivEq: UnivEq[CommonStyle.Value] = new UnivEq[CommonStyle.Value] {}

  val csDomain = Domain.ofValues(default, primary, success, info, warning, danger)

  val contextDomain = Domain.ofValues(success, info, warning, danger)

  def commonStyle[A: UnivEq](domain: Domain[A], base: String) = styleF(domain)(opt =>
    styleS(addClassNames(base, s"$base-$opt"))
  )

  def styleWrap(classNames: String*) = style(addClassNames(classNames: _*))

  val buttonOpt = commonStyle(csDomain, "btn")

  val button = buttonOpt(default)

  val panelOpt = commonStyle(csDomain, "panel")

  val panel = panelOpt(default)

  val labelOpt = commonStyle(csDomain, "label")

  val label = labelOpt(default)

  val alert = commonStyle(contextDomain, "alert")

  val panelHeading = styleWrap("panel-heading")

  val panelBody = styleWrap("panel-body")

  // wrap styles in a namespace, assign to val to prevent lazy initialization
  object modal {
    val modal = styleWrap("modal")
    val fade = styleWrap("fade")
    val dialog = styleWrap("modal-dialog")
    val content = styleWrap("modal-content")
    val header = styleWrap("modal-header")
    val body = styleWrap("modal-body")
    val footer = styleWrap("modal-footer")
  }

  val _modal = modal


 */
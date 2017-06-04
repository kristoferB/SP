package spgui.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.all.aria

object SPButton {
	case class Props(
		text:String,
		content: Option[VdomNode] = None,
		tags: Option[Seq[TagMod]] = None
	)

	private val component = ScalaComponent.builder[Props]("SPButton")
		.render_P(p =>
    // TODO not sure why everything is an option, putting in gets to get migration done
			<.a(p.text, ^.className:= "btn btn-default "+ComponentCSS.buttonStyle.htmlClass , p.tags.get.toTagMod, p.content.getOrElse(EmptyVdom))
	).build

		def apply(t:String,content:VdomNode,tags:Seq[TagMod])
		= component(Props(t,Some(content),Some(tags)))

		def apply(t:String,content:VdomNode)
		= component(Props(t,Some(content),None))

		def apply(t:String,tags:Seq[TagMod])
		= component(Props(t,None,Some(tags)))

		def apply(t:String)
		= component(Props(t))
}

package sp.core

import scala.scalajs.js.JSApp
import japgolly.scalajs.react._
import org.scalajs.dom
import scala.scalajs.js.annotation.JSExport
import monix.execution.Scheduler.Implicits.global

object Main extends JSApp {
  @JSExport
  override def main(): Unit = {
    // this should maybe actually be a singleton
    val componentLoaderService = new ComponentLoaderService()

    // trying out the loader, for now using a hard link 
    componentLoaderService.fetchFactory(
      "http://localhost:3000/build/sp-example-widget.js", "TestWidget")
      .onComplete( res => {
        // create a widget from the factory
        // i have not gone object-oriented-crazy: this is how react needs things to work. i think.
        val widget = res.get.apply(WrapObj(Unit))

        val widgetWrapper = new WidgetWrapper(widget)

        // inject the component into DOM
        ReactDOM.render(widgetWrapper.getComponent(), dom.document.body)
      }
    )
  }
}

package spgui.widgets.injection

import japgolly.scalajs.react._
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

//class ComponentLoaderService {
//  // this is a react component in the pure JS representation. Components are loaded in
//  // in this form to not break compatibility with non-scalajs react components.
//  type BuiltComponent = ReactClass[Unit,Unit,Unit,org.scalajs.dom.raw.Element]
//
//  type ComponentFactory = ReactComponentCU[Unit,Unit,Unit,org.scalajs.dom.raw.Element]
//
//  // intended to represent a cache of some sort. should probably be a map
//  var factory: ComponentFactory = null
//
//  // takes a JS-defined react component and returns a scalajs-react factory
//  def createFactory(input: BuiltComponent): ComponentFactory = {
//    React.createFactory(input)
//  }
//
//  @JSExport
//  def fetchFactory(url: String, widgetName: String): Future[ComponentFactory] = {
//    val request = HttpRequest(url)
//    val result = Promise[ComponentFactory]()
//
//    request.send().onComplete({
//      case res:Success[SimpleHttpResponse] => {
//
//        // this part has to be done using eval (yuck). I think so at least
//        // this will create a widget in the global namespace (also yuck), assuming it is
//        // setup like the test widget.
//        js.eval( res.get.body )
//
//        // but this part should be done using js.Dynamic (TODO)
//        // this retrieves the widget from global javascript-space so we can use it
//        // inside of scalajs
//        // we should wrap this method in a few dozen runtime unit tests (TODO)
//        val component = js.eval( "sp.widgets." + widgetName + "().getComponent" )
//
//        result.success( createFactory( component.asInstanceOf[BuiltComponent] ))
//      }
//      case e: Failure[SimpleHttpResponse] => {
//        // TODO handle this at all
//        println(e)
//        result.failure(null)
//      }
//    })
//    result.future
//  }
//}

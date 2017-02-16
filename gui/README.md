## Prerequisites ##
You will need an sbt installation  (`scala-sbt.org/0.13/docs/`) 
You will also need an installation of node (`nodejs.org`) 

## Compiling the SP frontend ##

To install dependencies, cd to `SP/gui/npmdependencies` and run `npm install`.

To compile your code, cd to `SP/gui` and run `sbt fastOptJS`.
To see it, start the spcore (in SP root folder, run sbt spcore/run), and goto localhost:8080

To compile the optimized version run `fullOptJS` (slow process, not recommended in development)
To see it, open `index-prod.html` in a browser.

To get automatic compilation on file change, run `~fastOptJS`.

## Making a widget ##
The simplest possible widget is created with `SPWidget(spwb => <.h1("Hello, World!"))`. So to get started making a widget, create a file in `widgets/` containing the following.
```scala
package spgui.widgets

import spgui.SPWidget

object MyWidget {
  def apply() = SPWidget(spwb => <.h1("Hello from MyWidget"))
}
```
(Defining `apply` is just the scala way of making your object callable with `MyWidget()`.) To make your new widget available in the SP-menu, open `WidgetList.scala` and add it like so: `("My new widget", spgui.MyWidget())`, next to the other widgets.

The argument to the function given as argument to `SPWidget`, above named `spwb`, provides the API to interact with SP. For example it contains access to a string of data stored in the browser storage, via the field `data: String` and the method `saveData(data: String)`. This is conveniently used together with a case class, `upickle` and `Try`, as in the example below (found in the code in `widgets/examples/`).
```scala
object WidgetWithData {
  // calling MyData() (with no arguments) will give MyData(someInt = -17)
  case class MyData(someInt: Int = -17)

  def apply() = SPWidget{spwb =>
    // upickle's read tries to turn the string in browser storage into a MyData-instance
    // if there is nothing there or casting fails, creates the standard instance instead
    val myData = Try(read[MyData](spwb.data)).getOrElse(MyData())
    val theInt = myData.someInt

    // upickle's write turns a new version of myData into a string that is saved in storage
    def increment = Callback(spwb.saveData(write(myData.copy(theInt + 1))))

    <.div(
      <.h3("count is " + theInt),
      <.button("increment", ^.onClick --> increment),
      <.p("this piece of data is stored in the browser")
    )
  }
}
```
Note that no explicit re-rendering is necessary after calling `saveData`. This is handled automatically.

The case class can contain anything data-ish, i.e. strings, doubles, ints, lists of them as well as nested data-ish case classes.

README-TODO: add more about what's inside spwb here.

The html-like scala-objects prefixed by `<` and `^` are provided by the scalajs-react library. The function given as argument to `SPWidget` need to return either an `<`-object or a scalajs-react component. Learn about scalajs-react [here] (https://github.com/japgolly/scalajs-react/blob/master/doc/USAGE.md).


## JavaScript dependencies ##
JS dependencies are handled by npm and made available through a bundle file generated with webpack. To add a JS dependency, go to `npmdependencies/`, add it to `package.json` and `vendor.js`, then run `npm install`.

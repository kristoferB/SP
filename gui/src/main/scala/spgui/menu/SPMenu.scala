package spgui.menu

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom._

import spgui.components.SPButton
import spgui.circuit.{CloseAllWidgets, SPGUICircuit, UpdateGlobalAttributes}

import spgui.communication.BackendCommunication
import spgui.widgets.{API_Patient, API_PatientEvent, ToAndFrom}
import sp.domain._
import sp.messages.Pickles._


object SPMenu {
  val storage = SPGUICircuit.connect(x => (x.openWidgets.xs, x.globalState))
  val flowStorage = SPGUICircuit.connect(x => (x.openWidgets.xs, x.globalState))

  private val component = ReactComponentB[Unit]("SPMenu")
  .render(_ =>
    <.nav(
      ^.className := "navbar navbar-default",
      ^.className := SPMenuCSS.topNav.htmlClass,
      <.div(
        ^.className := "navbar-header",
        <.div(
          ^.className := SPMenuCSS.spLogoDiv.htmlClass,
          spLogo
        )
      ),
      NavBar(
        Seq(
          NavItem( WidgetMenu()),
          NavItem( SPButton("Close All", Seq(^.onClick -->  Callback(SPGUICircuit.dispatch(CloseAllWidgets)))  ))
        )
      ),
      noIndataNotifier,
      section   // make this a NavItem!!!
    )
  ).build



  private val noIndataNotifier = flowStorage{x =>
    val elvisDataIsFlowing = (for {
      v <- x()._2.attributes.get("elvisDataFlowing")
      theRes <- v.getAs[Boolean]()
    }yield{ theRes}).getOrElse(false)


    val notifierHeight = 50
    val notifierWidth = 400
    val fontSize = 15
    val contentColorLight = "#ffffff"
    val backgroundColor = "#000000"
    println("=================================================================DDJKFJDLFPLSDMFÖP" + elvisDataIsFlowing)
    if (!elvisDataIsFlowing) (
      <.svg.svg(
        ^.`class` := "notifier",
        ^.svg.width := notifierWidth.toString,
        ^.svg.height := "100%",//notifierHeight.toString,
        ^.svg.viewBox := "0 0 "+ notifierWidth.toString +" "+ notifierHeight.toString,
        <.svg.g(
          ^.`class` := "notifier-graphics",
          <.svg.rect(
            ^.`class` := "bg-field",
            ^.svg.y := 0,
            ^.svg.x := 0,
            ^.svg.height := notifierHeight,
            ^.svg.width := notifierWidth,
            ^.svg.fill := backgroundColor
          ),
          <.svg.text(
            ^.`class` := "notifier-text",
            ^.svg.y := ((notifierHeight/2)+10).toString,
            ^.svg.x := (notifierWidth/2).toString,
            ^.svg.textAnchor := "middle",
            ^.svg.fontSize :=  fontSize.toString + "px",
            ^.svg.fill := contentColorLight,
            //elvisDataIsFlowing.toString
            "Tillfälligt problem med uppkopplingen mot ELVIS."
          )
        )
      )
    ) else (
      <.div(^.className := "notifier-not-shown")
    )
  }



  val messObs = BackendCommunication.getMessageObserver(
    mess => {
      ToAndFrom.eventBody(mess).map {
        case API_PatientEvent.ElvisDataFlowing(_) => {
          println("In SPMenu: " + mess.getBodyAs[API_PatientEvent.ElvisDataFlowing])
          setElvisFlow(mess.getBodyAs[API_PatientEvent.ElvisDataFlowing].getOrElse(API_PatientEvent.ElvisDataFlowing(false)))
        }
        case _ => println("something else in SPMenu: " + mess)
      }
    }
    , "spevents")

    def setElvisFlow(elvisDataFlowing: API_PatientEvent.ElvisDataFlowing) = {
      println("SETTING ELVIS FLOW: " + elvisDataFlowing)

      SPGUICircuit.dispatch(UpdateGlobalAttributes("elvisDataFlowing", SPValue(elvisDataFlowing.dataFlowing)))
    }


    import sp.domain._
    import sp.messages.Pickles._

    def onFilterTextChange(e: ReactEventI) = {
      Callback(SPGUICircuit.dispatch(UpdateGlobalAttributes("team", SPValue(e.target.value))))
    }

    private val section = storage{x =>
      val currentTeam = x()._2.attributes.get("team").map(x => x.str).getOrElse("medicin")

      <.div(
        ^.className := "input-group",
        <.input(
          ^.className := "form-control",
          ^.placeholder := "team",
          ^.aria.describedby := "basic-addon1",
          ^.onChange ==> onFilterTextChange,
          ^.value := currentTeam
        )
      )
    }

    private val spLogo:ReactNode = (
      <.div(
        ^.className := SPMenuCSS.splogoContainer.htmlClass,
        <.div(
          ^.className := SPMenuCSS.spLogo.htmlClass
        )
      ))
      def apply() = component()
    }

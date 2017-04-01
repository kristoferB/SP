package spgui.widgets

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.ReactDOM

object PatientCardComponent {

  val patientCard = ScalaComponent.builder[List[Patient]]("patientCard")
  .render_P(P => <.svg.svg( //ARTO: Skapar en <svg>-tagg att fylla med obekt
    ^.`class` := "patientcard",
    ^.svg.id := "patientCard",
    ^.svg.width := "300",
    ^.svg.height := "200",
    <.svg.rect(
      ^.svg.id := "bg",
      ^.svg.x := "0",
      ^.svg.y := "0",
      ^.svg.width := "300",
      ^.svg.height := "200",
      ^.svg.fill := "lightgrey"
    ),
    <.svg.rect(
      ^.svg.id := "triagefield",
      ^.svg.x := "0",
      ^.svg.y := "0",
      ^.svg.width := "80",
      ^.svg.height := "200",
      ^.svg.fill := "darkseagreen"
    ),
    <.svg.path(
      ^.svg.id := "klinikfield",
      ^.svg.d := "M 0,180, 60,180, -60,0 Z",
      ^.svg.fill := "lightblue"
    ),
    <.svg.text(
      ^.svg.id := "roomNr",
      ^.svg.x := "10",
      ^.svg.y := "50",
      ^.svg.fontSize := "52",
      //^.svg.text := "52",
      ^.svg.fill := "white",
      P.head.roomNr//"52"
    ),
    <.svg.text(
      ^.svg.id := "careContactId",
      ^.svg.x := "100",
      ^.svg.y := "40",
      ^.svg.fontSize := "47",
      //^.svg.text := "52",
      ^.svg.fill := "black",
      P.head.careContactId//"52"
    ),
    <.svg.text(
      ^.svg.id := "lastEvent",
      ^.svg.x := "100",
      ^.svg.y := "100",
      ^.svg.fontSize := "25",
      //^.svg.text := "52",
      ^.svg.fill := "black",
      P.head.lastEvent._1//"52"
    )
  )
)
.componentDidUpdate(dcb => Callback(addPatientCard(ReactDOM.findDOMNode(dcb.component), dcb.currentProps)))
.build

}

package spgui.widgets.examples
/*
import scala.scalajs.js
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all.{className, div, h4, key, p, span}
import japgolly.scalajs.react.vdom.prefix_<^.{<, ^}
import japgolly.scalajs.react.vdom.svg.all._
import spgui.SPWidget
import paths.high.Pie

import scala.util.Random.nextInt
object PathsTest{
  import Piechart._
  def apply() = SPWidget{spwb =>
    component()
  }
  case class Countries(countries: List[Country])
  //def makeCountries(): Countries
  val countries = List(
    Country("Italy", 59859996),
    Country("Mexico", 118395054),
    Country("France", 65806000),
    Country("Argentina", 40117096),
    Country("Japan", 127290000)
  )
  val countries2 = List(
    Country("Halal", 118395054),
    Country("Tjohoh", 59859996),
    Country("Tralaa", 65806000),
    Country("Nihjo", 127290000),
    Country("Såla", 40117096)
  )
  private val component = ReactComponentB[Unit]("D3Example")
      .initialState(countries)
    .render{dcb =>
      <.div(
        <.button("Countries", ^.onClick --> dcb.modState(_ => countries)),
          <.button("Countries2", ^.onClick --> dcb.modState(_ => countries2)),
            PieChart(dcb.state)
      )
    }
    .build

}

object Piechart extends MakeColor {
  case class Country(name: String, population: Int)
  private def move(p: js.Array[Double]) = s"translate(${ p(0) },${ p(1) })"
  private val palette = mix(Color(130, 140, 210), Color(180, 205, 150))

  val PieChart = ReactComponentB[List[Country]]("Pie chart")
    .render(countries => {

      val pie = Pie[Country](
        data = countries.props,
        accessor = _.population,
        r = 60,
        R = 140,
        center = (0, 0)
      )

      val slices = pie.curves map { curve =>
        g(key := curve.item.name)(
          lineargradient(
            id := s"grad-${ curve.index }",
            stop(stopColor := string(palette(curve.index)), offset := "0%"),
            stop(stopColor := string(lighten(palette(curve.index))), offset := "100%")
          ),
          path(d := curve.sector.path.print, fill := s"url(#grad-${ curve.index })"),
          text(
            transform := move(curve.sector.centroid),
            textAnchor := "middle",
            curve.item.name
          )
        )
      }
      div(
      div(className := "country-info",
        h4("Population"),
        p("Italy.: ", span(className := "label label-info", countries.props(0).population)),
        p("Mexico: ", span(className := "label label-info", countries.props(1).population)),
        p("France: ", span(className := "label label-info", countries.props(2).population)),
        p("Argentina: ", span(className := "label label-info", countries.props(3).population)),
        p("Japan: ", span(className := "label label-info", countries.props(4).population)),
        svg(width := 400, height := 400,
          g(transform := move(js.Array(200, 200)), slices)
        )
      )
      )
    })
    .build
}

trait MakeColor {
  case class Color(r: Double, g: Double, b: Double, alpha: Double = 1)

  def cut(x: Double) = x.floor min 255

  def multiply(factor: Double) = { c: Color =>
    Color(cut(factor * c.r), cut(factor * c.g), cut(factor * c.b), c.alpha)
  }

  def average(c1: Color, c2: Color) =
    Color(
      cut((c1.r + c2.r) / 2),
      cut((c1.g + c2.g) / 2),
      cut((c1.b + c2.b) / 2),
      (c1.alpha + c2.alpha / 2)
    )

  val lighten = multiply(1.2)
  val darken = multiply(0.8)

  def mix(c1: Color, c2: Color) = {
    val c3 = average(c1, c2)
    val colors = List(
      lighten(c1),
      c1,
      darken(c1),
      lighten(c3),
      c3,
      darken(c3),
      lighten(c2),
      c2,
      darken(c2)
    )

    Stream.continually(colors).flatten
  }

  def transparent(c: Color, alpha: Double = 0.7) = c.copy(alpha = alpha)

  def string(c: Color) =
    if (c.alpha == 1) s"rgb(${ c.r.floor },${ c.g.floor },${ c.b.floor })"
    else s"rgba(${ c.r.floor },${ c.g.floor },${ c.b.floor },${ c.alpha })"
}
*/
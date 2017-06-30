/****************************************
  *      HELP CLASS TO GOOGLE CHARTS     *
  ****************************************/

package spgui.googleCharts.timeline

import spgui.googleCharts.DescriptionObject
import spgui.googleCharts.GoogleVisualization.Timeline
import spgui.googleCharts.GoogleVisualization.DataTable

import scala.scalajs.js


trait TimelineHelperTrait {
  val data:             DataTable
  val timeline:         Timeline
  val options:          OptionsTimeline

  def draw(): Unit
}

// simple helper to timeline
// todo: make class more functional
class TimelineHelper (
                       override val data:           DataTable,
                       override val timeline:       Timeline,
                       override val options:        OptionsTimeline
                     ) extends TimelineHelperTrait {
  /*
  // auxilary constructor with element and options,
  // calls initDataTable to setup data
  def this(element: js.Dynamic, options: OptionsTimeline) =
    this(new DataTable, new Timeline(element), options)

  // auxilary constructor with element
  // creates a new OptionsTimeline and call upwards
  def this(element: js.Dynamic) = this(element, new OptionsTimeline())
  */

  override def draw(): Unit = timeline.draw(data, options.toDynamic())

  def newRow(rowLabel: String, barLabel: String, startDate: js.Date, endDate: js.Date): Unit =
    data.addRow(new TimelineRow(rowLabel, barLabel, startDate, endDate).toArray())
}

object TimelineHelper{
  // simple init of dataTable
  def preDefTimelineData(toInit: DataTable): DataTable = {
    /*
    // simple setup of the description objects for each column
    val descriptionObjects: List[DescriptionObject] =
      new DescriptionObject("date"     , "TimelineDate"     , "End Date"  ) ::
      new DescriptionObject("date"     , "TimelineDate"     , "Start Date") ::
      new DescriptionObject("string"   , "TimelineString"   , "Bar Label" ) ::
      new DescriptionObject("string"   , "TimelineString"   , "Row Label" ) :: Nil
    // add each description object to table
    descriptionObjects.foreach(obj => toInit addColumn obj.toArray() )
    */
    // creates a example column setup to the DataTable
    toInit.addColumn("string" , "TimelineString", "Row Label")
    toInit.addColumn("string" , "TimelineString", "Bar Label")
    toInit.addColumn("date"   , "TimelineDate"  , "Start Date")
    toInit.addColumn("date"   , "TimelineDate"  , "End Date")

    // result value is the table that now is ready to use
    toInit
  }

  def apply(
           element: js.Dynamic,
           options: OptionsTimeline
           ) = new TimelineHelper (
    preDefTimelineData(new DataTable()),
    new Timeline(element),
    options
  )

  def apply(
            element: js.Dynamic
           ) = new TimelineHelper (
    preDefTimelineData(new DataTable()),
    new Timeline(element),
    new OptionsTimeline()
  )
}
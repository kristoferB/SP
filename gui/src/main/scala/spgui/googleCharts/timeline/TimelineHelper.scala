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

  def clear(): Unit
  def draw(): Unit
  def newRow(rowLabel: String, barLabel: String, startDate: js.Date, endDate: js.Date): Unit
}

// simple helper to timeline
// companion class
// todo: make class more functional
class TimelineHelper (
                       override val data:           DataTable,
                       override val timeline:       Timeline,
                       override val options:        OptionsTimeline
                     ) extends TimelineHelperTrait {
  // draw()-method
  // this calls the Timeline.draw() with the given data and options
  override def draw(): Unit = timeline.draw(data, options.toDynamic())

  override def clear(): Unit = timeline.clearChart()

  // newRow()-method
  // set the rowLabel, barLabel, startDate and endDate
  // add a new row to the DataTable with the given data, with some minor help of TimelineRow
  override def newRow(rowLabel: String, barLabel: String, startDate: js.Date, endDate: js.Date): Unit =
    data.addRow(new TimelineRow(rowLabel, barLabel, startDate, endDate).toArray())
}
// simple helper to timeline
// companion object
// this setups the predefined data
object TimelineHelper{
  // simple init of dataTable
  def preDefTimelineData(toInit: DataTable): DataTable = {
    // todo: use description object
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

  // element and options apply
  def apply(
           element: js.Dynamic,
           options: OptionsTimeline
           ) = new TimelineHelper ( // create a new TimelineHelper
    // get the predef datatable
    preDefTimelineData(new DataTable()),
    // create a new GoogleVisualization.Timeline() with the given element as argument
    new Timeline(element),
    // set the options
    options
  )
  // only element apply
  def apply(
            element: js.Dynamic
           ) = new TimelineHelper (// create a new TimelineHelper
    // get the predef datatable
    preDefTimelineData(new DataTable()),
    // create a new GoogleVisualization.Timeline() with the given element as argument
    new Timeline(element),
    // create a new
    new OptionsTimeline()
  )
}
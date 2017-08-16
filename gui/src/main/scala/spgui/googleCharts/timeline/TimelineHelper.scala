/****************************************
  *      HELP CLASS TO GOOGLE CHARTS     *
  ****************************************/

package spgui.googleCharts.timeline

import aleastchs.googleCharts.google.visualization.{DataTable, Timeline}

import scala.scalajs.js

trait TimelineHelperTrait {
  // variables
  val data:             DataTable
  val timeline:         Timeline
  val options:          OptionsTimeline
  // methods
  def clear(): Unit
  def draw(): Unit
  def newRow(rowLabel: String, barLabel: String, startDate: js.Date, endDate: js.Date): Unit
  def newRow(row: TimelineRow): Unit
  def emptyRow(): Unit
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
  def draw(): Unit = {
    // TODO: this can work, but then you need to filter rows of the same row label
    // Todo: this approach can work, look at SQL-formatting with data.group()
    // options.setHeight(data.getNumberOfRows() * heightPerRow + margin )
    timeline.draw(
      data,
      OptionsTimeline(
        this.data.getNumberOfRows() * 31 + 40,
        this.options.width,
        this.options.timeline,
        this.options.tooltip,
        this.options.title,
        this.options.avoidOverlappingGridLines,
        this.options.backgroundColor,
        this.options.colors,
        this.options.enableInteractivity,
        this.options.fontName,
        this.options.fontSize,
        this.options.forceIFrame
      ).toDynamic
    )
  }

  // clears the timelineChart
  def clear(): Unit = timeline.clearChart()

  // TODO: convert to js.Date so User cannot see any js-code??
  // newRow()-method
  // set the rowLabel, barLabel, startDate and endDate
  // add a new row to the DataTable with the given data, with some minor help of TimelineRow
  def newRow(rowLabel: String, barLabel: String, startDate: js.Date, endDate: js.Date): Unit =
    data.addRow(TimelineRow(rowLabel, barLabel, startDate, endDate).toArray)
  def newRow(row: TimelineRow): Unit =
    data.addRow(row.toArray)
  // TODO: Fix data.addRow()
  // TODO: Error 'Missing value in row 1'
  def emptyRow(): Unit = data.addRow()
}
// simple helper to timeline
// companion object
// this setups the predefined data
object TimelineHelper{
  // if we set colorByRowLabel and !showBarLabel we get a nicer view
  private val innerOptions = TimelineInner(null, true, true, null, false, true, null)

  // simple init of dataTable
  private def preDefTimelineData: DataTable = {
    val toInit: DataTable = new DataTable()
    // todo: use description object
    // todo: do functional
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
           dataTable: DataTable,
           timeline: Timeline,
           optionsTimeline: OptionsTimeline
           ) = new TimelineHelper(
    dataTable,
    timeline,
    optionsTimeline
  )

  // element and options apply
  def apply(
             element: js.Dynamic,
             options: OptionsTimeline
           ) = new TimelineHelper ( // create a new TimelineHelper
    // get the predef data table
    preDefTimelineData,
    // create a new GoogleVisualization.Timeline() with the given element as argument
    new Timeline(element),
    // set the options
    options
  )

  def apply(
             element: js.Dynamic,
             jsonData: String,
             title: String
           ) = new TimelineHelper(
    // create a new DataTable with the jsonData-string
    new DataTable(jsonData),
    // create a new GoogleVisualization.Timeline() with the given element as argument
    new Timeline(element),
    // create a new OptionsTimeline
    OptionsTimeline(
      0,
      0,
      innerOptions,
      title
    )
  )

  def apply(
             element: js.Dynamic,
             title: String
           ) = new TimelineHelper(// create a new TimelineHelper
    // get the predef data table
    preDefTimelineData,
    // create a new GoogleVisualization.Timeline() with the given element as argument
    new Timeline(element),
    // create a new OptionsTimeline
    OptionsTimeline(
      0,
      0,
      innerOptions,
      title
    )
  )
}
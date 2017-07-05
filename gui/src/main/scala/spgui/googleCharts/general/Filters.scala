/****************************************
 *      HELP CLASS TO GOOGLE CHARTS     *
 ****************************************/

package spgui.googleCharts.general

import scala.scalajs.js

// https://developers.google.com/chart/interactive/docs/reference#DataTable
// SEE getFilteredRows(filters)
trait FiltersTrait {
  val columnId: Int
  val value: Int
  val minValue: String
  val maxValue: String
  val cases: Int

  def toObject: js.Object
}

// TODO: Do functional

class Filters (
                override val columnId: Int,
                override val value: Int,
                override val minValue: String,  // null for no lower bound
                override val maxValue: String, // null for no upper bound
                override val cases: Int = 0
              ) extends FiltersTrait {


  // AUXILARRY CONSTRUCTORS
  def this(columnId: Int, value: Int) =
    this(columnId, value, null, null)
  def this(columnId: Int, minValue: String, maxValue: String) =
    this(columnId, 0, minValue, maxValue, 1)

  // toObject method
  // returns a js.Object
  override def toObject: js.Object =
  if(cases == 0) {
    js.Dynamic.literal(
      column = this.columnId,
      value = this.value
    )
  } else if (cases == 1) {
    js.Dynamic.literal(
      column = this.columnId,
      minValue = this.minValue,
      maxValue = this.maxValue
    )
  } else {
    js.Dynamic.literal()
  }

}
/**
  * Created by alexa on 21/06/2017.
  */
package spgui.googleAPI

import scala.scalajs.js


trait DescriptionObject extends js.Object {
  // A string with the data type of the values of the column
  val column_type: String = js.native
  // A label for the column
  val optional_label: String = js.native
  // An ID for the column
  val optional_id: String = js.native
  // TODO:Implement GOOGLEVISUALIZATION ROLE
  // A role for the column
  val optional_role: String = js.native
  // A number (or date) format string specifying how to display the column value
  val optional_pattern: String = js.native
}

object DescriptionObject {
  def apply(
             column_type:     String,
             optional_label:  String = "",
             optional_id:     String = "",
             optional_role:   String = "",
             optional_pattern: String = ""
           ) = js.Dynamic.literal(
    columnt_type = column_type,
    optional_label = optional_label,
    optional_id = optional_id,
    optinal_role = optional_role,
    optional_pattern = optional_pattern
  )
}

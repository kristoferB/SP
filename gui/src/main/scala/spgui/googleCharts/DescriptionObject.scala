/**
  * Created by alexa on 21/06/2017.
  */
package spgui.googleCharts

import scala.scalajs.js

trait DescriptionObjectTrait {
  // A string with the data type of the values of the column
  val columnType:       String
  // A label for the column
  val optionalLabel:    String
  // An ID for the column
  val optionalId:       String
  // TODO:Implement GOOGLEVISUALIZATION ROLE
  // A role for the column
  val optionalRole:     String
  // A number (or date) format string specifying how to display the column value
  val optionalPattern:  String
}

/*
object DescriptionObject {
  def apply(
           columnType:      String,
           optionalId:      String,
           optionalLabel:   String = "",
           optionalRole:    String = "",
           optionalPattern: String = ""
           ) = js.Dynamic.literal(
    columnType = columnType,
    optionalLabel = optionalLabel,
    optionalId = optionalId,
    optionalRole = optionalRole,
    optionalPattern = optionalPattern
  )
}
*/


class DescriptionObject(
                         override val columnType: String,
                         override val optionalLabel: String = "",
                         override val optionalId: String = "",
                         override val optionalRole: String,
                         override val optionalPattern: String = ""
                       ) extends DescriptionObjectTrait {

  def toArray(): Array[String] =
    Array(this.columnType, this.optionalLabel, this.optionalId, this.optionalRole, this.optionalPattern)


  override def toString = s"DescriptionObject($columnType, $optionalLabel, " +
    s"$optionalId, $optionalRole, $optionalPattern)"
}


/*
object DescriptionObject_Trait {
  def apply(
             column_type:     String,
             optional_id:     String = "",
             optional_label:  String = "",
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
*/
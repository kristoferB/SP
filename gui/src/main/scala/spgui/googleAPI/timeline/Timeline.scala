/**
  * Created by alexa on 21/06/2017.
  */

package spgui.googleAPI.timeline

import spgui.googleAPI.{DataTable_Trait, GoogleChart_Trait, Options_Trait}

trait Timeline_Trait extends GoogleChart_Trait {
  val element
  override def draw(data: DataTable_Trait, options: Options_Trait): Unit = super.draw(data, options)

  override def clearChart(): Unit = super.clearChart()
}
class Timeline() extends Timeline_Trait {
  // Draws a Timeline Chart
  // arguments: data
  //            options
  override def draw(data: DataTable_Trait,
                    options: Options_Trait
                   ): Unit = super.draw(data, options)

  override def clearChart(): Unit = super.clearChart()
}
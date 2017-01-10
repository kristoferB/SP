package sp.robotServices.core

import org.json4s._

/**
  * Created by Henrik on 2016-04-18.
  */

object Helpers {
  implicit class JValueExtended(value: JValue) {
    def has(childString: String): Boolean = {
      if ((value \ childString) != JNothing)
        true
      else
        false
    }
  }
}
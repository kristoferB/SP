package sp.domain

import sp.domain.logic._

/**
 * Created by kristofer on 15-05-27.
 */
object Logic extends
  AttributeLogics with
  StateLogics with
  JsonLogics with
  OperationLogics with
  ThingLogics {

  implicit val formats = jsonFormats
}

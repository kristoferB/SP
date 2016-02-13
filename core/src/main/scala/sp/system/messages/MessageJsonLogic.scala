package sp.system.messages

import sp.domain._
import org.json4s._
import sp.domain.logic.JsonLogics

/**
  * Created by kristofer on 2016-02-12.
  */
trait JsonFormatsMessages extends JsonLogics {
  val th = new JsonFormats {}.typeHints
  implicit val formatWithMess = new JsonFormats {
    override val typeHints = ShortTypeHints(th.hints ++ List(
      classOf[ModelAdded],
      classOf[ModelDeleted],
      classOf[ModelUpdated],
      classOf[ModelDiff],
      classOf[Response],
      classOf[Progress],
      classOf[SPOK]
    ))
  }
}

object JsonFormatsMessage extends JsonFormatsMessages

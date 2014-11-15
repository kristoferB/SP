package sp.domain

/**
 * Created by Kristofer on 2014-08-18.
 */
trait Result extends IDAble {
  val name: String
  val model: ID
  val modelVersion: Long
  val attributes: SPAttributes
}

case class RelationResult(name: String,
                          relationMap: Option[RelationMap],
                          noRelations: Option[NoRelations],
                          model: ID,
                          modelVersion: Long,
                          attributes: SPAttributes = SPAttributes(Map()),
                          id: ID = ID.newID
                           ) extends Result {

}


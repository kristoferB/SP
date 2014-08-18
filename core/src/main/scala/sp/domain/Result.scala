package sp.domain

/**
 * Created by Kristofer on 2014-08-18.
 */
trait Result extends IDAble {
  val name: String
  val model: String
  val modelVersion: Long
}

case class RelationResult(name: String,
                          relationMap: RelationMap,
                          model: String,
                          modelVersion: Long,
                          attributes: SPAttributes = SPAttributes(Map())
                           ) extends Result {
  override def update(currentID: ID, currentVersion: Long): IDAble = {
      new RelationResult(name, relationMap, model, modelVersion, attributes){
        override lazy val id = currentID
        override lazy val version = currentVersion + 1
      }
  }
}

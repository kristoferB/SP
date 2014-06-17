package sp.domain

/**
 * Created by Kristofer on 2014-06-12.
 */
trait Specification extends IDAble {
  val label: String
  val attributes: SPAttributes
}

case class SOPSpec(sop: SOP, label: String, attributes: SPAttributes = SPAttributes(Map())) extends Specification {
  override def update(currentID: ID, currentVersion: Long): IDAble = {
      new SOPSpec(sop, label, attributes){
        override lazy val id = currentID
        override lazy val version = currentVersion + 1
      }
  }
}
case class SPSpec(label: String, attributes: SPAttributes = SPAttributes(Map())) extends Specification {
  override def update(currentID: ID, currentVersion: Long): IDAble = {
    new SPSpec(label, attributes){
      override lazy val id = currentID
      override lazy val version = currentVersion + 1
    }
  }
}

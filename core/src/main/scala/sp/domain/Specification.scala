package sp.domain

/**
 * Created by Kristofer on 2014-06-12.
 */
trait Specification extends IDAble {
  val name: String
}

case class SOPSpec(sop: SOP, name: String, attributes: SPAttributes = SPAttributes(Map())) extends Specification {
  override def update(currentID: ID, currentVersion: Long): IDAble = {
      new SOPSpec(sop, name, attributes){
        override lazy val id = currentID
        override lazy val version = currentVersion + 1
      }
  }
}
case class SPSpec(name: String, attributes: SPAttributes = SPAttributes(Map())) extends Specification {
  override def update(currentID: ID, currentVersion: Long): IDAble = {
    new SPSpec(name, attributes){
      override lazy val id = currentID
      override lazy val version = currentVersion + 1
    }
  }
}

package sp.domain

/**
 * Started more simple then before. Now we need to encode if it is a resource or entitiy in attributes
 * Also hierarchy will be defined by attributes. When a good structure have been found, maybe we move out
 * some attributes
 *
 * Created by Kristofer on 2014-06-11.
 */
case class Thing(name: String,
                 stateVariables: List[StateVariable] = List(),
                 attributes: SPAttributes = SPAttributes(Map())) extends IDAble with StateVariable {
  //override def valueInDomain(v: Any): Boolean = v.isInstanceOf[Boolean]

  override def update(currentID: ID, currentVersion: Long): IDAble = {
    new Thing(name, stateVariables, attributes){
      override lazy val id = currentID
      override lazy val version = currentVersion + 1
    }
  }
}

/**
 * An SPObject can be used to store objects where the semantics is defined outside of SP, i.e. by other
 * algorithms and requirements. The content of an SPObject must be encoded in the attributes.
 *
 * An SPObject does not have a state. It can for example be a schedule not in the form of a SOP.
 *
 * Try avoid using this class to store things in the model.
 *
 */
case class SPObject(name: String, attributes: SPAttributes = SPAttributes(Map())) extends IDAble {
  override def update(currentID: ID, currentVersion: Long): IDAble = {
    new SPObject(name, attributes){
      override lazy val id = currentID
      override lazy val version = currentVersion + 1
    }
  }
}

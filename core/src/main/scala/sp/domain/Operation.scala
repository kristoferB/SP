package sp.domain

/**
 * Created by Kristofer on 2014-06-08.
 */
case class Operation(name: String,
                     conditions: List[Condition] = List(),
                     attributes: SPAttributes = SPAttributes(Map()))
        extends IDAble with StateVariable {


  override def valueInDomain(v: Any): Boolean = v.isInstanceOf[OpLocation]
  override def update(currentID: ID, currentVersion: Long): IDAble = {
    new Operation(name, conditions, attributes){
      override lazy val id = currentID
      override lazy val version = currentVersion + 1
    }
  }
}

trait OpLocation
case object Initial extends OpLocation
case object Executing extends OpLocation
case object Finished extends OpLocation
object OpLocation {
  def apply(s: String): OpLocation = s match {
    case "i"=> Initial
    case "init"=> Initial
    case "initial" => Initial
    case "e"=> Executing
    case "exec"=> Executing
    case "executing" => Executing
    case "f"=> Finished
    case "fin"=> Finished
    case "finished" => Finished
    case "completed" => Finished
    case _ => Initial
  }
}

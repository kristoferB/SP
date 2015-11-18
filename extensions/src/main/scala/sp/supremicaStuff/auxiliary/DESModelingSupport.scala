package sp.supremicaStuff.auxiliary
import sp.domain._
import sp.domain.Logic._

/**
 * Created by patrik on 2015-11-11.
 */
trait DESModelingSupport {
  def getIdleState(vars: Set[Thing]) = {
    val state = vars.foldLeft(Map(): Map[ID, SPValue]) { case (acc, v) =>
      lazy val optDomain = v.attributes.findField(f => f._1 == "domain").flatMap(_._2.to[List[String]])
      v.attributes.getAs[Int]("idleValue") match {
        //Do nothing if idleValue is an int
        case Some(value) => acc + (v.id -> SPValue(value))
        //If not an int try with a string
        case _ => v.attributes.getAs[String]("idleValue") match {
          case Some(value) => optDomain match {
            //replace string value with position in domain
            case Some(domain) if domain.contains(value) => acc + (v.id -> SPValue(domain.indexOf(value)))
            case _ => acc
          }
          case _ => acc
        }
      }
    }
    State(state)
  }

  def stateIsMarked(vars: Set[Thing]): State => Boolean = { thatState =>
    lazy val goalState = getIdleState(vars)
    def checkState(stateToCheck: Seq[(ID, SPValue)] = thatState.state.toSeq): Boolean = stateToCheck match {
      case kv +: rest => goalState.get(kv._1) match {
        case Some(v) => if (v.equals(kv._2)) checkState(rest) else false
        case _ => false //"stateToCheck" contains variables that is not in "goalState". This should although not happen...
      }
      case _ => true //"stateToCheck" == "goalState"
    }
    checkState()
  }
}

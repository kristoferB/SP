package sp.services.relations

import akka.actor._
import sp.domain._
import sp.domain.Logic._

/**
 * Created by kristofer on 15-06-22.
 */
class MultiRelationFinder extends Actor with MultiRelationFinderLogic {
  def receive = {
    case FindMultiRelations(ops, init, goal) => {

    }
  }

}

// api
case class FindMultiRelations(ops: List[Operation], init: State, goal: State => Boolean)

trait MultiRelationFinderLogic {

  def findOneSequence(ops: List[Operation], init: State, goal: State => Boolean) = {

  }

  def eval(o: Operation, s: State) = {
    //o.conditions.
  }

}

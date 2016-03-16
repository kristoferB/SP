package sp.optimizerService

import gnu.jel.OP
import sp.domain._
import sp.extensions._


/**
  * Created by Kristian Eide on 2016-03-09.
  */
class optimizerService extends IDAble {

  case class tempState (
    name: String,
    id: ID = id.newID(),
    values: List(Contiodns) = List(),
    fCost: Int) extends IDAble{
  }
  case class Transition (
    gCost: Int,
    head: tempState,
    tail: tempState,
    OP: tempOP,
    id: ID = id.newID()) extends IDAble{
  }
  case class node (
    name: String,
    id: ID = id.newID(),
    in: List(Transition) = List(),
    out: List(Transition) = List(),
    hCost: Int)
  extends IDAble {
  }

  case class tempOP(
  condisitons: List(condition) = List(),
  actions: List(abillity) = List())
  extends IDAble{
  }

  var opLIST: List(tempOP) = List(
  var runFlexlink: teamOP = new tempOP List(condition) = List((moveOut || moveIN), List(abillity)= List(flexlink.run = true)),
  var loadBuildPallets: teamOP = new tempOP List(condition) = List((orderBuild && operatorReady), List(abillity)= List(operatorLoading = true)),
  var loadBuildPallets: teamOP = new tempOP List(condition) = List((orderMaterial && operatorReady), List(abillity)= List(operatorLoading = true)),
  var raiseGive: teamOP = new tempOP List(condition) = List((), List(abillity)= List()), //ingen abiility då detta sker automatiskt då det finns pallet vid stop

  )
}

package sp.domain

import sp.domain.logic._

/**
 * Created by kristofer on 15-05-27.
 */
object Logic extends
  StateLogics with
  OperationLogics with
  ThingLogics with
  PropositionConditionLogics with
  StructLogics

object LogicNoImplicit extends
  StateLogics with
  OperationLogics with
  ThingLogics with
  PropositionConditionLogics with
  StructLogics

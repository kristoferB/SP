package sp.domain

import sp.domain.logic._

/**
 * Created by kristofer on 15-05-27.
 */
object Logic extends
  AttributeLogics with
  StateLogics with
  OperationLogics with
  ThingLogics with
  PropositionConditionLogics with
  StructLogics with
  JsonImplicit

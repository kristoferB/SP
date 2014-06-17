package sp.domain

/**
 * Created by Kristofer on 2014-06-10.
 */
trait Condition {
  def guard: State => Boolean
  def action: State => State
  val attributes: SPAttributes
}

case class stateMapCondition(guardMap: Map[String, Any] = Map(), actionMap: Map[String, Any] = Map(), conditionType: String = "pre", label: String = "") {
  def guard: State => Boolean = _.evaluateWithString(guardMap)
  def action: State => State = _.updateWithStrings(actionMap)

  val attributes = SPAttributes(Map("conditionType"-> conditionType, "label"-> label))
}

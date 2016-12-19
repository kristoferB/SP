package sp.optimization

import sp.domain.SPAttributes
import sp.domain.Logic._ // for construction of SPAttributes

import oscar.algo.search.SearchStatistics


/**
  * Created by fredrik on 2016-08-03.
  */
class OptimizationWithOscaR(val model: String, val data: String) extends OptimizationInterface(model,data) {
  private def opt: OscaRModel = Class.forName("sp.optimization.oscarmodels." + model).getConstructor(data.getClass).newInstance(data).asInstanceOf[OscaRModel]
  private val solution: SPAttributes = SPAttributes("solution" -> opt.solution, "stats" -> opt.stats)
  override def getSolution: SPAttributes = solution
}
abstract class OscaRModel(data: String) {
  var solution: Array[AnyVal]
  val stats: SearchStatistics
}
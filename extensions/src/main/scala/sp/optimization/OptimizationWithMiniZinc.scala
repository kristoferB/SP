package sp.optimization

import sp.domain.SPAttributes
import sp.domain.Logic._ // for construction of SPAttributes

import oscar.algo.search.SearchStatistics


/**
  * Created by fredrik on 2016-08-03.
  */
class OptimizationWithMiniZinc(val model: String, val data: String) extends OptimizationInterface(model,data) {
  import scala.sys.process._
  final val path = "extensions/src/main/scala/sp/optimization/models/MiniZinc/"

  val command = s"""minizinc ${path + model}.mzn"""
  // val command = """echo %path%"""
  val os = sys.props("os.name").toLowerCase
  val exec = os match {
    case x if x contains "windows" => "cmd /C " + command
    case _ => command
  }
  private val opt = exec.!!
  private val solution: SPAttributes = SPAttributes("solution" -> opt)
  override def getSolution: SPAttributes = solution
}
package sp.optimization

import org.json4s.JsonAST.JString
import sp.domain._

// enumeration for the optimization interfaces
object Interface extends Enumeration {
  type Interface = Value
  val MiniZinc, OscaR/*, or_tools*/ = Value
}
import sp.optimization.Interface._

/**
  * Created by fredrik on 2016-08-03.
  */
object OptimizationModels {
  // Define available optimization models
  final val models: List[OptimizationModel] = List(
    new OptimizationModel("Queens",OscaR),
    new OptimizationModel("send-more-money",MiniZinc)
  )

  def getSelectionList: List[SPValue] = models map {x => SPValue(x.name)}
  def getModel(name: String): OptimizationModel = models.find(x => x.name == name) match {
    case None => throw new IllegalArgumentException("Error in optimization service, model not found: " + name)
    case Some(x) => x
  }

}
case class OptimizationModel(name: String, interface: Interface)

abstract class OptimizationInterface(model: String, data: String) {
  def getSolution: SPAttributes
}

class OptimizationCoordinator(modelParams: ModelParams) {
  val model = OptimizationModels.getModel(modelParams.model)
  val optimize: OptimizationInterface = model.interface match {
    case OscaR => new OptimizationWithOscaR(model.name, modelParams.data)
    case MiniZinc => new OptimizationWithMiniZinc(model.name, modelParams.data)
  }
  def getSolution: SPAttributes = optimize.getSolution
}

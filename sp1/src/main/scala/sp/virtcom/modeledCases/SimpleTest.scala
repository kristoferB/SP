package sp.virtcom.modeledCases
import sp.virtcom.CollectorModel

/**
 * Created by patrik on 2015-11-11.
 */
case class SimpleTest(modelName: String = "Simple test") extends CollectorModel {
  v(name = "vCounter", idleValue = "0", domain = Seq("0", "1", "2", "3", "4"))
  op("first", c("vCounter", "0", "1", "2"))
  op("second", c("vCounter", "2", "3", "0"))
  v(name = "vOther", idleValue = "0", domain = Seq("0", "1"))
  op("other", c("vOther", "0", "1", "0"))

}

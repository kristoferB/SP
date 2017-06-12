package sp.itemServiceDummy

import sp.domain._

object SampleSPItems {
  def apply(): List[IDAble] = {
    val gustaf = Operation("Gustaf")
    val gustafNode = StructNode(gustaf.id, None)
    val klas = Operation("Klas")
    val klasNode = StructNode(klas.id)
    val wilhelm = Operation("Wilhelm")
    val wilhelmNode = StructNode(wilhelm.id, Some(gustaf.id))
    val bengt = Thing("Bengt")
    val bengtNode = StructNode(bengt.id, Some(gustaf.id))
    val karlsson = Thing("Karlsson")
    val karlssonNode = StructNode(karlsson.id, Some(gustaf.id))

    //def apply() = Struct("Rot", gustafNode :: klasNode :: Nil) // TODO this is what we should paint the tree with, sometime
    gustaf :: klas :: wilhelm :: bengt :: karlsson :: Nil
  }
}

package sp.domain.logic

import org.json4s._
import sp.domain._
import sp.system.messages._


/**
 * Created by kristofer on 15-05-27.
 */
object JsonLogic extends JsonLogics

trait JsonLogics {
  trait JsonFormats extends DefaultFormats {
    override val typeHintFieldName = "isa"
    override val customSerializers: List[Serializer[_]] = org.json4s.ext.JodaTimeSerializers.all :+
      org.json4s.ext.UUIDSerializer :+
      new IDSerializer :+
      new StateSerializer :+
      new RelationSerializer
    override val dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    override val typeHints = ShortTypeHints(List(
      classOf[Operation],
      classOf[Thing],
      classOf[RelationResult],
      classOf[SPSpec],
      classOf[HierarchyRoot],
      classOf[SOPSpec],
      classOf[Parallel],
      classOf[Alternative],
      classOf[Arbitrary],
      classOf[Hierarchy],
      classOf[Other],
      classOf[Sequence],
      classOf[SometimeSequence],
      classOf[AND],
      classOf[OR],
      classOf[NOT],
      classOf[EQ],
      classOf[NEQ],
      classOf[GREQ],
      classOf[LEEQ],
      classOf[GR],
      classOf[LE],
      classOf[INCR],
      classOf[DECR],
      classOf[ASSIGN],
      classOf[ValueHolder],
      classOf[SVIDEval],
      classOf[PropositionCondition],
      classOf[ModelAdded],
      classOf[ModelDeleted],
      classOf[ModelUpdated],
      classOf[ModelDiff],
      classOf[Response],
      classOf[Progress],
      classOf[SPOK]

    ))
  }
  def jsonFormats = new JsonFormats {}
  implicit val formats2 = jsonFormats

  def timeStamp = {
    Extraction.decompose(org.joda.time.DateTime.now)
  }

  class IDSerializer extends CustomSerializer[ID](format => (
    {
      case JString(idStr) => {
        val id = ID.makeID(idStr)
        if (id == None) println(s"ID: $idStr is not an ID. A new one is created!!!")
        id.getOrElse(ID.newID)
      }
      case JObject(JField("id", JString(idStr)) :: Nil) => {
        val id = ID.makeID(idStr)
        if (id == None) println(s"ID: $idStr is not an ID. A new one is created!!!")
        id.getOrElse(ID.newID)
      }
    },
    {
      case x: ID =>
        JString(x.toString())
    }
    ))

  class StateSerializer extends CustomSerializer[State](format => (
    {
      case JObject(xs) => {
        val filtered = for {
          kv <- xs if ID.isID(kv._1)
          id <- ID.makeID(kv._1)
        } yield (id, kv._2)
        State(filtered.toMap)
      }
      case JArray(xs) => {
        import sp.domain.logic.AttributeLogic._
        val filtered = for {
          sv @ JObject(xs) <- xs
          key <- sv.getAs[ID]("id")
          v <- sv.get("value")
        } yield (key -> v)
        State(filtered.toMap)
      }
    },
    {
      case x: State => {
        val res = x.state.map(kv => kv._1.toString() -> kv._2).toList
        JObject(res)
      }
    }
    ))

  class RelationSerializer extends CustomSerializer[RelationMap](format => (
    {
      case JObject(JField("relations", JArray(ops)) :: JField("enabledMap", JArray(enabled)) :: Nil) => {
        println("Not parsing relationsmap just yet!!!")
        RelationMap(Map(),EnabledStatesMap(Map()) )
      }
    },
    {
      case x: RelationMap =>
        val remap = x.relations map{
          case (ops, sop) =>
            SPAttributes("operations" -> ops, "sop" -> sop)
        }
        SPAttributes(
          "relations" -> remap
          //"enabledMap" -> x.enabledStates
        )
    }

    ))

}

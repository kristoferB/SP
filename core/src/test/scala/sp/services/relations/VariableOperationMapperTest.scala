package sp.services.relations

import akka.actor._
import akka.testkit._
import com.typesafe.config._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import sp.domain._
import sp.system.messages._
import sp.system.{ServiceExample, ServiceHandler}
import sp.domain.Logic._

import scala.concurrent.duration._

class VariableOperationMapperTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("myTest", ConfigFactory.parseString(
    """
      |akka.loglevel = INFO
    """.stripMargin)))

  val p = TestProbe()
  val e = TestProbe()
  val s = system.actorOf(VariableOperationMapper.props)

  override def beforeAll: Unit = {

  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "The Service Example" must {
    "filter the items" in {
      val t1 = Thing("t1")
      val t2 = Thing("t2")
      val t3 = Thing("t3")

      val parserG = sp.domain.logic.PropositionParser(List(t1, t2, t3))
      val parserA = sp.domain.logic.ActionParser(List(t1, t2, t3))

      val g1 = parserG.parseStr("t1 == hej").right.get
      val g2 = parserG.parseStr("t1 == då and t2 == 0").right.get
      val g3 = parserG.parseStr("t3 == foo").right.get
      val g4 = parserG.parseStr("t2 == 3 and t3 == bar").right.get

      val a1 = parserA.parseStr("t1 = då").right.get
      val a2 = parserA.parseStr("t2 = 200").right.get
      val a3 = parserA.parseStr("t3 = foobar").right.get


      val o1 = Operation("o1", List(PropositionCondition(g1, List(a1))))
      val o2 = Operation("o2", List(PropositionCondition(AND(List(g2, g4)), List(a1, a2))))
      val o3 = Operation("o3", List(PropositionCondition(g3, List(a3))))

      val longList: List[IDAble] = List(t1, t2, t3, o1, o2, o3)


      val r = Request("test",
        SPAttributes(),
        longList
      )

      val vdmT1 = VariableDomainMap(id = t1.id,
        domain = Set(SPValue("hej"), SPValue("då")),
        inGuards = Set(SPValue("hej"), SPValue("då")),
        inActions = Set( SPValue("då"))
      )
      val vdmT2 = VariableDomainMap(id = t2.id,
        domain = Set(SPValue(0), SPValue(3), SPValue(200)),
        inGuards = Set(SPValue(0), SPValue(3)),
        inActions = Set(SPValue(200))
      )
      val vdmT3 = VariableDomainMap(id = t3.id,
        domain = Set(SPValue("foo"), SPValue("bar"), SPValue("foobar")),
        inGuards = Set(SPValue("foo"), SPValue("bar")),
        inActions = Set( SPValue("foobar"))
      )

      val ovmo1 = OperationVariableMap(id = o1.id,
        guardVars = Set(t1.id),
        actionsVar = Set(t1.id)
      )
      val ovmo2 = OperationVariableMap(id = o2.id,
        guardVars = Set(t1.id, t2.id, t3.id),
        actionsVar = Set(t1.id, t2.id)
      )
      val ovmo3 = OperationVariableMap(id = o3.id,
        guardVars = Set(t3.id),
        actionsVar = Set(t3.id)
      )

      val vomT1 = VariableOperationMap(id = t1.id,
        guardOps = Set(o1.id, o2.id),
        actionsOps = Set(o1.id, o2.id)
      )
      val vomT2 = VariableOperationMap(id = t2.id,
        guardOps = Set(o2.id),
        actionsOps = Set(o2.id)
      )
      val vomT3 = VariableOperationMap(id = t3.id,
        guardOps = Set(o2.id, o3.id),
        actionsOps = Set(o3.id)
      )

      val correct = VariableOperationMapResult(
        List(vdmT1, vdmT2, vdmT3),
        List(ovmo1, ovmo2, ovmo3),
        List(vomT1, vomT2, vomT3)
      )


      s ! r

      fishForMessage(3 seconds){
        case x: Response => {
          val resMap = x.attributes.getAs[VariableOperationMapResult]("variableOperationMap")
          resMap.map{ m =>
            assert(m.variableDomain == correct.variableDomain)
            assert(m.operationMap == correct.operationMap)
            assert(m.variableMap == correct.variableMap)

          }
          true
        }
        case x => println(x); false
      }
    }
  }
}

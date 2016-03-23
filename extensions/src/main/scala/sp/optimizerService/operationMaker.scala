import sp.domain._
import sp.psl._
import sp.domain.Logic._


/**
  * Created by Dell on 2016-03-22.
  */
class operationMaker {
  //Operation Init
  /*
  Thing (Namn, attributes: ?SPAttributes = SPAttributes()?, id)

  Operation(name: String,
                     conditions: List[Condition] = List(),
                     attributes: SPAttributes = SPAttributes(),
                     id: ID = ID.newID)
För exempel:
sp/core/src/test/scala/sp/services/relations/variableOperationMapperTest.scala

      val t1 = Thing("t1")
      val t2 = Thing("t2")
      val t3 = Thing("t3")

      val parserG = sp.domain.logic.PropositionParser(List(t1, t2, t3))
      val parserA = sp.domain.logic.ActionParser(List(t1, t2, t3))

      val g1 = parserG.parseStr("t1 == hej").right.get
      val g2 = parserG.parseStr("t1 == då and t2 == 0").right.get
      val g3 = parserG.parseStr("t3 == foo").right.get
      val g4 = parserG.parseStr("t2 == 3 and t3 == bar").right.get

      val a1 = parserA.parseStr("t1 = r1.moveToFixture").right.get
      val a2 = parserA.parseStr("t2 = 200").right.get
      val a3 = parserA.parseStr("t3 = foobar").right.get


      val o1 = Operation("o1", List(PropositionCondition(g1, List(a1))))
      val o2 = Operation("o2", List(PropositionCondition(AND(List(g2, g4)), List(a1, a2))))
      val o3 = Operation("o3", List(PropositionCondition(g3, List(a3))))

   */

  val t1 = Thing("t1")

  val parserG = sp.domain.logic.PropositionParser(List(t1))
  val parserA = sp.domain.logic.ActionParser(List(t1))

  val g1 = parserG.parseStr("t1 == systemReady AND buildOrder").right.get
  val a1 = parserA.parseStr("t1 = r1.moveToFixture").right.get

  val o1 = Operation("init", List(PropositionCondition(g1, List(a1))))
}
package sp.psl

import akka.actor._
import sp.domain.logic.IDAbleLogic
import scala.concurrent._
import sp.system.messages._
import sp.system._
import sp.domain._
import sp.domain.Logic._


object PSLModel extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "models",
      "description" -> "Creates items describing the PSL system"
    )
  )
  val transformation: List[TransformValue[_]] = List()
  def props = Props(classOf[PSLModel])
}

class PSLModel extends Actor with ServiceSupport with ModelMaking {
  import context.dispatcher

  def receive = {
    case r @ Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get

      // Resources
      val r2 = makeResource("r2",
        List("position", "mode"),
        List("moveHomeToFixture"->List("speed"), "moveFixtureToHome"->List())
      )

      val flS2 = makeResource("flexlinkS2",
        List("stopper", "mode"),
        List("open"->List(), "close"->List())
      )

      val connection = Thing("PLCConnection", SPAttributes(
        "connection"->SPAttributes(
          makeTuple(DBConnection("R2.moveHomeToFixture", "boolean", 150, 0, 0)),
          makeTuple(DBConnection("R2.moveFixtureToHome", "boolean", 150, 0, 1)),
          makeTuple(DBConnection("R2.moveFixtureToHome.speed", "int", 150, 2, 0)),
          makeTuple(DBConnection("R2.position", "int", 151, 0, 0, Map(0->"home", 1->"atFlexlink", 2->"atFixture"))),
          makeTuple(DBConnection("R2.mode", "int", 151, 0, 1, Map(0->"notReady", 1->"ready", 2->"working"))),
          makeTuple(DBConnection("flexlinkS2.open", "boolean", 160, 0, 0)),
          makeTuple(DBConnection("flexlinkS2.close", "boolean", 160, 0, 1)),
          makeTuple(DBConnection("flexlinkS2.stopper", "boolean", 161, 0, 0)),
          makeTuple(DBConnection("flexlinkS2.mode", "int", 161, 2, 0, Map(0->"notReady", 1->"ready", 2->"working")))
        )
      ))

      val root = HierarchyRoot("Resources", List(r2._1, flS2._1))
      val items = r2._2 ++ flS2._2 :+ root :+ connection


      replyTo ! Response(items, SPAttributes("info"->"Items created from PSLModel service"), rnr.req.service, rnr.req.reqID)

    }
  }
}


case class DBConnection(name: String, valueType: String, db: Int, byte: Int = 0, bit: Int = 0, intMap: Map[Int, String] = Map())

trait ModelMaking {
  def makeResource(name: String, state: List[String], abilities: List[(String, List[String])]) = {
    val t = Thing(name)
    val stateVars = state.map(x => Thing(s"$name.$x"))
    val ab = abilities.map{case (n, parameters) =>
      val abilityName = name +"."+n
      val o = Operation(abilityName, List(), SPAttributes("operationType"->"ability"))
      (o, parameters.map(x => Thing(abilityName+"."+x)))
    }
    val abHir = ab.map{case (op, para) => HierarchyNode(op.id, para.map(p => HierarchyNode(p.id)))}
    val hier = HierarchyNode(t.id, stateVars.map(x => HierarchyNode(x.id)) ++ abHir)
    val temp: List[IDAble] = t :: stateVars ++ ab.map(_._1) ++ ab.flatMap(_._2)

    (hier, temp)
  }

  def makeTuple(db: DBConnection) = {
    db.name -> db
  }

}







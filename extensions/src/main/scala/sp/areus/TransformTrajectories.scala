package sp.areus

import akka.actor._
import scala.concurrent._
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._
import sp.system._
import scala.util.{Success, Try}


//case class Pose(time: Double, joints: List[Double])
case class ZoneMark(name: String, start: Double, end: Double)


object TransformTrajectories extends SPService  {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "import",
        "description" -> "Transform a trajectory"
    ),
    "trajectory" -> KeyDefinition("ID", List(), None)
  )
  val transformTuple  = (
    TransformValue("trajectory", _.getAs[ID]("trajectory"))
  )
  val transformation = transformToList(transformTuple.productIterator.toList)
  def props = ServiceLauncher.props(Props(classOf[TransformTrajectories]))
}

class TransformTrajectories extends Actor with ServiceSupport with TrajectoryLogic {
  import context.dispatcher
  def receive = {
    case r@Request(service, attr, ids, reqID) => {
      println(s"service got: $attr")
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val progress = context.actorOf(progressHandler)

      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get
      val idMap = ids.map(x => x.id -> x).toMap

      val trajectory = transform(TransformTrajectories.transformTuple)
      val root = tryWithOption(idMap(trajectory).asInstanceOf[HierarchyRoot]).get

      val opsNCh = getOperationsAndItsChildren(idMap, root)
      val opsNZones = opsNCh.map { case (o, ch) =>
        val zones = ch.filter { t =>
          t.isInstanceOf[Thing] &&
            t.attributes.getAs[ZoneMark]("mark").nonEmpty &&
            t.attributes.dig[String]("mark", "type").get == "zone"
        }
        o -> zones.map(_.attributes.getAs[ZoneMark]("mark").get)
      }

      val operationsDiviedOnZones = splitOperationDueToZones(opsNZones)
      val newHierarchy = addItemsToHierarchy(root, operationsDiviedOnZones.toList)
      val sopSpecs = createSopSpecs(operationsDiviedOnZones)
      val chOps = operationsDiviedOnZones.flatMap(_._2).toList

      val updOps = addConditionsDueToSpec(chOps, sopSpecs)

      println("opsNCh: " + opsNCh.map(x => x._1.name -> x._2))
      println("zones: " + opsNZones.map(x => x._1.name -> x._2))
      println("ops: " + operationsDiviedOnZones.map(x => x._1.name -> x._2.map(_.name)))
      println("sops: " + sopSpecs)

      replyTo ! Response(updOps :+ newHierarchy, SPAttributes("info" -> "transformed trajectory"), service, reqID)

    }


  }
}

// TODO move to core
trait HierarchyLogic {



}

// Håller på att städa
trait TrajectoryLogicCleaning {
  implicit class HierarchyExtras(x: HierarchyRoot){
    def toIDAbleHierarchy(ids: List[IDAble]): IDAbleHierarchy = {
      val idMap = ids.map(i => i.id -> i).toMap
      def itr(node: HierarchyNode): Option[IDAbleHierarchy] = {
        val ch = node.children.flatMap(itr)
        idMap.get(node.item).map(IDAbleHierarchy(_, ch))
      }
      IDAbleHierarchy(x,x.children.flatMap(itr))
    }
  }


  implicit class IDAbleHierarchyExtras(x: IDAbleHierarchy){
    def toHierarchy: HierarchyRoot = {
      def itr(node: IDAbleHierarchy): HierarchyNode = {
        val ch = node.children.map(itr)
        HierarchyNode(node.item.id, ch)
      }
      val newH = if (x.item.isInstanceOf[HierarchyRoot])
        x.item.asInstanceOf[HierarchyRoot]
      else
        HierarchyRoot(x.item.name + "H")

      // fixa så att idn från gamla rooten kommer med
      newH.copy(children = x.children.map(itr))
    }
  }
}

trait GanttLogic {

}



trait TrajectoryLogic {
  // TODO: Fix this since .isInstanceOf[Some[Thing]] doesn't work
  def getOperationsAndItsChildren(idMap: Map[ID, IDAble], root: HierarchyRoot) = {
    (for {
      ch <- root.children
      if idMap.get(ch.item).isInstanceOf[Some[Thing]]
      tch <- ch.children
      if idMap.get(tch.item).isInstanceOf[Some[Operation]]
      och <- tch.children
      child <- idMap.get(och.item)
    } yield (idMap(tch.item).asInstanceOf[Operation], child)).
      foldLeft(Map[Operation, List[IDAble]]()){(result, tuple) =>
      val op = tuple._1
      val item = tuple._2
      result + (op -> (item :: result.getOrElse(tuple._1, List[IDAble]())))
    }
  }

  def splitOperationDueToZones(opsNZones: Map[Operation, List[ZoneMark]]) = {
    opsNZones.map { case (o, zones) =>
      val poses = o.attributes.getAs[List[Pose]]("poses").get
      val zoneNp = zones.map { z =>
        val zonePoses = poses.foldLeft(List[Pose]()) { (list, p) =>
          if (p.time >= z.start && p.time <= z.end)
            list :+ p
          else list
        }
        z -> zonePoses
      }.toMap
      val zonePoses = zoneNp.values.flatten.toSet
      val init = (List[Operation](), List[Pose]())
      var opCounter = 1
      val opsNAggr = poses.foldLeft(init) { (tuple, p) =>
        val xs = tuple._1
        val aggr = tuple._2
        if (zonePoses.contains(p) && aggr.nonEmpty) {
          val newO = Operation(o.name + s"_$opCounter", List(), SPAttributes("poses" -> aggr, "time"->getDuration(aggr, poses)))
          opCounter += 1
          val zone = zoneNp.find(_._2.contains(p)).get
          val zoneOp = Operation(o.name + s"_$opCounter", List(),
            SPAttributes("poses" -> zone._2, "mark" -> zone._1, "time"->getDuration(zone._2, poses)))
          opCounter += 1
          (xs ++ List(newO, zoneOp), List())
        } else if (zonePoses.contains(p)) {
          tuple
        } else {
          (xs, aggr :+ p)
        }
      }
      val ops = if (opsNAggr._2.isEmpty) opsNAggr._1
      else
        opsNAggr._1 :+ Operation(o.name + s"_$opCounter", List(), SPAttributes("poses" -> opsNAggr._2, "time"->getDuration(opsNAggr._2, poses)))
      o -> ops
    }
  }

  def getDuration(poses: List[Pose], allPoses: List[Pose]) = {
    val start = poses.head.time
    val endP = getNextPose(allPoses, Try(poses.last).getOrElse(poses.head))
    val end = endP.time
    end - start
  }

  def getNextPose(poses: List[Pose], p: Pose) = {
    var resPos = p;
    var found = false;
    poses.foreach(aP => {
      if (aP == p) found = true
      else if (found){
        resPos = aP
        found = false
      }
    })
    resPos
  }

  def addItemsToHierarchy(root: HierarchyRoot, items: List[(IDAble, List[IDAble])]) = {
    def itr(node: HierarchyNode, parent: ID, children: List[ID]): HierarchyNode = {
      node match {
        case x @ HierarchyNode(`parent`, xs, id) =>
          val newCH = children.map(ch => HierarchyNode(ch))
          x.copy(children = xs ++ newCH)
        case x @ HierarchyNode(other, xs, id) =>
          val newCH = xs.map(ch => itr(ch, parent, children))
          if (newCH != xs)
            x.copy(children = newCH)
          else
            x
      }
    }
    val itemsAsID = items.map(i => i._1.id -> i._2.map(_.id))
    // Do not handle adding to the root
    itemsAsID.foldLeft(root){(r, tuple) =>
      val upd = r.children.map(ch => itr(ch, tuple._1, tuple._2))
      r.copy(children = upd)
    }
  }

  def createSopSpecs(opsMap: Map[Operation, List[Operation]]): List[SOP] = {
    val sequences = opsMap.map{case (o, seq) => Sequence(opListToHierarchies(seq):_*)}.toList
    val grouped = opsMap.values.flatten.foldLeft(Map[String, List[Operation]]()){(map, op)=>
      val mark = op.attributes.getAs[ZoneMark]("mark")
      mark.map{zone =>
        map + (zone.name -> (op +: map.getOrElse(zone.name, List())))
      }.getOrElse(map)
    }
    val arbitrary = for {
      tuple <- grouped if tuple._2.size > 1
    } yield Arbitrary(opListToHierarchies(tuple._2):_*)
    sequences ++ arbitrary
  }

  def opListToHierarchies(xs: List[Operation]) = xs.map(x => Hierarchy(x.id, List()))

  def addConditionsDueToSpec(ops: List[Operation], spec: List[SOP]) = {
    val conditions = sp.domain.logic.SOPLogic.extractOperationConditions(spec, "traj")
    ops.map { o =>
      val cond = conditions.get(o.id).map(List(_)).getOrElse(List())
      o.copy(conditions = cond)
    }
  }


    def tryWithOption[T](t: => T): Option[T] = {
      try {
        Some(t)
      } catch {
        case e: Exception => None
      }
    }


  }




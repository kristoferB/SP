//package sp.merger
//
//import akka.actor._
//import akka.pattern.ask
//import akka.util.Timeout
//import sp.domain._
//import sp.system.messages._
//import scala.concurrent.Future
//import scala.concurrent.duration._
//
//
//
///**
// * Created by Kristofer on 2014-06-27.
// */
//class ProductAbilityMerger(modelHandler: ActorRef) extends Actor {
//  implicit val timeout = Timeout(1 seconds)
//  import context.dispatcher
//
//  def receive = {
//    case Request(_, attr) => {
//      val reply = sender
//
//      val attributes = extractAttr(attr)
//
//      attributes.foreach(x =>
//        println(s"Attributes: $x")
//      )
//
//      val items = getItems(attributes)
//
//      val prodnAb = prodAndAb(attributes, items)
//
//      prodnAb.foreach(x =>
//        println(s"prodnAB: $x")
//      )
//
//      val res = for {
//        pnA <- prodnAb
//        xs <- items
//      } yield {
//        val opsInProd = getAllOperations(pnA._1, xs.items.toSet)
//        val opsInAbil = getAllOperations(pnA._2, xs.items.toSet)
//
//        val matchThem = matchOperations(opsInProd, opsInAbil)
//        matchThem.foreach{kv =>
//          val name = kv._1.name
//          val names = kv._2.map(_.name)
//          println(name +": "+names)
//          kv._2.foreach(o => println(o.conditions))
//        }
//
//
//        val newOps: List[IDAble] = matchThem.values.toList.flatten.distinct
//        val spec = SPSpec("merged", Attr("children"-> ListPrimitive(newOps.map(o => IDPrimitive(o.id)))))
//        spec :: newOps
//
//      }
//
//      val sendInfo = for {
//        attr <- attributes
//        items <- res
//      } yield {
//        val modelID = attr._1
//        modelHandler ! UpdateIDs(modelID, -1, items)
//        reply ! SPIDs(items)
//      }
//
//      sendInfo.onFailure{case t: Throwable => reply ! SPError(t.getMessage)}
//
//    }
//  }
//
//  def matchOperations(opsInProd: Set[Operation], opsInAbil: Set[Operation]) = {
//    opsInProd.flatMap{ o =>
//      val base = o.attributes.getAsString("base")
//      val abilities = opsInAbil.filter(_.attributes.getAsString("base") == base)
//
//      println(s"${o.name} matches: ${abilities.map(_.name)}")
//
//
//
//      val mergeOps = abilities.map{ a =>
//
//        val newID = ID.newID
//
//        val prodcond = o.conditions.map {
//          case c: PropositionCondition => {
//            c.copy(attributes = c.attributes +("group", "product"))
//          }
//        }
//
//        val abC = a.conditions.map {
//          case c: PropositionCondition => c.copy(attributes = c.attributes +("group", "ability"))
//        }
//
//        a -> Operation(o.name + "-" + a.name, prodcond ++ abC,
//          Attr(
//            "product" -> MapPrimitive(o.attributes.attrs + ("productID" -> o.id)),
//            "ability" -> MapPrimitive(a.attributes.attrs + ("abilityID" -> a.id))
//          ),
//          newID
//        )
//      }.toMap
//
//      val alternate = addAlternatives(mergeOps.values.toList)
//      val mergeOpsUpdate = mergeOps map{
//        case (op1, op2) => {
//          val newOp2 = alternate.find(_.id == op2.id).get
//          op1 -> newOp2
//        }
//      }
//
//      val prodMap = Map(o -> mergeOpsUpdate.map(_._2).toSet)
//      val abilityMap = mergeOpsUpdate.map(kv => kv._1 -> Set(kv._2)).toMap
//      prodMap ++ abilityMap
//
//    }.toMap
//  }
//
//  def addAlternatives(ops: List[Operation]) = {
//    val cond = ops.map(o =>  EQ(o.id, "i"))
//
//    ops.map { o =>
//      val and = AND(cond.filter(_.left != SVIDEval(o.id)))
//      val newC = PropositionCondition(and, List(), Attr("kind" -> "pre", "group" -> "product"))
//      o.copy(conditions = newC :: o.conditions)
//    }
//  }
//
//  def prodAndAb(attributes: Future[(ID, ID, ID)], items: Future[SPIDs]) = {
//    for {
//      at <- attributes
//      it <- items
//      prod <- futurize(it.items.find(_.id == at._2))
//      abil <- futurize(it.items.find(_.id == at._3))
//    } yield {
//      (prod, abil)
//    }
//  }
//
//
//  private case class Collector(ops: Set[Operation], visited: Set[IDAble])
//  def getAllOperations(root: IDAble, allItems: Set[IDAble]) = {
//
//    def getChildren(x: IDAble) = {
//      val childrenIDs = getChildrenIds(x)
//      childrenIDs flatMap (id => allItems.find(_.id == id))
//    }
//
//    def req(items: List[IDAble], c: Collector): Collector = {
//      items match {
//        case Nil => c
//        case (x: Operation) :: xs => {
//          val newC = c.copy(ops = c.ops + x)
//          req(xs, newC)
//        }
//        case x :: xs if c.visited.contains(x) => req(xs, c)
//        case x :: xs => {
//          val children = getChildren(x)
//          val newC = c.copy(visited = c.visited + x)
//          req(children ++ xs, newC)
//        }
//      }
//
//    }
//
//    val rootCh = getChildren(root)
//    val collector = req(rootCh, Collector(Set(), Set()))
//    collector.ops
//  }
//
//  def getChildrenIds(item: IDAble) = {
//    val lp = item.attributes.getAsList("children").getOrElse(List())
//    for {
//      a <- lp
//      id <- a.asID
//    } yield id
//  }
//
//  def extractAttr(attr: SPAttributes) = {
//    val temp = (for {
//      m <- attr.getAsID("model")
//      p <- attr.getAsID("product")
//      a <- attr.getAsID("abilities")
//    } yield (m, p, a))
//    futurize(temp, "Missing Attributes")
//  }
//
//  def getItems(attr: Future[(ID, ID, ID)]) = {
//    attr.flatMap{case (m, p, a) =>
//      (modelHandler ? GetIds(m, List())).mapTo[SPIDs]
//    }
//  }
//
//  def futurize[T](o: Option[T], error: String = "") = {
//    o.map(Future(_)).getOrElse(Future.failed(new Throwable(error)))
//  }
//
//
//
//
//
//
//
//
//}
//
//
//object ProductAbilityMerger{
//  def props(modelHandler: ActorRef) = Props(classOf[ProductAbilityMerger], modelHandler)
//}

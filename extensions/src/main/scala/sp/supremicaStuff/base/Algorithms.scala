package sp.supremicaStuff.base

import scala.collection.JavaConverters._
import org.supremica.automata.algorithms.EditorSynthesizerOptions
import org.supremica.automata.algorithms.SynthesisType
import org.supremica.automata.algorithms.SynthesisAlgorithm
import org.supremica.automata.BDD.EFA.BDDExtendedSynthesizer
import org.supremica.automata.ExtendedAutomata
import net.sourceforge.waters.xsd.base.EventKind
import java.util.Vector
import net.sourceforge.waters.model.module.EnumSetExpressionProxy
import org.supremica.automata.IO.ProjectBuildFromWaters
import net.sourceforge.waters.model.marshaller.DocumentManager
import org.supremica.automata.Automata
import org.supremica.automata.Automaton
import org.supremica.automata.algorithms.SynthesizerOptions
import org.supremica.automata.algorithms.SynchronizationOptions
import org.supremica.automata.algorithms.AutomataSynthesizer
import org.supremica.automata.algorithms.SynchronizationType
import org.supremica.properties.Config
import org.supremica.automata.BDD.EFA.BDDExtendedAutomata
import org.supremica.util.ActionTimer
import org.supremica.automata.BDD.EFA.BDDExtendedManager
import net.sourceforge.waters.model.module.SimpleExpressionProxy
import net.sourceforge.waters.model.expr.Operator
import org.supremica.automata.BDD.EFA.BDDExtendedAutomata
import org.supremica.util.BDD.BDDAutomata
import scala.util.parsing.combinator.RegexParsers

trait Algorithms extends BaseFunctionality with RegexParsers {

  def getSupervisorGuards(synthesisType: SynthesisAlgorithm = SynthesisAlgorithm.PARTITIONBDD): Option[Map[String, String]] = {
    getVariables.foreach(v => if (v.getType().isInstanceOf[EnumSetExpressionProxy]) { println("Algorithm can't handle enum sets"); return None })

    lazy val options = new EditorSynthesizerOptions()
    options.setSynthesisType(SynthesisType.NONBLOCKINGCONTROLLABLE)
    options.setSynthesisAlgorithm(synthesisType)
    //    options.setSynthesisAlgorithm(SynthesisAlgorithm.MONOLITHICBDD)
    options.setReachability(true)
    options.setMaximallyPermissive(true)
    lazy val bddSynthesizer = new BDDExtendedSynthesizer(new ExtendedAutomata(mModule), options)
    lazy val manager = bddSynthesizer.bddAutomata.getManager()

    //Preload forbidden states directly into BDD synthesizer if forbidden expressions given in comment.
    if (!getComment.split("\n").filter(_.startsWith(TextFilePrefix.FORBIDDEN_PREFIX)).isEmpty) {
      try {
        def seps = getComment.split("\n").map(str => parse(s"${TextFilePrefix.FORBIDDEN_PREFIX}".r ~> s"(.*)".r, str) match {
          case Success(exp, _) => Some(mParser.parse(exp, Operator.TYPE_BOOLEAN).asInstanceOf[SimpleExpressionProxy])
          case _ => None
        }).flatten
        lazy val forbiddenStatesAsBdd = seps.foldLeft((seps.size, manager.getZeroBDD())) {
          case ((sepsLeft, acc), sep) => if (sepsLeft % 200 == 0) { println(s"$sepsLeft additions of forbidden state combinations left") }; (sepsLeft - 1, acc.orWith(manager guard2BDD sep))
        }._2
        bddSynthesizer.bddAutomata.mForbiddenStates = forbiddenStatesAsBdd
      } catch {
        case _: Throwable => println("Problem when parsing forbidden state combinations from comment\nor\nBuilding BDD of these combinations."); return None
      }
    }
    //-----

    try {
      bddSynthesizer.synthesize(options)
    } catch {
      case _: Throwable => println("Problem to calculate supervisor."); return None
    }
    lazy val nbrOfStates = bddSynthesizer.nbrOfStates()
    if (bddSynthesizer == null | nbrOfStates == 0) { return None }
    // Guard extraction		
    lazy val eventNames = new Vector(getAlphabet.filter(_.getKind == EventKind.CONTROLLABLE).map(_.getName).toIndexedSeq.toVector.asJavaCollection)
    options.setExpressionType(2) // 0=fromForbiddenStates, 1=fromAllowedStates, 2=mix
    bddSynthesizer.generateGuard(eventNames, options)
    lazy val eventGuardMap = bddSynthesizer.getEventGuardMap().asScala.map { case (event, bddegg) => event -> bddegg.getGuard() }.toMap

    println("Nbr of states in supervisor: " + nbrOfStates)
    Some(eventGuardMap)
  }

  def getDFA: Option[Iterable[Automaton]] = {
    try {
      val proj = new ProjectBuildFromWaters(new DocumentManager())
      Config.OPTIMIZING_COMPILER.set(true)
      val automata = proj.build(mModule).asInstanceOf[Automata]
      return Some(automata.asScala)
    } catch { case t: Throwable => println(t); None }
  }
  
  def getAutomataSupervisor(synthesisType: SynthesisAlgorithm = SynthesisAlgorithm.MONOLITHIC): Option[Iterable[Automaton]] = {
    getDFA match {
      case Some(as) => getAutomataSupervisor(as,synthesisType)
      case _ => None
    }
  }

  def getAutomataSupervisor(automatonIt: Iterable[Automaton], synthesisType: SynthesisAlgorithm): Option[Iterable[Automaton]] = {
    val automata = new Automata()
    automatonIt.foreach(automata.addAutomaton)
    val syntho = new SynthesizerOptions()
    syntho.setSynthesisType(SynthesisType.NONBLOCKINGCONTROLLABLE)
    syntho.setSynthesisAlgorithm(synthesisType)
    syntho.setSynthesisAlgorithm(SynthesisAlgorithm.COMPOSITIONAL_WATERS)
    syntho.setPurge(true)
    val syncho = new SynchronizationOptions()
    syncho.setSynchronizationType(SynchronizationType.FULL)
    val as = new AutomataSynthesizer(automata, syncho, syntho)
    try {
      val supervisors = as.execute()
      return Some(supervisors.asScala)
    } catch {
      case e: Exception => println(e); None
    }
  }

}
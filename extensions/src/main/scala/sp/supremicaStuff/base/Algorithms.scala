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

  case class SupervisorAsBDD(synthesisType: SynthesisAlgorithm = SynthesisAlgorithm.PARTITIONBDD) {
    //Init of variables
    var supervisorExists = false
    lazy val options = new EditorSynthesizerOptions()
    options.setSynthesisType(SynthesisType.NONBLOCKINGCONTROLLABLE)
    options.setSynthesisAlgorithm(synthesisType)
    //    options.setSynthesisAlgorithm(SynthesisAlgorithm.MONOLITHICBDD)
    options.setReachability(true)
    options.setMaximallyPermissive(true)
    lazy val bddSynthesizer = new BDDExtendedSynthesizer(new ExtendedAutomata(mModule), options)
    lazy val manager = bddSynthesizer.bddAutomata.getManager()
    def nbrOfStates() = if(supervisorExists) Some(bddSynthesizer.nbrOfStates) else None

    //Start to calculate a supervisor
    getVariables.foreach(v => if (v.getType().isInstanceOf[EnumSetExpressionProxy]) {
      println("Algorithm can't handle enum sets")
    })

    //Preload forbidden states directly into BDD synthesizer if forbidden expressions given in comment.
    if (getComment.split("\n").exists(_.startsWith(TextFilePrefix.FORBIDDEN_PREFIX))) {
      try {
        def seps = getComment.split("\n").flatMap(str => parse(s"${TextFilePrefix.FORBIDDEN_PREFIX}".r ~> s"(.*)".r, str) match {
          case Success(exp, _) => Some(mParser.parse(exp, Operator.TYPE_BOOLEAN).asInstanceOf[SimpleExpressionProxy])
          case _ => None
        })
        lazy val forbiddenStatesAsBdd = seps.foldLeft((seps.size, manager.getZeroBDD())) {
          case ((sepsLeft, acc), sep) => if (sepsLeft % 200 == 0) {
            println(s"$sepsLeft additions of forbidden state combinations left")
          };
            (sepsLeft - 1, acc.orWith(manager guard2BDD sep))
        }._2
        bddSynthesizer.bddAutomata.mForbiddenStates = forbiddenStatesAsBdd
      } catch {
        case _: Throwable => println("Problem when parsing forbidden state combinations from comment\nor\nBuilding BDD of these combinations.")
      }
    }
    //-----

    try {
      bddSynthesizer.synthesize(options)
    } catch {
      case _: Throwable => println("Problem to calculate supervisor.")
    }
    if (bddSynthesizer != null & bddSynthesizer.nbrOfStates != 0) {
      supervisorExists = true
    }
    //Done with the supervisor calculation

    /**
     * Returns the supervisor as extra guards for the controllable events.
     * There are 3 types of guards
     * "0": The event is always disabled, thus the extra guard is always false
     * "1": The event is always enabled,  thus no extra guard is required
     * "exp: The event is sometimes enabled/disabled. The extra guard should be appended to all transitions labeled by the event.
     * Returns None if no supervisor exits
     */
    lazy val getSupervisorGuards: Option[Map[String, String]] = {
      if (!supervisorExists) None
      else {
        lazy val eventNames = new Vector(getAlphabet.filter(_.getKind == EventKind.CONTROLLABLE).map(_.getName).toIndexedSeq.toVector.asJavaCollection)
        options.setExpressionType(2) // 0=fromForbiddenStates, 1=fromAllowedStates, 2=mix
        bddSynthesizer.generateGuard(eventNames, options)
        lazy val eventGuardMap = bddSynthesizer.getEventGuardMap().asScala.map { case (event, bddegg) => event -> bddegg.getGuard() }.toMap
        Some(eventGuardMap)
      }
    }

    /**
     *
     * @param state KeyValue map for a subset of the variables in the module with assigned values.
     * @return None if no supervisor exits, otherwise true (false) if the supervisor contains (not contains) the state
     */
    def containsState(state: Map[String, Int]): Option[Boolean] = {
      if (!supervisorExists) None
      else {
        val expAsString = state.map { case (k, v) => s"$k==$v" }.mkString("&")
        try {
          val exp = mParser.parse(expAsString, Operator.TYPE_BOOLEAN)
          val resultAsBDD = bddSynthesizer.getResult.and(manager.guard2BDD(exp))
          val containsState = !resultAsBDD.toStringWithDomains.equals("F")
//          println(s"Supervisor contains state: ${state.map { case (k, v) => s"$k:$v" }.mkString("[", ",", "]")} is $containsState")
          return Some(containsState)
        } catch {
          case _: Throwable => println(s"Problem to parse state to expression: $expAsString."); None
        }
      }
    }
  }

  def getDFA: Option[Iterable[Automaton]] = {
    try {
      val proj = new ProjectBuildFromWaters(new DocumentManager())
      Config.OPTIMIZING_COMPILER.set(true)
      val automata = proj.build(mModule).asInstanceOf[Automata]
      return Some(automata.asScala)
    } catch {
      case t: Throwable => println(t); None
    }
  }

  def getAutomataSupervisor(synthesisType: SynthesisAlgorithm = SynthesisAlgorithm.MONOLITHIC): Option[Iterable[Automaton]] = {
    getDFA match {
      case Some(as) => getAutomataSupervisor(as, synthesisType)
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
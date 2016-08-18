package sp.optimization.oscarmodels

import sp.system._
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._
import oscar.cp._

class VolvoRobots(params: SPAttributes) extends CPModel {
  def test = {
    val names = List("RS1_B940WeldSeg1", "RS1_B940WeldSeg2", "RS1_B941WeldSeg3", "RS1_B940WeldSeg4", "RS2_B940WeldSeg1", "RS2_B941WeldSeg2", "RS2_B940WeldSeg3", "RS2_B940WeldSeg4", "RS2_B941WeldSeg5", "RS2_B940WeldSeg6", "RS2_B941WeldSeg8", "RS2_B941WeldSeg9", "RS3_B941WeldSeg1", "RS3_B940WeldSeg2", "RS3_B941WeldSeg3", "RS3_B940WeldSeg4", "RS4_B940WeldSeg1", "RS4_B940WeldSeg2", "RS4_B941WeldSeg3", "RS4_B940WeldSeg4", "RS4_B940WeldSeg5")
    val nameMap = names.zipWithIndex.toMap
    val numTasks = names.size
    val durations = Array(1600,1000,1600,400,600,200,800,200,200,1200,1000,600,4618,2534,1539,2895,800,800,1000,200,2000);
    val totalDuration = durations.sum


    val precedences = List(("RS1_B940WeldSeg1","RS1_B940WeldSeg2"),("RS1_B940WeldSeg2","RS1_B941WeldSeg3"),("RS1_B941WeldSeg3","RS1_B940WeldSeg4"),("RS2_B940WeldSeg1","RS2_B941WeldSeg2"),("RS2_B941WeldSeg2","RS2_B940WeldSeg3"),("RS2_B940WeldSeg3","RS2_B940WeldSeg4"),("RS2_B940WeldSeg4","RS2_B941WeldSeg5"),("RS2_B941WeldSeg5","RS2_B940WeldSeg6"),("RS2_B940WeldSeg6","RS2_B941WeldSeg8"),("RS2_B941WeldSeg8","RS2_B941WeldSeg9"),("RS3_B941WeldSeg1","RS3_B940WeldSeg2"),("RS3_B940WeldSeg2","RS3_B941WeldSeg3"),("RS3_B941WeldSeg3","RS3_B940WeldSeg4"),("RS4_B940WeldSeg1","RS4_B940WeldSeg2"),("RS4_B940WeldSeg2","RS4_B941WeldSeg3"),("RS4_B941WeldSeg3","RS4_B940WeldSeg4"),("RS4_B940WeldSeg4","RS4_B940WeldSeg5"))

    val mutexes = List(("RS4_B941WeldSeg3", "RS2_B941WeldSeg2"), ("RS4_B940WeldSeg4", "RS2_B940WeldSeg1"), ("RS4_B941WeldSeg3", "RS2_B940WeldSeg3"), ("RS4_B940WeldSeg2", "RS2_B941WeldSeg2"), ("RS4_B940WeldSeg2", "RS2_B940WeldSeg3"), ("RS4_B940WeldSeg2", "RS2_B940WeldSeg1"), ("RS4_B941WeldSeg3", "RS2_B940WeldSeg1"), ("RS4_B940WeldSeg4", "RS2_B941WeldSeg2"), ("RS4_B940WeldSeg4", "RS2_B940WeldSeg3"),("RS4_B940WeldSeg2", "RS1_B941WeldSeg3"), ("RS4_B941WeldSeg3", "RS1_B940WeldSeg4"), ("RS4_B940WeldSeg1", "RS1_B940WeldSeg4"), ("RS4_B941WeldSeg3", "RS1_B941WeldSeg3"), ("RS4_B940WeldSeg1", "RS1_B941WeldSeg3"), ("RS4_B940WeldSeg2", "RS1_B940WeldSeg4"), ("RS1_B941WeldSeg3", "RS4_B940WeldSeg4"), ("RS4_B940WeldSeg4", "RS1_B940WeldSeg4"),("RS3_B940WeldSeg4", "RS1_B940WeldSeg1"), ("RS3_B940WeldSeg2", "RS1_B940WeldSeg1"), ("RS3_B941WeldSeg3", "RS1_B940WeldSeg1"),("RS4_B940WeldSeg5", "RS2_B940WeldSeg4"), ("RS4_B940WeldSeg5", "RS2_B940WeldSeg1"), ("RS4_B940WeldSeg5", "RS2_B941WeldSeg2"), ("RS4_B940WeldSeg5", "RS2_B940WeldSeg3"),("RS2_B941WeldSeg8", "RS3_B941WeldSeg1"))
 
    var startTimes = Array.fill(numTasks)(CPIntVar(0, totalDuration))
    var endTimes = Array.fill(numTasks)(CPIntVar(0, totalDuration))
    var makespan = CPIntVar(0 to totalDuration)

    precedences.foreach { case (t1,t2) => add(startTimes(nameMap(t1)) + durations(nameMap(t1)) <= startTimes(nameMap(t2)) ) }
    mutexes.foreach { case (t1,t2) =>
      val leq1 = startTimes(nameMap(t1)) + durations(nameMap(t1)) <== startTimes(nameMap(t2))
      val leq2 = startTimes(nameMap(t2)) + durations(nameMap(t2)) <== startTimes(nameMap(t1))
      add(leq1 || leq2)
    }

    search(binaryFirstFail(startTimes ++ Array(makespan)))

    onSolution {
      println("Start times: ")
      nameMap.foreach { case (name, index) =>
        println(name + ": " + startTimes(index))
      }
    }

    // Execution, search for one solution
    val stats = start(nSols = 1)
    println("stats " + stats)
  }
}

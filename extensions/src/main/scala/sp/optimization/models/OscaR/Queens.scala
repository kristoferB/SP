package sp.optimization.oscarmodels

import sp.optimization.OscaRModel
import oscar.cp.{CPModel, _}

class Queens(data: String) extends OscaRModel(data) with CPModel {

  val nQueens = 8 // Number of queens
  val Queens = 0 until nQueens

  // Variables
  val queens = Array.fill(nQueens)(CPIntVar.sparse(0, nQueens - 1))
  var solution: Array[AnyVal] = Array.fill(nQueens)(-1)

  // Constraints
  add(allDifferent(queens))
  add(allDifferent(Queens.map(i => queens(i) + i)))
  add(allDifferent(Queens.map(i => queens(i) - i)))

  // Search heuristic
  search(binaryFirstFail(queens))

  onSolution {
    solution = queens map (x => x.value)
  }

  // Execution, search for one solution
  val stats = start(nSols = 1)

}
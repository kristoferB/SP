package astar
import state.State
import move.Move
import scala.collection.mutable.ArrayBuffer
import Array._
object Astar {
	def main(args: Array[String]) {
		var leftPlates1: Array[Byte] = Array(1,1,1,1,2,3,4,0,0,0,0,0,0,0,0,0)
		var rightPlates1: Array[Byte] = Array(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)
		var middle1: Array[Byte] = Array(0,0,0,0)
		var leftRobot1: Byte = 0
		var rightRobot1: Byte = 0
		
		var leftPlates2: Array[Byte] = Array(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)
		var rightPlates2: Array[Byte] = Array(1,1,1,1,2,3,4,0,0,0,0,0,0,0,0,0)
		var middle2: Array[Byte] = Array(0,0,0,0)
		var leftRobot2: Byte = 0
		var rightRobot2: Byte = 0
	
		var desiredState = new State(leftPlates2, rightPlates2, middle2, leftRobot2, rightRobot2, 0, null, null)
		var start = new State(leftPlates1, rightPlates1, middle1, leftRobot1, rightRobot1, 0, desiredState, ArrayBuffer[Move]())
		var finnish = start
		
		if(start.h != 0) {
			var states = ArrayBuffer[State]()
			var oldStates = ArrayBuffer[State]()
			states += start
			var foundSolution = false
			var currentIndex = 0;
			//State.printState(states(currentIndex))
			//var s = readLine
			while (!foundSolution) {
				var state = states(currentIndex)
				states.remove(currentIndex)
				oldStates += state
			
				var nextStates = state.nextStates
				for (newState <- nextStates) {
					if (!foundSolution && newState.h == 0) {
						foundSolution = true
						finnish = newState
					}
				}
				
				if(!foundSolution) {
					for (newState <- nextStates) {
						var equalsOtherState = false
						for(otherState <- states) equalsOtherState |= State.stateEquals(newState,otherState)
						for(otherState <- oldStates) equalsOtherState |= State.stateEquals(newState,otherState)
						if(!equalsOtherState) states += newState
					}
					
					var fmin: Int = Int.MaxValue
					var h: Int = 0
					for (index <- 0 to states.length - 1 ) {
						if(states(index).f <= fmin) {
							h = states(index).h
							fmin = states(index).f
						}
					}
					for (index <- 0 to states.length - 1 ) {
						while(index < states.length && states(index).h > h + 1 ) {
							oldStates += states(index)
							states.remove(index)
						}
					}
					for (index <- 0 to states.length - 1 ) {
						if(states(index).f == fmin) currentIndex = index
					}
				//println(states.length)
				//State.printState(states(currentIndex))
				//var s = readLine
				}
				
			}
		}
		
		//State.printState(finnish)
		var moves = finnish.moves
		var s = ""
		for (i <- 0 to moves.length-1) s += moves(i).toString + ", "
		println(s)
		
		var parallellMoves = ofDim[Move](moves.length,2)
		var states = Array[State](moves.length)
		
		parallellMoves(0)(0) = moves(0)
		states(0) = start.doMove(moves(0))
		var index1 = 1
		var index2 = 0
		
	}
}


















package astar
import scala.collection.mutable.ArrayBuffer
import Array._
object Astar {
	def solver(startState: State) : Array[Move] = {
		var finish = startState
		
		if(startState.h != 0) {
			var states = ArrayBuffer[State]()
			var oldStates = ArrayBuffer[State]()
			states += startState
			var foundSolution = false
			var currentIndex = 0;
			
			while (!foundSolution) {
				var state = states(currentIndex)
				states.remove(currentIndex)
				oldStates += state
			
				var nextStates = state.nextStates
				for (newState <- nextStates) {
					if (!foundSolution && newState.h == 0) {
						foundSolution = true
						finish = newState
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
					
				}
				
			}
		}
		
		var moves = finish.moves
		return moves.toArray
		
		/*var parallellMoves = ofDim[Move](moves.length,2)
		var states = Array[State](moves.length)
		
		parallellMoves(0)(0) = moves(0)
		states(0) = startState.doMove(moves(0))
		var index1 = 1
		var index2 = 0*/
		
	}
}


















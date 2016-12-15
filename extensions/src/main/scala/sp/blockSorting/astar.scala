package astar
import scala.collection.mutable.ArrayBuffer
import Array._

class Collection( var state: BlockState, var moveUsed: Array[Boolean], var parallelMoves: ArrayBuffer[Boolean], var movesDone: Int, var movesLeft: Int) {
	var f = movesDone + movesLeft/2 + movesLeft%2
}

object Astar {
	def solver(startState: BlockState) : Array[Array[Move]] = {
		var finish = findStateSequence(startState)
		return	easyParallel(finish.moves.toArray)
	}
	
	private def findStateSequence(startState: BlockState) : BlockState = {
		var finish = startState
		
		if(startState.h != 0) {
			var states = ArrayBuffer[BlockState]()
			var oldStates = ArrayBuffer[BlockState]()
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
						for(otherState <- states) equalsOtherState |= BlockState.stateEquals(newState,otherState)
						for(otherState <- oldStates) equalsOtherState |= BlockState.stateEquals(newState,otherState)
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
						while(index < states.length && states(index).h > h + 3 ) {
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
		return finish
	}
		
	private def easyParallel(moves: Array[Move]) : Array[Array[Move]] =  {
		var leftMoves = ArrayBuffer[Move]()
		var rightMoves = ArrayBuffer[Move]()
		var i = 0
		
		if(!moves(i).isPicking){
			if(moves(i).usingLeftRobot){
				leftMoves += moves(i)
			} else {
				rightMoves += moves(i)
			}
			i += 1
		}
		while (i < moves.length) {
			if (leftMoves.length == rightMoves.length) {
				if (moves(i).usingLeftRobot) {
					leftMoves += moves(i)
					leftMoves += moves(i+1)
				} else {
					rightMoves += moves(i)
					rightMoves += moves(i+1)
				}
				i += 2
			}
			else if (leftMoves.length - rightMoves.length == 2) {
				if (!moves(i).usingLeftRobot && !(leftMoves(leftMoves.length-2).usingMiddle && moves(i).usingMiddle) && !(leftMoves(leftMoves.length-1).usingMiddle && moves(i+1).usingMiddle) && !(leftMoves(leftMoves.length-1).usingMiddle && moves(i).usingMiddle && leftMoves(leftMoves.length-1).position == moves(i).position) ) {
					rightMoves += moves(i)
					rightMoves += moves(i+1)
					i += 2
				} else {
					rightMoves += null
				}
			}
			else if (rightMoves.length - leftMoves.length == 2) {
				if (moves(i).usingLeftRobot && !(rightMoves(rightMoves.length-2).usingMiddle && moves(i).usingMiddle) && !(rightMoves(rightMoves.length-1).usingMiddle && moves(i+1).usingMiddle) && !(rightMoves(rightMoves.length-1).usingMiddle && moves(i).usingMiddle && rightMoves(rightMoves.length-1).position == moves(i).position) ) {
					leftMoves += moves(i)
					leftMoves += moves(i+1)
					i += 2
				} else {
					leftMoves += null
				}
			}
			else if (leftMoves.length - rightMoves.length == 1) {
				if (!moves(i).usingLeftRobot && !(leftMoves(leftMoves.length-1).usingMiddle && moves(i).usingMiddle)) {
					rightMoves += moves(i)
					rightMoves += moves(i+1)
					i += 2
				} else {
					rightMoves += null
				}
			}
			else {
				if (moves(i).usingLeftRobot && !(rightMoves(rightMoves.length-1).usingMiddle && moves(i).usingMiddle)) {
					leftMoves += moves(i)
					leftMoves += moves(i+1)
					i += 2
				} else {
					leftMoves += null
				}
			}
}
		
		while (leftMoves.length < rightMoves.length) leftMoves += null
		while (leftMoves.length > rightMoves.length) rightMoves += null
		
		var output = new Array[Array[Move]](2)
		output(0) =leftMoves.toArray
		output(1) = rightMoves.toArray
		
		return output
	}
}


















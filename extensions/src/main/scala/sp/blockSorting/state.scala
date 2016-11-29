package astar
import scala.collection.mutable.ArrayBuffer
class State( var leftPlates: Array[Byte], var rightPlates: Array[Byte], var middle: Array[Byte], var leftRobot: Byte, var rightRobot: Byte, var n: Int, var desiredState: State, var moves: ArrayBuffer[Move]) {
	if ( leftPlates.length != 16 && rightPlates.length != 16 && middle.length != 4 ) {
		throw new IllegalArgumentException
	}
	var h = -1
	var f = -1
	if(desiredState != null) {
		h = State.h(this, desiredState)
		f = n + h
	}
	
	private def Update() {
	if(desiredState != null) {
		h = State.h(this, desiredState)
	}
	f = n + h
	}
	
	private def emptySpacesLeft() : Int = {
		var es = 0
		for(i <- 0 to 15 ) {
			if(leftPlates(i) == 0) es += 1
		}
		return es
	}
	private def emptySpacesRight() : Int = {
		var es = 0
		for(i <- 0 to 15 ) {
			if(rightPlates(i) == 0) es += 1
		}
		return es
	}
	private def emptySpacesMiddle() : Int = {
		var es = 0
		for(i <- 0 to 3 ) {
			if(middle(i) == 0) es += 1
		}
		return es
	}
	
	private def newState() : State = new State(leftPlates.clone, rightPlates.clone, middle.clone, leftRobot, rightRobot, n+1, desiredState, moves.clone)
	
	def doMove(move: Move) : State = { //update with colors
		var state = newState();
		if (move.usingLeftRobot && move.usingMiddle && move.isPicking) {
			if (leftRobot != 0 || middle(move.position) == 0) return null
				state.leftRobot = state.middle(move.position)
				state.middle(move.position) = 0
		}
		else if (move.usingLeftRobot && !move.usingMiddle && move.isPicking) {
			if (leftRobot != 0 || leftPlates(move.position) == 0) return null
				state.leftRobot = state.leftPlates(move.position)
				state.leftPlates(move.position) = 0
		}
		else if (move.usingLeftRobot && move.usingMiddle && !move.isPicking) {
			if (leftRobot == 0 || middle(move.position) != 0) return null
				state.middle(move.position) = state.leftRobot
				state.leftRobot = 0
		}
		else if (move.usingLeftRobot && !move.usingMiddle && !move.isPicking) {
			if (leftRobot == 0 || leftPlates(move.position) != 0) return null
				state.leftPlates(move.position) = state.leftRobot
				state.leftRobot = 0
		}
		else if (!move.usingLeftRobot && move.usingMiddle && move.isPicking) {
			if (rightRobot != 0 || middle(move.position) == 0) return null
				state.rightRobot = state.middle(move.position)
				state.middle(move.position) = 0
		}
		else if (!move.usingLeftRobot && !move.usingMiddle && move.isPicking) {
			if (rightRobot != 0 || rightPlates(move.position) == 0) return null
				state.rightRobot = state.rightPlates(move.position)
				state.rightPlates(move.position) = 0
		}
		else if (!move.usingLeftRobot && move.usingMiddle && !move.isPicking) {
			if (leftRobot == 0 || middle(move.position) != 0) return null
				state.middle(move.position) = state.rightRobot
				state.rightRobot = 0
		}
		else if (!move.usingLeftRobot && !move.usingMiddle && !move.isPicking) {
			if (leftRobot == 0 || leftPlates(move.position) != 0) return null
				state.rightPlates(move.position) = state.rightRobot
				state.rightRobot = 0
		}
		return state
	}
	def nextStates() : ArrayBuffer[State] = {
		var states = ArrayBuffer[State]()
		val nEmptyLeft = emptySpacesLeft
		val nEmptyRight = emptySpacesRight
		val nEmptyMiddle = emptySpacesMiddle
		
		// left robot
		if (leftRobot == 0 && rightRobot == 0) {
			//pick left side
			for (i <- 0 to 15 ) {
				if(leftPlates(i) > 0) {
					var state = newState();
					state.leftRobot = state.leftPlates(i)
					state.leftPlates(i) = 0
					state.moves += new Move(true, false, true, (i+1), leftPlates(i))
					state.Update()
					states += state
				}
			}
			//pick middle
			for (i <- 0 to 3 ) {
				if(middle(i) > 0) {
					var state = newState();
					state.leftRobot = state.middle(i)
					state.middle(i) = 0
					state.moves += new Move(true, true, true,(i+1), middle(i))
					state.Update()
					states += state
				}
			}
			
		}
		else if (rightRobot == 0) {
			//place left side
			for (i <- 0 to 15 ) {
				if( leftPlates(i) == 0) {
					var state = newState();
					state.leftPlates(i) = state.leftRobot
					state.leftRobot = 0
					state.moves += new Move(true, false, false,(i+1), leftRobot)
					state.Update()
					states += state
				}
			}
			//place middle
			if( !(nEmptyRight == 0 && nEmptyMiddle < 2) || h == 0 ) {
				for (i <- 0 to 3 ) {
					if (middle(i) == 0) {
						var state = newState();
						state.middle(i) = state.leftRobot
						state.leftRobot = 0
						state.moves += new Move(true, true, false,(i+1), leftRobot)
						state.Update()
						states += state
					}
				}
			}
			
		}
		// right robot
		if (rightRobot == 0 && leftRobot == 0) {
			//pick right side
			for (i <- 0 to 15 ) {
				if (rightPlates(i) > 0) {
					var state = newState();
					state.rightRobot = state.rightPlates(i)
					state.rightPlates(i) = 0
					state.moves += new Move(false, false, true,(i+1), rightPlates(i))
					state.Update()
					states += state
				}
			}
			//pick middle
			for (i <- 0 to 3 ) {
				if (middle(i) > 0) {
					var state = newState();
					state.rightRobot = state.middle(i)
					state.middle(i) = 0
					state.moves += new Move(false, true, true,(i+1), middle(i))
					state.Update()
					states += state
				}
			}
		}
		else if (leftRobot == 0) {
			// place right side
			for (i <- 0 to 15 ) {
				if (rightPlates(i) == 0) {
					var state = newState();
					state.rightPlates(i) = state.rightRobot
					state.rightRobot = 0
					state.moves += new Move(false, false, false, (i+1), rightRobot)
					state.Update()
					states += state
				}
			}
			//place middle
			if( !(nEmptyLeft == 0 && nEmptyMiddle < 2) || h == 0 ) {
				for (i <- 0 to 3 ) {
					if (middle(i) == 0) {
						var state = newState();
						state.middle(i) = state.rightRobot
						state.rightRobot = 0
						state.moves += new Move(false, true, false, (i+1), rightRobot)
						state.Update()
						states += state
					}
				}
			}
			
		}		
		return states
	}
}
object State {
	private def compare(b1: Byte, b2: Byte) : Int = {
		if (b1==b2 || b2==0) {
			return 0 }
		else {
			return 1 }
	}

	private def sameZoneCost(s1: State, s2: State) : Int = {
		var difference: Int = 0
		var i: Int = 0
		for (i <- 0 to 15 ) {
		difference += compare(s1.leftPlates(i), s2.leftPlates(i)) }
		for (i <- 0 to 15 ) {
		difference += compare(s1.rightPlates(i), s2.rightPlates(i)) }
		for (i <- 0 to 3 ) {
		difference += compare(s1.middle(i), s2.middle(i)) }
		difference += compare(s1.leftRobot, s2.leftRobot)
		difference += compare(s1.rightRobot, s2.rightRobot)
		
		return 2*difference
	}
	
	private def h(s1: State, s2: State) : Int = {
		var difference: Int = 0
		for(k <- 1 to 4 ) {
			var	nl1 = 0; var nr1 = 0; var nm1 = 0; var rl1 = 0; var rr1 = 0;
			var	nl2 = 0; var nr2 = 0; var nm2 = 0; var rl2 = 0; var rr2 = 0;
			for (i <- 0 to 15 ) {
				if (s1.leftPlates(i) == k) nl1 += 1
				if (s2.leftPlates(i) == k) nl2 += 1
				if (s1.rightPlates(i) == k) nr1 += 1
				if (s2.rightPlates(i) == k) nr2 += 1
			}
			for (i <- 0 to 3 ) {
				if (s1.middle(i) == k) nm1 += 1
				if (s2.middle(i) == k) nm2 += 1
			}
			if (s1.leftRobot == k) rl1 = 1
			if (s2.leftRobot == k) rl2 = 1
			if (s1.rightRobot == k) rr1 = 1
			if (s2.rightRobot == k) rr2 = 1
		
		
			while(nl2 > 0 && nl1 > 0 ) { nl2 -= 1 ; nl1-=1 }
			while(rl2 > 0 && rl1 > 0 ) { rl2 -= 1 ; rl1-=1 }
			while(nm2 > 0 && nm1 > 0 ) { nm2 -= 1 ; nm1-=1 }
			while(rr2 > 0 && rr1 > 0 ) { rr2 -= 1 ; rr1-=1 }
			while(nr2 > 0 && nr1 > 0 ) { nr2 -= 1 ; nr1-=1 }
			
			while(nl2 > 0 && rl1 > 0 ) { nl2 -= 1 ; rl1-=1 ; difference -=1 }
		
			while(rl2 > 0 && nl1 > 0 ) { rl2 -= 1 ; nl1-=1 ; difference -=1 }
			while(rl2 > 0 && nm1 > 0 ) { rl2 -= 1 ; nm1-=1 ; difference -=1 }
		
			while(nm2 > 0 && rl1 > 0 ) { nm2 -= 1 ; rl1-=1 ; difference -=1 }
			while(nm2 > 0 && rr1 > 0 ) { nm2 -= 1 ; rr1-=1 ; difference -=1 }
		
			while(rr2 > 0 && nm1 > 0 ) { rr2 -= 1 ; nm1-=1 ; difference -=1 }
			while(rr2 > 0 && nr1 > 0 ) { rr2 -= 1 ; nr1-=1 ; difference -=1 }
			
			while(nr2 > 0 && rr1 > 0 ) { nr2 -= 1 ; rr1-=1 ; difference -=1 }
					
		
			while(nl2 > 0 && nm1 > 0 ) { nl2 -= 1 ; nm1-=1 ; difference +=1 }
		
			while(rl2 > 0 && rr1 > 0 ) { rl2 -= 1 ; rr1-=1 ; difference +=1 }
		
			while(nm2 > 0 && nl1 > 0 ) { nm2 -= 1 ; nl1-=1 ; difference +=1 }
			while(nm2 > 0 && nr1 > 0 ) { nm2 -= 1 ; nr1-=1 ; difference +=1 }
		
			while(rr2 > 0 && rl1 > 0 ) { rr2 -= 1 ; rl1-=1 ; difference +=1 }
		
			while(nr2 > 0 && nm1 > 0 ) { nr2 -= 1 ; nm1-=1 ; difference +=1 }
		
		
			while(nl2 > 0 && rr1 > 0 ) { nl2 -= 1 ; rr1-=1 ; difference +=2 }
		
			while(rl2 > 0 && nr1 > 0 ) { rl2 -= 1 ; nr1-=1 ; difference +=2 }
		
			while(rr2 > 0 && nl1 > 0 ) { rr2 -= 1 ; nl1-=1 ; difference +=2 }
		
			while(nr2 > 0 && rl1 > 0 ) { nr2 -= 1 ; rl1-=1 ; difference +=2 }
		
		
			while(nl2 > 0 && nr1 > 0 ) { nl2 -= 1 ; nr1-=1 ; difference +=3 }
		
			while(nr2 > 0 && nl1 > 0 ) { nr2 -= 1 ; nl1-=1 ; difference +=3 }
		}
		return difference + sameZoneCost(s1,s2)
	}
	
	def stateEquals(s1: State, s2: State) : Boolean = {
		var equals = true
		var i: Int = 0
		for (i <- 0 to 15 ) {
		equals &= s1.leftPlates(i) == s2.leftPlates(i) }
		for (i <- 0 to 15 ) {
		equals &= s1.rightPlates(i) == s2.rightPlates(i) }
		for (i <- 0 to 3 ) {
		equals &= s1.middle(i) == s2.middle(i) }
		equals &= s1.leftRobot == s2.leftRobot
		equals &= s1.rightRobot == s2.rightRobot
		
		return equals
	}
	
	def quickSort(states: ArrayBuffer[State]) {
	
		def swap(i: Int, j: Int) {
			val temp = states(i)
			states(i) = states(j)
			states(j) = temp
		}
		def sort(l: Int, r: Int) {
			val pivot = states((l+r)/2)
			var i = l
			var j = r
			while (i <= j) {
				while(states(i).f < pivot.f) i += 1
				while(states(j).f > pivot.f) j -= 1
				if (i <= j) {
					swap(i, j)
					i += 1
					j -= 1
				}
			}
			if (l < j) sort(l, j)
			if (j < r) sort(i, r)
		}
	
		sort(0, states.length - 1)
	}
	
	def printState(s: State) {
		var i: Int = 0
		var line: String = ""
		for (i <- 0 to 15 ) {
		line += s.leftPlates(i).toString }
		println(line)
		println(s.leftRobot)
		line = ""
		for (i <- 0 to 3 ) {
		line += s.middle(i).toString }
		println(line)
		println(s.rightRobot)
		line = ""
		for (i <- 0 to 15 ) {
		line += s.rightPlates(i).toString }
		println(line)
		println("")
		println("n=" + s.n.toString + " h=" + s.h.toString + " f=" + s.f.toString)
		println("")
		
	}
}

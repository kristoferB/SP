package astar
import scala.collection.mutable.ArrayBuffer
class BlockState( var leftPlates: Array[Byte], var rightPlates: Array[Byte], var middle: Array[Byte], var leftRobot: Byte, var rightRobot: Byte, var n: Int, var desiredState: BlockState, var moves: ArrayBuffer[Move]) {
	if ( leftPlates.length != 16 && rightPlates.length != 16 && middle.length != 4 ) {
		throw new IllegalArgumentException
	}
	var h = -1
	var f = -1
	if(desiredState != null) {
		h = BlockState.h(this, desiredState)
		f = n + h
	}
	
	private def Update() {
	if(desiredState != null) {
		h = BlockState.h(this, desiredState)
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
	
	private def newState() : BlockState = new BlockState(leftPlates.clone, rightPlates.clone, middle.clone, leftRobot, rightRobot, n+1, desiredState, moves.clone)
	
	
	private def newStatesLeft(states: ArrayBuffer[BlockState]) {
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
					state.moves += new Move(true, false, true, i, leftPlates(i))
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
					state.moves += new Move(true, true, true, i, middle(i))
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
					state.moves += new Move(true, false, false, i, leftRobot)
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
						state.moves += new Move(true, true, false, i, leftRobot)
						state.Update()
						states += state
					}
				}
			}
			
		}
	}
	private def newStatesRight(states: ArrayBuffer[BlockState]) {
		val nEmptyLeft = emptySpacesLeft
		val nEmptyRight = emptySpacesRight
		val nEmptyMiddle = emptySpacesMiddle
		// right robot
		if (rightRobot == 0 && leftRobot == 0) {
			//pick right side
			for (i <- 0 to 15 ) {
				if (rightPlates(i) > 0) {
					var state = newState();
					state.rightRobot = state.rightPlates(i)
					state.rightPlates(i) = 0
					state.moves += new Move(false, false, true, i, rightPlates(i))
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
					state.moves += new Move(false, true, true, i, middle(i))
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
					state.moves += new Move(false, false, false, i, rightRobot)
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
						state.moves += new Move(false, true, false, i, rightRobot)
						state.Update()
						states += state
					}
				}
			}
			
		}		
	}
	
	
	def nextStates() : ArrayBuffer[BlockState] = {
		var states = ArrayBuffer[BlockState]()
		var leftFirst = true 
		if (moves.length != 0) leftFirst = moves(moves.length-1).usingLeftRobot
		if (leftFirst) {
			newStatesLeft(states)
			newStatesRight(states)
		}
		else {
			newStatesRight(states)
			newStatesLeft(states)
		}
		
		return states
	}
}
object BlockState {
	private def compare(b1: Byte, b2: Byte) : Int = {
		if (b1==b2 || b2==0) {
			return 0 }
		else {
			return 1 }
	}

	private def sameZoneCost(s1: BlockState, s2: BlockState) : Int = {
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
	
	private def h(s1: BlockState, s2: BlockState) : Int = {
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
	
	def stateEquals(s1: BlockState, s2: BlockState) : Boolean = {
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
	
	def printState(s: BlockState) {
		var i: Int = 0
		var line: String = ""
		for (i <- 0 to 15 ) {
		line += s.leftPlates(i).toString }
		println(line)
		//println(s.leftRobot)
		line = ""
		for (i <- 0 to 3 ) {
		line += s.middle(i).toString }
		println(line)
		//println(s.rightRobot)
		line = ""
		for (i <- 0 to 15 ) {
		line += s.rightPlates(i).toString }
		println(line)
		//println("")
		//println("n=" + s.n.toString + " h=" + s.h.toString + " f=" + s.f.toString)
		println("")
		
	}
}

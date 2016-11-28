package astar.move
class Move ( var usingLeftRobot: Boolean, var usingMiddle: Boolean, var isPicking: Boolean, var position: Int, var color: Byte ) {

	override def toString() : String = {
		var s = ""
		
		if(isPicking && usingMiddle) s += "M" + position.toString
		else if (isPicking && usingLeftRobot) s += "L" + position.toString
		else if (isPicking) s += "R" + position.toString
		else if (usingLeftRobot) s+= "LR"
		else s+= "RR"
		
		s+= "->"
		
		if(!isPicking && usingMiddle) s += "M" + position.toString
		else if (!isPicking && usingLeftRobot) s += "L" + position.toString
		else if (!isPicking) s += "R" + position.toString
		else if (usingLeftRobot) s+= "LR"
		else s+= "RR"
		
		return s
	}
	
}
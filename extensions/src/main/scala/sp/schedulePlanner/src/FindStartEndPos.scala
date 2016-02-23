/**
  * Created by Malin on 2016-02-22.
  */
import Array._
// import scala.util.control.Breaks._

class FindStartEndPos(brickTower: Array[Array[String]]) {
  val nbrOfRows = brickTower.length
  val nbrOfCols = brickTower(0).length
  var endPositions = new Array[Int](16)
  var preDefStartPos = new Array[Int](8)
  var preDefEndPos = ofDim[Int](4,4)

  // matris för alla slutpositioner, lägg i eget sen när man vet hur positioner ska definieras
  var place = 1
  for (row <- 3 to 0 by -1) {
    for (col <- 0 to 3) {
      preDefEndPos(row)(col) = place
      place = place + 1
    }
  }

  // matris för alla startpositioner, lägg i eget sen när man vet hur positionerna ska definieras
  for (row <- 0 to 7) {
    preDefStartPos(row) = row+1
  }


  // returnerar alla positioner i en enkel array
  def getPos(): (Array[Int], Array[Int], Array[Int], Array[Int]) = {
    var pos = 0
    for (row <- nbrOfRows - 1 to 0 by -1) {
      for (col <- 0 until nbrOfCols) {
        if (brickTower(row)(col) != null) {
          endPositions(pos) = preDefEndPos(row)(col)
          pos = pos + 1
        }
      }
    }
    if(pos <= 8) {
      val (start1, startNull1) = preDefStartPos.splitAt(pos)
      val (end1, endNull1) = endPositions.splitAt(pos)
      return (start1, null, end1, null)
    } else if( pos <= 16) {
      val start1 = preDefStartPos
      val (start2, start2NullPos) = preDefStartPos.splitAt(pos - 8)
      val (end1, end1Rest) = endPositions.splitAt(8)
      val (end2, end3) = end1Rest.splitAt(pos - 8)
      return (start1, start2, end1, end2)
    }
    (null, null, null, null)
  }
}

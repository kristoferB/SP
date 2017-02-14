/**
  * Created by Malin on 2016-02-22.
  */
import Array._
object Main {

  def main(args: Array[String]) = {
    val array = ofDim[String](4,4)


    array(0)(0) = "Hello"
    array(0)(1) = "it's me"
    array(0)(2) = "I was"
    array(0)(3) = "wondering"
    array(1)(0) = "zuuuup"
    array(3)(3) = "Tudilii"
    array(2)(1) = "..."
    array(2)(2) = "+++"
    array(3)(1) = "-.-."
    array(1)(1) = "  "
    array(1)(2) = " dada"

    val brickTower = new FindStartEndPos(array)
    val (start, start2, end, end2) = brickTower.getPos()

    for(row <- end) {
      println("end ROW1: " + row)
    }
    for(row <- end2) {
      println("end row2: " + row)
    }

    for(row <- start) {
      println(row)
    }

    for(row <- start2) {
      println("start2: " + row)
    }

  }

}

import sp.areus._
import scala.util._
val file = dummyData.file
val poses = file.getLines.toList.flatMap{ l =>
  val xs = l.trim.split("""\s+""").toList.map(x => Try(x.toDouble))
  xs match {
    case  Success(time) ::
      Success(j1) ::
      Success(j2) ::
      Success(j3) ::
      Success(j4) ::
      Success(j5) ::
      Success(j6) :: Nil => {
      Some(sp.areus.Pose(time, List(j1, j2, j3, j4, j5, j6)))
    }
    case x => None
  }
}
import sp.domain._
import sp.domain.Logic._
//val x = SPAttributes("poses"->poses)


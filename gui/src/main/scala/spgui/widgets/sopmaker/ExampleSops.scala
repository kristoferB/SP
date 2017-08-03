package spgui.widgets.sopmaker

import sp.domain._
import japgolly.scalajs.react.vdom.all.svg

object ExampleSops {
  val ops = List(
    Operation("op1"),
    Operation("op2"),
    Operation("op3"),
    Operation("op4"),
    Operation("op5"),
    Operation("op6"),
    Operation("op7"),
    Operation("op8"),
    Operation("op9"),
    Operation("op10"),
    Operation("op11"),
    Operation("test")
  )

  def tinySop = Sequence( List(SOP(ops(0)), SOP(ops(1))))

  def giantSop = Sequence(List(
    Sequence(
      List(SOP(ops(3)), SOP(ops(8)))),
    Parallel(
      List(
        SOP(ops(0)),
        SOP(ops(0)),
        SOP(ops(1)),
        SOP(ops(2)),
        Parallel(
          List(
            SOP(ops(11)),
            SOP(ops(11)),
            Parallel(
              List(
                SOP(ops(11)),
                SOP(ops(11))
              )
            )
          )
        )
      )
    ),
    Parallel(
      List(
        Parallel(
          List(
            SOP(ops(11)),
            SOP(ops(11))
          )
        ),
        Parallel(
          List(
            SOP(ops(11)),
            SOP(ops(11))
          )
        ),
        Parallel(
          List(
            SOP(ops(11)),
            SOP(ops(11))
          )
        )
      )
    ),

    Parallel(
      List( SOP(ops(4)), SOP(ops(5)), SOP(ops(6)),
        Sequence(
          List(
            SOP(ops(7)),
            SOP(ops(7)),
            SOP(ops(8)),
            Parallel(
              List( SOP(ops(9)), SOP(ops(10)) ))
          )
        ),
        SOP(ops(7))
      )),
    Sequence(
      List(SOP(ops(7)), SOP(ops(8)))),
    Parallel(
      List(
        SOP(ops(9)),
        Sequence(
          List(
            Parallel(
              List( SOP(ops(9)), SOP(ops(10)))),
            Parallel(
              List( SOP(ops(9)), SOP(ops(10)),SOP(ops(10))))
          ))
      )),
    Parallel(
      List( SOP(ops(9)), SOP(ops(10)), SOP(ops(10)) )),
    Parallel(
      List( SOP(ops(9)), SOP(ops(10)) ))
  ))

  def megaSop = Sequence(List(
    Parallel(List(
      giantSop, giantSop, giantSop, giantSop
    )),
    giantSop,
    giantSop
  ))
}

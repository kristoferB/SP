

/**
  * Created by kristofer on 2017-02-15.
  */
package object spgui {

  object SPParser extends upickle.AttributeTagged {
    import scala.reflect.ClassTag
    import upickle._
    override val tagName = "isa"



    override def annotate[V: ClassTag](rw: Reader[V], n: String) = Reader[V]{
      case Js.Obj(x@_*) if x.contains((tagName, Js.Str(n.split('.').takeRight(2).mkString(".")))) =>
        rw.read(Js.Obj(x.filter(_._1 != tagName):_*))
    }

    override def annotate[V: ClassTag](rw: Writer[V], n: String) = Writer[V]{ case x: V =>
      val filter = n.split('.').takeRight(2).mkString(".")
      Js.Obj((tagName, Js.Str(filter)) +: rw.write(x).asInstanceOf[Js.Obj].value:_*)
    }

    def toJson[T: Writer](expr: T, indent: Int = 0): String = json.write(writeJs(expr), indent)
    def toSPValue[T: Writer](expr: T): SPValue = implicitly[Writer[T]].write(expr)
    def fromJson[T: Reader](expr: String): T = readJs[T](json.read(expr))
    def fromSPValue[T: Reader](expr: SPValue): T = implicitly[Reader[T]].read(expr)

  }




  import SPParser._

  type SPAttributes = upickle.Js.Obj
  type SPValue = upickle.Js.Value



  object SPValue {
    def apply[T: Writer](expr: T): SPValue = toSPValue(expr)
  }

  object SPAttributes {
    /**
      * This metod will throw an exception if the expr is not a case class or a map
      * @param expr The object to convert to SPAttributes
      * @tparam T The type of the object. Is usually infereed
      * @return An SPAttributes or throws an exception
      */
    def apply[T: Writer](expr: T): SPAttributes = upickle.Js.Obj(toSPValue(expr).obj.toSeq:_*)
  }









}

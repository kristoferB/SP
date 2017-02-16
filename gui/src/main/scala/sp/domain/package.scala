package sp

import upickle.json

import scala.util.Try

/**
  * Created by kristofer on 2017-02-15.
  */
package object domain extends SPParser{

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
    def apply(map: Map[String, SPValue] = Map()): SPAttributes = upickle.Js.Obj(map.toSeq:_*)
    def fromJson(x: String): Option[SPAttributes] = Try{upickle.json.read(x).asInstanceOf[SPAttributes]}.toOption
  }


  implicit class valueLogic(value: SPValue) {
    def getAs[T: Reader](key: String = "") = {
      val x: SPValue = Try{value.obj(key)}.getOrElse(value)
      Try{readJs[T](x)}.toOption
    }

    def /(key: String) = Try{value.obj(key)}.toOption

    def toJson = upickle.json.write(value)

  }

  implicit class valueLogicOption(value: Option[SPValue]) {
    def getAs[T: Reader](key: String = "") = {
      val x = Try{value.get.obj.get(key)}.getOrElse(value)
      x.flatMap(v => Try{readJs[T](v)}.toOption)
    }

    def /(key: String) = Try{value.get.obj(key)}.toOption

  }

  def toJson[T: Writer](expr: T, indent: Int = 0): String = json.write(writeJs(expr), indent)
  def toSPValue[T: Writer](expr: T): SPValue = implicitly[Writer[T]].write(expr)
  def toSPAttributes[T: Writer](expr: T): SPAttributes = toSPValue[T](expr).asInstanceOf[SPAttributes]
  def *[T: Writer](expr: T): SPValue = toSPValue[T](expr)
  def **[T: Writer](expr: T): SPAttributes = toSPAttributes[T](expr)

  def fromJson[T: Reader](expr: String): Try[T] = Try{readJs[T](json.read(expr))}
  def fromSPValue[T: Reader](expr: SPValue): Try[T] = Try{implicitly[Reader[T]].read(expr)}

}


trait SPParser extends upickle.AttributeTagged {
  import upickle._
  import scala.reflect.ClassTag

  override val tagName = "isa"

  override def annotate[V: ClassTag](rw: Reader[V], n: String) = Reader[V]{
    case Js.Obj(x@_*) if x.contains((tagName, Js.Str(n.split('.').takeRight(2).mkString(".")))) =>
      rw.read(Js.Obj(x.filter(_._1 != tagName):_*))
  }

  override def annotate[V: ClassTag](rw: Writer[V], n: String) = Writer[V]{ case x: V =>
    val filter = n.split('.').takeRight(2).mkString(".")
    Js.Obj((tagName, Js.Str(filter)) +: rw.write(x).asInstanceOf[Js.Obj].value:_*)
  }

}



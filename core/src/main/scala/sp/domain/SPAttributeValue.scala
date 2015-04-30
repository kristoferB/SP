package sp.domain

// wrapper for jodatime
import com.github.nscala_time.time.Imports._

sealed abstract class SPAttributeValue {
  def asString: Option[String] = SPAttributeValue.this match {
    case StringPrimitive(s) => Some(s)
    case _ => None
  }
  def asInt: Option[Int] = SPAttributeValue.this match {
    case IntPrimitive(i) => Some(i)
    case LongPrimitive(i) => Some(i.toInt)
    case _ => None
  }
  def asLong: Option[Long] = SPAttributeValue.this match {
    case LongPrimitive(i) => Some(i)
    case IntPrimitive(i) => Some(i)
    case _ => None
  }
  def asDouble: Option[Double] = SPAttributeValue.this match {
    case DoublePrimitive(d) => Some(d)
    case _ => None
  }
  def asBool: Option[Boolean] = SPAttributeValue.this match {
    case BoolPrimitive(b) => Some(b)
    case _ => None
  }
  def asDate: Option[DateTime] = SPAttributeValue.this match {
    case DatePrimitive(d) => Some(d)
    case _ => None
  }
  def asDuration: Option[Duration] = SPAttributeValue.this match {
    case DurationPrimitive(d) => Some(d)
    case _ => None
  }
  def asID: Option[ID] = SPAttributeValue.this match {
    case IDPrimitive(id) => Some(id)
    case _ => None
  }
  def asList: Option[List[SPAttributeValue]] = SPAttributeValue.this match {
    case ListPrimitive(l) => Some(l)
    case _ => None
  }
  def asMap: Option[Map[String,SPAttributeValue]] = SPAttributeValue.this match {
    case MapPrimitive(m) => Some(m)
    case _ => None
  }
  
  
  
  def +(lv: SPAttributeValue) = SPAttributeValue(exp(this) ++ exp(lv))
  
  private def exp(lv: SPAttributeValue): List[SPAttributeValue] = lv match{
    case l: ListPrimitive => l.value
    case _ => List(lv)
  }
}

case class StringPrimitive(value: String) extends SPAttributeValue
case class IntPrimitive(value: Int) extends SPAttributeValue
case class LongPrimitive(value: Long) extends SPAttributeValue
case class DoublePrimitive(value: Double) extends SPAttributeValue
case class BoolPrimitive(value: Boolean) extends SPAttributeValue
case class DatePrimitive(value: DateTime) extends SPAttributeValue
case class DurationPrimitive(value: Duration) extends SPAttributeValue
case class IDPrimitive(value: ID) extends SPAttributeValue
case class ListPrimitive(value: List[SPAttributeValue]) extends SPAttributeValue
case class OptionAsPrimitive(value: Option[SPAttributeValue]) extends SPAttributeValue
case class MapPrimitive(value: Map[String, SPAttributeValue]) extends SPAttributeValue {
  def asSPAttributes = SPAttributes(value)
}


object SPAttributeValue{
  def apply(x: Any): SPAttributeValue = x match {
    case x: String => StringPrimitive(x)
    case x: Int => IntPrimitive(x)
    case x: Long => LongPrimitive(x)
    case x: Boolean => BoolPrimitive(x)
    case x: DateTime => DatePrimitive(x)
    case x: Duration => DurationPrimitive(x)
    case x: ID => IDPrimitive(x)
    case xs: List[_] => ListPrimitive((xs filter (_.isInstanceOf[SPAttributeValue])).asInstanceOf[List[SPAttributeValue]])
    case x: Option[_] => x match {
    	case Some(y) => SPAttributeValue.apply(y)
    	case None => OptionAsPrimitive(None)
    }
    case xs: Map[_, _] => MapPrimitive((xs filter {
      case (k,v)=> k.isInstanceOf[String] && v.isInstanceOf[SPAttributeValue]}).asInstanceOf[Map[String, SPAttributeValue]])
      
    case x: SPAttributeValue => x
    case x: IDAble => IDPrimitive(x.id)
    case a:Any => println(a+a.getClass.toString());OptionAsPrimitive(None)
    
  }
  
  def as[T](o: Option[T]): T = {
    o match {
      case Some(x) => x
      case None => throw new NoSuchElementException
    }
  }
  
  
  implicit def stringToPrimitive(x: String): SPAttributeValue = StringPrimitive(x)
  implicit def intToPrimitive(x: Int): SPAttributeValue = IntPrimitive(x)
  implicit def longToPrimitive(x: Long): SPAttributeValue = LongPrimitive(x)
  implicit def doubleToPrimitive(x: Double): SPAttributeValue = DoublePrimitive(x)
  implicit def boolToPrimitive(x: Boolean): SPAttributeValue = BoolPrimitive(x)
  implicit def dateToPrim(x: org.joda.time.DateTime): SPAttributeValue = DatePrimitive(x)
  implicit def durationToPrim(x: org.joda.time.Duration): SPAttributeValue = DurationPrimitive(x)
  implicit def idToPrim(x: ID): SPAttributeValue = IDPrimitive(x)
}

object DatePrimitive {
  def stringToDate(s: String, pattern: String = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"): Option[SPAttributeValue] = {
     val fmt = org.joda.time.format.DateTimeFormat.forPattern(pattern)
     try
       Some(SPAttributeValue(fmt.parseDateTime(s)))
     catch {
       case e:Exception => None
     }   
  }
  def now = DatePrimitive(DateTime.now)
}
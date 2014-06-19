package sp.domain


case class SPAttributes(attrs: Map[String, SPAttributeValue]) {
  def +(key: String, value: List[SPAttributeValue]): SPAttributes = SPAttributes(attrs + (key->ListPrimitive(value)))
  def +(mapEntry: (String,SPAttributeValue)): SPAttributes = SPAttributes(attrs + mapEntry)
  def +(key: String, value: SPAttributeValue): SPAttributes = SPAttributes(attrs + (key->value))
  def ++(maps: (String, SPAttributeValue)*): SPAttributes = SPAttributes(attrs ++ maps)
  def ++(maps: Map[String, SPAttributeValue]): SPAttributes = SPAttributes(attrs ++ maps)
  def get(attribute: String) = attrs.get(attribute)
  def getAsString(attribute: String) = extract(get(attribute), _.asString)
  def getAsInt(attribute: String) = extract(get(attribute), _.asInt)
  def getAsLong(attribute: String) = extract(get(attribute), _.asLong)
  def getAsDouble(attribute: String) = extract(get(attribute), _.asDouble)
  def getAsBool(attribute: String) = extract(get(attribute), _.asBool)
  def getAsList(attribute: String) = extract(get(attribute), _.asList)
  def getAsMap(attribute: String) = extract(get(attribute), _.asMap)
  def getAsDate(attribute: String) = extract(get(attribute), _.asDate)
  def getAsDuration(attribute: String) = extract(get(attribute), _.asDuration)
  def getAsID(attribute: String) = extract(get(attribute), _.asID)


  def getAttribute(levels: List[String]): Option[SPAttributeValue] = {
    def req(value : MapPrimitive, levels: List[String]): Option[SPAttributeValue] = {
        levels match {
          case x :: Nil => value.value.get(x)
          case x :: xs => {
            for {
              v <- value.value.get(x) if v.isInstanceOf[MapPrimitive]
              root <- req(v.asInstanceOf[MapPrimitive], xs)
            } yield root
          }
        }
    }

    req(MapPrimitive(this.attrs), levels)
  }
  
  /**
   * Helper method that extracts a type from the message. Used by the above 
   * getAs* methods.
   */
  def extract[T](attr: Option[SPAttributeValue], f: SPAttributeValue => Option[T]) = {
    for {
      lv <- attr
      v <- f(lv)
    } yield v
  }
  
  /**
   * Helper method to be used instead of a try - catch
   */
  def tryWithOption[T](t: => T): Option[T] = {
    try {
      Some(t)
    } catch {
      case e: Exception => None
    }
  }
  /**
   * Helper method used together with getWith* above. If you are sure these methods will return 
   * a value, you can extract the value with this method, 
   * ex: val date = as(message.getAsDate("StartTime"))
   * if StartTime does not exist in the message, an NoSuchElementException will be thrown
   */
  def as[T](o: Option[T]): T = {
    o match {
      case Some(x) => x
      case None => throw new NoSuchElementException
    }
  }
}



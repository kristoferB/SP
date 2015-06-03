package sp.supremicaStuff.auxiliary

object MySupport {

  def saveToFile(file: String, stringSeq: Seq[String]): Boolean = {
    try { printToFile(file)(stringSeq); true } catch {
      case _: Throwable => println(s"Could not save to given file: $file"); false
    }
  }

  /**
   * printToFile(new File("C:/Users/patrik/Desktop/output.txt"))(linesToFile)
   */
  private def printToFile(f: String)(stringSeq: Seq[String]) = {
    val pw = new java.io.PrintWriter(new java.io.File(f))
    try { stringSeq.foreach(pw.println(_)) } finally { pw.close() }
  }

  /**
   * scala> getFormattedTimeString(12345)
   * res: java.lang.String = 12.345
   *
   * scala> getFormattedTimeString(123)
   * res: java.lang.String = .123
   */
  def getFormattedTimeString: Long => String = { time =>
    val timeString = time.toString
    (timeString.reverse.take(3) + "." + timeString.reverse.drop(3)).reverse
  }
}
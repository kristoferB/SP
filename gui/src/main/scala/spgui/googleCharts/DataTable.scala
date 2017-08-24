package spgui.googleCharts

import scala.scalajs.js.annotation.JSGlobal
import scalajs.js

@js.native
@JSGlobal("google.visualization.DataTable")
class DataTable(
                 val optional_data: String = "",
                 val optional_version: String = "") extends js.Object {
  def addColumn(`type`: String, opt_label: String = "", opt_id: String = ""): Int = js.native

  def addColumn(description_object: Array[String]): Int = js.native

  def addRows(rows: js.Array[js.Array[js.Any]]): Int = js.native

  def addRows(numOrArray: Int): Int = js.native

  def addRow(row: js.Array[js.Any]): Int = js.native

  def addRow(): Int = js.native

  def getColumnId(columnIndex: Int): String = js.native

  def getColumnLabel(columnIndex: Int): String = js.native

  def getColumnPattern(columnIndex: Int): String = js.native

  def getColumnProperties(columnIndex: Int): js.Object = js.native

  def getColumnProperty(columnIndex: Int, name: String): js.Any = js.native

  def getColumnRange(columnIndex: Int): js.Object = js.native

  def getColumnRole(columnIndex: Int): String = js.native

  def getColumnType(columnIndex: Int): String = js.native

  def getDistinctValues(columnIndex: Int): js.Array[js.Object] = js.native

  def getFilteredRows(filters: js.Array[js.Object]): js.Array[js.Object] = js.native

  def getFormattedValue(rowIndex: Int, columnIndex: Int): String = js.native

  def getNumberOfColumns(): Int = js.native

  def getNumberOfRows(): Int = js.native

  def getProperties(rowIndex: Int, columnIndex: Int): js.Object = js.native

  def getProperty(rowIndex: Int, columnIndex: Int, name: String): js.Any = js.native

  def getRowProperties(rowIndex: Int): js.Object = js.native

  def getRowProperty(rowIndex: Int, name: String): js.Any = js.native

  def getSortedRows(singleNumber: Int): js.Array[Int] = js.native

  def getSortedRows(singleObject: js.Object): js.Array[Int] = js.native

  def getSortedRows(numberOrObjectArray: js.Array[js.Any]): js.Array[Int] = js.native

  def getTableProperties(): js.Object = js.native

  def getTableProperty(name: String): js.Any = js.native

  def getValue(rowIndex: Int, columnIndex: Int): js.Object = js.native

  def insertColumn(columnIndex: Int, `type`: String): Unit = js.native

  def insertColumn(columnIndex: Int, `type`: String, label: String = "", id: String = ""): Unit = js.native

  def insertRows(rowIndex: Int, numberOrArray: Int): Unit = js.native

  def insertRows(rowIndex: Int, numberOrArray: js.Array[js.Array[js.Any]]): Unit = js.native

  def removeColumn(columnIndex: Int): Unit = js.native

  def removeColumns(columnIndex: Int, numberOfColumns: Int): Unit = js.native

  def removeRow(rowIndex: Int): Unit = js.native

  def removeRows(rowIndex: Int, numberOfRows: Int): Unit = js.native

  def setCell(rowIndex: Int, columnIndex: Int): Unit = js.native

  def setCell(rowIndex: Int, columnIndex: Int, value: String): Unit = js.native

  def setCell(rowIndex: Int, columnIndex: Int, value: String = null, formattedValue: String = null, properties: js.Object = null): Unit = js.native

  def setColumnLabel(columnIndex: Int, label: String): Unit = js.native

  def setColumnProperty(columnIndex: Int, name: String, value: String): Unit = js.native

  def setColumnProperties(columnIndex: Int, properties: js.Object): Unit = js.native

  def setFormattedValue(rowIndex: Int, columnIndex: Int, formattedValue: String): Unit = js.native

  def setProperty(rowIndex: Int, columnIndex: Int, name: String, value: String): Unit = js.native

  def setProperties(rowIndex: Int, columnIndex: Int, properties: js.Object): Unit = js.native

  def setRowProperty(rowIndex: Int, name: String, value: String): Unit = js.native

  def setRowProperties(rowIndex: Int, properties: js.Object): Unit = js.native

  def setTableProperty(name: String, value: String): Unit = js.native

  def setTableProperties(properties: js.Object): Unit = js.native

  def setValue(rowIndex: Int, columnIndex: Int, value: String): Unit = js.native

  def sort(singleNumber: Int): Unit = js.native

  def sort(singleObject: js.Object): Unit = js.native

  def sort(numberOrObjectArray: js.Array[js.Any]): Unit = js.native

  def toJSON(): String = js.native

  override def clone(): DataTable = js.native
}
package pedigree

//class CPT(variable0: Variable, header0: Array[String], matrix0: Array[Array[Double]]) {
class CPT(variable0: Variable, header0: Array[String], matrix0: Iterator[Array[Double]],matrixSize0 : Int ) {

  val variable = variable0

  var header: Array[String] = header0

//  var matrix: Array[Array[Double]] = matrix0
  var matrix: Iterator[Array[Double]] = matrix0

  var matrixSize : Int = matrixSize0

  private def findKey(condition: String => Boolean): Option[Int] = {
    header.zipWithIndex.find { case (key, _) => condition(key) }.map(_._2)
  }

  def ap(row: Array[Double]) = findKey(key => key.endsWith("p") && key != name).map(row(_))
  def am(row: Array[Double]) = findKey(key => key.endsWith("m") && key != name).map(row(_))
  def s(row: Array[Double]) = findKey(_.endsWith("s")).map(row(_))
  def node(row: Array[Double]) = row(findKey(_ == name).get)

  def name = variable.name
  def marker = variable.marker
  def kind = variable.kind

  def getPlain(): PlainCPT = {
    PlainCPT(this.header, this.matrix, this.matrixSize)
  }
}
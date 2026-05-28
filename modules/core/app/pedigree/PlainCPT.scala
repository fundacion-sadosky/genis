package pedigree

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

import Ordering.Implicits.*
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class PlainCPT(header0: Array[String], matrix0: BayesianNetwork.Matrix, matrixSize0: Int) {
  var header     = header0
  var matrix     = matrix0
  var matrixSize = matrixSize0

  def prodFactorOriginaOptimizado(otherCPT: PlainCPT): PlainCPT = {
    val joinFields     = (this.header.init intersect otherCPT.header.init).sorted
    val joinFieldsPos1 = this.header.zipWithIndex.filter(z => joinFields.contains(z._1)).sorted
    val diffFieldsPos1 = this.header.zipWithIndex.filterNot(z => joinFields.contains(z._1) || z._1 == "Probability")
    val joinFieldsPos2 = otherCPT.header.zipWithIndex.filter(z => joinFields.contains(z._1)).sorted
    val diffFieldsPos2 = otherCPT.header.zipWithIndex.filterNot(z => joinFields.contains(z._1) || z._1 == "Probability")

    var resultMatrixIterator = Iterator[Array[Double]]()
    var resultMatrixSize     = 0
    var row                  = Array[Double]()
    val otherMatrix          = otherCPT.matrix.toArray
    var otherMatrixIterator  = Iterator[Array[Double]]()

    while (this.matrix.hasNext) {
      row = this.matrix.next()
      val values1     = joinFieldsPos1.map { case (_, position) => row(position) }
      val diffValues1 = diffFieldsPos1.map { case (_, position) => row(position) }
      val rowProb     = row.last
      otherMatrixIterator = otherMatrix.iterator
      while (otherMatrixIterator.hasNext) {
        val otherRow = otherMatrixIterator.next()
        val values2  = joinFieldsPos2.map { case (_, position) => otherRow(position) }
        if (values1 sameElements values2) {
          val probability  = rowProb * otherRow.last
          val diffValues2  = diffFieldsPos2.map { case (_, position) => otherRow(position) }
          resultMatrixIterator = resultMatrixIterator ++ Array(values1 ++ diffValues1 ++ diffValues2 :+ probability)
          resultMatrixSize += 1
        }
      }
    }
    val resultHeader = joinFields ++ diffFieldsPos1.map(_._1) ++ diffFieldsPos2.map(_._1) :+ "Probability"
    PlainCPT(resultHeader, resultMatrixIterator, resultMatrixSize)
  }

  def prodFactor(otherCPT: PlainCPT): PlainCPT = {
    val joinFields     = (this.header.init intersect otherCPT.header.init).sorted
    val joinFieldsPos1 = this.header.zipWithIndex.filter(z => joinFields.contains(z._1)).sorted
    val diffFieldsPos1 = this.header.zipWithIndex.filterNot(z => joinFields.contains(z._1) || z._1 == "Probability")
    val joinFieldsPos2 = otherCPT.header.zipWithIndex.filter(z => joinFields.contains(z._1)).sorted
    val diffFieldsPos2 = otherCPT.header.zipWithIndex.filterNot(z => joinFields.contains(z._1) || z._1 == "Probability")

    if (joinFields.nonEmpty) {
      if (
        (this.matrixSize < 1000 && otherCPT.matrixSize < 10000) ||
          (this.matrixSize < 100 && otherCPT.matrixSize < 60000) ||
          (this.matrixSize < 100000 && otherCPT.matrixSize < 200)
      ) {
        prodFactorOriginaOptimizado(otherCPT)
      } else {
        var resultMatrixIterator = Iterator[Array[Double]]()
        var resultMatrixSize     = 0
        if (this.matrixSize != 0 && otherCPT.matrixSize != 0) {
          var thisMatrixPartitions = partitionOfjoinFields(
            this.matrix,
            this.matrixSize,
            joinFieldsPos1,
            diffFieldsPos1
          )
          matrix = null
          var otherMatrixPartitions = partitionOfjoinFields(
            otherCPT.matrix,
            otherCPT.matrixSize,
            joinFieldsPos2,
            diffFieldsPos2
          )
          otherCPT.matrix = null
          val keysIterator = thisMatrixPartitions.keysIterator
          var partitionNr  = 1
          while (keysIterator.hasNext) {
            val joinFieldsValues1 = keysIterator.next()
            if (otherMatrixPartitions.contains(joinFieldsValues1)) {
              val valuesDiff1ToAdd = thisMatrixPartitions.remove(joinFieldsValues1).get
              val valuesDiff2ToAdd = otherMatrixPartitions.remove(joinFieldsValues1).get
              val cantPart1        = valuesDiff1ToAdd.length
              val cantPart2        = valuesDiff2ToAdd.length
              var j                = 0
              if (cantPart1 < cantPart2) {
                while (j < valuesDiff1ToAdd.length) {
                  val diffValues1 = valuesDiff1ToAdd(j)
                  j += 1
                  var k = 0
                  while (k < valuesDiff2ToAdd.length) {
                    val diffValues2 = valuesDiff2ToAdd(k)
                    k += 1
                    resultMatrixIterator = resultMatrixIterator ++
                      Array(
                        joinFieldsValues1.toArray ++
                          diffValues1._1 ++
                          diffValues2._1 :+
                          (diffValues1._2 * diffValues2._2)
                      )
                    resultMatrixSize += 1
                  }
                }
              } else {
                while (j < valuesDiff2ToAdd.length) {
                  val diffValues2 = valuesDiff2ToAdd(j)
                  j += 1
                  var k = 0
                  while (k < valuesDiff1ToAdd.length) {
                    val diffValues1 = valuesDiff1ToAdd(k)
                    k += 1
                    resultMatrixIterator = resultMatrixIterator ++
                      Array(
                        joinFieldsValues1.toArray ++
                          diffValues1._1 ++
                          diffValues2._1 :+
                          (diffValues1._2 * diffValues2._2)
                      )
                    resultMatrixSize += 1
                  }
                }
              }
              partitionNr += 1
            }
          }
          thisMatrixPartitions  = null
          otherMatrixPartitions = null
        }
        val resultHeader = joinFields ++
          diffFieldsPos1.map(_._1) ++
          diffFieldsPos2.map(_._1) :+ "Probability"
        PlainCPT(resultHeader, resultMatrixIterator, resultMatrixSize)
      }
    } else {
      println("+++++++++++++++++++++++++++++++++++ cross join ++++++++++++++++++++++++++++++++")
      val otherMatrixArray     = otherCPT.matrix.toArray
      var resultMatrixIterator = Iterator[Array[Double]]()
      var resultMatrixSize     = 0
      var otherMatrixIterator  = otherMatrixArray.iterator
      while (this.matrix.hasNext) {
        val row = this.matrix.next()
        while (otherMatrixIterator.hasNext) {
          val otherRow    = otherMatrixIterator.next()
          val probability = row.last * otherRow.last
          val resultRow   = diffFieldsPos1.map { case (_, position) => row(position) } ++
            diffFieldsPos2.map { case (_, position) => otherRow(position) } :+ probability
          resultMatrixIterator = resultMatrixIterator ++ Array(resultRow)
          resultMatrixSize += 1
        }
        otherMatrixIterator = otherMatrixArray.iterator
      }
      val resultHeader = diffFieldsPos1.map(_._1) ++ diffFieldsPos2.map(_._1) :+ "Probability"
      PlainCPT(resultHeader, resultMatrixIterator, resultMatrixSize)
    }
  }

  def partitionOfjoinFields(
    matrix: BayesianNetwork.Matrix,
    matrixSize: Int,
    joinFieldsPos1: Array[(String, Int)],
    diffFieldsPos1: Array[(String, Int)]
  ): mutable.HashMap[List[Double], ArrayBuffer[(Array[Double], Double)]] = {
    var matrixPartitions = mutable.HashMap[List[Double], ArrayBuffer[(Array[Double], Double)]]()
    if (matrixSize < 1000000) {
      matrixPartitions = singleOrdererPartition(matrix, joinFieldsPos1, diffFieldsPos1)
    } else if (matrixSize <= 6000000) {
      val grupedIterator = matrix.grouped(matrixSize / 2)
      val partitions1    = singleOrdererPartition(grupedIterator.next().iterator, joinFieldsPos1, diffFieldsPos1)
      val partitions2    = singleOrdererPartition(grupedIterator.next().iterator, joinFieldsPos1, diffFieldsPos1)
      matrixPartitions = mergePartitions(partitions1, partitions2)
    } else {
      val partitions  = matrix.grouped(3000000)
      val partitions1 = singleOrdererPartition(partitions.next().iterator, joinFieldsPos1, diffFieldsPos1)
      val partitions2 = singleOrdererPartition(partitions.next().iterator, joinFieldsPos1, diffFieldsPos1)
      matrixPartitions = mergePartitions(partitions1, partitions2)
      while (partitions.hasNext) {
        val partitionMap = singleOrdererPartition(partitions.next().iterator, joinFieldsPos1, diffFieldsPos1)
        matrixPartitions = mergePartitions(matrixPartitions, partitionMap)
      }
    }
    matrixPartitions
  }

  def singleOrdererPartition(
    matrix: BayesianNetwork.Matrix,
    joinFieldsPos1: Array[(String, Int)],
    diffFieldsPos1: Array[(String, Int)]
  ): mutable.HashMap[List[Double], ArrayBuffer[(Array[Double], Double)]] = {
    var matrixArray = matrix.toArray
    java.util.Arrays.parallelSort(matrixArray, Ordering.by[Array[Double], List[Double]](row => joinFieldsPos1.map { case (_, position) => row(position) }.toList))
    var matrixPartitions        = mutable.HashMap[List[Double], ArrayBuffer[(Array[Double], Double)]]()
    var i                       = 0
    var lastGroupProcessed      = Array[Double]()
    var lastGroupValuesTupla    = ArrayBuffer[(Array[Double], Double)]()
    val iterator                = matrixArray.iterator
    while (iterator.hasNext) {
      val row         = iterator.next()
      val valuesJoin1 = joinFieldsPos1.map { case (_, position) => row(position) }
      val valuesDiff  = diffFieldsPos1.map { case (_, position) => row(position) }
      val probability = row.last
      if (lastGroupProcessed.isEmpty || (valuesJoin1 sameElements lastGroupProcessed)) {
        if (lastGroupProcessed.isEmpty) {
          lastGroupProcessed = valuesJoin1
        }
        lastGroupValuesTupla += Tuple2(valuesDiff, probability)
      } else {
        matrixPartitions.put(lastGroupProcessed.toList, lastGroupValuesTupla)
        lastGroupProcessed   = valuesJoin1
        lastGroupValuesTupla = ArrayBuffer(Tuple2(valuesDiff, probability))
      }
      i += 1
      if (i.equals(matrixArray.length)) {
        matrixPartitions.put(lastGroupProcessed.toList, lastGroupValuesTupla)
      }
    }
    matrixPartitions
  }

  def mergePartitions(
    partition1: mutable.HashMap[List[Double], ArrayBuffer[(Array[Double], Double)]],
    partition2: mutable.HashMap[List[Double], ArrayBuffer[(Array[Double], Double)]]
  ): mutable.HashMap[List[Double], ArrayBuffer[(Array[Double], Double)]] = {
    var mergeResult   = mutable.HashMap[List[Double], ArrayBuffer[(Array[Double], Double)]]()
    val keysIterator  = partition1.keysIterator
    while (keysIterator.hasNext) {
      val partitonKey      = keysIterator.next()
      val partitionVuales2 = partition2.remove(partitonKey)
      mergeResult.put(partitonKey, (partition1.get(partitonKey).get.++(if (partitionVuales2.isEmpty) ArrayBuffer() else partitionVuales2.get)))
      partition1.remove(partitonKey)
    }
    partition2.keys.foreach { partitonKey =>
      mergeResult.put(partitonKey, (partition2.get(partitonKey).get))
      partition2.remove(partitonKey)
    }
    mergeResult
  }

  def sumFactorOriginal(variable: String): PlainCPT = {
    var repetitions = Map.empty[List[Double], Double]
    val position    = this.header.indexOf(variable)
    this.matrix.foreach { row =>
      val values = row.init.zipWithIndex.filterNot(_._2 == position).map(_._1).toList
      repetitions += values -> (repetitions.getOrElse(values, 0.0) + row.last)
    }
    val resultHeader     = this.header.filterNot(_ == variable)
    var resultMatrixSize = 0
    val resultMatrix     = repetitions.iterator.map {
      case (values, probability) => {
        values.toArray :+ probability
      }
    }
    resultMatrixSize = repetitions.size
    PlainCPT(resultHeader, resultMatrix, resultMatrixSize)
  }

  def sumFactor(variable: String): PlainCPT = {
    val position         = this.header.indexOf(variable)
    var resultMatrixSize = 0
    if (this.matrixSize < 1000000) {
      sumFactorOriginal(variable)
    } else if (this.matrixSize <= 6000000) {
      val grupedIterator    = this.matrix.grouped(this.matrixSize / 2)
      val resultTuple       = sumAndMerge(grupedIterator.next().iterator, grupedIterator.next().iterator, position)
      val resultMatrixIterator = resultTuple._1
      resultMatrixSize      = resultTuple._2
      val resultHeader      = this.header.filterNot(_ == variable)
      PlainCPT(resultHeader, resultMatrixIterator, resultMatrixSize)
    } else {
      val partitions = matrix.grouped(2000000)
      matrix = null
      val resultTuple0         = sumAndMerge(partitions.next().iterator, partitions.next().iterator, position)
      var resultMatrixIterator = resultTuple0._1
      resultMatrixSize         = resultTuple0._2
      while (partitions.hasNext) {
        System.gc()
        val repetitionMap = sumRepetitionsMap(partitions.next().iterator, position)
        val resultTuple2  = sumMergeResults(resultMatrixIterator, repetitionMap)
        resultMatrixIterator = resultTuple2._1
        resultMatrixSize     = resultTuple2._2
      }
      val resultHeader = this.header.filterNot(_ == variable)
      PlainCPT(resultHeader, resultMatrixIterator, resultMatrixSize)
    }
  }

  def sumMergeResults(
    resultMatrix: Iterator[Array[Double]],
    repetitionMap: mutable.HashMap[List[Double], Double]
  ): (Iterator[Array[Double]], Int) = {
    var returnMatrix2    = Iterator[Array[Double]]()
    var returnMatrixSize = 0
    resultMatrix.foreach { row =>
      val values    = row.init
      val resultRow = values :+ (row.last + repetitionMap.getOrElse(values.toList, 0.0))
      returnMatrix2 = returnMatrix2 ++ Array(resultRow)
      returnMatrixSize += 1
      repetitionMap.remove(values.toList)
    }
    repetitionMap.keys.foreach { values =>
      val resultRow = values.toArray :+ repetitionMap.get(values).get
      returnMatrix2 = returnMatrix2 ++ Array(resultRow)
      returnMatrixSize += 1
    }
    Tuple2(returnMatrix2, returnMatrixSize)
  }

  def sumAndMerge(
    matrix1: BayesianNetwork.Matrix,
    matrix2: BayesianNetwork.Matrix,
    position: Int
  ): (Iterator[Array[Double]], Int) = {
    val repetitions1: mutable.HashMap[List[Double], Double] = sumRepetitionsMap(matrix1, position)
    System.gc()
    val repetitions2: mutable.HashMap[List[Double], Double] = sumRepetitionsMap(matrix2, position)
    System.gc()
    var resultMatrixIterator = Iterator[Array[Double]]()
    var resultMatrixSize     = 0
    val keyIterator          = repetitions1.keysIterator
    while (keyIterator.hasNext) {
      val values        = keyIterator.next()
      val sumOfValues2  = repetitions2.remove(values)
      val resultRow     = values.toArray :+ (repetitions1.get(values).get + (if (sumOfValues2.isEmpty) 0.0 else sumOfValues2.get))
      resultMatrixIterator = resultMatrixIterator ++ Array(resultRow)
      resultMatrixSize += 1
      repetitions1.remove(values)
    }
    repetitions2.keys.foreach { values =>
      val sumOfValues1  = repetitions1.remove(values)
      val resultRow     = values.toArray :+ (repetitions2.get(values).get + (if (sumOfValues1.isEmpty) 0.0 else sumOfValues1.get))
      resultMatrixIterator = resultMatrixIterator ++ Array(resultRow)
      resultMatrixSize += 1
      repetitions2.remove(values)
    }
    Tuple2(resultMatrixIterator, resultMatrixSize)
  }

  def sumRepetitionsMap(
    matrix: BayesianNetwork.Matrix,
    position: Int
  ): mutable.HashMap[List[Double], Double] = {
    var repetitions = mutable.HashMap[List[Double], Double]()
    val iterator    = matrix
    while (iterator.hasNext) {
      val row    = iterator.next()
      val values = row.init.zipWithIndex.filterNot(_._2 == position).map(_._1).toList
      repetitions.put(values, (repetitions.getOrElse(values, 0.0) + row.last))
    }
    repetitions
  }
}

object PlainCPT {

  def apply(header: Array[String], matrix: BayesianNetwork.Matrix, matrixSize: Int): PlainCPT =
    new PlainCPT(header, matrix, matrixSize)

  def apply(header: Array[String], matrix: Array[Array[Double]]): PlainCPT =
    new PlainCPT(header, matrix.iterator, matrix.length)

  implicit val plainCptReads: Reads[PlainCPT] = (
    (__ \ "header").read[Array[String]] ~
    (__ \ "matrix").read[Array[Array[Double]]]
  )((header: Array[String], matrix: Array[Array[Double]]) => {
    new PlainCPT(header, matrix.iterator, matrix.size)
  })

  implicit val plainCptWrites: OWrites[PlainCPT] = (
    (__ \ "header").write[Array[String]] ~
    (__ \ "matrix").write[Array[Array[Double]]]
  )((plainCPT: PlainCPT) => (plainCPT.header, plainCPT.matrix.toArray))

  implicit val format: OFormat[PlainCPT] = OFormat(plainCptReads, plainCptWrites)
}

case class PlainCPT2(header0: Array[String], matrix0: Array[Array[Double]]) {
  val header: Array[String]       = header0
  val matrix: Array[Array[Double]] = matrix0
}

object PlainCPT2 {

  def apply(header: Array[String], matrix: Array[Array[Double]]): PlainCPT2 =
    new PlainCPT2(header, matrix)

  implicit val plainCptReads: Reads[PlainCPT2] = (
    (__ \ "header").read[Array[String]] ~
    (__ \ "matrix").read[Array[Array[Double]]]
  )(PlainCPT2.apply)

  implicit val plainCptWrites: OWrites[PlainCPT2] = (
    (__ \ "header").write[Array[String]] ~
    (__ \ "matrix").write[Array[Array[Double]]]
  )((plainCPT: PlainCPT2) => (plainCPT.header, plainCPT.matrix))

  implicit val plainCpt2Format: OFormat[PlainCPT2] = OFormat(plainCptReads, plainCptWrites)
}


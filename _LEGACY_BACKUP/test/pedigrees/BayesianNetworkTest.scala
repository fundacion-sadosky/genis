package pedigrees

import kits.AnalysisType
import org.scalatestplus.play.PlaySpec
import pedigree._
import profile.{Allele, Profile}
import types.{SampleCode, Sex}

import scalax.collection.Graph
import scalax.collection.GraphPredef._

class BayesianNetworkTest extends PlaySpec {

  val grandfather = Profile(null, SampleCode("AR-C-SHDG-1115"), null, null, null, Map(
    1 -> Map("LOCUS" -> List(Allele(6), Allele(8)))
  ), None, None, None, None)

  val grandmother = Profile(null, SampleCode("AR-C-SHDG-1121"), null, null, null, Map(
    1 -> Map("LOCUS" -> List(Allele(8), Allele(9)))
  ), None, None, None, None)

  val father = Profile(null, SampleCode("AR-C-SHDG-1100"), null, null, null, Map(
    1 -> Map("LOCUS" -> List(Allele(7), Allele(9)))
  ), None, None, None, None)

  val child1 = Profile(null, SampleCode("AR-C-SHDG-1150"), null, null, null, Map(
    1 -> Map("LOCUS" -> List(Allele(7), Allele(10)))
  ), None, None, None, None)

  val child2 = Profile(null, SampleCode("AR-C-SHDG-1151"), null, null, null, Map(
    1 -> Map("LOCUS" -> List(Allele(6), Allele(8)))
  ), None, None, None, None)

  val unknown = Profile(null, SampleCode("AR-C-SHDG-1152"), null, null, null, Map(
    1 -> Map("LOCUS" -> List(Allele(7), Allele(6)))
  ), None, None, None, None)

  val profiles = Array(grandfather, grandmother, father, child1, child2, unknown)

  val genogram = Array(
    Individual(NodeAlias("Abuelo"), None, None, Sex.Male, Some(SampleCode("AR-C-SHDG-1115")), false,None),
    Individual(NodeAlias("Abuela"), None, None, Sex.Female, Some(SampleCode("AR-C-SHDG-1121")), false,None),
    Individual(NodeAlias("Padre"), None, None, Sex.Male, Some(SampleCode("AR-C-SHDG-1100")), false,None),
    Individual(NodeAlias("Madre"), Some(NodeAlias("Abuelo")), Some(NodeAlias("Abuela")), Sex.Female, None, false,None),
    Individual(NodeAlias("PI"), Some(NodeAlias("Padre")), Some(NodeAlias("Madre")), Sex.Male, Some(SampleCode("AR-C-SHDG-1152")), true,None),
    Individual(NodeAlias("Hijo"), Some(NodeAlias("PI")), None, Sex.Male, Some(SampleCode("AR-C-SHDG-1150")), false,None),
    Individual(NodeAlias("Hija"), Some(NodeAlias("PI")), None, Sex.Female, Some(SampleCode("AR-C-SHDG-1151")), false,None)
  )

  val frequencyTable: BayesianNetwork.FrequencyTable = Map("LOCUS" -> Map(
    0.0 -> 0.0002874554,
    6.0 -> 0.0002874554,
    7.0 -> 0.00167,
    8.0 -> 0.00448,
    8.3 -> 0.0002874554,
    9.0 -> 0.02185,
    10.0 -> 0.26929,
    10.3 -> 0.0002874554,
    11.0 -> 0.28222,
    12.0 -> 0.34753,
    13.0 -> 0.06209,
    14.0 -> 0.00868,
    15.0 -> 0.00167,
    -1.0 -> 0.00005)
  )

  "A Bayesian Network" must {

    "find a mutation with no father" in {
      val alleles = Some(Array(9.0,14.0))
      val allelesM = Some(Array(10.0,15.0))
      val allelesP = None

      val mutation = BayesianNetwork.allelesMutated(alleles, allelesM, allelesP)

      mutation mustBe true
    }

    "find a mutation with no mother" in {
      val alleles = Some(Array(9.0,14.0))
      val allelesM = None
      val allelesP = Some(Array(10.0,15.0))

      val mutation = BayesianNetwork.allelesMutated(alleles, allelesM, allelesP)

      mutation mustBe true
    }

    "find a mutation with both parents" in {
      val alleles = Some(Array(10.0,10.0))
      val allelesM = Some(Array(10.0,11.0))
      val allelesP = Some(Array(11.0,15.0))

      val mutation = BayesianNetwork.allelesMutated(alleles, allelesM, allelesP)

      mutation mustBe true
    }

    "find no mutation with all alleles" in {
      val alleles = Some(Array(10.0,11.0))
      val allelesM = Some(Array(10.0,11.0))
      val allelesP = Some(Array(11.0,15.0))

      val mutation = BayesianNetwork.allelesMutated(alleles, allelesM, allelesP)

      mutation mustBe false
    }

    "find no mutation with no alleles" in {
      val alleles = None
      val allelesM = Some(Array(10.0,11.0))
      val allelesP = Some(Array(11.0,15.0))

      val mutation = BayesianNetwork.allelesMutated(alleles, allelesM, allelesP)

      mutation mustBe false
    }

    "generate permutations" in {
      val header = Array("Abu_LOCUS_m", "Abu_LOCUS_p", "Madre_LOCUS_p_s", "Madre_LOCUS_p")
      val possibilities = Array(Array(8.0),Array(9.0,10.0), Array(1.0,2.0), Array(10.0,11.0,12.0))

      val result = BayesianNetwork.generatePermutations(possibilities, header,
        Variable("Madre_LOCUS_p", "LOCUS", VariableKind.Genotype, None, false), frequencyTable, Map.empty)

      var resultArray = result.map(rowIterator => rowIterator.toArray).toArray

      resultArray mustBe Array(
        Array(8.0, 9.0, 1.0, 10.0, 0.0),
        Array(8.0, 9.0, 1.0, 11.0, 0.0),
        Array(8.0, 9.0, 1.0, 12.0, 0.0),
        Array(8.0, 9.0, 2.0, 10.0, 0.0),
        Array(8.0, 9.0, 2.0, 11.0, 0.0),
        Array(8.0, 9.0, 2.0, 12.0, 0.0),
        Array(8.0, 10.0, 1.0, 10.0, 1.0),
        Array(8.0, 10.0, 1.0, 11.0, 0.0),
        Array(8.0, 10.0, 1.0, 12.0, 0.0),
        Array(8.0, 10.0, 2.0, 10.0, 0.0),
        Array(8.0, 10.0, 2.0, 11.0, 0.0),
        Array(8.0, 10.0, 2.0, 12.0, 0.0))
    }

    "get probability for heterocygotes" in {
      val header = Array("Hijo_LOCUS_m", "Hijo_LOCUS_p")
      val matrix = Array(Array(6.0, 6.0), Array(6.0, 7.0), Array(7.0, 6.0), Array(7.0, 7.0))
      val variable = Variable("Hijo", "LOCUS", VariableKind.Heterocygote)

      val p1 = BayesianNetwork.getRowProbability(Array(6.0, 6.0), header, variable, frequencyTable, Map.empty)
      val p2 = BayesianNetwork.getRowProbability(Array(6.0, 7.0), header, variable, frequencyTable, Map.empty)
      val p3 = BayesianNetwork.getRowProbability(Array(7.0, 6.0), header, variable, frequencyTable, Map.empty)
      val p4 = BayesianNetwork.getRowProbability(Array(7.0, 7.0), header, variable, frequencyTable, Map.empty)

      p1 mustBe 0.0
      p2 mustBe 1.0
      p3 mustBe 1.0
      p4 mustBe 0.0
    }

    "get probability for homocygotes" in {
      val header = Array("Hijo_LOCUS_m", "Hijo_LOCUS_p")
      val matrix = Array(Array(6.0, 6.0), Array(6.0, 7.0), Array(7.0, 6.0), Array(7.0, 7.0))
      val variable = Variable("Hijo", "LOCUS", VariableKind.Homocygote)

      val p1 = BayesianNetwork.getRowProbability(Array(6.0, 6.0), header, variable, frequencyTable, Map.empty)
      val p2 = BayesianNetwork.getRowProbability(Array(6.0, 7.0), header, variable, frequencyTable, Map.empty)
      val p3 = BayesianNetwork.getRowProbability(Array(7.0, 6.0), header, variable, frequencyTable, Map.empty)
      val p4 = BayesianNetwork.getRowProbability(Array(7.0, 7.0), header, variable, frequencyTable, Map.empty)

      p1 mustBe 1.0
      p2 mustBe 0.0
      p3 mustBe 0.0
      p4 mustBe 1.0
    }

    "generate graph" in {
      val graph = BayesianNetwork.generateGraph(profiles, Array("LOCUS"), genogram, Map.empty)

      graph._2.nodes.size mustBe 22
      graph._2.edges.size mustBe 22
    }

    "get subgraphs" in {
      val graph = BayesianNetwork.generateGraph(profiles, Array("LOCUS"), genogram, Map.empty)

      val startTime = System.currentTimeMillis()
      val subgraphs = BayesianNetwork.getSubgraphs(graph._2)
      val endTime = System.currentTimeMillis()
      println(s"Duration: ${endTime-startTime}")

      subgraphs.size mustBe 1
    }

    "perform variable elimination without prunned variables" in {
      val startTime = System.currentTimeMillis()

      val frequencyTable: BayesianNetwork.FrequencyTable = Map("LOCUS" -> Map(
        0.0 -> 0.0002874554,
        6.0 -> 0.0002874554,
        7.0 -> 0.00167,
        8.0 -> 0.00448,
        8.3 -> 0.0002874554,
        9.0 -> 0.02185,
        10.0 -> 0.26929,
        10.3 -> 0.0002874554,
        11.0 -> 0.28222,
        12.0 -> 0.34753,
        13.0 -> 0.06209,
        14.0 -> 0.00868,
        15.0 -> 0.00167,
        -1.0 -> 0.00005)
      )

      val individuals = Array(
        Individual(NodeAlias("Abuelo"), None, None, Sex.Male, Some(SampleCode("AR-C-SHDG-1115")), false,None),
        Individual(NodeAlias("Abuela"), None, None, Sex.Female, Some(SampleCode("AR-C-SHDG-1121")), false,None),
        Individual(NodeAlias("Padre"), None, None, Sex.Male, Some(SampleCode("AR-C-SHDG-1100")), false,None),
        Individual(NodeAlias("Madre"), Some(NodeAlias("Abuelo")), Some(NodeAlias("Abuela")), Sex.Female, None, false,None),
        Individual(NodeAlias("PI"), Some(NodeAlias("Padre")), Some(NodeAlias("Madre")), Sex.Male, Some(SampleCode("AR-C-SHDG-1152")), true,None),
        Individual(NodeAlias("Hijo"), Some(NodeAlias("PI")), None, Sex.Male, None, false,None),
        Individual(NodeAlias("Hija"), Some(NodeAlias("PI")), None, Sex.Female, None, false,None)
      )

      val result = BayesianNetwork.calculateProbability(profiles, individuals, frequencyTable, AnalysisType(1, "Autosomal"), Map.empty)

//      result mustBe 130358.68131737539 +- 0.0001
      result mustBe 130371.70930589015 +- 0.0001

      val endTime = System.currentTimeMillis()
      println(s"Duration: ${endTime-startTime}")
    }

    "filter cpts using stateRemoval" in {
      val matrix1:Array[Array[Double]] = Array(
        Array(8.0, 9.0, 1.0, 10.0, 0.0),
        Array(8.0, 9.0, 1.0, 11.0, 1.0),
        Array(8.0, 9.0, 1.0, 12.0, 1.0),
        Array(8.0, 9.0, 2.0, 10.0, 0.0),
        Array(8.0, 9.0, 2.0, 11.0, 0.0),
        Array(8.0, 9.0, 2.0, 12.0, 0.0),
        Array(8.0, 10.0, 1.0, 10.0, 0.0),
        Array(8.0, 10.0, 1.0, 11.0, 1.0),
        Array(8.0, 10.0, 1.0, 12.0, 1.0),
        Array(8.0, 10.0, 2.0, 10.0, 0.0),
        Array(8.0, 10.0, 2.0, 11.0, 0.0),
        Array(8.0, 10.0, 2.0, 12.0, 0.0))

      val matrix2:Array[Array[Double]] = Array(
        Array(8.0, 9.0, 1.0, 10.0, 1.0),
        Array(8.0, 9.0, 1.0, 11.0, 1.0))

      val cpts = Array(
        new CPT(null, Array("Abu_m", "Abu_p", "madre_p_s", "Madre_p", "Probability"), matrix1.iterator, matrix1.length),
        new CPT(null, Array("Abu_m", "Abu_p", "madre_p_s", "Madre_p", "Probability"), matrix2.iterator, matrix2.length)
      )

      val matrixResult1 = Array(
        Array(8.0, 9.0, 1.0, 11.0, 1.0),
        Array(8.0, 9.0, 1.0, 12.0, 1.0),
        Array(8.0, 10.0, 1.0, 11.0, 1.0),
        Array(8.0, 10.0, 1.0, 12.0, 1.0)
      )

      val matrixResult2 = Array(Array(8.0, 9.0, 1.0, 11.0, 1.0))

      val result = BayesianNetwork.stateRemoval(cpts)

      result(0).matrix.toArray mustBe matrixResult1
      result(1).matrix.toArray mustBe matrixResult2
    }

    "filter cpts using zero probability prunnning" in {
      val matrix = Array(
        Array(8.0, 9.0, 1.0, 10.0, 0.0),
        Array(8.0, 9.0, 1.0, 11.0, 1.0),
        Array(8.0, 9.0, 1.0, 12.0, 1.0),
        Array(8.0, 9.0, 2.0, 10.0, 0.0),
        Array(8.0, 9.0, 2.0, 11.0, 0.0),
        Array(8.0, 9.0, 2.0, 12.0, 0.0),
        Array(8.0, 10.0, 1.0, 10.0, 0.0),
        Array(8.0, 10.0, 1.0, 11.0, 1.0),
        Array(8.0, 10.0, 1.0, 12.0, 1.0),
        Array(8.0, 10.0, 2.0, 10.0, 0.4),
        Array(8.0, 10.0, 2.0, 11.0, 0.0),
        Array(8.0, 10.0, 2.0, 12.0, 0.1))

      val cpts = Array(
        new CPT(null, Array("Abu_m", "Abu_p", "madre_p_s", "Madre_p", "Probability"), matrix.iterator, matrix.size)
      )

      val matrixResult = Array(
        Array(8.0, 9.0, 1.0, 11.0, 1.0),
        Array(8.0, 9.0, 1.0, 12.0, 1.0),
        Array(8.0, 10.0, 1.0, 11.0, 1.0),
        Array(8.0, 10.0, 1.0, 12.0, 1.0),
        Array(8.0, 10.0, 2.0, 10.0, 0.4),
        Array(8.0, 10.0, 2.0, 12.0, 0.1)
      )

      val result = BayesianNetwork.zeroProbabilityPrunning(cpts)

      result(0).matrix.toArray mustBe matrixResult
    }

    "filter cpts using node prunning" in {
      val frequencyTable: BayesianNetwork.FrequencyTable =
        Map("LOCUS" ->
          Map(5.0 -> 0.22,
              6.0 -> 0.1,
              7.0 -> 0.03,
              8.0 -> 0.3,
              9.0 -> 0.23,
              10.0 -> 0.12,
            -1.0 -> 0.00005))

      val individuals = Array(
        Individual(NodeAlias("Abuelo"), None, None, Sex.Male, None, false,None),
        Individual(NodeAlias("Abuela"), None, None, Sex.Female, None, false,None),
        Individual(NodeAlias("Padre"), None, None, Sex.Male, None, false,None),
        Individual(NodeAlias("Madre"), Some(NodeAlias("Abuelo")), Some(NodeAlias("Abuela")), Sex.Female, None, false,None),
        Individual(NodeAlias("PI"), Some(NodeAlias("Padre")), Some(NodeAlias("Madre")), Sex.Male, None, true,None),
        Individual(NodeAlias("Hijo"), Some(NodeAlias("PI")), Some(NodeAlias("Esposa")), Sex.Male, None, false,None),
        Individual(NodeAlias("Esposa"), None, None, Sex.Female, None, false,None)
      )

      val graph = BayesianNetwork.generateGraph(Array.empty, Array("LOCUS"), individuals, Map.empty)

      val aggregation = graph._2 //BayesianNetwork.graphToMap(graph)
      val variables = graph._1 //graph.vertices.values.keyBy(_.name).collect().toMap
      val cpts = BayesianNetwork.generateCPTs(variables, aggregation, frequencyTable, Map.empty, individuals = individuals)

      cpts.length mustBe 20

      val result = BayesianNetwork.nodePrunning(variables, aggregation)(cpts)

      result.length mustBe 14
    }

    "prod cpts using prod factor" in {
      val matrix1:Array[Array[Double]] = Array(
        Array(5.0, 1.0, 5.0, 5.0, 0.5),
        Array(6.0, 1.0, 7.0, 7.0, 0.1)
      )

      val matrix2:Array[Array[Double]] = Array(
        Array(5.0, 1.0, 2.0, 4.0, 0.25),
        Array(6.0, 2.0, 7.0, 6.0, 0.3)
      )

      val cpts = Array(
        PlainCPT(Array("Madre_m", "PI_m_s", "Madre_p", "PI_m", "Probability"), matrix1.iterator, matrix1.length),
        PlainCPT(Array("Madre_m", "Hermano_m_s", "Madre_p", "Hermano_m", "Probability"), matrix2.iterator, matrix2.length)
      )

      val result = BayesianNetwork.prodFactor("", cpts)
      val resuktMatrix = result.matrix.toArray
      resuktMatrix.length mustBe 1
//      result.matrix(0).last mustBe 0.03
      resuktMatrix(0).last mustBe 0.03
    }

    "sum cpt using sum factor" in {
      val matrix = Array(
        Array(4.0, 1.0, 5.0, 5.0, 0.2),
        Array(6.0, 2.0, 6.0, 6.0, 0.4),
        Array(4.0, 2.0, 6.0, 6.0, 0.1),
        Array(5.0, 1.0, 5.0, 5.0, 0.5),
        Array(4.0, 2.0, 5.0, 4.0, 0.05)
      )

      val cpt = PlainCPT(Array("Madre_m", "PI_m_s", "Madre_p", "PI_m", "Probability"), matrix.iterator, matrix.length)

      val result = BayesianNetwork.sumFactor(cpt, "Madre_m")

      var resultMatrix = result.matrix.toArray
/*
      while (result.matrix.hasNext) {
        resultMatrix = resultMatrix ++ Array(result.matrix.next())
      }
*/
//      result.matrix.length mustBe 3
      resultMatrix.length mustBe 3


//      val sorted = result.matrix.sortBy(_.last)
      val sorted = resultMatrix.sortBy(_.last)
      sorted(0).last mustBe 0.05
      sorted(1).last mustBe 0.5
      sorted(2).last mustBe 0.7
    }

/*
    "get ordering smoothly chosen" in {
      val vertices = Array("A", "B", "C", "D", "E")
      val edges = List("A"~>"B", "A"~>"C", "B"~>"C", "B"~>"D", "C"~>"D", "C"~>"E")
      val graph = Graph.from(vertices, edges)

      val result = BayesianNetwork.getOrdering(graph, Array.empty)

      result(0) mustBe "E"
      result(1) mustBe "A"
      result(2) mustBe "B"
      result(3) mustBe "C"
      result(4) mustBe "D"
    }

    "get ordering smoothly chosen real case" in {
      val vertices = Array("Abuelo_LOCUS_m", "Abuelo_LOCUS_p", "Abuela_LOCUS_m", "Abuela_LOCUS_p", "Madre_LOCUS_m",
                           "Madre_LOCUS_p", "Madre_LOCUS_m_s", "Madre_LOCUS_p_s", "ZDesempate")
      val edges = List("Abuelo_LOCUS_m"~>"Madre_LOCUS_p", "Abuelo_LOCUS_p"~>"Madre_LOCUS_p", "Madre_LOCUS_p_s"~>"Madre_LOCUS_p",
                       "Abuela_LOCUS_m"~>"Madre_LOCUS_m", "Abuela_LOCUS_p"~>"Madre_LOCUS_m", "Madre_LOCUS_m_s"~>"Madre_LOCUS_m",
                       "Madre_LOCUS_p"~>"ZDesempate", "Madre_LOCUS_m"~>"ZDesempate")
      val graph = Graph.from(vertices, edges)

      val result = BayesianNetwork.getOrdering(graph, Array.empty)

      result(0) mustBe "Abuela_LOCUS_m"
      result(1) mustBe "Abuela_LOCUS_p"
      result(2) mustBe "Abuelo_LOCUS_m"
      result(3) mustBe "Abuelo_LOCUS_p"
      result(4) mustBe "Madre_LOCUS_m_s"
      result(5) mustBe "Madre_LOCUS_m"
      result(6) mustBe "Madre_LOCUS_p_s"
      result(7) mustBe "Madre_LOCUS_p"
      result(8) mustBe "ZDesempate"
    }

    "get ordering smoothly chosen with a disconnected vertex" in {
      val vertices = Array("A", "B", "C", "D", "E")
      val edges = List("A"~>"B", "A"~>"C", "B"~>"C", "B"~>"D", "C"~>"D")
      val graph = Graph.from(vertices, edges)

      val result = BayesianNetwork.getOrdering(graph, Array.empty)

      result(0) mustBe "E"
      result(1) mustBe "A"
      result(2) mustBe "B"
      result(3) mustBe "C"
      result(4) mustBe "D"
    }
*/

    "get conditional" in {
      val matrix = Array(
        Array(8.0, 9.0, 1.0, 10.0, 0.3),
        Array(8.0, 9.0, 1.0, 11.0, 0.25),
        Array(8.0, 9.0, 1.0, 12.0, 0.05),
        Array(8.0, 10.0, 2.0, 10.0, 0.4),
        Array(8.0, 10.0, 2.0, 12.0, 0.6))

      val cpts = Array(
        new CPT(null, Array("Abu_m", "Abu_p", "madre_p_s", "Madre_p", "Probability"), matrix.iterator, matrix.length)
      )

      val result = BayesianNetwork.getConditional(cpts)

/*
      result(0).matrix(0).last mustBe 0.1875 +- 0.0001
      result(0).matrix(1).last mustBe 0.15625 +- 0.0001
      result(0).matrix(2).last mustBe 0.03125 +- 0.0001
      result(0).matrix(3).last mustBe 0.25 +- 0.0001
      result(0).matrix(4).last mustBe 0.375 +- 0.0001
*/
      result(0).matrix.next().toArray.last mustBe 0.1875 +- 0.0001
      result(0).matrix.next().toArray.last mustBe 0.15625 +- 0.0001
      result(0).matrix.next().toArray.last mustBe 0.03125 +- 0.0001
      result(0).matrix.next().toArray.last mustBe 0.25 +- 0.0001
      result(0).matrix.next().toArray.last mustBe 0.375 +- 0.0001
    }

    "calculate probability for PI markers" in {
      val startTime = System.currentTimeMillis()

      val one = Profile(null, SampleCode("AR-C-SHDG-0001"), null, null, null, Map(
        1 -> Map("CSF1PO" -> List(Allele(8), Allele(8)),
          "D13S317" -> List(Allele(6), Allele(6)),
          "D16S539" -> List(Allele(7), Allele(9)))
      ), None, None, None, None)

      val three = Profile(null, SampleCode("AR-C-SHDG-0003"), null, null, null, Map(
        1 -> Map("CSF1PO" -> List(Allele(8), Allele(11)),
          "D16S539" -> List(Allele(9), Allele(5)))
      ), None, None, None, None)

      val profiles = Array(one, three)

      val genogram = Array(
        Individual(NodeAlias("1"), None, None, Sex.Male, Some(SampleCode("AR-C-SHDG-0001")), false,None),
        Individual(NodeAlias("2"), None, None, Sex.Female, None, false,None),
        Individual(NodeAlias("3"), Some(NodeAlias("1")), Some(NodeAlias("2")), Sex.Female, Some(SampleCode("AR-C-SHDG-0003")), true,None)
      )

      val frequencyTable: BayesianNetwork.FrequencyTable = Map(
        "CSF1PO" -> Map(
          0.0 -> 0.000287455444406117,
          6.0 -> 0.000287455444406117,
          7.0 -> 0.00167,
          8.0 -> 0.00448,
          8.3 -> 0.000287455444406117,
          9.0 -> 0.02185,
          10.0 -> 0.26929,
          10.3 -> 0.000287455444406117,
          11.0 -> 0.28222,
          12.0 -> 0.34753,
          13.0 -> 0.06209,
          14.0 -> 0.00868,
          15.0 -> 0.00167,
          -1.0 -> 0.00005),
        "D13S317" -> Map(
          6.0 -> 0.00054,
          7.0 -> 0.000271827769924976,
          8.0 -> 0.09117,
          9.0 -> 0.15994,
          10.0 -> 0.07562,
          11.0 -> 0.22469,
          12.0 -> 0.24182,
          13.0 -> 0.12787,
          14.0 -> 0.07606,
          15.0 -> 0.00196,
          16.0 -> 0.000271827769924976,
          -1.0 -> 0.00005),
        "D16S539" -> Map(
          5.0 -> 0.000273822562979189,
          7.0 -> 0.000273822562979189,
          8.0 -> 0.01758,
          9.0 -> 0.16177,
          10.0 -> 0.10915,
          11.0 -> 0.27673,
          12.0 -> 0.27623,
          13.0 -> 0.1379,
          14.0 -> 0.01917,
          15.0 -> 0.00131,
          -1.0 -> 0.00005)
      )

      val result = BayesianNetwork.calculateProbability(profiles, genogram, frequencyTable, AnalysisType(1, "Autosomal"), Map.empty, true)

      result mustBe >(0.0)

      val endTime = System.currentTimeMillis()
      println(s"Duration: ${endTime-startTime}")
    }

    "perform real test" in {
      val frequencyTable: BayesianNetwork.FrequencyTable = Map(
        "TH01" -> Map(4.0 -> 0.00006, 5.0 -> 0.00064, 6.0 -> 0.29564, 7.0 -> 0.26031, 8.0 -> 0.08001, 9.0 -> 0.12342, 9.3 -> 0.23091, 10.0 -> 0.00808, 11.0 -> 0.0007, 13.3 -> 0.00006, -1.0 -> 0.00006),
        "D21S11" -> Map(19.0 -> 0.00005, 24.0 -> 0.00005, 24.2 -> 0.00087, 25.0 -> 0.00033, 25.2 -> 0.00054, 26.0 -> 0.00158, 26.2 -> 0.00016, 27.0 -> 0.01638, 27.2 -> 0.00011, 28.0 -> 0.09375, 28.2 -> 0.00076, 29.0 -> 0.19643, 29.2 -> 0.00207, 30.0 -> 0.27228, 30.2 -> 0.02536, 31.0 -> 0.06209, 31.2 -> 0.11312, 32.0 -> 0.0092, 32.1 -> 0.00016, 32.2 -> 0.13843, 33.0 -> 0.00212, 33.1 -> 0.00011, 33.2 -> 0.0543, 33.3 -> 0.00011, 34.0 -> 0.00044, 34.2 -> 0.00637, 35.0 -> 0.00103, 35.1 -> 0.00027, 35.2 -> 0.00087, 36.0 -> 0.00027, 36.2 -> 0.00005, 37.0 -> 0.00005, -1.0 -> 0.00005),
        "D3S1358" -> Map(10.0 -> 0.00005, 11.0 -> 0.00055, 12.0 -> 0.00186, 14.0 -> 0.07485, 15.0 -> 0.35112, 16.0 -> 0.27983, 17.0 -> 0.16096, 18.0 -> 0.11706, 19.0 -> 0.00891, 20.0 -> 0.00071, 21.0 -> 0.00011, -1.0 -> 0.00005),
        "D18S51" -> Map(9.0 -> 0.00038, 10.0 -> 0.00728, 10.2 -> 0.00011, 11.0 -> 0.01144, 12.0 -> 0.12708, 13.0 -> 0.11537, 13.2 -> 0.00011, 14.0 -> 0.20496, 14.2 -> 0.00016, 15.0 -> 0.14191, 15.2 -> 0.00005, 16.0 -> 0.12002, 16.2 -> 0.00005, 17.0 -> 0.12407, 18.0 -> 0.06715, 19.0 -> 0.03639, 20.0 -> 0.01954, 21.0 -> 0.01144, 22.0 -> 0.00859, 22.2 -> 0.00005, 23.0 -> 0.00235, 24.0 -> 0.00104, 25.0 -> 0.00016, 26.0 -> 0.00005, 27.0 -> 0.00016, -1.0 -> 0.00005),
        "Penta E" -> Map(5.0 -> 0.03706, 6.0 -> 0.00051, 7.0 -> 0.09435, 8.0 -> 0.02785, 9.0 -> 0.00695, 10.0 -> 0.06017, 11.0 -> 0.0878, 12.0 -> 0.1822, 13.0 -> 0.09463, 13.2 -> 0.00011, 14.0 -> 0.07299, 15.0 -> 0.09814, 16.0 -> 0.05927, 16.3 -> 0.0009, 16.4 -> 0.00006, 17.0 -> 0.05322, 18.0 -> 0.03859, 19.0 -> 0.02616, 20.0 -> 0.02537, 21.0 -> 0.02107, 22.0 -> 0.00757, 23.0 -> 0.00345, 24.0 -> 0.00113, 25.0 -> 0.00028, 26.0 -> 0.00006, -1.0 -> 0.00006),
        "D5S818" -> Map(7.0 -> 0.0693, 8.0 -> 0.00635, 9.0 -> 0.0421, 10.0 -> 0.05326, 11.0 -> 0.42216, 12.0 -> 0.27108, 13.0 -> 0.12503, 14.0 -> 0.00843, 15.0 -> 0.00208, 16.0 -> 0.00022, -1.0 -> 0.00022),
        "D13S317" -> Map(6.0 -> 0.00054, 7.0 -> 0.00027, 8.0 -> 0.09117, 9.0 -> 0.15994, 10.0 -> 0.07562, 11.0 -> 0.22469, 12.0 -> 0.24182, 13.0 -> 0.12787, 14.0 -> 0.07606, 15.0 -> 0.00196, 16.0 -> 0.00005, -1.0 -> 0.00005),
        "D7S820" -> Map(6.0 -> 0.00011, 7.0 -> 0.01408, 8.0 -> 0.10337, 9.0 -> 0.0882, 9.1 -> 0.00005, 10.0 -> 0.26243, 11.0 -> 0.30968, 12.0 -> 0.1863, 12.1 -> 0.00005, 12.2 -> 0.00005, 12.3 -> 0.00005, 13.0 -> 0.03094, 14.0 -> 0.00462, 15.0 -> 0.00016, -1.0 -> 0.00005),
        "D16S539" -> Map(5.0 -> 0.00005, 7.0 -> 0.00011, 8.0 -> 0.01758, 9.0 -> 0.16177, 10.0 -> 0.10915, 11.0 -> 0.27673, 12.0 -> 0.27623, 13.0 -> 0.1379, 14.0 -> 0.01917, 15.0 -> 0.00131, -1.0 -> 0.00005),
        "CSF1PO" -> Map(6.0 -> 0.00006, 7.0 -> 0.00167, 8.0 -> 0.00448, 8.3 -> 0.00006, 9.0 -> 0.02185, 10.0 -> 0.26929, 10.3 -> 0.00006, 11.0 -> 0.28222, 12.0 -> 0.34753, 13.0 -> 0.06209, 14.0 -> 0.00868, 15.0 -> 0.00167, -1.0 -> 0.00006),
        "Penta D" -> Map(2.2 -> 0.00358, 3.2 -> 0.00011, 5.0 -> 0.00146, 6.0 -> 0.00039, 7.0 -> 0.01014, 8.0 -> 0.01366, 9.0 -> 0.19036, 9.2 -> 0.00056, 10.0 -> 0.20671, 11.0 -> 0.16045, 12.0 -> 0.17008, 13.0 -> 0.1684, 14.0 -> 0.0541, 15.0 -> 0.01512, 16.0 -> 0.00364, 17.0 -> 0.0009, 18.0 -> 0.00017, 19.0 -> 0.00006, -1.0 -> 0.00027),
        "vWA" -> Map(11.0 -> 0.00087, 12.0 -> 0.00082, 13.0 -> 0.00414, 14.0 -> 0.06419, 15.0 -> 0.0943, 16.0 -> 0.30923, 17.0 -> 0.28278, 18.0 -> 0.16705, 19.0 -> 0.06425, 20.0 -> 0.01123, 21.0 -> 0.00093, 22.0 -> 0.00005, 23.0 -> 0.00005, -1.0 -> 0.00005),
        "D8S1179" -> Map(8.0 -> 0.00898, 9.0 -> 0.00762, 10.0 -> 0.06872, 11.0 -> 0.07247, 12.0 -> 0.14353, 13.0 -> 0.30446, 14.0 -> 0.22236, 15.0 -> 0.13732, 16.0 -> 0.03047, 17.0 -> 0.00337, 18.0 -> 0.00065, 19.0 -> 0.00005, -1.0 -> 0.00005),
        "TPOX" -> Map(5.0 -> 0.00016, 6.0 -> 0.00261, 7.0 -> 0.00142, 8.0 -> 0.48349, 9.0 -> 0.0779, 10.0 -> 0.04761, 11.0 -> 0.28963, 12.0 -> 0.09435, 13.0 -> 0.00256, 14.0 -> 0.00011, 16.0 -> 0.00005, 21.0 -> 0.00005, 21.2 -> 0.00005, -1.0 -> 0.00005),
        "FGA" -> Map(16.0 -> 0.00038, 17.0 -> 0.00223, 18.0 -> 0.01084, 18.2 -> 0.00038, 19.0 -> 0.08764, 19.2 -> 0.00016, 20.0 -> 0.09248, 20.2 -> 0.00022, 21.0 -> 0.14417, 21.2 -> 0.00202, 22.0 -> 0.12609, 22.2 -> 0.00398, 22.3 -> 0.00005, 23.0 -> 0.1213, 23.2 -> 0.00245, 24.0 -> 0.15153, 24.2 -> 0.00071, 25.0 -> 0.15044, 25.2 -> 0.00005, 25.3 -> 0.00005, 26.0 -> 0.07375, 26.2 -> 0.00005, 27.0 -> 0.022, 28.0 -> 0.00599, 29.0 -> 0.00054, 30.0 -> 0.00016, 31.0 -> 0.00022, -1.0 -> 0.00005),
        "D19S433" -> Map(9.0 -> 0.00289017, 11.0 -> 0.02601156, 11.2 -> 0.00289017, 12.0 -> 0.06936416, 12.2 -> 0.02312139, 13.0 -> 0.19942197, 13.2 -> 0.09248555, 14.0 -> 0.28612717, 14.2 -> 0.05780347, 15.0 -> 0.13872832, 15.2 -> 0.05491329, 16.0 -> 0.03468208, 16.2 -> 0.01156069, -1.0 -> 0.01445087),
        "D2S1338" -> Map(14.0 -> 0.005682, 16.0 -> 0.059659, 17.0 -> 0.232955, 18.0 -> 0.071023, 19.0 -> 0.15625, 20.0 -> 0.127841, 21.0 -> 0.025568, 22.0 -> 0.056818, 23.0 -> 0.139205, 24.0 -> 0.053977, 25.0 -> 0.051136, 26.0 -> 0.017045, 27.0 -> 0.002841, -1.0 -> 0.01420455),
        "D1S1656" -> Map(10.0 -> 0.0035, 11.0 -> 0.04553, 12.0 -> 0.09545, 13.0 -> 0.09895, 14.0 -> 0.09019, 14.3 -> 0.00175, 15.0 -> 0.1366, 15.3 -> 0.04466, 16.0 -> 0.16637, 16.3 -> 0.04729, 17.0 -> 0.04729, 17.3 -> 0.14799, 18.0 -> 0.00876, 18.3 -> 0.04904, 19.3 -> 0.01138, 20.0 -> 0.00175, 21.0 -> 0.00088, 22.0 -> 0.00175, 23.0 -> 0.00088, -1.0 -> 0.00874),
        "D6S1043" -> Map(9.0 -> 0.00179, 10.0 -> 0.00896, 11.0 -> 0.20072, 12.0 -> 0.18728, 13.0 -> 0.08154, 14.0 -> 0.13082, 15.0 -> 0.01523, 15.3 -> 0.0009, 16.0 -> 0.01434, 17.0 -> 0.0448, 17.3 -> 0.00179, 18.0 -> 0.09588, 19.0 -> 0.07616, 19.3 -> 0.00538, 20.0 -> 0.02509, 20.3 -> 0.02419, 21.0 -> 0.00538, 21.3 -> 0.05914, 22.3 -> 0.01703, 23.3 -> 0.00358, -1.0 -> 0.0088),
        "D12S391" -> Map(11.0 -> 0.00351, 12.0 -> 0.02285, 12.2 -> 0.00439, 13.0 -> 0.05624, 13.2 -> 0.01142, 14.0 -> 0.08875, 14.2 -> 0.00879, 15.0 -> 0.0703, 15.2 -> 0.01142, 16.0 -> 0.02548, 16.2 -> 0.00264, 17.0 -> 0.05272, 17.2 -> 0.00088, 17.3 -> 0.00967, 18.0 -> 0.15114, 18.3 -> 0.01318, 19.0 -> 0.1239, 19.1 -> 0.00088, 19.3 -> 0.00527, 20.0 -> 0.13972, 20.3 -> 0.00088, 21.0 -> 0.07293, 22.0 -> 0.058, 23.0 -> 0.04218, 24.0 -> 0.01406, 25.0 -> 0.00879, -1.0 -> 0.00874),
        "D22S1045" -> Map(10.0 -> 0.0148, 11.0 -> 0.0636, 12.0 -> 0.0127, 13.0 -> 0.00847, 14.0 -> 0.0275, 15.0 -> 0.426, 16.0 -> 0.35, 17.0 -> 0.0911, 18.0 -> 0.00636, -1.0 -> 0.0106),
        "D10S1248" -> Map(11.0 -> 0.00424, 12.0 -> 0.0424, 13.0 -> 0.273, 14.0 -> 0.339, 15.0 -> 0.212, 16.0 -> 0.0996, 17.0 -> 0.0254, 18.0 -> 0.00212, 19.0 -> 0.00212, -1.0 -> 0.0106),
        "D2S441" -> Map(10.0 -> 0.337, 11.0 -> 0.299, 11.3 -> 0.0445, 12.0 -> 0.036, 12.3 -> 0.00212, 13.0 -> 0.0233, 13.3 -> 0.001386, 14.0 -> 0.206, 15.0 -> 0.0487, 16.0 -> 0.00212, 17.0 -> 0.00212, -1.0 -> 0.0106)
      )

      val tiop = Profile(null, SampleCode("AR-C-SHDG-0001"), null, null, null, Map(1 -> Map("D3S1358" -> List(Allele(10), Allele(10)),"FGA" -> List(Allele(16), Allele(16)),"vWA" -> List(Allele(11), Allele(11)))), None, None, None, None)
      val tiap = Profile(null, SampleCode("AR-C-SHDG-0002"), null, null, null, Map(1 -> Map("D3S1358" -> List(Allele(10), Allele(12)),"FGA" -> List(Allele(16), Allele(18)),"vWA" -> List(Allele(11), Allele(13)))), None, None, None, None)
      val padre = Profile(null, SampleCode("AR-C-SHDG-0003"), null, null, null, Map(1 -> Map("D3S1358" -> List(Allele(10), Allele(11)),"FGA" -> List(Allele(16), Allele(17)),"vWA" -> List(Allele(11), Allele(12)))), None, None, None, None)
      val tiom = Profile(null, SampleCode("AR-C-SHDG-0004"), null, null, null, Map(1 -> Map("D3S1358" -> List(Allele(12), Allele(14)),"FGA" -> List(Allele(18), Allele(19)),"vWA" -> List(Allele(13), Allele(14)))), None, None, None, None)
      val tiam = Profile(null, SampleCode("AR-C-SHDG-0005"), null, null, null, Map(1 -> Map("D3S1358" -> List(Allele(12), Allele(11)),"FGA" -> List(Allele(18), Allele(17)),"vWA" -> List(Allele(13), Allele(12)))), None, None, None, None)
      val hna = Profile(null, SampleCode("AR-C-SHDG-0006"), null, null, null, Map(1 -> Map("D3S1358" -> List(Allele(11), Allele(11)),"FGA" -> List(Allele(17), Allele(17)),"vWA" -> List(Allele(12), Allele(12)))), None, None, None, None)
      val hno = Profile(null, SampleCode("AR-C-SHDG-0007"), null, null, null, Map(1 -> Map("D3S1358" -> List(Allele(10), Allele(11)),"FGA" -> List(Allele(16), Allele(17)),"vWA" -> List(Allele(11), Allele(12)))), None, None, None, None)
      val pi = Profile(null, SampleCode("AR-C-SHDG-0008"), null, null, null, Map(1 -> Map("D3S1358" -> List(Allele(10), Allele(14)),"FGA" -> List(Allele(16), Allele(19)),"vWA" -> List(Allele(11), Allele(14)))), None, None, None, None)

      val profiles = Array(tiop, tiap, padre, tiom, tiam, hna, hno, pi)

      val genogram = Array(
        Individual(NodeAlias("Abam"), None, None, Sex.Female, None, false,None),
        Individual(NodeAlias("Abom"), None, None, Sex.Male, None, false,None),
        Individual(NodeAlias("Abap"), None, None, Sex.Female, None, false,None),
        Individual(NodeAlias("Abop"), None, None, Sex.Male, None, false,None),
        Individual(NodeAlias("Madre"), Some(NodeAlias("Abom")), Some(NodeAlias("Abam")), Sex.Female, None, false,None),
        Individual(NodeAlias("Padre"), Some(NodeAlias("Abop")), Some(NodeAlias("Abap")), Sex.Male, Some(SampleCode("AR-C-SHDG-0003")), false,None),
        Individual(NodeAlias("Tiom"), Some(NodeAlias("Abom")), Some(NodeAlias("Abam")), Sex.Male, Some(SampleCode("AR-C-SHDG-0004")), false,None),
        Individual(NodeAlias("Tiam"), Some(NodeAlias("Abom")), Some(NodeAlias("Abam")), Sex.Female, Some(SampleCode("AR-C-SHDG-0005")), false,None),
        Individual(NodeAlias("Tiop"), Some(NodeAlias("Abop")), Some(NodeAlias("Abap")), Sex.Male, Some(SampleCode("AR-C-SHDG-0001")), false,None),
        Individual(NodeAlias("Tiap"), Some(NodeAlias("Abop")), Some(NodeAlias("Abap")), Sex.Female, Some(SampleCode("AR-C-SHDG-0002")), false,None),
        Individual(NodeAlias("PI"), Some(NodeAlias("Padre")), Some(NodeAlias("Madre")), Sex.Male, Some(SampleCode("AR-C-SHDG-0008")), true,None),
        Individual(NodeAlias("Hna"), Some(NodeAlias("Padre")), Some(NodeAlias("Madre")), Sex.Female, Some(SampleCode("AR-C-SHDG-0006")), false,None),
        Individual(NodeAlias("Hno"), Some(NodeAlias("Padre")), Some(NodeAlias("Madre")), Sex.Male, Some(SampleCode("AR-C-SHDG-0007")), false,None)
      )

      val startTime = System.currentTimeMillis()
      val result = BayesianNetwork.calculateProbability(profiles, genogram, frequencyTable, AnalysisType(1, "Autosomal"), Map.empty, true)
      val endTime = System.currentTimeMillis()
      println(s"Duration: ${endTime-startTime}")
    }

    "calculate probability for three independent markers" in {

      val startTime = System.currentTimeMillis()

      val one = Profile(null, SampleCode("AR-C-SHDG-0001"), null, null, null, Map(
        1 -> Map("CSF1PO" -> List(Allele(8), Allele(8)),
          "D13S317" -> List(Allele(6), Allele(6)),
          "D16S539" -> List(Allele(7), Allele(9)))
      ), None, None, None, None)

      val three = Profile(null, SampleCode("AR-C-SHDG-0003"), null, null, null, Map(
        1 -> Map("CSF1PO" -> List(Allele(8), Allele(11)),
          "D13S317" -> List(Allele(6), Allele(7)),
          "D16S539" -> List(Allele(9), Allele(5)))
      ), None, None, None, None)

      val four = Profile(null, SampleCode("AR-C-SHDG-0004"), null, null, null, Map(
        1 -> Map("CSF1PO" -> List(Allele(8), Allele(12)),
          "D13S317" -> List(Allele(6), Allele(8)),
          "D16S539" -> List(Allele(7), Allele(8)))
      ), None, None, None, None)

      val six = Profile(null, SampleCode("AR-C-SHDG-0006"), null, null, null, Map(
        1 -> Map("CSF1PO" -> List(Allele(15), Allele(11)),
          "D13S317" -> List(Allele(12), Allele(7)),
          "D16S539" -> List(Allele(10), Allele(9)))
      ), None, None, None, None)

      val profiles = Array(one, three, four, six)

      val genogram = Array(
        Individual(NodeAlias("1"), None, None, Sex.Male, Some(SampleCode("AR-C-SHDG-0001")), false, None),
        Individual(NodeAlias("2"), None, None, Sex.Female, None, false, None),
        Individual(NodeAlias("3"), Some(NodeAlias("1")), Some(NodeAlias("2")), Sex.Female, Some(SampleCode("AR-C-SHDG-0003")), false, None),
        Individual(NodeAlias("4"), Some(NodeAlias("1")), Some(NodeAlias("2")), Sex.Male, Some(SampleCode("AR-C-SHDG-0004")), false, None),
        Individual(NodeAlias("5"), None, None, Sex.Male, None, false, None),
        Individual(NodeAlias("6"), Some(NodeAlias("5")), Some(NodeAlias("3")), Sex.Male, Some(SampleCode("AR-C-SHDG-0006")), true, None)
      )


      val frequencyTable: BayesianNetwork.FrequencyTable = Map(
        "CSF1PO" -> Map(
          0.0 -> 0.000287455444406117,
          6.0 -> 0.000287455444406117,
          7.0 -> 0.00167,
          8.0 -> 0.00448,
          8.3 -> 0.000287455444406117,
          9.0 -> 0.02185,
          10.0 -> 0.26929,
          10.3 -> 0.000287455444406117,
          11.0 -> 0.28222,
          12.0 -> 0.34753,
          13.0 -> 0.06209,
          14.0 -> 0.00868,
          15.0 -> 0.00167,
          -1.0 -> 0.00005),
        "D13S317" -> Map(
          6.0 -> 0.00054,
          7.0 -> 0.000271827769924976,
          8.0 -> 0.09117,
          9.0 -> 0.15994,
          10.0 -> 0.07562,
          11.0 -> 0.22469,
          12.0 -> 0.24182,
          13.0 -> 0.12787,
          14.0 -> 0.07606,
          15.0 -> 0.00196,
          16.0 -> 0.000271827769924976,
          -1.0 -> 0.00005),
        "D16S539" -> Map(
          5.0 -> 0.000273822562979189,
          7.0 -> 0.000273822562979189,
          8.0 -> 0.01758,
          9.0 -> 0.16177,
          10.0 -> 0.10915,
          11.0 -> 0.27673,
          12.0 -> 0.27623,
          13.0 -> 0.1379,
          14.0 -> 0.01917,
          15.0 -> 0.00131,
          -1.0 -> 0.00005)
      )

      val result = BayesianNetwork.calculateProbability(profiles, genogram, frequencyTable, AnalysisType(1, "Autosomal"), Map.empty, true)

      result mustBe 1260.78225504304 +- 0.0001
      val endTime = System.currentTimeMillis()
      println(s"Duration for three independent markers: ${endTime-startTime}")
    }

    "calculate probability for three markers and two linked" in {

      val startTime = System.currentTimeMillis()

      val one = Profile(null, SampleCode("AR-C-SHDG-0001"), null, null, null, Map(
        1 -> Map("CSF1PO" -> List(Allele(8), Allele(8)),
          "D13S317" -> List(Allele(6), Allele(6)),
          "D16S539" -> List(Allele(7), Allele(9)))
      ), None, None, None, None)

      val three = Profile(null, SampleCode("AR-C-SHDG-0003"), null, null, null, Map(
        1 -> Map("CSF1PO" -> List(Allele(8), Allele(11)),
          "D13S317" -> List(Allele(6), Allele(7)),
          "D16S539" -> List(Allele(9), Allele(5)))
      ), None, None, None, None)

      val four = Profile(null, SampleCode("AR-C-SHDG-0004"), null, null, null, Map(
        1 -> Map("CSF1PO" -> List(Allele(8), Allele(12)),
          "D13S317" -> List(Allele(6), Allele(8)),
          "D16S539" -> List(Allele(7), Allele(8)))
      ), None, None, None, None)

      val six = Profile(null, SampleCode("AR-C-SHDG-0006"), null, null, null, Map(
        1 -> Map("CSF1PO" -> List(Allele(15), Allele(11)),
          "D13S317" -> List(Allele(12), Allele(7)),
          "D16S539" -> List(Allele(10), Allele(9)))
      ), None, None, None, None)

      val profiles = Array(one, three, four, six)

      val genogram = Array(
        Individual(NodeAlias("1"), None, None, Sex.Male, Some(SampleCode("AR-C-SHDG-0001")), false, None),
        Individual(NodeAlias("2"), None, None, Sex.Female, None, false, None),
        Individual(NodeAlias("3"), Some(NodeAlias("1")), Some(NodeAlias("2")), Sex.Female, Some(SampleCode("AR-C-SHDG-0003")), false, None),
        Individual(NodeAlias("4"), Some(NodeAlias("1")), Some(NodeAlias("2")), Sex.Male, Some(SampleCode("AR-C-SHDG-0004")), false, None),
        Individual(NodeAlias("5"), None, None, Sex.Male, None, false, None),
        Individual(NodeAlias("6"), Some(NodeAlias("5")), Some(NodeAlias("3")), Sex.Male, Some(SampleCode("AR-C-SHDG-0006")), true, None)
      )

      val frequencyTable: BayesianNetwork.FrequencyTable = Map(
        "CSF1PO" -> Map(
          0.0 -> 0.000287455444406117,
          6.0 -> 0.000287455444406117,
          7.0 -> 0.00167,
          8.0 -> 0.00448,
          8.3 -> 0.000287455444406117,
          9.0 -> 0.02185,
          10.0 -> 0.26929,
          10.3 -> 0.000287455444406117,
          11.0 -> 0.28222,
          12.0 -> 0.34753,
          13.0 -> 0.06209,
          14.0 -> 0.00868,
          15.0 -> 0.00167,
          -1.0 -> 0.00005),
        "D13S317" -> Map(
          6.0 -> 0.00054,
          7.0 -> 0.000271827769924976,
          8.0 -> 0.09117,
          9.0 -> 0.15994,
          10.0 -> 0.07562,
          11.0 -> 0.22469,
          12.0 -> 0.24182,
          13.0 -> 0.12787,
          14.0 -> 0.07606,
          15.0 -> 0.00196,
          16.0 -> 0.000271827769924976,
          -1.0 -> 0.00005),
        "D16S539" -> Map(
          5.0 -> 0.000273822562979189,
          7.0 -> 0.000273822562979189,
          8.0 -> 0.01758,
          9.0 -> 0.16177,
          10.0 -> 0.10915,
          11.0 -> 0.27673,
          12.0 -> 0.27623,
          13.0 -> 0.1379,
          14.0 -> 0.01917,
          15.0 -> 0.00131,
          -1.0 -> 0.00005)
      )

      val linkage = Map("D16S539" -> ("D13S317", 0.2))
      val result = BayesianNetwork.calculateProbability(profiles, genogram, frequencyTable, AnalysisType(1, "Autosomal"), linkage, true)
//      result mustBe 504.23729 +- 0.0001
      result mustBe 12.561621327801074 +- 0.0001

      val endTime = System.currentTimeMillis()
      println(s"Duration: ${endTime-startTime}")
    }

    "calculate probability with a mutation in the family" in {

      val startTime = System.currentTimeMillis()

      val abom = Profile(null, SampleCode("AR-C-SHDG-0001"), null, null, null, Map(
        1 -> Map("CSF1PO" -> List(Allele(12), Allele(13)),
          "D13S317" -> List(Allele(11), Allele(11)),
          "D16S539" -> List(Allele(10), Allele(15)))
      ), None, None, None, None)

      val tiom7 = Profile(null, SampleCode("AR-C-SHDG-0003"), null, null, null, Map(
        1 -> Map("CSF1PO" -> List(Allele(10), Allele(13)),
          "D13S317" -> List(Allele(11), Allele(12)),
          "D16S539" -> List(Allele(9), Allele(14)))
      ), None, None, None, None)

      val tiom8 = Profile(null, SampleCode("AR-C-SHDG-0004"), null, null, null, Map(
        1 -> Map("CSF1PO" -> List(Allele(11), Allele(12)),
          "D13S317" -> List(Allele(11), Allele(11)),
          "D16S539" -> List(Allele(10), Allele(14)))
      ), None, None, None, None)

      val pi = Profile(null, SampleCode("AR-C-SHDG-0006"), null, null, null, Map(
        1 -> Map("CSF1PO" -> List(Allele(10), Allele(11)),
          "D13S317" -> List(Allele(8), Allele(11)),
          "D16S539" -> List(Allele(11), Allele(12)))
      ), None, None, None, None)

      val profiles = Array(abom, tiom7, tiom8, pi)

      val genogram = Array(
        Individual(NodeAlias("ABOM"), None, None, Sex.Male, Some(SampleCode("AR-C-SHDG-0001")), false, None),
        Individual(NodeAlias("ABAM"), None, None, Sex.Female, None, false, None),
        Individual(NodeAlias("Madre"), Some(NodeAlias("ABOM")), Some(NodeAlias("ABAM")), Sex.Female, None, false, None),
        Individual(NodeAlias("Padre"), None, None, Sex.Male, None, false, None),
        Individual(NodeAlias("TIOM7"), Some(NodeAlias("ABOM")), Some(NodeAlias("ABAM")), Sex.Male, Some(SampleCode("AR-C-SHDG-0003")), false, None),
        Individual(NodeAlias("TIOM8"), Some(NodeAlias("ABOM")), Some(NodeAlias("ABAM")), Sex.Male, Some(SampleCode("AR-C-SHDG-0004")), false, None),
        Individual(NodeAlias("PI"), Some(NodeAlias("Padre")), Some(NodeAlias("Madre")), Sex.Male, Some(SampleCode("AR-C-SHDG-0006")), true, None)
      )

      val frequencyTable: BayesianNetwork.FrequencyTable = Map(
        "CSF1PO" -> Map(
          0.0 -> 0.000287455444406117,
          6.0 -> 0.000287455444406117,
          7.0 -> 0.00167,
          8.0 -> 0.00448,
          8.3 -> 0.000287455444406117,
          9.0 -> 0.02185,
          10.0 -> 0.26929,
          10.3 -> 0.000287455444406117,
          11.0 -> 0.28222,
          12.0 -> 0.34753,
          13.0 -> 0.06209,
          14.0 -> 0.00868,
          15.0 -> 0.00167,
          -1.0 -> 0.00005),
        "D13S317" -> Map(
          6.0 -> 0.00054,
          7.0 -> 0.000271827769924976,
          8.0 -> 0.09117,
          9.0 -> 0.15994,
          10.0 -> 0.07562,
          11.0 -> 0.22469,
          12.0 -> 0.24182,
          13.0 -> 0.12787,
          14.0 -> 0.07606,
          15.0 -> 0.00196,
          16.0 -> 0.000271827769924976,
          -1.0 -> 0.00005),
        "D16S539" -> Map(
          5.0 -> 0.000273822562979189,
          7.0 -> 0.000273822562979189,
          8.0 -> 0.01758,
          9.0 -> 0.16177,
          10.0 -> 0.10915,
          11.0 -> 0.27673,
          12.0 -> 0.27623,
          13.0 -> 0.1379,
          14.0 -> 0.01917,
          15.0 -> 0.00131,
          -1.0 -> 0.00005)
      )

      val result = BayesianNetwork.calculateProbability(profiles, genogram, frequencyTable, AnalysisType(1, "Autosomal"), Map.empty, true)
      result mustBe 0.37879 +- 0.0001
      val endTime = System.currentTimeMillis()
      println(s"Duration: ${endTime-startTime}")
    }

    "calculate probability with a mutation in the PI" in {

      val startTime = System.currentTimeMillis()

      val abom = Profile(null, SampleCode("AR-C-SHDG-0001"), null, null, null, Map(
        1 -> Map("CSF1PO" -> List(Allele(12), Allele(13)),
          "D13S317" -> List(Allele(11), Allele(11)),
          "D16S539" -> List(Allele(10), Allele(15)))
      ), None, None, None, None)

      val abam = Profile(null, SampleCode("AR-C-SHDG-0011"), null, null, null, Map(
        1 -> Map("CSF1PO" -> List(Allele(10), Allele(13)),
          "D13S317" -> List(Allele(11), Allele(12)),
          "D16S539" -> List(Allele(9), Allele(11)))
      ), None, None, None, None)

      val pi = Profile(null, SampleCode("AR-C-SHDG-0021"), null, null, null, Map(
        1 -> Map("CSF1PO" -> List(Allele(10), Allele(11)),
          "D13S317" -> List(Allele(8), Allele(11)),
          "D16S539" -> List(Allele(8), Allele(14)))
      ), None, None, None, None)

      val profiles = Array(abom, abam, pi)

      val genogram = Array(
        Individual(NodeAlias("ABOM"), None, None, Sex.Male, Some(SampleCode("AR-C-SHDG-0001")), false, None),
        Individual(NodeAlias("ABAM"), None, None, Sex.Female, Some(SampleCode("AR-C-SHDG-0011")), false, None),
        Individual(NodeAlias("Madre"), Some(NodeAlias("ABOM")), Some(NodeAlias("ABAM")), Sex.Female, None, false, None),
        Individual(NodeAlias("Padre"), None, None, Sex.Male, None, false, None),
        Individual(NodeAlias("PI"), Some(NodeAlias("Padre")), Some(NodeAlias("Madre")), Sex.Male, Some(SampleCode("AR-C-SHDG-0021")), true, None)
      )

      val frequencyTable: BayesianNetwork.FrequencyTable = Map(
        "CSF1PO" -> Map(
          0.0 -> 0.000287455444406117,
          6.0 -> 0.000287455444406117,
          7.0 -> 0.00167,
          8.0 -> 0.00448,
          8.3 -> 0.000287455444406117,
          9.0 -> 0.02185,
          10.0 -> 0.26929,
          10.3 -> 0.000287455444406117,
          11.0 -> 0.28222,
          12.0 -> 0.34753,
          13.0 -> 0.06209,
          14.0 -> 0.00868,
          15.0 -> 0.00167,
          -1.0 -> 0.00005),
        "D13S317" -> Map(
          6.0 -> 0.00054,
          7.0 -> 0.000271827769924976,
          8.0 -> 0.09117,
          9.0 -> 0.15994,
          10.0 -> 0.07562,
          11.0 -> 0.22469,
          12.0 -> 0.24182,
          13.0 -> 0.12787,
          14.0 -> 0.07606,
          15.0 -> 0.00196,
          16.0 -> 0.000271827769924976,
          -1.0 -> 0.00005),
        "D16S539" -> Map(
          5.0 -> 0.000273822562979189,
          7.0 -> 0.000273822562979189,
          8.0 -> 0.01758,
          9.0 -> 0.16177,
          10.0 -> 0.10915,
          11.0 -> 0.27673,
          12.0 -> 0.27623,
          13.0 -> 0.1379,
          14.0 -> 0.01917,
          15.0 -> 0.00131,
          -1.0 -> 0.00005)
      )

      val result = BayesianNetwork.calculateProbability(profiles, genogram, frequencyTable, AnalysisType(1, "Autosomal"), Map.empty, true)
//      result mustBe 0.0
      result mustBe 0.7754377538832518

      val endTime = System.currentTimeMillis()
      println(s"Duration: ${endTime-startTime}")
    }
  }
}

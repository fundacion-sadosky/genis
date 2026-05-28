package pedigree

import profile.Profile._
import profile._

import scala.concurrent.{Await, ExecutionContext, Future}
import types.SampleCode

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.util.control.Breaks._

object PedigreeConsistencyAlgorithm {

  def initialize(genogram: Array[Individual]): mutable.Map[String, Array[(Double, Double)]] = {
    mutable.Map() ++ genogram.map(individual => {
      val vacio: Array[(Double, Double)] = Array()
      individual.alias.text -> vacio
    }).toMap
  }

  private def isAnyEmptyGenotification(
    couple: (Option[NodeAlias], Option[NodeAlias]),
    childs: Array[NodeAlias],
    gIn: mutable.Map[String, Array[(Double, Double)]]
  ): Boolean = {
    var result = false
    if (couple._1.nonEmpty) {
      if (gIn(couple._1.get.text).length == 0) {
        result = true
      }
    }
    if (couple._2.nonEmpty) {
      if (gIn(couple._2.get.text).length == 0) {
        result = true
      }
    }
    childs.foreach(
      child => {
        if (gIn(child.text).length == 0) {
          result = true
        }
      }
    )
    result
  }

  def isConsistent(
    profiles: Array[Profile],
    genogram: Array[Individual]
  )(implicit ec: ExecutionContext): Seq[PedigreeConsistencyCheck] = {
    val autosomal = 1
    val autosomalProfiles = profiles
      .map(
        p => p.copy(genotypification = p.genotypification.filter(_._1 == autosomal))
      )
    val markers = profiles
      .flatMap(p => p.genotypification.flatMap(_._2.keys))
      .toSet
    val genoContains = (marker: Marker) => (profile: Profile) => {
      profile.genotypification.nonEmpty &&
        profile.genotypification(autosomal).contains(marker)
    }
    val globalCodeForAlias: NodeAlias => Option[SampleCode] = (nodeAlias: NodeAlias) => {
      genogram
        .find(_.alias == nodeAlias)
        .flatMap(_.globalCode)
    }
    val globalCodeForNodeAliases = (nodes: Seq[NodeAlias]) => {
      nodes flatMap { y => globalCodeForAlias(y) }
    }
    val incompatibleProfilesByMarker = Await
      .result(
        Future.sequence(
          markers
            .map(
              marker => {
                langeGoradiaElimination(
                  autosomalProfiles filter genoContains(marker),
                  genogram,
                  marker
                )
              }
            )
        ),
        Duration.Inf
      )
      .map {
        case (marker: Marker, nodes: Seq[NodeAlias]) => (
          marker, globalCodeForNodeAliases(nodes)
        )
      }
    val incompatibleMarkersByProfile = incompatibleProfilesByMarker
      .flatMap(x => x._2.map(y => (y, x._1)))
      .groupBy(_._1)
      .map(
        x => PedigreeConsistencyCheck(
          x._1.text,
          x._2.map(_._2.toString).toList
        )
      )
    incompatibleMarkersByProfile.toSeq
  }

  private def langeGoradiaElimination(
    profiles: Array[Profile],
    genogram: Array[Individual],
    marker: Profile.Marker
  )(implicit ec: ExecutionContext): Future[(Profile.Marker, Seq[NodeAlias])] = {
    langeGoradia(profiles, genogram, marker)
      .flatMap(
        result => {
          if (result._2.nonEmpty) { // Hay una inconsistencia
            Future
              .sequence(
                generateCombinations(profiles, genogram, marker)
              )
              .map(
                result2 => {
                  val primero = result2.filter(x => x._2._2.isEmpty)
                  if (primero.nonEmpty) {
                    // me encontro alguno que me remueve la inconsistencia
                    (marker, primero.flatMap(_._1).distinct)
                  } else {
                    // ninguno me quita la inconsistencia
                    // devuelvo todos
                    (marker, genogram.toList.map(_.alias))
                  }
                }
              )
          } else {
            // es consistente
            Future.successful(result)
          }
        }
      )
  }

  private def generateCombinations(
    profiles: Array[Profile],
    genogram: Array[Individual],
    marker: Marker
  )(implicit ec: ExecutionContext): Seq[Future[(List[NodeAlias], (Marker, List[NodeAlias]))]] = {
    profiles
      .map(p => (p.globalCode, profiles.filter(_.globalCode != p.globalCode)))
      .map(
        profilesCombination => {
          langeGoradia(profilesCombination._2, genogram, marker)
            .map(
              result =>
                (
                  List(
                    genogram
                      .find(_.globalCode.contains(profilesCombination._1))
                      .map(_.alias)
                  )
                    .flatten,
                  result
                )
            )
        }
      )
      .toList
  }

  private def langeGoradia(
    profiles: Array[Profile],
    genogram: Array[Individual],
    marker: Profile.Marker
  )(implicit ec: ExecutionContext): Future[(Profile.Marker, List[NodeAlias])] = {
    val pedAlleles: Array[Double] = getPedAlleles(profiles, genogram, marker)
    // posibles genotipos a partir de los alelos del pedigree
    val gtypes = getPosiblesGenotypes(
      genogram,
      pedAlleles,
      profiles,
      marker
    ) // individuos con sus posibles genotipos
    val subNucs: mutable.Map[(Option[NodeAlias], Option[NodeAlias]), Array[NodeAlias]] = getSubnuclearFamilies(genogram)
    val parent = subNucs.keys
    var parentsToOrder: Array[((Option[NodeAlias], Option[NodeAlias]), Double)] = Array()
    // Ordenar familias subnucleares
    parent.foreach(
      couple => {
        var infoFather, infoMother: Double = 0.0
        if (couple._1.nonEmpty) {
          infoFather = 1.0 / gtypes(couple._1.get.text).length
        }
        if (couple._2.nonEmpty) {
          infoMother = 1.0 / gtypes(couple._2.get.text).length
        }
        val infoParents: Double = infoFather + infoMother
        val children = subNucs(couple)
        var infoChildren: Double = 0.0
        children.foreach(
          child => {
            infoChildren += 1.0 / gtypes(child.text).length
          }
        )
        val order: Double = 2.0 * infoParents + infoChildren
        parentsToOrder = parentsToOrder.+:(couple, order)
      }
    )
    parentsToOrder = parentsToOrder.sortBy(_._2)
    var orderedParents: Array[(Option[NodeAlias], Option[NodeAlias])] = Array()
    parentsToOrder.foreach(couple => {
      orderedParents = orderedParents.+:(couple._1)
    })
    var itera = true
    var incompatibleFam: List[NodeAlias] = Nil
    while (itera) {
      var gIn: mutable.Map[String, Array[(Double, Double)]] = initialize(genogram)
      var change, change1, change2, change3 = false

      orderedParents.foreach(couple => {
        val children = subNucs(couple)
        var gfa, gmo: Array[(Double, Double)] = Array()
        if (couple._1.nonEmpty) gfa = gtypes.getOrElse(couple._1.get.text, Array.empty[(Double, Double)]) else gfa = combinationOfTwo(pedAlleles)
        if (couple._2.nonEmpty) gmo = gtypes.getOrElse(couple._2.get.text, Array.empty[(Double, Double)]) else gmo = combinationOfTwo(pedAlleles)

        val parentalGenotypes = combinationOfParent(gfa, gmo)
        var isSomeGenoCompatWithAllChildren = false
        parentalGenotypes.foreach(parentCombination => {
          var lAux: mutable.Map[String, Array[(Double, Double)]] = mutable.Map.empty
          val gMate: Array[(Double, Double)] = getMate(parentCombination._1, parentCombination._2)
          var hijosComp = 0

          breakable {
            children.foreach(
              child => {
                val gChild = gtypes.getOrElse(child.text, Array.empty[(Double, Double)])
                val comp = gChild.filter(geno => gMate.contains(geno))
                if (comp.length > 0) {
                  hijosComp = hijosComp + 1
                  lAux.put(child.text, (lAux.getOrElse(child.text, Array.empty[(Double, Double)])) ++ (comp))
                  if (couple._1.nonEmpty) {
                    lAux.put(couple._1.get.text, lAux.getOrElse(couple._1.get.text, Array.empty[(Double, Double)]) :+ parentCombination._1)
                  }
                  if (couple._2.nonEmpty) {
                    lAux.put(couple._2.get.text, lAux.getOrElse(couple._2.get.text, Array.empty[(Double, Double)]) :+ parentCombination._2)
                  }
                } else {
                  break()
                }
              }
            )
          }

          if (hijosComp == children.length) { // todos los hijos compatibles?
            isSomeGenoCompatWithAllChildren = true
            if (couple._1.nonEmpty) { // padre
              val lAuxFiltradaFa = lAux(couple._1.get.text).filter(geno => !gIn.getOrElse(couple._1.get.text, Array.empty[(Double, Double)]).contains(geno))
              if (lAuxFiltradaFa.length == lAux(couple._1.get.text).length) {
                change1 = true
                var ginFa = gIn(couple._1.get.text)
                gIn.put(couple._1.get.text, ginFa.++(lAuxFiltradaFa))
              }
            }

            if (couple._2.nonEmpty) { // madre
              val lAuxFiltradaMo = lAux(couple._2.get.text).filter(geno => !gIn.getOrElse(couple._2.get.text, Array.empty[(Double, Double)]).contains(geno))
              if (lAuxFiltradaMo.length == lAux(couple._2.get.text).length) {
                change2 = true
                var ginMo = gIn(couple._2.get.text)
                gIn.put(couple._2.get.text, ginMo.++(lAuxFiltradaMo))
              }
            }

            children.foreach(child => { // hijos
              val lAuxFiltradaCh = lAux(child.text).filter(geno => !gIn.getOrElse(child.text, Array.empty[(Double, Double)]).contains(geno))
              if (lAuxFiltradaCh.length == lAux(child.text).length) {
                var ginCh = gIn(child.text)
                gIn.put(child.text, ginCh.++(lAuxFiltradaCh))
              }
            })
          }
        }) // parentalCombination

        if (!isSomeGenoCompatWithAllChildren || isAnyEmptyGenotification(couple, children, gIn)) {
          // familia incompatible
          incompatibleFam = incompatibleFam ++ children.toList ++ List(couple._1, couple._2).flatten
        } else {
          // guardo los genotipos descubiertos
          if (couple._1.nonEmpty) { // padre
            val fatherGenotypeIter = gIn.getOrElse(couple._1.get.text, Array.empty[(Double, Double)])
            val fatherGenotype = gtypes.getOrElse(couple._1.get.text, Array.empty[(Double, Double)])
            val filterFaGen = fatherGenotype.filter(gen => !fatherGenotypeIter.contains(gen))

            if (fatherGenotypeIter.nonEmpty && filterFaGen.length != 0) {
              change = true
              gtypes.put(couple._1.get.text, fatherGenotypeIter)
            }
          }

          if (couple._2.nonEmpty) { // madre
            val motherGenotypeIter = gIn.getOrElse(couple._2.get.text, Array.empty[(Double, Double)])
            val motherGenotype = gtypes.getOrElse(couple._2.get.text, Array.empty[(Double, Double)])
            val filterMoGen = motherGenotype.filter(gen => !motherGenotypeIter.contains(gen))

            if (motherGenotypeIter.nonEmpty && filterMoGen.length != 0) {
              change = true
              gtypes.put(couple._2.get.text, motherGenotypeIter)
            }
          }

          children.foreach(child => { // hijos
            val childGenotypeIter = gIn.getOrElse(child.text, Array.empty[(Double, Double)])
            val childGenotype = gtypes.getOrElse(child.text, Array.empty[(Double, Double)])
            val filterChildGen = childGenotype.filter(gen => !childGenotypeIter.contains(gen))

            if (childGenotypeIter.nonEmpty && filterChildGen.length != 0) {
              change = true
              gtypes.put(child.text, childGenotypeIter)
            }
          })
        }
      }) // subNucs

      itera = change
    }
    Future.successful((marker, incompatibleFam.distinct))
  }

  private def getMate(
    gfa: (Double, Double),
    gma: (Double, Double)
  ): Array[(Double, Double)] = {
    Array(
      transformToGenogram(Array(gfa._1, gma._1)),
      transformToGenogram(Array(gfa._1, gma._2)),
      transformToGenogram(Array(gfa._2, gma._1)),
      transformToGenogram(Array(gfa._2, gma._2))
    )
  }

  private def getPedAlleles(
    profiles: Array[Profile],
    genogram: Array[Individual],
    marker: Profile.Marker
  ): Array[Double] = {
    val pedAlleles = genogram
      .filter(!_.unknown)
      .flatMap(
        individual => {
          individual.globalCode match {
            case Some(sampleCode) => {
              val profileOpt = profiles
                .find(profile => profile.globalCode == individual.globalCode.get)
              profileOpt
                .fold[scala.List[Double]](Nil) {
                  p => getProfileAlleles(p, 1, marker).toList
                }
            }
            case None => Nil
          }
        }
      )
      .distinct
    (666.0 +: pedAlleles.toList)
      .sortBy(identity)
      .toArray
  }

  private def getProfileAlleles(
    profile: Profile,
    analysisType: Int,
    marker: Profile.Marker
  ): Array[Double] = {
    val strs = profile
      .genotypification
      .getOrElse(1, Map.empty)
      .getOrElse(marker, Nil)
    strs
      .map { allele => transformAlleleValues(allele) }
      .toSet
      .toArray
  }

  private def transformToGenogram(alelles: Array[Double]): (Double, Double) = {
    if (alelles.length == 2) {
      if (alelles(0) < alelles(1)) {
        (alelles(0), alelles(1))
      } else {
        (alelles(1), alelles(0))
      }
    } else if (alelles.length == 1) {
      (alelles(0), alelles(0))
    } else {
      (666.0, 666.0)
    }
  }

  def transformAlleleValues(allele: AlleleValue): Double = {
    allele match {
      case Allele(v)            => v.toDouble
      case OutOfLadderAllele(_, _) => -1.0
      case MicroVariant(_)      => -1.0
      case _                    => 0
    }
  }

  private def getPosiblesGenotypes(
    pedIndividual: Array[Individual],
    pedAlleles: Array[Double],
    profiles: Array[Profile],
    marker: Profile.Marker
  ): mutable.Map[String, Array[(Double, Double)]] = {
    val genotypes = pedIndividual map { individual =>
      individual.alias.text -> {
        individual.globalCode match {
          case Some(sampleCode) => {
            val profileOpt = profiles.find(profile => profile.globalCode == individual.globalCode.get)
            profileOpt match {
              case Some(profile) => {
                Array(transformToGenogram(getProfileAlleles(profile, 1, marker)))
              }
              case None => {
                combinationOfTwo(pedAlleles)
              }
            }
          }
          case None => {
            combinationOfTwo(pedAlleles)
          }
        }
      }
    }
    mutable.Map() ++ genotypes.toMap
  }

  private def combinationOfTwo(alleles: Array[Double]): Array[(Double, Double)] = {
    val copyList = alleles.clone()
    for (x <- alleles; y <- copyList) yield (x, y)
  }

  private def combinationOfParent(
    gFA: Array[(Double, Double)],
    gMO: Array[(Double, Double)]
  ): Array[((Double, Double), (Double, Double))] = {
    for (f <- gFA; m <- gMO) yield (f, m)
  }

  private def getSubnuclearFamilies(
    genogram: Array[Individual]
  ): mutable.Map[(Option[NodeAlias], Option[NodeAlias]), Array[NodeAlias]] = {
    val subNucs: mutable.Map[(Option[NodeAlias], Option[NodeAlias]), Array[NodeAlias]] = mutable.Map()
    genogram map {
      individual =>
        if (individual.idFather.nonEmpty || individual.idMother.nonEmpty) {
          if (subNucs.contains((individual.idFather, individual.idMother))) {
            var children = subNucs((individual.idFather, individual.idMother))
            children = children.+:(individual.alias)
            subNucs.put((individual.idFather, individual.idMother), children)
          } else {
            subNucs += ((individual.idFather, individual.idMother) -> Array(individual.alias))
          }
        }
    }
    subNucs
  }
}

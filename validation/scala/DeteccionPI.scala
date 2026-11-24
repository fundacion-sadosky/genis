package validation

import scala.annotation.tailrec

// El PI ("persona de interes") es el individuo que GENis marca con
// unknown = true. GENis no toma dos hipotesis sino una familia mas un nodo
// marcado, y calcula P(g_PI | familia) / P(g_PI). Eso es igual a
// P(datos|H1)/P(datos|H2) solo si H2 es "H1 sacandole el PI", asi que hay
// que verificarlo, no suponerlo.
object DeteccionPI {

  private val PLACEHOLDER = "__PH__"

  def detectar(c: Caso): Either[String, String] = {
    // El placeholder es un alias sintetico interno. Si coincidiera con un
    // alias real del caso, desconectar lo pisaria en silencio
    // (last-write-wins de Map.+) y corromperia la comparacion de
    // isomorfismo sin ningun error visible. Se chequea en H1 y en H2:
    // desconectar arma el placeholder a partir de H1, pero la comparacion
    // final es contra H2, asi que una colision en cualquiera de los dos
    // lados corrompe el resultado.
    if (c.h1.contains(PLACEHOLDER) || c.h2.contains(PLACEHOLDER))
      return Left(s"el alias '$PLACEHOLDER' esta reservado para la deteccion " +
        "interna del PI y no puede aparecer como alias real en el pedigri")

    val tip = c.tipificados

    val hijosEnH2 = contarHijos(c.h2)
    val candidatos = tip.toList.sorted.filter { a =>
      c.h1.contains(a) && c.h2.contains(a) &&
        c.h2(a).padre.isEmpty && c.h2(a).madre.isEmpty && hijosEnH2.getOrElse(a, 0) == 0
    }

    val verifican = candidatos.filter { pi =>
      isomorfos(podar(desconectar(c.h1, pi), tip), podar(c.h2, tip), tip)
    }

    verifican match {
      case pi :: Nil => Right(pi)
      case Nil =>
        Left("ningun individuo tipificado cumple 'H2 = H1 sin el PI'; " +
          s"candidatos evaluados: ${if (candidatos.isEmpty) "ninguno" else candidatos.mkString(", ")}")
      case varios =>
        Left(s"la deteccion del PI es ambigua: ${varios.mkString(", ")}")
    }
  }

  private def contarHijos(ped: Map[String, IndividuoPed]): Map[String, Int] =
    ped.values.toList
      .flatMap(i => List(i.padre, i.madre).flatten)
      .groupBy(identity)
      .map { case (a, xs) => a -> xs.size }

  // H1 con el PI sacado de su lugar: un placeholder no tipificado ocupa su
  // posicion y el PI queda como fundador aislado. Es exactamente lo que H2
  // deberia ser.
  private def desconectar(h1: Map[String, IndividuoPed], pi: String): Map[String, IndividuoPed] = {
    val original = h1(pi)
    val redirigido = (h1 - pi).map { case (alias, i) =>
      alias -> i.copy(
        padre = i.padre.map(p => if (p == pi) PLACEHOLDER else p),
        madre = i.madre.map(m => if (m == pi) PLACEHOLDER else m))
    }
    redirigido +
      (PLACEHOLDER -> IndividuoPed(PLACEHOLDER, original.padre, original.madre, original.sexo)) +
      (pi -> IndividuoPed(pi, None, None, original.sexo))
  }

  // Un individuo no tipificado sin hijos no aporta nada a la verosimilitud
  // -- tenga o no padres propios, no hace falta que sea fundador -- y el
  // paquete a veces lo omite y a veces no. Se podan iterativamente de los
  // dos lados antes de comparar: podar un nodo puede dejar sin hijos a su
  // padre o madre, que se poda en la pasada siguiente.
  private def podar(ped: Map[String, IndividuoPed], tip: Set[String]): Map[String, IndividuoPed] = {
    @tailrec def paso(m: Map[String, IndividuoPed]): Map[String, IndividuoPed] = {
      val conHijos = m.values.flatMap(i => List(i.padre, i.madre).flatten).toSet
      val sobrantes = m.keySet.filter(a => !tip.contains(a) && !conHijos.contains(a))
      if (sobrantes.isEmpty) m else paso(m -- sobrantes)
    }
    paso(ped)
  }

  // Isomorfismo de pedigries: los tipificados son nodos con nombre fijo, los
  // no tipificados son anonimos y se pueden renombrar. Los pedigries del
  // catalogo tienen a lo sumo 8 individuos, asi que la fuerza bruta sobra.
  private def isomorfos(
    a: Map[String, IndividuoPed],
    b: Map[String, IndividuoPed],
    tip: Set[String]
  ): Boolean = {
    if (a.size != b.size) return false
    if ((a.keySet & tip) != (b.keySet & tip)) return false
    val anonimosA = (a.keySet -- tip).toList.sorted
    val anonimosB = (b.keySet -- tip).toList.sorted
    if (anonimosA.size != anonimosB.size) return false

    val fijos: Map[String, String] = (a.keySet & tip).map(t => t -> t).toMap
    anonimosB.permutations.exists { perm =>
      val mapa = fijos ++ anonimosA.zip(perm).toMap
      a.forall { case (alias, ind) =>
        b.get(mapa(alias)).exists { otro =>
          otro.padre == ind.padre.map(mapa) &&
            otro.madre == ind.madre.map(mapa) &&
            otro.sexo == ind.sexo
        }
      }
    }
  }
}

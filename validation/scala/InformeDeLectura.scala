package validation

import java.io.File

// Informe de lectura: prueba que el arnes lee el paquete correctamente,
// antes de calcular ningun LR.
object InformeDeLectura {

  def main(args: Array[String]): Unit = {
    val dir = new File(if (args.nonEmpty) args(0) else "validation/cases")
    println(s"Paquete: ${dir.getAbsolutePath}")
    println()

    val fallasManifiesto = Paquete.verificarManifiesto(dir)
    if (fallasManifiesto.isEmpty) println("MANIFIESTO.txt: OK, todos los hashes coinciden")
    else {
      println(s"MANIFIESTO.txt: ${fallasManifiesto.size} discrepancia(s)")
      fallasManifiesto.foreach(f => println(s"  - $f"))
    }
    println()

    val casos = Paquete.leer(dir)
    println(s"Casos leidos: ${casos.size}")
    println(s"  con modelo de mutacion: ${casos.count(_.conModeloDeMutacion)}" +
      s" (matriz: ${casos.count(_.matriz.nonEmpty)}, modelo declarado: ${casos.count(_.modelo.nonEmpty)})")
    println(s"  sin modelo de mutacion: ${casos.count(!_.conModeloDeMutacion)}")
    println(s"  con theta distinto de 0: " +
      casos.filter(_.theta != 0.0).map(c => s"${c.id}=${c.theta}").mkString(", "))
    println(s"  multi-locus: " +
      casos.filter(_.frecuencias.size > 1).map(c => s"${c.id}(${c.frecuencias.size})").mkString(", "))
    println()

    println("Por grupo:")
    casos.groupBy(_.grupo).toList.sortBy(_._1).foreach { case (g, cs) =>
      println(f"  $g%-14s ${cs.size}%3d")
    }
    println()

    val fallasInvariantes = Paquete.verificarInvariantes(casos)
    if (fallasInvariantes.isEmpty) println("Invariantes: OK")
    else {
      println(s"Invariantes: ${fallasInvariantes.size} violacion(es)")
      fallasInvariantes.foreach(f => println(s"  - $f"))
    }

    if (fallasManifiesto.nonEmpty || fallasInvariantes.nonEmpty) sys.exit(1)
  }
}

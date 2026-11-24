package validation

import java.io.{File, PrintWriter}

// El sello de la corrida que produjo las particiones de `parcial/`.
//
// Para que existe: RunnerMasiva reanuda salteando toda familia cuyo parcial
// exista, y sin un sello nada dice QUE corrida lo escribio. Sobre una salida
// que quedo de otra version del motor, "reanudar" seria rearmar la entrega vieja
// entera y firmarla con el commit nuevo en condiciones_masiva.txt: numeros de
// una version atribuidos a otra, sin que ningun archivo lo delate. Y como
// condiciones_masiva.txt recien se escribe al FINAL, una corrida interrumpida
// no dejaria procedencia alguna.
//
// El sello se escribe al ARRANCAR una corrida desde cero, en parcial/ mismo
// (es estado de reanudacion, como las particiones, y viaja con ellas si se
// mueven). La identidad de una corrida son el commit y el paquete: dos corridas
// con el mismo commit sobre el mismo MANIFIESTO son "la misma" a los efectos de
// reanudar; cualquier otra combinacion es ajena.
//
// Limite declarado: `-dirty` no distingue dos arboles sucios distintos sobre
// el mismo commit. Es el mismo limite que ya tiene el sello del bloque 1
// (Runner.hashDeCommit) y el de la referencia (commit_actual() de R).
case class Procedencia(
  commit: String,
  artefacto: String,
  manifiesto: String,             // SHA-256 del MANIFIESTO.txt del paquete
  arrancada: String,              // cuando arranco la corrida que escribio el sello
  reanudaciones: List[String] = Nil   // una linea por cada vez que se reanudo
) {
  // La hora de arranque no forma parte de la identidad: una reanudacion
  // arranca a otra hora por definicion.
  def mismaQue(otra: Procedencia): Boolean =
    commit == otra.commit && manifiesto == otra.manifiesto
}

// Que hay en la salida antes de decidir que familias correr.
sealed trait EstadoSalida
// Sin particiones: se arranca desde cero y se escribe el sello.
case object Vacia extends EstadoSalida
// Particiones de ESTA misma corrida (commit y paquete iguales) y la corrida no
// llego a cerrar: se saltean las hechas y se sigue.
case class Reanudable(sello: Procedencia, hechas: Int) extends EstadoSalida
// Particiones de otra corrida, o sin sello: no se reanuda sobre ellas.
// `sello` es None cuando no hay archivo.
case class Ajena(sello: Option[Procedencia], hechas: Int, motivo: String) extends EstadoSalida
// La corrida ya cerro (condiciones_masiva.txt escrito y sin puente pendiente):
// no hay nada que reanudar, y volver a correr es una medicion nueva.
case class Completa(sello: Option[Procedencia]) extends EstadoSalida

// Las dos negativas de RunnerMasiva.correr. Son tipos propios, y no un
// IllegalArgumentException mas, para que main() les pueda dar un codigo de
// salida distinto (5 y 6) y para que el orquestador (validation/correr) las
// reconozca por la linea que las anuncia y mueva la salida a `anteriores/`
// antes de relanzar. Ninguna de las dos toca el disco: mover o borrar no es
// tarea del arnes.
class CorridaAjena(msg: String) extends RuntimeException(msg)
class CorridaCompleta(msg: String) extends RuntimeException(msg)

object Procedencia {

  val ARCHIVO = "PROCEDENCIA.txt"
  val CONDICIONES = "condiciones_masiva.txt"

  private val FORMATO_FECHA = "yyyy-MM-dd HH:mm:ss"

  def ahora(): String =
    new java.text.SimpleDateFormat(FORMATO_FECHA).format(new java.util.Date())

  // El sello que le corresponde a una corrida que arranca ahora.
  def actual(dirPaquete: File, artefacto: String): Procedencia =
    Procedencia(
      commit = Runner.hashDeCommit(),
      artefacto = artefacto,
      manifiesto = Paquete.sha256(new File(dirPaquete, "MANIFIESTO.txt")),
      arrancada = ahora())

  private def archivo(dirParcial: File) = new File(dirParcial, ARCHIVO)

  // Mismo formato "clave = valor" que condiciones_masiva.txt, para que se lea
  // igual desde un editor, desde `grep` o desde el orquestador (validation/correr
  // lo lee con sed para nombrar el directorio al que mueve una salida ajena).
  def escribir(dirParcial: File, p: Procedencia): Unit = {
    dirParcial.mkdirs()
    val w = new PrintWriter(archivo(dirParcial), "UTF-8")
    try {
      w.print(s"commit = ${p.commit}\n")
      w.print(s"artefacto = ${p.artefacto}\n")
      w.print(s"manifiesto = ${p.manifiesto}\n")
      w.print(s"arrancada = ${p.arrancada}\n")
      p.reanudaciones.foreach(r => w.print(s"reanudada = $r\n"))
    } finally w.close()
  }

  def leer(dirParcial: File): Option[Procedencia] = {
    val f = archivo(dirParcial)
    if (!f.exists()) return None
    val fuente = scala.io.Source.fromFile(f, "UTF-8")
    val lineas = try fuente.getLines().toList finally fuente.close()
    val pares = lineas.flatMap { l =>
      l.split(" = ", 2) match {
        case Array(k, v) => Some(k.trim -> v)
        case _ => None
      }
    }
    def valor(k: String) = pares.collectFirst { case (`k`, v) => v }
    for {
      commit <- valor("commit")
      artefacto <- valor("artefacto")
      manifiesto <- valor("manifiesto")
      arrancada <- valor("arrancada")
    } yield Procedencia(commit, artefacto, manifiesto, arrancada,
      pares.collect { case ("reanudada", v) => v })
  }

  // Particiones FINALES: los .tmp son familias a medio escribir y no cuentan.
  def particiones(dirParcial: File): Int =
    Option(dirParcial.listFiles()).toList.flatten
      .count(f => f.isFile && f.getName.endsWith(".csv"))

  def clasificar(dirSalida: File, vigente: Procedencia): EstadoSalida = {
    val dirParcial = new File(dirSalida, "parcial")
    val sello = leer(dirParcial)
    val condiciones = new File(dirSalida, CONDICIONES)
    if (condiciones.exists()) {
      val fuente = scala.io.Source.fromFile(condiciones, "UTF-8")
      val cerrada = try !fuente.mkString.contains("pendiente") finally fuente.close()
      if (cerrada) return Completa(sello)
    }
    val hechas = particiones(dirParcial)
    if (hechas == 0) Vacia
    else sello match {
      case None =>
        Ajena(None, hechas, s"$hechas particiones sin sello de procedencia " +
          s"(anteriores a que existiera $ARCHIVO)")
      case Some(s) if s.mismaQue(vigente) => Reanudable(s, hechas)
      case Some(s) =>
        val motivos =
          (if (s.commit != vigente.commit)
            List(s"commit ${s.commit} y no ${vigente.commit}") else Nil) ++
          (if (s.manifiesto != vigente.manifiesto)
            List("otro manifiesto del paquete") else Nil)
        Ajena(Some(s), hechas,
          s"$hechas particiones de la corrida arrancada el ${s.arrancada}: " +
            motivos.mkString(", "))
    }
  }
}

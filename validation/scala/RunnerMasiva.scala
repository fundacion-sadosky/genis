package validation

import java.io.{File, PrintWriter}

import pedigree.BayesianNetwork

import scala.collection.parallel.ForkJoinTaskSupport
import scala.concurrent.forkjoin.ForkJoinPool
import scala.util.control.NonFatal

// Una celda de la sonda de costo: cuanto sale genotipificar UNA familia del
// tipo `tipo`, y cuanto sale en promedio cada POI cotejado contra ella.
//
// `cptGenotipificados` (el tamano de `g.principal`, la cantidad de CPT que
// devolvio getGenotypification) no es un dato de costo: es la prueba de que
// el motor corrio de verdad. Un tiempo no alcanza para eso: una maquina
// rapida puede medir 0 ms en una celda chica. `cptGenotipificados` no tiene
// esa ambiguedad: ese numero no puede existir si el motor no corrio.
case class CeldaCosto(
  tipo: String,
  grado: String,
  conMutacion: Boolean,
  msGenotipificacion: Long,
  msPorPoi: Double,
  poiMedidos: Int,
  cptGenotipificados: Int
)

// Punto de entrada de la busqueda masiva. Corre el paquete entero por el camino
// de busqueda de GENis (genotipificar una vez por familia, cotejar cada
// POI) y emite la entrega.
object RunnerMasiva {

  // Argumentos parseados, separados de main() para que se puedan probar sin
  // sys.exit. main() queda como despacho y nada mas.
  case class Opciones(
    dirPaquete: File,
    dirSalida: File,
    hilos: Int,
    humo: Boolean,
    puenteK: Int,
    sondaPoi: Option[Int],  // Some(k) => corre la sonda de costo y termina
    reanudar: Boolean       // seguir sobre particiones de OTRA corrida (ver correr)
  )

  val USO: String =
    "uso: RunnerMasiva <dirPaquete> <dirSalida> [hilos] [--humo] [--puente K] [--sonda K] [--reanudar]"

  // Version del artefacto que se declara en condiciones_masiva.txt y en el
  // sello de procedencia. Misma via que Runner.scala (bloque 1): si algun dia
  // alguien setea la propiedad `genis.version`, los dos la toman y ninguno
  // queda con un literal fijo desactualizado.
  def artefacto(): String = scala.util.Properties.propOrElse("genis.version", "5.1.15")

  // Las lineas con las que correr() anuncia que hizo con la salida. Son el
  // protocolo con validation/correr, que no puede leer el codigo de salida
  // de la JVM (sbt lo aplana a 1) y por eso busca estas lineas en el registro
  // de la corrida.
  val AVISO_AJENA = "[masiva] PROCEDENCIA ajena:"
  val AVISO_COMPLETA = "[masiva] PROCEDENCIA completa:"

  // Devuelve Left con el motivo cuando los argumentos no alcanzan o no se
  // entienden, en vez de terminar el proceso: asi main() decide que hacer y el
  // parseo se puede probar.
  def opciones(args: Array[String]): Either[String, Opciones] = {
    val posicionales = args.takeWhile(!_.startsWith("--"))
    if (posicionales.length < 2) return Left(USO)

    def entero(bandera: String): Either[String, Option[Int]] =
      args.indexOf(bandera) match {
        case -1 => Right(None)
        case i if i + 1 >= args.length =>
          Left(s"$bandera necesita un numero y no se le paso ninguno")
        case i =>
          try Right(Some(args(i + 1).toInt))
          catch { case _: NumberFormatException =>
            Left(s"$bandera necesita un numero; se recibio '${args(i + 1)}'") }
      }

    for {
      puente <- entero("--puente").right
      sonda <- entero("--sonda").right
      hilos <- (posicionales.lift(2) match {
        case None => Right(Runtime.getRuntime.availableProcessors())
        case Some(h) =>
          try Right(h.toInt)
          catch { case _: NumberFormatException =>
            Left(s"la cantidad de hilos tiene que ser un numero; se recibio '$h'") }
      }).right
    } yield Opciones(
      dirPaquete = new File(posicionales(0)),
      dirSalida = new File(posicionales(1)),
      hilos = hilos,
      humo = args.contains("--humo"),
      puenteK = puente.getOrElse(0),
      sondaPoi = sonda,
      reanudar = args.contains("--reanudar"))
  }

  def main(args: Array[String]): Unit = opciones(args) match {
    case Left(motivo) =>
      Console.err.println(motivo)
      sys.exit(2)
    // La sonda es lo PRIMERO que conviene correr contra un paquete real: mide el
    // costo por tipo de pedigri antes de comprometer una corrida larga. Termina
    // sin escribir entrega.
    case Right(o) if o.sondaPoi.isDefined =>
      val destino = new File(o.dirSalida, "costo_masiva.csv")
      val celdas = sondear(o.dirPaquete, destino, o.sondaPoi.get)
      // La tabla ya salio linea por linea, a medida que se midio (sondear);
      // aca no se repite.
      println(s"--- sonda: ${celdas.size} celdas -> ${destino.getPath} ---")
    case Right(o) =>
      // Codigos propios para las dos negativas, distintos del 1 de cualquier
      // otra excepcion. La linea que las explica ya la imprimio correr().
      val filas =
        try correr(o.dirPaquete, o.dirSalida, o.hilos, o.humo, o.puenteK, o.reanudar)
        catch {
          case _: CorridaAjena => sys.exit(5)
          case _: CorridaCompleta => sys.exit(6)
        }
      println(s"--- filas escritas: $filas ---")
  }

  def correr(
    dirPaquete: File, dirSalida: File, hilos: Int, humo: Boolean, puenteK: Int = 0,
    reanudar: Boolean = false
  ): Long = {
    // El flujo empieza por verificar el MANIFIESTO.txt. Se reusa
    // Paquete.verificarManifiesto (bloque 1, SHA-256 archivo por archivo) tal
    // cual hace Runner.main: si falla o falta, se aborta ANTES de
    // leer una sola fila del paquete. No se mete adentro de
    // PaqueteMasiva.leer: igual que en el bloque 1, la verificacion es un
    // paso del orquestador, no de la lectura, y asi la lectura sobre un CSV
    // alterado sigue informando la invariante que falla y no un hash que no
    // coincide.
    val fallasManifiesto = Paquete.verificarManifiesto(dirPaquete)
    if (fallasManifiesto.nonEmpty)
      throw new IllegalArgumentException(
        s"MANIFIESTO.txt no verifica; se aborta la corrida: ${fallasManifiesto.mkString("; ")}")

    val p = PaqueteMasiva.leer(dirPaquete)
    // El panel es UNO para todo el escenario: estas tres se calculan una vez,
    // afuera del foreach paralelo de mas abajo, a proposito. alelos,
    // frecuencias y universo son funciones PURAS del paquete -- no dependen
    // de ninguna familia -- asi que llamarlas 200 veces (una por familia)
    // daria exactamente el mismo resultado que llamarlas una sola vez, solo
    // que 200 veces mas caro.
    val alelos = MapeoMasiva.alelos(p) match {
      case Right(m) => m
      case Left(motivo) => throw new IllegalArgumentException(s"panel no representable: $motivo")
    }
    val frecuencias = MapeoMasiva.frecuencias(p, alelos)
    val universo = MapeoMasiva.universo(p, alelos)
    val modelo = ModeloMutacionGenis.desdeDeclarados("masiva", p.modelo, universo) match {
      case Right(m) => m
      case Left(motivo) => throw new IllegalArgumentException(s"modelo no representable: $motivo")
    }

    // La nomina se traduce una sola vez: es la misma para las 200 familias, y
    // por el mismo motivo que arriba -- perfilDePoi tambien es pura --
    // traducirla adentro del foreach daria filas identicas a traducirla aca,
    // solo que 200 veces. Se traduce antes del bucle para no pagar ese costo
    // por nada.
    val poi: List[(String, Either[String, profile.Profile])] =
      p.poi.zipWithIndex.map { case (x, i) => x.poi -> MapeoMasiva.perfilDePoi(x, alelos, i) }

    val dirParcial = new File(dirSalida, "parcial")

    // Antes de saltear una sola familia: DE QUIEN son las particiones que hay.
    // Reanudar es legitimo solo sobre una corrida incompleta de este mismo
    // commit y este mismo paquete. Sobre cualquier otra cosa, correr se niega
    // sin tocar el disco; mover la salida es tarea del orquestador. Ver
    // Procedencia.scala.
    val vigente = Procedencia.actual(dirPaquete, artefacto())
    val procedencia: Procedencia = Procedencia.clasificar(dirSalida, vigente) match {
      case Vacia =>
        println(s"[masiva] PROCEDENCIA nueva: commit ${vigente.commit}, arrancada ${vigente.arrancada}")
        vigente
      case Reanudable(sello, hechas) =>
        println(s"[masiva] PROCEDENCIA reanuda: $hechas particiones de la corrida arrancada el " +
          s"${sello.arrancada} (commit ${sello.commit})")
        sello.copy(reanudaciones = sello.reanudaciones :+ s"${vigente.arrancada} en ${vigente.commit}")
      case Ajena(sello, hechas, motivo) if reanudar =>
        println(s"[masiva] PROCEDENCIA reanuda por --reanudar sobre particiones ajenas: $motivo")
        val base = sello.getOrElse(vigente.copy(commit = "desconocido", arrancada = "desconocida"))
        base.copy(reanudaciones = base.reanudaciones :+
          s"${vigente.arrancada} en ${vigente.commit} (--reanudar: $motivo)")
      case Ajena(_, _, motivo) =>
        val msg = s"$motivo. No se reanuda sobre particiones de otra corrida: mover " +
          s"${dirSalida.getPath} (validation/correr lo hace solo) o, si el codigo no cambio, --reanudar"
        println(s"$AVISO_AJENA $msg")
        throw new CorridaAjena(msg)
      case Completa(sello) =>
        val msg = s"la salida ${dirSalida.getPath} ya esta completa" +
          sello.map(s => s" (corrida arrancada el ${s.arrancada}, commit ${s.commit})").getOrElse("") +
          ". No hay nada que reanudar; una corrida nueva es otra medicion: mover la " +
          "salida (validation/correr lo hace solo) antes de volver a correr"
        println(s"$AVISO_COMPLETA $msg")
        throw new CorridaCompleta(msg)
    }
    Procedencia.escribir(dirParcial, procedencia)

    val pendientes = p.familias.filterNot(f => SalidaMasiva.yaHecha(dirParcial, f.familia))
    val paralelas = pendientes.par
    paralelas.tasksupport = new ForkJoinTaskSupport(new ForkJoinPool(math.max(1, hilos)))

    // Una linea por familia terminada. Sin esto la corrida no emite NADA entre
    // el arranque y la entrega -- horas, en la condicion con mutacion -- y no
    // hay forma de distinguir "avanza despacio" de "se colgo". El directorio
    // `parcial` ya deja una huella por familia y se puede contar desde afuera,
    // pero no dice cuanto tardo ninguna ni desde cuando no avanza, que es
    // justo lo que hay que mirar. println() esta sincronizado sobre el
    // PrintStream, asi que las lineas de los distintos hilos no se parten
    // entre si; el contador es atomico por el mismo motivo.
    val totalPendientes = pendientes.size
    val hechas = new java.util.concurrent.atomic.AtomicInteger(0)
    val t0Lote = System.currentTimeMillis()
    def avisar(fam: FamiliaMasiva, t0Fam: Long): Unit = {
      val k = hechas.incrementAndGet()
      val ahora = System.currentTimeMillis()
      val segFam = (ahora - t0Fam) / 1000.0
      val minLote = (ahora - t0Lote) / 60000.0
      // Extrapolacion por CAUDAL observado (familias terminadas por minuto de
      // reloj), no por el promedio de duracion de una familia: con `hilos` en
      // paralelo el promedio por familia sobrestima lo que falta por el factor
      // de paralelismo. Sigue siendo una extrapolacion y no una promesa: los
      // tipos son de costo muy distinto y el orden en que caen no es uniforme.
      val minResta = minLote / k * (totalPendientes - k)
      println(f"[masiva] $k%d/$totalPendientes%d ${fam.familia}%-8s ${fam.tipo}%-14s" +
        f" $segFam%8.1f s | transcurrido $minLote%7.1f min | resta ~$minResta%7.1f min")
    }
    println(s"--- masiva: $totalPendientes familias pendientes de ${p.familias.size}" +
      s" x ${poi.size} poi sobre $hilos hilos ---")

    paralelas.foreach { fam =>
      val t0Fam = System.currentTimeMillis()
      // SalidaMasiva.escribirFamilia va ADENTRO de este try a proposito: es
      // el bloque que existe para que una familia no pueda tumbar el lote, y
      // un fallo de IO ahi afuera escaparia del foreach paralelo y mataria la
      // corrida entera antes de armar la entrega, el resumen y las
      // condiciones. Adentro, si falla (real o al escribir el "error" de mas
      // abajo), se aisla igual que un fallo de calculo.
      try {
        val filas = MapeoMasiva.familia(p, fam, alelos) match {
          case Left(motivo) =>
            // Regla 4: la familia entrega todo o nada, pero se informa igual.
            poi.map { case (id, _) => FilaMasiva(fam.familia, id, None, "no_representable", motivo) }
          case Right(e) =>
            val g = CorredorMasiva.genotipificar(e, frecuencias, universo, modelo)
            poi.map {
              case (id, Left(motivo)) =>
                FilaMasiva(fam.familia, id, None, "no_representable", motivo)
              case (id, Right(perfil)) =>
                CorredorMasiva.cotejar(g, frecuencias, perfil, modelo) match {
                  case CeldaOk(v, obs) => FilaMasiva(fam.familia, id, Some(v), "ok", obs)
                  case CeldaError(motivo) => FilaMasiva(fam.familia, id, None, "error", motivo)
                }
            }
        }
        SalidaMasiva.escribirFamilia(dirParcial, fam.familia, filas)
      } catch {
        // El try cubre MapeoMasiva.familia, CorredorMasiva.genotipificar Y
        // SalidaMasiva.escribirFamilia, las tres por el mismo motivo: ninguna
        // esta protegida contra excepciones. MapeoMasiva.familia hace
        // `p.tipos(f.tipo)` con `apply` (lanza si el tipo no existe); hoy no
        // puede pasar porque PaqueteMasiva.leer ya lo valida, pero es el
        // mismo modo de falla. genotipificar llama a
        // BayesianNetwork.getGenotypification, que puede lanzar
        // (PedigreeNotHavingUnknownException) y
        // nada garantiza que un pedigri que ya paso MapeoMasiva.familia complete
        // la genotipificacion. escribirFamilia mueve un archivo (Files.move) y
        // puede fallar por IO. Sin este catch, la excepcion se propaga fuera del
        // foreach paralelo y tumba TODA la corrida antes de armar la entrega, el
        // resumen y las condiciones -- contra la regla 4 (una familia
        // que no se puede calcular se informa, no se omite) y a escala de 200
        // familias un solo caso raro tumba el lote entero. Se aisla aca: la
        // familia entera queda en "error" -- no "no_representable": esa etiqueta
        // sigue siendo exclusiva del Left explicito de MapeoMasiva.familia (el
        // mapeo SI funciono y dijo que no puede); "error" es para cuando el
        // calculo aborto -- y la corrida sigue con las demas
        // familias.
        //
        // NonFatal, no Throwable: un
        // OutOfMemoryError en una corrida de 200 familias/horas no puede
        // quedar disfrazado de "error" de una celda mas -- tiene que tumbar
        // la corrida entera para que se note, no perderse en el CSV.
        case NonFatal(ex) =>
          val motivo = s"${ex.getClass.getSimpleName}: " +
            Option(ex.getMessage).getOrElse("sin mensaje")
          // La escritura de emergencia va en su PROPIO try, y es lo que hace
          // cierto el parrafo de arriba. Sin el, el caso mas probable de todos
          // --el fallo de IO que tumbo la primera escritura sigue vigente y
          // tumba tambien esta-- volveria a escaparse del foreach paralelo y
          // mataria la corrida entera: exactamente lo que este bloque existe
          // para impedir.
          //
          // Si tampoco se puede escribir, no se reintenta mas: la familia
          // queda SIN PARCIAL, y SalidaMasiva.armar la reporta por nombre
          // como faltante (regla 4) en vez de dejar pasar una entrega corta
          // por completa. Se avisa por stderr porque en una corrida de horas
          // el operador no tiene otra forma de enterarse hasta el final.
          try {
            SalidaMasiva.escribirFamilia(
              dirParcial, fam.familia,
              poi.map { case (id, _) => FilaMasiva(fam.familia, id, None, "error", motivo) })
          } catch {
            case NonFatal(ex2) =>
              Console.err.println(
                s"--- ${fam.familia}: no se pudo escribir ni el resultado ni el error " +
                  s"(${ex2.getClass.getSimpleName}: ${Option(ex2.getMessage).getOrElse("sin mensaje")}); " +
                  s"la familia queda sin particion y armar la va a reportar ---")
          }
      }
      // Fuera del try/catch a proposito: el catch ya absorbio todo lo NonFatal,
      // asi que una familia que fallo tambien se cuenta y se ve pasar. Lo unico
      // que saltea esta linea es un error fatal, que tumba la corrida entera.
      avisar(fam, t0Fam)
    }

    val destino = new File(dirSalida, "resultados_genis_masiva.csv")
    val escritas = SalidaMasiva.armar(dirParcial, destino, p.familias.map(_.familia))
    // "escritas" va a condiciones_masiva.txt, y antes se cruza contra lo
    // que tenia que salir. Con
    // el chequeo de SalidaMasiva.armar de arriba (familia sin parcial =
    // aborta) esto solo puede fallar si una familia entrego una cantidad de
    // filas distinta de `poi.size` -- pero se deja explicito, porque es la
    // regla 4: una familia entrega todas sus comparaciones o ninguna.
    val esperadas = p.familias.size.toLong * poi.size
    if (escritas != esperadas)
      throw new IllegalArgumentException(
        s"regla 4 violada: se esperaban $esperadas filas (${p.familias.size} familias x " +
          s"${poi.size} poi) y se escribieron $escritas")

    // El resumen se arma releyendo la entrega, y con la variante que PLIEGA
    // sobre el archivo sin materializarlo: con un millon de filas la lista
    // completa no tiene por que entrar en memoria.
    val tipoDe = p.familias.map(f => f.familia -> f.tipo).toMap
    SalidaMasiva.escribirResumen(
      new File(dirSalida, "resumen_masiva.csv"),
      SalidaMasiva.resumenDesdeArchivo(destino, tipoDe))

    // El puente hacia calculateProbability: una submuestra chica,
    // estratificada por tipo de pedigri y por decil del log10LR que esta
    // misma corrida acaba de observar, MAS un estrato aparte de exclusiones
    // (sin el, el puente no contrastaria los dos caminos sobre la cola mas
    // extrema -- la exclusion total). Ver PuenteMasiva.
    // La procedencia se escribe ANTES del puente, y despues se reescribe.
    //
    // Escribirla toda junta despues del puente ataria la entrega a la
    // parte mas cara del proceso: con mutacion el puente completo son
    // horas. Con ese orden,
    // interrumpirlo dejaria `resultados_genis_masiva.csv` y `resumen_masiva.csv`
    // en disco pero SIN `condiciones_masiva.txt` ni `MANIFIESTO.txt`, o sea una
    // entrega sin procedencia: numeros que no se pueden atribuir a ninguna
    // version, ningun paquete ni ningun camino de calculo. Escribirlos primero
    // cuesta milisegundos y vuelve la entrega utilizable apenas termina el
    // calculo; el puente pasa a ser un agregado incremental en vez de un
    // requisito.
    val archivoCondiciones = new File(dirSalida, "condiciones_masiva.txt")
    escribirCondiciones(
      archivoCondiciones, p, modelo, procedencia, hilos, humo, escritas, puenteK, Nil,
      puentePendiente = puenteK > 0)
    copiarManifiesto(dirPaquete, dirSalida)

    val tiposSinMuestraPuente: List[String] =
      if (puenteK > 0) {
        val filas = SalidaMasiva.leerFilas(destino)
        val muestras = PuenteMasiva.muestrear(filas, tipoDe, puenteK, semilla = 20260828L) ++
          PuenteMasiva.muestrearExcluidas(filas, tipoDe, puenteK, semilla = 20260828L)
        val celdas = PuenteMasiva.contrastar(p, alelos, frecuencias, universo, modelo, muestras)
        PuenteMasiva.escribir(new File(dirSalida, "puente_masiva.csv"), celdas)
        // Que un tipo se haya quedado sin
        // ninguna celda muestreada (de decil o de exclusion) se declara,
        // no se pierde en silencio.
        PuenteMasiva.tiposSinMuestra(tipoDe, muestras)
      } else Nil

    // Segunda escritura: ahora con el resultado del puente. Si la corrida se
    // interrumpio antes de llegar aca, queda la primera, que declara
    // `puente = pendiente` en vez de decir que no se pidio.
    if (puenteK > 0)
      escribirCondiciones(
        archivoCondiciones, p, modelo, procedencia, hilos, humo, escritas, puenteK,
        tiposSinMuestraPuente)
    escritas
  }

  // Mide el costo real por tipo de pedigri antes de comprometer una corrida
  // larga. Se mide sobre UNA familia de cada
  // tipo: el costo de la genotipificacion es por familia y por locus, no por
  // comparacion, asi que es la unidad correcta.
  //
  // Que mide y que NO mide esta muestra de una sola familia (`fams.head`):
  // cubre la variacion ESTRUCTURAL entre tipos -- la topologia del pedigri es
  // la misma para todas las familias de un tipo (todas las de "abuelos"
  // tienen la misma forma) -- pero NO cubre la variacion
  // de DATOS dentro de un mismo tipo: CorredorMasiva.genotipificar llama a
  // BayesianNetwork.extendNWithObservedAlleles, que extiende el universo con
  // los alelos OBSERVADOS en esa familia concreta, y dos familias del mismo
  // tipo con alelos distintos pueden terminar con `universoGeno` de tamano
  // distinto y por lo tanto con un costo de genotipificacion distinto.
  // `fams.head` no tiene ningun criterio de representatividad sobre esa
  // segunda fuente de variacion. No se corrige midiendo mas de una familia
  // por tipo: eso multiplicaria el costo de la sonda, que existe justamente
  // para ser barata (la pregunta que contesta es "cuanto sale, aproximado y
  // rapido de medir", no "cual es el costo exacto de cada familia").
  //
  // Que NO se puede concluir con el paquete SINTETICO: PaqueteSintetico solo
  // declara t1 (progenitores), t2 (abuelos) y t3 (hermanos); no tiene tipos
  // con tios. La limitacion es del fixture, no del instrumento -- `sondear`
  // es agnostico al tipo (agrupa por `p.familias.groupBy(_.tipo)`, lo que sea
  // que declare `tipos.csv`), asi que sobre un paquete completo los mide sin
  // que este codigo tenga que cambiar.
  //
  // Que se acepta como esta: esta sonda no hace warm-up de JVM ni repite
  // mediciones. Sobre el paquete real eso no importa -- las familias cuestan
  // de segundos a horas, y el ruido de JIT/carga de clases es insignificante
  // al lado de eso -- pero sobre un fixture chico y sintetico (como el de
  // PaqueteSintetico) ese ruido puede dominar la medicion: la
  // primera celda calculada en una JVM fria puede medir varias veces mas que
  // las que siguen, sin que eso diga nada sobre el costo relativo real entre
  // tipos. Quien lea la tabla de una corrida sobre un paquete chico no deberia
  // sacar conclusiones de las cifras absolutas por ese motivo.
  def sondear(dirPaquete: File, destino: File, poiPorTipo: Int): List[CeldaCosto] = {
    // Mismo chequeo que correr -- ver su comentario.
    // sondear es lo PRIMERO que corre contra el paquete real, precisamente
    // para no comprometer horas de corrida a ciegas; dejarla correr sobre un
    // paquete cuyo manifiesto no verifica lo contradice.
    val fallasManifiesto = Paquete.verificarManifiesto(dirPaquete)
    if (fallasManifiesto.nonEmpty)
      throw new IllegalArgumentException(
        s"MANIFIESTO.txt no verifica; se aborta la sonda: ${fallasManifiesto.mkString("; ")}")

    val p = PaqueteMasiva.leer(dirPaquete)
    val alelos = MapeoMasiva.alelos(p) match {
      case Right(m) => m
      case Left(motivo) => throw new IllegalArgumentException(s"panel no representable: $motivo")
    }
    val frecuencias = MapeoMasiva.frecuencias(p, alelos)
    val universo = MapeoMasiva.universo(p, alelos)
    val modelo = ModeloMutacionGenis.desdeDeclarados("masiva", p.modelo, universo) match {
      case Right(m) => m
      case Left(motivo) => throw new IllegalArgumentException(s"modelo no representable: $motivo")
    }
    // match+throw y no `.right.get`, aca y en MapeoMasiva.familia mas
    // abajo: un `.right.get` sobre un Left lanza NoSuchElementException sin
    // decir CUAL poi o CUAL familia declara mal el paquete -- justo lo que
    // `sondear` existe para evitar antes de comprometer una corrida larga.
    // Es el mismo patron que usa `correr`.
    val muestra = p.poi.take(poiPorTipo).zipWithIndex.map { case (x, i) =>
      MapeoMasiva.perfilDePoi(x, alelos, i) match {
        case Right(perfil) => perfil
        case Left(motivo) =>
          throw new IllegalArgumentException(s"poi ${x.poi} no representable: $motivo")
      }
    }

    val tipos = p.familias.groupBy(_.tipo).toList.sortBy(_._1)
    println(s"--- sonda: ${tipos.size} tipos x ${muestra.size} poi ---")
    val celdas = tipos.map { case (tipo, fams) =>
      val e = MapeoMasiva.familia(p, fams.head, alelos) match {
        case Right(entrada) => entrada
        case Left(motivo) =>
          throw new IllegalArgumentException(s"familia ${fams.head.familia} no representable: $motivo")
      }
      val t0 = System.currentTimeMillis()
      val g = CorredorMasiva.genotipificar(e, frecuencias, universo, modelo)
      val t1 = System.currentTimeMillis()
      muestra.foreach(pf => { CorredorMasiva.cotejar(g, frecuencias, pf, modelo); () })
      val t2 = System.currentTimeMillis()
      val celda = CeldaCosto(tipo, p.tipos(tipo).grado, p.conMutacion,
        t1 - t0, if (muestra.isEmpty) 0.0 else (t2 - t1).toDouble / muestra.size, muestra.size,
        g.principal.length)
      // La sonda es serial sobre los tipos y existe para medir un costo que
      // puede ser de horas: imprimir las celdas recien al final la vuelve tan
      // ciega como la corrida que viene a proteger.
      println(f"[sonda] ${celda.tipo}%-14s ${celda.grado}%-10s" +
        f" genotipificar ${celda.msGenotipificacion}%8d ms," +
        f" ${celda.msPorPoi}%9.2f ms por poi, ${celda.cptGenotipificados}%d CPT")
      celda
    }

    Option(destino.getParentFile).foreach(_.mkdirs())
    val w = com.github.tototoshi.csv.CSVWriter.open(destino, "UTF-8")(SalidaMasiva.FormatoLF)
    try {
      w.writeRow(List("tipo", "grado", "con_mutacion", "ms_genotipificacion",
        "ms_por_poi", "poi_medidos", "cpt_genotipificados"))
      celdas.foreach(c => w.writeRow(List(c.tipo, c.grado, c.conMutacion.toString,
        c.msGenotipificacion.toString, SalidaMasiva.formatear(c.msPorPoi),
        c.poiMedidos.toString, c.cptGenotipificados.toString)))
    } finally w.close()
    celdas
  }

  private def escribirCondiciones(
    destino: File, p: PaqueteMasivo,
    modelo: (Option[Long], Option[List[BayesianNetwork.MutationModelData]]),
    procedencia: Procedencia,
    hilos: Int, humo: Boolean, filas: Long, puenteK: Int,
    tiposSinMuestraPuente: List[String],
    // Primera de las dos escrituras: el puente se pidio pero todavia no corrio.
    // Tiene default para que los llamadores que escriben el archivo definitivo
    // no tengan que enterarse de que existe la escritura previa.
    puentePendiente: Boolean = false
  ): Unit = {
    val w = new PrintWriter(destino, "UTF-8")
    try {
      if (humo) w.print("# CORRIDA DE HUMO sobre paquete sintetico - NO es una entrega\n")
      w.print(s"artefacto = ${artefacto()}\n")
      w.print(s"commit = ${Runner.hashDeCommit()}\n")
      // De donde vienen las particiones que esta entrega rearma: cuando
      // arranco la corrida que las empezo, con que commit, y cada vez que se
      // reanudo. `commit` de arriba es el de ESTE proceso; si difiere del de
      // `arrancada`, la entrega mezcla familias de dos versiones y aca queda
      // dicho (solo puede pasar con --reanudar).
      w.print(s"arrancada = ${procedencia.arrancada} en ${procedencia.commit}\n")
      procedencia.reanudaciones.foreach(r => w.print(s"reanudada = $r\n"))
      w.print("camino = busqueda (getGenotypification por familia + findCompatibilityMatches por POI)\n")
      w.print("umbral de reporte = salteado (se entregan todas las celdas)\n")
      w.print(s"familias = ${p.familias.size}\n")
      w.print(s"poi = ${p.poi.size}\n")
      w.print(s"celdas = $filas\n")
      w.print(s"hilos = $hilos\n")
      // El puente hacia calculateProbability (PuenteMasiva): K celdas
      // por decil y tipo de pedigri MAS el estrato de exclusiones, o "no
      // corrido" si no se pidio (puenteK <= 0). La semilla y los tipos sin
      // muestra solo se reportan cuando el puente de verdad corrio.
      // `puente.tipos_sin_muestra`: que un tipo entero se haya quedado sin
      // ninguna celda muestreada tiene que
      // quedar declarado ACA, no descubrirse cruzando a mano contra
      // resumen_masiva.csv.
      // Tres estados distintos y no dos: "no corrido" (no se pidio),
      // "pendiente" (se pidio y el archivo se escribio antes de correrlo) y el
      // numero (corrio). Sin el estado intermedio, una corrida interrumpida en
      // el puente dejaria un acta diciendo "no corrido", que es falso: se pidio
      // y no se sabe si llego a terminar.
      w.print(s"puente = ${
        if (puenteK <= 0) "no corrido"
        else if (puentePendiente) s"$puenteK (pendiente: la corrida no llego a terminarlo)"
        else puenteK.toString}\n")
      if (puenteK > 0 && !puentePendiente) {
        w.print("puente.semilla = 20260828\n")
        w.print(s"puente.tipos_sin_muestra = " +
          s"${if (tiposSinMuestraPuente.isEmpty) "ninguno" else tiposSinMuestraPuente.mkString(", ")}\n")
      }
      // Estas lineas NO se derivan de `p.modelo`, el CSV DECLARADO, sino de
      // lo que de verdad llego al motor: el `modelo` ya
      // TRADUCIDO -- el mismo tuple que CorredorMasiva.genotipificar/cotejar
      // reciben --, incluida la cantidad de ki no nulos: un modelo con la
      // calibracion vacia (todos los ki en 0) o un (None, None) colado por
      // error quedan visibles ACA, en el artefacto que declara la corrida,
      // en vez de quedar indistinguibles de una corrida con mutacion sana.
      val (tipoModelo, datosModelo) = modelo
      val nombreModelo = tipoModelo.flatMap(t => ModeloMutacionGenis.TIPO.map(_.swap).get(t))
      w.print(s"mutacion = ${nombreModelo.getOrElse("sin modelo")}\n")
      datosModelo.foreach { datos =>
        val rango = datos.flatMap(_._1.mutationRange).headOption
        val pasos = datos.map(_._3.cantSaltos).headOption
        val kisNoNulos = datos.flatMap(_._2).count(_.ki != BigDecimal(0))
        w.print(s"mutacion.rango = ${rango.map(_.toString).getOrElse("")}\n")
        w.print(s"mutacion.pasos = ${pasos.map(_.toString).getOrElse("")}\n")
        w.print(s"mutacion.ki_no_nulos = $kisNoNulos\n")
      }
    } finally w.close()
  }

  private def copiarManifiesto(dirPaquete: File, dirSalida: File): Unit = {
    val origen = new File(dirPaquete, "MANIFIESTO.txt")
    if (origen.exists()) {
      val w = new PrintWriter(new File(dirSalida, "MANIFIESTO.txt"), "UTF-8")
      try {
        val fuente = scala.io.Source.fromFile(origen, "UTF-8")
        try w.print(fuente.mkString) finally fuente.close()
      } finally w.close()
    }
  }
}

package validation

import pedigree.BayesianNetwork

sealed trait Resultado
// `matrices` lleva la matriz de mutacion que el motor USO en esta corrida, no
// una reconstruccion: se arma adentro de correr(), con el mismo universo y el
// mismo modelo que se le pasaron al motor. Vacia en los casos sin modelo.
case class Calculado(lr: Double, matrices: List[MatrizVolcada] = Nil) extends Resultado
case class NoSoportado(motivo: String) extends Resultado
case class NoRepresentable(motivo: String) extends Resultado
case class Fallo(motivo: String) extends Resultado

// Invoca el motor de pedigri de GENis para un caso del catalogo.
object Corredor {

  // `piso` es la frecuencia minima que se le declara a GENis. Por omision va
  // el piso inerte, que deja el panel del caso intacto. Se puede pasar otro
  // para medir el efecto del piso.
  def correr(c: Caso, piso: Double = MapeoGenis.PISO_INERTE): Resultado = {
    // Coancestria: el motor de pedigri no tiene parametro que recibirla (su
    // calculo equivale a theta = 0). Un caso que pide theta distinto de cero
    // no se calcula: el valor con theta = 0 no responde lo que el caso
    // pregunta.
    if (c.theta != 0.0)
      return NoSoportado(
        s"el motor de pedigri de GENis no aplica correccion por coancestria; " +
          s"el caso pide theta = ${c.theta}")

    // El try cubre tambien el mapeo, no solo la llamada al motor.
    // MapeoGenis.mapear y DeteccionPI.detectar (que mapear invoca) hacen
    // varios accesos de Map sin `.get` (alelos(nombre), codigos(individuo),
    // h1(pi), mapa(alias)) que asumen invariantes del caso -- por ejemplo,
    // que toda referencia de padre/madre en el pedigri apunta a un individuo
    // que existe. Con el catalogo actual (que pasa por Paquete.leer y su
    // verificarInvariantes) esas precondiciones siempre se cumplen y la
    // excepcion nunca se dispara, pero el contrato de correr() es no
    // propagar NUNCA, y un Caso puede llegar por otra via (un test, un uso
    // futuro). Importante: no hay ningun `return` dentro de este try. En
    // Scala, un `return` dentro de un try se implementa lanzando una
    // excepcion de control (scala.runtime.NonLocalReturnControl); un catch
    // amplio como el de aca abajo la interceptaria igual que cualquier otra
    // excepcion, convirtiendo un resultado valido (por ejemplo un
    // NoRepresentable legitimo) en un Fallo espurio. Por eso cada rama deja
    // su resultado como el valor final del match, que a su vez es el valor
    // final del try -- nunca se "retorna" desde adentro.
    try {
      MapeoGenis.mapear(c, piso) match {
        case Left(motivo) => NoRepresentable(motivo)
        case Right(e) => modeloDe(c, e) match {
          case Left(motivo) => NoRepresentable(motivo)
          case Right((tipo, datos)) =>
          val (lr, _) = BayesianNetwork.calculateProbability(
            profiles = e.perfiles,
            genogram = e.genograma,
            frequencyTable = e.frecuencias,
            analysisType = MapeoGenis.TIPO_DE_ANALISIS,
            linkage = Map.empty,
            verbose = false,
            mutationModelType = tipo,
            mutationModelData = datos,
            seenAlleles = e.universo,
            // Sin rango explicito el motor convierte el codigo del silente a
            // fuera de escalera (-1.0). Ver MapeoGenis.rangoDeAlelos.
            locusRangeMap = e.rango)
          // Se le pasa `datos`, la MISMA lista de modelos que acaba de
          // recibir el motor, para que el volcado no construya otro.
          Calculado(lr, volcarDelCaso(c, e, datos))
        }
      }
    } catch {
      case ex: Throwable =>
        Fallo(s"${ex.getClass.getSimpleName}: ${Option(ex.getMessage).getOrElse("sin mensaje")}")
    }
  }

  // Un caso sin modelo declarado (c.modelo vacio) corre con None: el LR
  // mendeliano estricto, que es el que corresponde a MUT-08 y PAR-17.
  //
  // La matriz explicita (c.matriz) NO se usa: GENis no puede recibir una
  // matriz.
  //
  // El armado de argumentos de mutacion en si vive en
  // ModeloMutacionGenis.desdeDeclarados (lo comparte con la busqueda masiva);
  // aca queda solo la delegacion con los nombres de campo del catalogo.
  private def modeloDe(
    c: Caso,
    e: EntradaGenis
  ): Either[String, (Option[Long], Option[List[BayesianNetwork.MutationModelData]])] =
    ModeloMutacionGenis.desdeDeclarados(c.id, c.modelo, e.universo)

  // El volcado ocurre ACA, con `e` y con los modelos en la mano: los mismos
  // que acaban de entrar al motor. No se rearma desde el CSV ni desde el
  // Caso, y tampoco se rearma el modelo: se vuelca el objeto que recibio
  // `calculateProbability`, uno por fila de `modeloDe`. Cada uno sabe su
  // locus y su sexo, asi que el emparejamiento no necesita volver a mirar
  // c.modelo.
  //
  // Los DOS dominios tienen que ser los que el motor uso de verdad, y no son
  // el mismo:
  //
  //  - UNIVERSO: el AMPLIADO. `e.universo` es lo que se le PASA al motor,
  //    pero no es lo que el motor USA: calculateProbability lo amplia
  //    con extendNWithObservedAlleles antes de armar ninguna tabla, asi que
  //    volcar sobre e.universo daria una matriz de tamano distinto del real
  //    en cuanto un alelo observado no este en el panel. Por eso se llama a
  //    la MISMA funcion publica que llama el motor.
  //
  //  - CALIBRACION: el PANEL, e.universo, no el ampliado. Es el dominio
  //    sobre el que modeloDe calibro las constantes de normalizacion del
  //    modelo que se le paso al motor. Volcar con `calibracion = universo`
  //    (el ampliado) haria que, en cuanto los dos dominios difieran -- un
  //    solo alelo observado fuera del panel alcanza --, el volcado reporte
  //    constantes de un dominio mientras el LR viene de las de otro. Con el
  //    catalogo de hoy los dos coinciden (todo alelo observado esta en el
  //    panel), pero el catalogo trae modelos a pasos, que son
  //    precisamente los que dependen de esas constantes.
  private def volcarDelCaso(
    c: Caso,
    e: EntradaGenis,
    modelos: Option[List[BayesianNetwork.MutationModelData]]
  ): List[MatrizVolcada] = modelos match {
    case None => Nil
    case Some(ms) =>
      // La grafia incluye los alelos OBSERVADOS ademas de los del panel y de
      // la escalera declarada: el universo ampliado puede traer un alelo que
      // solo aparece en un perfil, y sin su nombre el volcado no podria
      // escribir esa fila. mapearAlelos ya corrio sobre panel+observados
      // dentro de MapeoGenis.mapear (estamos en su rama Right), asi que este
      // mapeo no puede fallar por algo que aquel no haya visto.
      val codigos = MapeoGenis.mapearAlelos(
        c.frecuencias.values.flatMap(_.keys).toSet ++
          c.perfiles.flatMap(_.alelos).toSet ++
          c.modelo.flatMap(_.alelos).toSet) match {
        case Right(m) => m
        case Left(motivo) => throw new IllegalArgumentException(s"${c.id}: $motivo")
      }
      val grafia = codigos.map { case (nombre, codigo) => codigo -> nombre }

      val universoReal = BayesianNetwork.extendNWithObservedAlleles(
        e.universo, e.perfiles, e.genograma, MapeoGenis.TIPO_DE_ANALISIS)

      ms.map { datos =>
        val locus = datos._1.locus
        VolcadoMatrices.volcarConModelo(
          origen = "caso",
          id = c.id,
          locus = locus,
          datos = datos,
          universo = universoReal.getOrElse(locus, Nil),
          calibracion = e.universo.getOrElse(locus, Nil),
          grafia = grafia)
      }
  }
}

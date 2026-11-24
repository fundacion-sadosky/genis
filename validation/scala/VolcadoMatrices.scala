package validation

import pedigree.{BayesianNetwork, MutationService, Variable, VariableKind}

// Vuelca la matriz de mutacion que el motor de GENis usa.
//
// REGLA DURA: este archivo NO tiene aritmetica propia. Es un doble bucle que
// llama a las dos funciones del motor que producen las celdas y escribe lo
// que devuelven. Si aca aparece una multiplicacion de probabilidades, dejo de
// ser un volcado y pasa a ser una reimplementacion. La unica cuenta permitida
// es comparar dos alelos
// por igualdad para elegir entre diagonal y no diagonal, que es lo mismo que
// hace getRowProbability.
//
// UNA SOLA EXCEPCION, y esta declarada: este archivo llama
// tambien a MutationService.recalibrateKis. No es aritmetica propia --es una
// funcion de app/, la misma que usa el motor-- pero SI es un paso del trabajo
// del motor reproducido aca, con lo que eso implica. Que cubre y que no esta
// en el comentario de volcarConModelo.

case class CeldaVolcada(
  origen: String,        // "grilla" | "caso"
  id: String,
  locus: String,         // vacio cuando origen = "grilla"
  sexo: String,
  aleloOrigen: String,
  aleloDestino: String,
  probabilidad: Double
)

case class ContextoVolcado(
  origen: String,
  id: String,
  locus: String,
  sexo: String,
  alelo: String,
  ki: BigDecimal,
  enCalibracion: Boolean,
  enUniverso: Boolean
)

case class MatrizVolcada(celdas: List[CeldaVolcada], contexto: List[ContextoVolcado])

object VolcadoMatrices {

  // getSex (BayesianNetwork) deduce el sexo del sufijo del nombre
  // de la variable. Aca el sexo se pasa explicito a las dos funciones, pero
  // el nombre se arma coherente igual: si alguien mas adelante llama por la
  // via de getRowProbability, tiene que dar lo mismo.
  private def sufijoDe(sexo: String): String = sexo match {
    case "M" => "_p"
    case "F" => "_m"
    case _ => ""
  }

  // ENTRADA PRINCIPAL: recibe el modelo YA CONSTRUIDO y vuelca la matriz que
  // el motor produce con el.
  //
  // Existe porque el modo caso tiene que volcar la matriz que produjo el LR, no
  // una equivalente. Armar otro modelo aca -- aunque fuera con los mismos
  // parametros -- lo calibraria sobre el dominio que se le pase a esta funcion,
  // y basta que ese dominio difiera del que uso el motor para que las
  // constantes de normalizacion cambien: la matriz entregada dejaria de ser la
  // que produjo el LR, sin ningun aviso. Todo el diseno de validacion se apoya en
  // que esa matriz sea la usada -- el modulo de referencia evalua su formula de
  // referencia SOBRE ella para separar el error del modelo del error del
  // algoritmo -- asi que un desajuste ahi no rompe un numero: invalida la
  // separacion entera.
  //
  // ESO OBLIGA A RECALIBRAR ACA, y conviene entender por que el
  // parrafo de arriba no se contradice. El motor no usa los k_i tal como
  // vienen: getGenotypification y getGenotypificationWithMarkers los pasan por
  // MutationService.recalibrateKis sobre el `n` vigente antes de armar ninguna
  // tabla. O sea que volcar el objeto recibido tal cual seria volcar una
  // matriz que el motor NO produce, en cuanto el universo difiere del dominio
  // de calibracion. Espejar la recalibracion no es reconstruir: es la unica
  // forma de volcar lo que el motor usa.
  //
  // La condicion de validez, que es lo unico que
  // sostiene el espejo: `universo` TIENE que ser el dominio que el motor uso.
  //   - modo caso: Corredor.volcarDelCaso lo calcula con
  //     BayesianNetwork.extendNWithObservedAlleles, la misma funcion publica
  //     que llama calculateProbability. No es una copia de la regla:
  //     es la regla.
  //   - modo grilla: no hay motor que igualar; el universo lo declara la
  //     propia fila (columna panel_universo).
  // Si alguna vez `universo` deja de ser ese dominio, este volcado deja de
  // corresponder a lo que el motor usa, sin ningun aviso.
  //
  // QUE CUBRE Y QUE NO. El espejo hace que el volcado refleje todo lo que
  // pasa DENTRO de recalibrateKis: la formula de k_i, el filtro del dominio (el
  // -1.0 del piso fmin, los alelos no positivos), la guarda del cero. Y
  // tambien todo lo de las dos funciones de celda.
  //
  // NO cubre que el motor efectivamente LLAME a recalibrateKis: si esa
  // llamada faltara en getGenotypification, este volcado seguiria espejando
  // por su cuenta y la grilla no lo notaria. Esa verificacion necesita
  // correr calculateProbability punta a punta, y no es tarea de este archivo.
  //
  // `calibracion` no se usa para construir nada: entra solo para INFORMAR en el
  // contexto sobre que dominio venia calibrado el modelo (columna
  // en_calibracion). Ojo que un alelo con en_calibracion = "no"
  // NO sale con ki = 0: se informa el k_i vigente, que es el que el motor
  // usa.
  def volcarConModelo(
    origen: String,
    id: String,
    locus: String,
    datos: BayesianNetwork.MutationModelData,
    universo: List[Double],
    calibracion: List[Double],
    grafia: Map[Double, String]
  ): MatrizVolcada = {
    // Una sola fuente para el sexo, el locus y el tipo: el modelo recibido.
    // Son los tres datos que el motor mira (getSex sobre el sufijo del nombre
    // de variable, findMutationParam sobre el locus, mutationType sobre la
    // cabecera), asi que tomarlos de un parametro aparte permitiria que el
    // rotulo del volcado y el modelo volcado dijeran cosas distintas.
    val sexo = datos._1.sex
    val locusDelModelo = datos._1.locus
    val tipo = Some(datos._3.mutationType)
    val n = Map(locusDelModelo -> universo)
    // El motor recalibra k_i sobre el universo vigente antes de armar
    // ninguna tabla, asi que volcar `datos` tal como vino daria una matriz que
    // el motor no produce. Se espeja esa recalibracion con la MISMA funcion de
    // app/ y el MISMO `n` que se les pasa abajo a las dos funciones de celda:
    // asi es imposible que el dominio del volcado y el de la recalibracion se
    // separen. Ver el comentario de la funcion para el alcance.
    val datosVigentes = MutationService.recalibrateKis(List(datos), n).head
    val lista = Some(List(datosVigentes))
    val variable =
      Variable(s"PI_$locusDelModelo${sufijoDe(sexo)}", locusDelModelo, VariableKind.Genotype)

    def nombre(a: Double): String =
      grafia.getOrElse(a, throw new IllegalArgumentException(
        s"$id: el alelo $a no esta en la grafia del panel"))

    val celdas = for {
      i <- universo
      j <- universo
    } yield {
      val prob =
        if (i == j)
          BayesianNetwork.getProbabityOfDiagonal(variable, tipo, lista, i, n, sexo)
        else
          BayesianNetwork.getProbabityOfNonDiagonal(
            variable, tipo, lista, universo, i, j, sexo)
      CeldaVolcada(origen, id, locus, sexo, nombre(i), nombre(j), prob)
    }

    // Los k_i que se informan son los VIGENTES, los mismos con los que se
    // produjeron las celdas de arriba. Consecuencia visible: un
    // alelo con en_calibracion = "no" no sale con ki = 0, porque el motor
    // tampoco lo usa en cero. La columna ki es informativa --ningun criterio
    // del lado R la lee (mutgrid.R: se importa y se cuelga como
    // atributo, nada mas)--, pero tiene que ser coherente con las celdas o el
    // volcado se contradice a si mismo.
    val kis = datosVigentes._2.map(k => k.allele -> k.ki).toMap
    val contexto = (universo ++ calibracion).distinct.sorted.map { a =>
      ContextoVolcado(
        origen = origen,
        id = id,
        locus = locus,
        sexo = sexo,
        alelo = nombre(a),
        ki = kis.getOrElse(a, BigDecimal(0)),
        enCalibracion = calibracion.contains(a),
        enUniverso = universo.contains(a))
    }

    MatrizVolcada(celdas, contexto)
  }

  // ENTRADA POR PARAMETROS: construye el modelo y delega. Es la del modo
  // grilla, donde no hay ningun modelo previo que respetar -- la grilla ES la
  // parametrizacion, y su universo y su calibracion son los dos paneles que
  // la propia fila declara. Aca construir el modelo no reconstruye nada: no
  // existe otro.
  //
  // La recalibracion NO se hace aca: la hace volcarConModelo, que es
  // el delegado de los dos modos. Un solo lugar, un solo comentario -- y asi
  // no puede pasar que un modo espeje y el otro no.
  def volcar(
    origen: String,
    id: String,
    locus: String,
    p: ParametrosMutacion,
    universo: List[Double],
    calibracion: List[Double],
    grafia: Map[Double, String]
  ): MatrizVolcada =
    volcarConModelo(
      origen = origen,
      id = id,
      locus = locus,
      datos = ModeloMutacionGenis.datos(p, calibracion),
      universo = universo,
      calibracion = calibracion,
      grafia = grafia)
}

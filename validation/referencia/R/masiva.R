## ---------------------------------------------------------------------------
## lrval :: busqueda masiva (muchos POI contra muchas familias MP)
##
## Bajo H1 el POI ES el desaparecido y bajo H2 no esta emparentado, de modo que
## los genotipos de la familia aparecen en el numerador y en el denominador y
## se cancelan:
##
##   LR = P(E_fam, G_poi | H1) / P(E_fam, G_poi | H2)
##      = P(G_poi | E_fam, poi = MP) / P(G_poi)
##      = P(G_MP = g | E_fam) / P(g)
##
## Es decir: alcanza con calcular UNA vez, por familia y locus, la
## distribucion condicional del genotipo del desaparecido dados sus parientes,
## y despues cada POI es una consulta a esa tabla. Reduce un millon de
## verosimilitudes de pedigri a unos pocos miles, y es exacto: no es una
## aproximacion. `masiva_engines.R` arma esa tabla para los dos motores
## (`family_lr_table()`) y expone tambien la via larga par por par
## (`pair_lr()`); `mass_search()` de aqui abajo es el que la recorre, y
## `check_mass_search()` contrasta la tabla contra la via larga del mismo
## motor sobre una muestra aleatoria.
##
## Alcance: se cubren configuraciones de parentesco cercano (progenitores,
## abuelos, tios, hermanos y medios hermanos). Primos y grados mas lejanos
## quedan fuera a proposito: con un panel de 20-25 STR autosomicos no aportan
## poder de discriminacion util.
## ---------------------------------------------------------------------------

## ===========================================================================
## Biblioteca de configuraciones de pedigri
## ===========================================================================
##
## Cada constructor devuelve una lista con
##   ped    pedigri completo, con el desaparecido rotulado "MP"
##   typed  parientes que aportan perfil
##   grado  grado de parentesco nominal mas cercano (para agrupar resultados)

.base_padres <- function() {
  ## PAD y MAD -> MP
  pedtools::nuclearPed(fa = "PAD", mo = "MAD", children = "MP")
}

.base_abuelos <- function() {
  ## los 4 abuelos -> PAD y MAD -> MP.  Se construye de forma explicita
  ## porque addSon() exige que al menos un progenitor ya exista.
  pedtools::ped(
    id  = c("ABP1","ABP2","ABM1","ABM2","PAD","MAD","MP"),
    fid = c("0","0","0","0","ABP1","ABM1","PAD"),
    mid = c("0","0","0","0","ABP2","ABM2","MAD"),
    sex = c(1, 2, 1, 2, 1, 2, 1))
}

.base_tios <- function(n_pat = 2, n_mat = 2) {
  ## tios paternos y maternos: hermanos completos de PAD y de MAD
  tp <- if (n_pat > 0) paste0("TP", seq_len(n_pat)) else character(0)
  tm <- if (n_mat > 0) paste0("TM", seq_len(n_mat)) else character(0)
  id  <- c("ABP1","ABP2","ABM1","ABM2","PAD", tp, "MAD", tm, "MP")
  fid <- c("0","0","0","0", rep("ABP1", 1 + length(tp)),
           rep("ABM1", 1 + length(tm)), "PAD")
  mid <- c("0","0","0","0", rep("ABP2", 1 + length(tp)),
           rep("ABM2", 1 + length(tm)), "MAD")
  sex <- c(1, 2, 1, 2, 1, rep(1, length(tp)), 2, rep(2, length(tm)), 1)
  pedtools::ped(id = id, fid = fid, mid = mid, sex = sex)
}

.base_hermanos <- function(n) {
  pedtools::nuclearPed(fa = "PAD", mo = "MAD",
                       children = c("MP", paste0("HNO", seq_len(n))))
}

.base_medios <- function(n, lado = c("paterno", "materno")) {
  lado <- match.arg(lado)
  ## MP y los medios hermanos comparten un solo progenitor
  mh    <- paste0("MH", seq_len(n))
  otros <- paste0("OTR", seq_len(n))
  if (lado == "paterno") {
    id  <- c("PAD", "MAD", otros, "MP", mh)
    fid <- c("0", "0", rep("0", n), "PAD", rep("PAD", n))
    mid <- c("0", "0", rep("0", n), "MAD", otros)
    sex <- c(1, 2, rep(2, n), 1, rep(1, n))
  } else {
    id  <- c("PAD", "MAD", otros, "MP", mh)
    fid <- c("0", "0", rep("0", n), "PAD", otros)
    mid <- c("0", "0", rep("0", n), "MAD", rep("MAD", n))
    sex <- c(1, 2, rep(1, n), 1, rep(1, n))
  }
  pedtools::ped(id = id, fid = fid, mid = mid, sex = sex)
}

#' Configuraciones de pedigri de la busqueda masiva
#'
#' Cada elemento es una lista con `ped` (pedigri, con el desaparecido rotulado
#' "MP"), `typed` (parientes que aportan perfil) y `grado` (agrupacion).
#' @export
PEDIGREE_TYPES <- list(

  ## --- progenitores --------------------------------------------------------
  padres = list(
    etiqueta = "Ambos progenitores",
    ped = .base_padres(), typed = c("PAD", "MAD"), grado = "progenitores"),

  padre = list(
    etiqueta = "Solo el padre",
    ped = .base_padres(), typed = "PAD", grado = "progenitores"),

  madre = list(
    etiqueta = "Solo la madre",
    ped = .base_padres(), typed = "MAD", grado = "progenitores"),

  padre_hermano = list(
    etiqueta = "Un progenitor y un hermano",
    ped = .base_hermanos(1), typed = c("PAD", "HNO1"), grado = "mixto"),

  madre_hermano = list(
    etiqueta = "La madre y un hermano",
    ped = .base_hermanos(1), typed = c("MAD", "HNO1"), grado = "mixto"),

  padres_hermano = list(
    etiqueta = "Ambos progenitores y un hermano",
    ped = .base_hermanos(1), typed = c("PAD", "MAD", "HNO1"), grado = "mixto"),

  ## --- abuelos -------------------------------------------------------------
  abuelos_4 = list(
    etiqueta = "Los cuatro abuelos",
    ped = .base_abuelos(), typed = c("ABP1", "ABP2", "ABM1", "ABM2"),
    grado = "abuelos"),

  abuelos_3 = list(
    etiqueta = "Tres abuelos (pareja paterna y uno materno)",
    ped = .base_abuelos(), typed = c("ABP1", "ABP2", "ABM1"), grado = "abuelos"),

  abuelos_2_mismo = list(
    etiqueta = "Dos abuelos del mismo lado (pareja paterna)",
    ped = .base_abuelos(), typed = c("ABP1", "ABP2"), grado = "abuelos"),

  abuelos_2_cruzados = list(
    etiqueta = "Dos abuelos, uno de cada lado",
    ped = .base_abuelos(), typed = c("ABP1", "ABM2"), grado = "abuelos"),

  abuelo_1 = list(
    etiqueta = "Un solo abuelo",
    ped = .base_abuelos(), typed = "ABP1", grado = "abuelos"),

  ## --- tios ----------------------------------------------------------------
  tios_2_cada_lado = list(
    etiqueta = "Dos tios de cada lado (cuatro en total)",
    ped = .base_tios(2, 2), typed = c("TP1", "TP2", "TM1", "TM2"), grado = "tios"),

  tios_2_paternos = list(
    etiqueta = "Dos tios paternos",
    ped = .base_tios(2, 0), typed = c("TP1", "TP2"), grado = "tios"),

  tios_2_maternos = list(
    etiqueta = "Dos tios maternos",
    ped = .base_tios(0, 2), typed = c("TM1", "TM2"), grado = "tios"),

  tio_1 = list(
    etiqueta = "Un solo tio",
    ped = .base_tios(1, 0), typed = "TP1", grado = "tios"),

  ## --- hermanos ------------------------------------------------------------
  hermano_1 = list(
    etiqueta = "Un hermano completo",
    ped = .base_hermanos(1), typed = "HNO1", grado = "hermanos"),

  hermanos_2 = list(
    etiqueta = "Dos hermanos completos",
    ped = .base_hermanos(2), typed = c("HNO1", "HNO2"), grado = "hermanos"),

  hermanos_3 = list(
    etiqueta = "Tres hermanos completos",
    ped = .base_hermanos(3), typed = c("HNO1", "HNO2", "HNO3"), grado = "hermanos"),

  ## --- medios hermanos -----------------------------------------------------
  medio_hermano_1 = list(
    etiqueta = "Un medio hermano",
    ped = .base_medios(1), typed = "MH1", grado = "medios_hermanos"),

  medios_hermanos_2 = list(
    etiqueta = "Dos medios hermanos del mismo lado",
    ped = .base_medios(2), typed = c("MH1", "MH2"), grado = "medios_hermanos")
)

#' Resumen de las configuraciones disponibles
#' @export
pedigree_types_table <- function(types = PEDIGREE_TYPES) {
  do.call(rbind, lapply(names(types), function(nm) {
    t <- types[[nm]]
    data.frame(tipo = nm, etiqueta = t$etiqueta, grado = t$grado,
               n_tipificados = length(t$typed),
               tipificados = paste(t$typed, collapse = " "),
               n_individuos = pedtools::pedsize(t$ped),
               stringsAsFactors = FALSE)
  }))
}

## ===========================================================================
## Simulacion del escenario
## ===========================================================================

#' Parametrizacion de la condicion con mutacion del escenario
#'
#' tasa: una por sexo del progenitor (M, F).
#' tasa2 = 0: el modelo a pasos (tipo 2) no usa tasa2. Con rate2 = 0, pedmut
#'   reproduce celda por celda MutationService.mutationNormalizationSum.
#' pasos = 22: el tope de saltos es un numero por modelo, no por locus. La
#'   escalera mas ancha del panel es PENTAE, con amplitud 21, asi que 22 no
#'   trunca en ningun locus.
#' @export
MUTACION_MASIVA <- list(
  modelo = "stepwise",
  tasa   = c(M = 0.001584, F = 0.000463),
  tasa2  = 0,
  rango  = 0.5,
  pasos  = 22)

#' Parametros del escenario de busqueda masiva
#'
#' Unica fuente. Los consume `scripts/02_busqueda_masiva.R` para correr el
#' escenario y `escenario_masiva()` para RECONSTRUIRLO desde las semillas al
#' comparar. Si estuvieran duplicados, la reconstruccion podria dejar de
#' reproducir la corrida sin que nada lo dijera.
#' @export
ESCENARIO_MASIVA <- list(
  n_poi          = 5000,
  n_fam_pleno    = 200,
  frac_ver       = 0.30,   # fraccion de familias con su desaparecido en la nomina
  semilla_fam    = 2026,
  semilla_poi    = 7,
  umbrales       = c(3, 4, 5, 6, 8),
  por_tipo_tios  = 5,      # los cuatro tipos que Stepwise vuelve prohibitivos
  por_tipo_otros = 10)

#' Asignacion estratificada de familias por tipo de pedigri
#'
#' Se deriva de `PEDIGREE_TYPES` por `grado` y no escribiendo los tipos a mano:
#' si algun dia se agrega una configuracion con tios, la asignacion la cubre
#' sola. En una corrida corta se reduce en la misma proporcion, con piso de una
#' familia por tipo.
#'
#' @param n_fam escala pedida; por omision la plena
#' @param types catalogo de configuraciones
#' @return vector entero con nombres de tipo, apto para `simulate_mp_families()`
#' @export
asignacion_masiva <- function(n_fam = ESCENARIO_MASIVA$n_fam_pleno,
                              types = PEDIGREE_TYPES) {
  es_tios <- vapply(types, function(t) identical(t$grado, "tios"), TRUE)
  plena <- ifelse(es_tios, ESCENARIO_MASIVA$por_tipo_tios,
                  ESCENARIO_MASIVA$por_tipo_otros)
  pmax(round(plena * n_fam / ESCENARIO_MASIVA$n_fam_pleno), 1)
}

#' Construye las matrices de mutacion de un panel
#'
#' Unica fuente de la matriz: de aca salen la que se usa para simular las
#' familias, la que consumen los dos motores (adjunta al marcador) y la que
#' verifica el paquete exportado contra su declaracion.
#'
#' @param freqs base de frecuencias (lista locus -> frecuencias)
#' @param mut parametrizacion, con la forma de MUTACION_MASIVA, o NULL
#' @return lista nombrada locus -> mutationModel de pedmut, o NULL si mut es NULL
#' @export
build_mutmods <- function(freqs, mut) {
  if (is.null(mut)) return(NULL)
  stopifnot(all(c("modelo", "tasa", "tasa2", "rango", "pasos") %in% names(mut)),
            all(c("M", "F") %in% names(mut$tasa)))
  stats::setNames(lapply(names(freqs), function(nm) {
    args <- list(mut$modelo, alleles = names(freqs[[nm]]), afreq = freqs[[nm]],
                 rate = list(male   = unname(mut$tasa[["M"]]),
                             female = unname(mut$tasa[["F"]])))
    ## rate2 y range solo los admite el modelo a pasos; Equal aborta si se los
    ## pasa, y el escenario tiene que poder volver a Equal sin tocar esta rama.
    if (mut$modelo == "stepwise")
      args <- c(args, list(rate2 = list(male = mut$tasa2, female = mut$tasa2),
                           range = mut$rango))
    do.call(pedmut::mutationModel, args)
  }), names(freqs))
}

#' Valida un vector de asignacion de familias por tipo
#'
#' Lo que se defiende es el nombre mal escrito: aplicado sin control produciria
#' cero familias de ese tipo en silencio, y la corrida quedaria sin el tipo que
#' se queria medir. Las cantidades tienen que ser enteros estrictamente
#' positivos: un tipo que no se quiere medir no se lista.
#'
#' @param asignacion vector nombrado tipo -> cantidad de familias
#' @param types configuraciones disponibles
#' @keywords internal
.check_asignacion <- function(asignacion, types) {
  if (!is.numeric(asignacion) || !length(asignacion))
    stop("asignacion: se espera un vector numerico nombrado tipo -> familias")
  nm <- names(asignacion)
  if (is.null(nm) || anyNA(nm) || !all(nzchar(nm)))
    stop("asignacion: cada cantidad tiene que nombrar su tipo de pedigri")
  if (anyDuplicated(nm))
    stop("asignacion: tipo repetido: ",
         paste(unique(nm[duplicated(nm)]), collapse = ", "))
  falta <- setdiff(nm, names(types))
  if (length(falta))
    stop("asignacion: tipo inexistente: ", paste(falta, collapse = ", "))
  ## el orden importa: con un NA presente, la comparacion contra 1 devolveria
  ## NA y el `if` abortaria con un mensaje que no dice cual es el problema
  if (!all(is.finite(asignacion)) || any(asignacion < 1) ||
      any(asignacion != round(asignacion)))
    stop("asignacion: las cantidades tienen que ser enteros positivos")
  invisible(asignacion)
}

#' Simula un conjunto de familias de personas desaparecidas
#'
#' Cada familia recibe una configuracion de pedigri, se le simulan los perfiles
#' de los parientes tipificados y se guarda ademas el perfil del propio
#' desaparecido, que es el que se plantara luego entre los POI para tener
#' verdad de referencia.
#'
#' El tipo de cada familia se sortea uniformemente entre las de `types`, salvo
#' que se declare una `asignacion`. Declararla existe porque con el modelo de
#' mutacion del escenario el costo deja de repartirse parejo: se
#' concentra en los tipos con tios, donde el desaparecido esta separado de todo
#' pariente tipificado por dos fundadores no tipificados (ver
#' `scripts/03_costo_mutacion.R`). Con sorteo uniforme esos tipos se llevan casi
#' todo el presupuesto de la corrida; con una asignacion se les da menos
#' familias sin dejarlos fuera. Las tasas de `search_summary()` promedian por
#' tipo justamente para que las dos escalas estimen la misma cantidad.
#'
#' @param n numero de familias. Con `asignacion` declarada se deriva de su suma
#'   si no se pasa, y se valida contra ella si se pasa: declarar una escala en
#'   la asignacion y otra en `n` es un error, no una preferencia.
#' @param freqs base de frecuencias (lista locus -> frecuencias)
#' @param types configuraciones disponibles
#' @param seed semilla
#' @param mut parametrizacion del modelo de mutacion, con la forma de
#'   MUTACION_MASIVA, o NULL para el escenario sin mutacion
#' @param asignacion vector nombrado tipo -> cantidad de familias, o NULL para
#'   sortear el tipo de cada familia uniformemente
#' @return lista de familias
#' @export
simulate_mp_families <- function(n = 200, freqs, types = PEDIGREE_TYPES, seed = 2026,
                                 mut = NULL, asignacion = NULL) {
  if (!is.null(asignacion)) {
    .check_asignacion(asignacion, types)
    total <- sum(asignacion)
    if (missing(n)) n <- total
    else if (n != total)
      stop(sprintf("n = %s no coincide con la suma de la asignacion (%s)", n, total))
  }
  set.seed(seed)
  ## Sin modelo de mutacion, un unico locus discordante produce LR = 0 y
  ## excluye al verdadero desaparecido. En una busqueda masiva real eso no es
  ## aceptable, de modo que el escenario se corre tambien con mutacion activa.
  mutmods <- build_mutmods(freqs, mut)
  ## plantillas: se arma el pedigri con marcadores vacios una sola vez por tipo
  plantillas <- lapply(types, function(t) {
    p <- t$ped
    for (nm in names(freqs))
      p <- pedtools::addMarker(p, afreq = freqs[[nm]], name = nm,
                               mutmod = if (is.null(mutmods)) NULL else mutmods[[nm]])
    p
  })
  ## El vector de asignacion se mezcla antes de construir las familias.
  ## mass_search() reparte dinamico, y el despacho dinamico es
  ## VORAZ POR ORDEN DE INDICE: toma siempre la primera tarea sin asignar. Si
  ## las familias caras quedaran juntas al FINAL de la cola --y sin mezclar el
  ## orden es el de `asignacion`, un dato de entrada sobre el que nadie mira
  ## esa propiedad-- las ultimas en despacharse serian todas de >= 6540 s y la
  ## corrida terminaria con una cola larga de nucleos ociosos. Repartidas a lo
  ## largo de la cola, esa cola se acorta. Lo optimo seria despachar las caras
  ## primero, que es otra discusion; la mezcla usa la misma semilla, asi que la
  ## corrida sigue siendo reproducible.
  asignado <- if (is.null(asignacion)) sample(names(types), n, replace = TRUE)
              else { v <- rep(names(asignacion), asignacion); v[sample(length(v))] }
  lapply(seq_len(n), function(i) {
    tp <- asignado[i]
    t  <- types[[tp]]
    sim <- forrel::profileSim(plantillas[[tp]], N = 1, ids = c(t$typed, "MP"),
                              verbose = FALSE)
    if (is.list(sim) && !pedtools::is.ped(sim)) sim <- sim[[1]]
    ## perfil del desaparecido: verdad de referencia
    d <- as.data.frame(sim)
    mp <- unlist(d[d$id == "MP", setdiff(names(d), c("id","fid","mid","sex"))])
    list(id = sprintf("FAM%03d", i), tipo = tp, grado = t$grado,
         etiqueta = t$etiqueta, typed = t$typed,
         ## el pedigri que ve el sistema: el desaparecido SIN perfil
         ped = pedtools::setAlleles(sim, ids = "MP", alleles = 0),
         perfil_mp = mp)
  })
}

#' Construye la nomina de POI
#'
#' Una fraccion de los POI son los desaparecidos reales de algunas familias
#' (verdad de referencia); el resto son individuos de la poblacion general,
#' sin parentesco con ninguna familia.
#'
#' @param n numero total de POI
#' @param families salida de `simulate_mp_families()`
#' @param n_verdaderos cuantas familias tienen a su desaparecido en la nomina
#' @export
build_poi_pool <- function(n = 5000, families, freqs, n_verdaderos = 60, seed = 7) {
  set.seed(seed)
  loci <- names(freqs)
  n_verdaderos <- min(n_verdaderos, length(families), n)
  cuales <- sample(seq_along(families), n_verdaderos)

  ## POI no emparentados: extraccion directa bajo equilibrio de Hardy-Weinberg
  n_ruido <- n - n_verdaderos
  g <- vapply(loci, function(nm) {
    f <- freqs[[nm]]; a <- names(f)
    a1 <- sample(a, n_ruido, replace = TRUE, prob = f)
    a2 <- sample(a, n_ruido, replace = TRUE, prob = f)
    paste(a1, a2, sep = "/")
  }, character(n_ruido))
  g <- matrix(g, nrow = n_ruido, dimnames = list(NULL, loci))

  verdad <- do.call(rbind, lapply(cuales, function(k) families[[k]]$perfil_mp[loci]))
  M <- rbind(g, verdad)
  familia_real <- c(rep(NA_character_, n_ruido),
                    vapply(families[cuales], `[[`, "", "id"))
  ord <- sample(nrow(M))
  M <- M[ord, , drop = FALSE]
  familia_real <- familia_real[ord]
  rownames(M) <- sprintf("POI%05d", seq_len(nrow(M)))
  list(geno = M, familia_real = setNames(familia_real, rownames(M)),
       loci = loci)
}

## ===========================================================================
## Motor de busqueda
## ===========================================================================

#' Clave canonica de un genotipo, independiente del orden de los alelos
#' @keywords internal
.geno_key <- function(g, alleles) {
  pz <- strsplit(g, "/", fixed = TRUE)
  i1 <- match(vapply(pz, `[`, "", 1L), alleles)
  i2 <- match(vapply(pz, `[`, "", 2L), alleles)
  ifelse(is.na(i1) | is.na(i2), NA_character_,
         paste(pmin(i1, i2), pmax(i1, i2), sep = "-"))
}

#' Busqueda masiva: todos los POI contra todas las familias
#'
#' @param families salida de `simulate_mp_families()`
#' @param pool salida de `build_poi_pool()`
#' @param freqs base de frecuencias
#' @param engine motor, ver `MASIVA_ENGINES`
#' @param cores nucleos a usar
#' @param progreso emite una linea por familia terminada, a stderr
#' @return matriz de log10(LR), familias en filas y POI en columnas
#' @export
mass_search <- function(families, pool, freqs, engine = "familias",
                        cores = max(1, parallel::detectCores() - 2),
                        progreso = TRUE) {
  engine <- match.arg(engine, MASIVA_ENGINES)
  loci <- pool$loci
  claves <- lapply(loci, function(nm) .geno_key(pool$geno[, nm], names(freqs[[nm]])))
  names(claves) <- loci
  ## Los objetos de locus de Familias dependen del panel y del modelo de
  ## mutacion, no de la familia: se arman una vez para todo el escenario.
  genos    <- if (engine == "familias") poi_genotypes(pool, freqs) else NULL
  loci_obj <- if (engine == "familias")
    case_to_familias_loci(list(H1 = families[[1]]$ped)) else NULL

  ## Una linea por familia terminada, a stderr. Con el modelo de mutacion este
  ## bucle dura HORAS y sin esto no emite nada hasta que termina entero: no
  ## habria forma de distinguir "avanza despacio" de "se colgo", que es
  ## justamente lo que hay que poder mirar en una corrida de una noche. Tres
  ## decisiones que no son de estilo:
  ##  - va a stderr y no a stdout, para no intercalarse con las tablas que el
  ##    script imprime ni ensuciar una captura de esas tablas;
  ##  - cada linea sale de UN solo cat(): los hijos de mclapply escriben sobre
  ##    el mismo descriptor y una linea partida en varios cat() se entrelaza
  ##    con la de otro hijo;
  ##  - no lleva contador de avance. Los hijos no comparten memoria, asi que un
  ##    contador de R contaria por proceso y daria otro numero; "cuantas van" se cuenta
  ##    sobre el log, que trae exactamente una linea por familia.
  t_ini <- Sys.time()
  avisar <- function(fam, t0) {
    if (!progreso) return(invisible(NULL))
    cat(sprintf("[masiva %-8s] %-7s %-14s %8.1f s  %s\n", engine, fam$id, fam$tipo,
                as.numeric(difftime(Sys.time(), t0, units = "secs")),
                format(Sys.time(), "%H:%M:%S")),
        file = stderr())
  }
  if (progreso)
    cat(sprintf("[masiva %-8s] arranca: %d familias x %d poi sobre %d nucleo(s)\n",
                engine, length(families), nrow(pool$geno), cores), file = stderr())

  fila <- function(fam) {
    t0 <- Sys.time()
    tab <- if (engine == "familias") .lr_table_familias(fam, freqs, genos, loci_obj)
           else .lr_table_pedsuite(fam, freqs)
    acc <- numeric(nrow(pool$geno))
    for (j in seq_along(loci)) {
      lr <- tab[[j]][claves[[j]]]
      ## un genotipo ausente de la tabla tiene probabilidad condicional cero
      ## (exclusion en ese locus); un genotipo sin dato no aporta informacion
      lr[is.na(lr) & !is.na(claves[[j]])] <- 0
      lr[is.na(claves[[j]])] <- 1
      acc <- acc + log10(lr)
    }
    avisar(fam, t0)
    acc
  }

  ## mc.preschedule = FALSE: reparto DINAMICO, y no es una preferencia de
  ## estilo. Con el reparto por omision (TRUE) mclapply calcula
  ## `seq(i, n, by = cores)` ANTES de forkear y le entrega a cada uno de los
  ## `cores` hijos su sublista completa; un hijo que se queda sin trabajo
  ## termina y muere sin que le pasen nada de los demas. Con 20 familias de
  ## tios de >= 6540 s cada una repartidas al azar entre 6 nucleos, el sorteo
  ## las deja 5/4/3/3/3/2 y el reloj lo fija el peor nucleo, no el promedio:
  ## medido, ~11 h contra las 6,4 h que da la cuenta balanceada, con cinco
  ## nucleos ociosos en el tramo final.
  ## Con FALSE mclapply mantiene una cola: arranca `cores` tareas, bloquea en
  ## selectChildren() hasta que alguna termina y forkea la siguiente sin
  ## asignar en ese lugar. Es despacho voraz por orden de indice, no "las
  ## pesadas primero", asi que no da el optimo: la cota es
  ## total/m + (1 - 1/m) * tarea_mas_larga, unas 7,8 h en el peor caso aca.
  ##
  ## Los VALORES no cambian. Lo unico que `mc.preschedule` altera del calculo
  ## es como se reparten los streams de RNG (una vez por nucleo con TRUE, una
  ## por tarea con FALSE), y `fila` no sortea: es consulta a la tabla y suma de
  ## logaritmos. El orden tampoco cambia -- el despachador deposita por indice
  ## original. Verificado ademas contra el calculo serial, que no depende de
  ## ningun reparto.
  res <- if (cores > 1 && .Platform$OS.type == "unix")
    parallel::mclapply(families, fila, mc.cores = cores, mc.preschedule = FALSE)
  else lapply(families, fila)

  if (progreso)
    cat(sprintf("[masiva %-8s] listo: %d familias en %.1f min\n", engine,
                length(families),
                as.numeric(difftime(Sys.time(), t_ini, units = "mins"))),
        file = stderr())

  M <- do.call(rbind, res)
  dimnames(M) <- list(vapply(families, `[[`, "", "id"), rownames(pool$geno))
  M
}

## ===========================================================================
## Verificacion de la via de calculo
## ===========================================================================

#' Contrasta la tabla de genotipos contra la via larga del mismo motor
#'
#' La tabla es exacta, pero conviene demostrarlo en vez de afirmarlo: se toma
#' una muestra aleatoria de pares (familia, POI) y se recalcula el LR armando
#' el pedigri con el POI en el lugar del desaparecido. Cada motor se verifica
#' contra su propia via larga: para pedsuite es forrel::kinshipLR, para
#' Familias es FamiliasPosterior sobre el pedigri conjunto.
#'
#' @param n numero de pares a verificar
#' @param engine motor, ver `MASIVA_ENGINES`
#' @param progreso emite una linea por par verificado, a stderr
#' @param escenario etiqueta de la condicion (p.ej. "sin mutacion"), para
#'   distinguir filas cuando se junten los resultados de varias corridas; no
#'   participa del calculo
#' @export
check_mass_search <- function(M, families, pool, freqs, engine = "familias",
                              n = 40, seed = 11, tol = LRVAL_TOL,
                              progreso = TRUE, escenario = NA_character_) {
  engine <- match.arg(engine, MASIVA_ENGINES)
  set.seed(seed)
  idx  <- cbind(sample(nrow(M), n, replace = TRUE), sample(ncol(M), n, replace = TRUE))
  fams <- setNames(families, vapply(families, `[[`, "", "id"))
  ## Este bucle es SERIAL y cada par es una verosimilitud de pedigri completa:
  ## con mutacion, un par de un tipo con tios cuesta lo que una iteracion del
  ## barrido, de modo que los 40 pares pueden ser varios minutos sin emitir
  ## nada. Aca el contador SI vale, que para eso no hay hijos.
  do.call(rbind, lapply(seq_len(n), function(k) {
    fam <- fams[[rownames(M)[idx[k, 1]]]]
    poi <- colnames(M)[idx[k, 2]]
    t0 <- Sys.time()
    lr_largo <- pair_lr(fam, pool$geno[poi, ], freqs, engine)
    lr_tabla <- 10^M[idx[k, 1], idx[k, 2]]
    cmp <- compare_lr(lr_tabla, lr_largo, tol)
    if (progreso)
      cat(sprintf("[via larga %-8s] %2d/%2d %-7s %-14s %8.1f s  %s\n", engine, k, n,
                  fam$id, fam$tipo,
                  as.numeric(difftime(Sys.time(), t0, units = "secs")),
                  cmp$veredicto),
          file = stderr())
    data.frame(escenario = escenario, familia = fam$id, tipo = fam$tipo,
               poi = poi, motor = engine,
               LR_tabla = lr_tabla, LR_via_larga = lr_largo,
               disc_rel = cmp$rel, veredicto = cmp$veredicto,
               stringsAsFactors = FALSE)
  }))
}

## ===========================================================================
## Metricas de la busqueda
## ===========================================================================

#' Resultado por familia: el verdadero desaparecido frente al ruido
#'
#' @param M matriz de log10(LR) de `mass_search()`
#' @export
family_results <- function(M, families, pool) {
  do.call(rbind, lapply(seq_along(families), function(i) {
    fam <- families[[i]]
    j <- which(pool$familia_real == fam$id)
    fila <- M[i, ]
    tiene <- length(j) == 1
    no_rel <- if (tiene) fila[-j] else fila
    data.frame(
      familia = fam$id, tipo = fam$tipo, grado = fam$grado,
      etiqueta = fam$etiqueta, n_tipificados = length(fam$typed),
      tiene_verdadero = tiene,
      log10LR_verdadero = if (tiene) unname(fila[j]) else NA_real_,
      rango_verdadero = if (tiene) unname(rank(-fila, ties.method = "min")[j]) else NA_integer_,
      max_no_relacionado = max(no_rel),
      p95_no_relacionado = unname(stats::quantile(no_rel, 0.95)),
      stringsAsFactors = FALSE)
  }))
}

#' Metricas de deteccion por tipo de pedigri
#'
#' Para cada umbral de log10(LR) informa:
#'   deteccion  fraccion de desaparecidos presentes que lo superan
#'   fp_por_fam numero esperado de falsos positivos por familia sobre la
#'              nomina completa de POI
#'
#' @export
search_metrics <- function(M, families, pool, umbrales = c(3, 4, 5, 6, 8)) {
  fr <- family_results(M, families, pool)
  tipos <- unique(fr$tipo)
  do.call(rbind, lapply(tipos, function(tp) {
    idx <- which(fr$tipo == tp)
    ver <- fr$log10LR_verdadero[idx]; ver <- ver[!is.na(ver)]
    ## LR de todos los pares NO emparentados de las familias de este tipo
    no_rel <- unlist(lapply(idx, function(i) {
      j <- which(pool$familia_real == fr$familia[i])
      if (length(j)) M[i, -j] else M[i, ]
    }))
    d <- data.frame(
      tipo = tp, etiqueta = fr$etiqueta[idx][1], grado = fr$grado[idx][1],
      n_familias = length(idx), n_con_verdadero = length(ver),
      mediana_log10LR_verdadero = if (length(ver)) stats::median(ver) else NA_real_,
      min_log10LR_verdadero = if (length(ver)) min(ver) else NA_real_,
      p999_no_relacionado = unname(stats::quantile(no_rel, 0.999)),
      stringsAsFactors = FALSE)
    for (u in umbrales) {
      d[[paste0("deteccion_", u)]] <- if (length(ver)) mean(ver >= u) else NA_real_
      d[[paste0("fp_por_familia_", u)]] <- mean(no_rel >= u) * ncol(M)
    }
    d
  }))
}

#' Resumen global de la busqueda
#'
#' Los conteos (`detectados`, `de_un_total_de`, `falsos_positivos`,
#' `comparaciones_no_relacionadas`) son totales crudos de la corrida. Las dos
#' tasas, en cambio, se calculan por tipo de pedigri y despues se promedian los
#' tipos **sin ponderar**: lo que estiman es la deteccion y la tasa de falsos
#' positivos de un tipo promedio del catalogo, y no las de la corrida.
#'
#' No es un promedio simple a proposito. Acumular todas las familias juntas
#' estima esa misma cantidad solo si la asignacion de tipos es pareja. Con la
#' asignacion estratificada de la condicion con mutacion --menos familias en
#' los tipos con tios, que son los caros y a la vez los mas dificiles-- el
#' acumulado sobrepondera los tipos baratos y la deteccion global sale
#' optimista. El promedio por tipo estima lo mismo bajo cualquier asignacion, y
#' es lo unico que deja comparar las dos condiciones, que no corren a la
#' misma escala.
#'
#' Las tasas por tipo son las de `search_metrics()`, de modo que el resumen
#' global y la tabla por tipo no pueden discrepar.
#'
#' @param umbrales umbrales de log10(LR)
#' @export
search_summary <- function(M, families, pool, umbrales = c(3, 4, 5, 6, 8)) {
  fr  <- family_results(M, families, pool)
  met <- search_metrics(M, families, pool, umbrales)
  no_rel <- unlist(lapply(seq_along(families), function(i) {
    j <- which(pool$familia_real == fr$familia[i])
    if (length(j)) M[i, -j] else M[i, ]
  }))
  ver <- fr$log10LR_verdadero[!is.na(fr$log10LR_verdadero)]
  data.frame(
    umbral_log10LR = umbrales,
    detectados = vapply(umbrales, function(u) sum(ver >= u), 0L),
    de_un_total_de = length(ver),
    ## un tipo sin ningun desaparecido en la nomina no tiene deteccion que
    ## aportar y queda fuera del promedio, no entra como cero
    deteccion = vapply(umbrales, function(u)
      mean(met[[paste0("deteccion_", u)]], na.rm = TRUE), 0),
    falsos_positivos = vapply(umbrales, function(u) sum(no_rel >= u), 0L),
    comparaciones_no_relacionadas = length(no_rel),
    ## search_metrics() informa los falsos positivos esperados por familia
    ## sobre la nomina completa; la tasa por comparacion es esa cifra dividida
    ## por el tamano de la nomina
    tasa_falsos_positivos = vapply(umbrales, function(u)
      mean(met[[paste0("fp_por_familia_", u)]]) / ncol(M), 0),
    stringsAsFactors = FALSE)
}

## ---------------------------------------------------------------------------
## lrval :: comparacion de la busqueda masiva (bloque 2)
##
## Junta la entrega del sistema evaluado con la matriz de referencia y emite
## veredicto CELDA A CELDA, con el mismo criterio que juzga el catalogo.
## ---------------------------------------------------------------------------

#' Reconstruye el escenario de una condicion y lo verifica contra el paquete
#'
#' La matriz de referencia se puede recalcular porque el escenario es
#' determinista. Pero "determinista" es una propiedad que hay que COMPROBAR, no
#' suponer: si la reconstruccion no reprodujera el escenario exacto, todo lo que
#' sigue serian numeros sin significado que igual pareceria razonables.
#'
#' @param condicion "sin_mutacion" o "con_mutacion"
#' @param dir_paquete directorio del paquete; por omision el de la condicion
#' @param verificar si FALSE, salta la verificacion (solo para pruebas)
#' @return lista con condicion, fams, pool, freqs, mut, dir_paquete
#' @export
escenario_masiva <- function(condicion = c("sin_mutacion", "con_mutacion"),
                             dir_paquete = NULL, verificar = TRUE) {
  condicion <- match.arg(condicion)
  if (is.null(dir_paquete))
    dir_paquete <- file.path(LRVAL_DIRS$masiva, "paquete", condicion)
  if (!dir.exists(dir_paquete))
    stop("escenario_masiva: no existe el paquete ", dir_paquete)

  freqs <- freqs_mispitools("Argentina")
  mut   <- if (condicion == "con_mutacion") MUTACION_MASIVA else NULL
  fams  <- if (condicion == "con_mutacion")
    simulate_mp_families(freqs = freqs, seed = ESCENARIO_MASIVA$semilla_fam,
                         mut = mut, asignacion = asignacion_masiva())
  else
    simulate_mp_families(ESCENARIO_MASIVA$n_fam_pleno, freqs,
                         seed = ESCENARIO_MASIVA$semilla_fam, mut = mut)

  n_ver <- max(1, round(length(fams) * ESCENARIO_MASIVA$frac_ver))
  pool  <- build_poi_pool(ESCENARIO_MASIVA$n_poi, fams, freqs,
                          n_verdaderos = n_ver,
                          seed = ESCENARIO_MASIVA$semilla_poi)

  esc <- list(condicion = condicion, fams = fams, pool = pool, freqs = freqs,
              mut = mut, dir_paquete = dir_paquete)
  if (verificar) verificar_escenario(esc)
  esc
}

#' Verifica que un escenario reconstruido reproduzca el paquete en disco
#'
#' Dura a proposito: cualquier discrepancia detiene la corrida. Un escenario
#' que no reproduce el paquete no es "parecido", es otro escenario.
#'
#' @param esc salida de `escenario_masiva(verificar = FALSE)`
#' @return invisible(TRUE), o `stop()`
#' @export
verificar_escenario <- function(esc) {
  leer <- function(f) {
    ruta <- file.path(esc$dir_paquete, f)
    if (!file.exists(ruta)) stop("verificar_escenario: falta ", ruta)
    utils::read.csv(ruta, colClasses = "character")
  }
  cmp <- function(a, b, que) {
    if (!identical(a, b))
      stop(sprintf("verificar_escenario: la reconstruccion no reproduce %s de %s",
                   que, esc$dir_paquete))
  }
  ord <- function(d, k) d[do.call(order, unname(as.list(d[k]))), k, drop = FALSE]
  chr <- function(d) { for (k in seq_along(d)) d[[k]] <- as.character(d[[k]]); d }
  fila <- function(d) { rownames(d) <- NULL; d }

  ## 1. familia -> tipo
  obs <- data.frame(familia = vapply(esc$fams, `[[`, "", "id"),
                    tipo = vapply(esc$fams, `[[`, "", "tipo"),
                    stringsAsFactors = FALSE)
  cmp(fila(ord(chr(leer("familias.csv")), c("familia", "tipo"))),
      fila(ord(obs, c("familia", "tipo"))), "familias.csv")

  ## 2. la nomina de POI
  k <- c("poi", "locus", "alelo_1", "alelo_2")
  cmp(fila(ord(chr(leer("poi.csv")), k)),
      fila(ord(.geno_largo(esc$pool$geno, "poi", esc$pool$loci), k)), "poi.csv")

  ## 3. los perfiles de los parientes tipificados
  loci <- names(esc$freqs)
  per <- do.call(rbind, lapply(esc$fams, function(f) {
    dm <- ped_to_datamatrix(f$ped)
    do.call(rbind, lapply(seq_along(loci), function(j) data.frame(
      familia = f$id, individuo = rownames(dm), locus = loci[j],
      alelo_1 = dm[, 2 * j - 1], alelo_2 = dm[, 2 * j], stringsAsFactors = FALSE)))
  }))
  kp <- c("familia", "individuo", "locus", "alelo_1", "alelo_2")
  cmp(fila(ord(chr(leer("perfiles.csv")), kp)), fila(ord(per, kp)), "perfiles.csv")

  invisible(TRUE)
}

## ===========================================================================
## El esquema de intercambio
##
## La referencia emite su matriz en el MISMO esquema que la entrega del sistema
## evaluado --familia,poi,log10LR,estado,observaciones-- y no como matriz ancha.
## Asi los dos artefactos son de la misma forma, directamente diffeables, el
## join es trivial y un solo lector sirve para los dos lados.
## ===========================================================================

#' Escribe una matriz de log10(LR) en el esquema de la entrega
#'
#' @param M matriz de `mass_search()`, con familias en filas y POI en columnas
#' @param archivo destino; si termina en .gz se escribe comprimido
#' @return invisible(archivo)
#' @export
escribir_matriz_masiva <- function(M, archivo) {
  if (is.null(rownames(M)) || is.null(colnames(M)))
    stop("escribir_matriz_masiva: la matriz tiene que traer dimnames")
  d <- data.frame(
    familia = rep(rownames(M), times = ncol(M)),
    poi     = rep(colnames(M), each = nrow(M)),
    log10LR = as.numeric(M),
    estado  = "ok",
    observaciones = "",
    stringsAsFactors = FALSE)
  d <- d[order(d$familia, d$poi), , drop = FALSE]
  if (grepl("[.]gz$", archivo)) {
    con <- gzfile(archivo, "w"); on.exit(close(con))
    write_csv_exact(d, con, num = "log10LR")
  } else {
    write_csv_exact(d, archivo, num = "log10LR")
  }
  invisible(archivo)
}

#' Lee un archivo en el esquema de la entrega
#'
#' Sirve para los dos lados: la entrega del sistema evaluado y la matriz de
#' referencia que escribe `escribir_matriz_masiva()`.
#'
#' @param archivo ruta; .gz se detecta solo
#' @return data.frame(familia, poi, log10LR, estado, observaciones)
#' @export
leer_masiva <- function(archivo) {
  if (!file.exists(archivo)) stop("leer_masiva: no existe ", archivo)
  d <- utils::read.csv(archivo, colClasses = "character")
  falta <- setdiff(c("familia", "poi", "log10LR", "estado", "observaciones"),
                   names(d))
  if (length(falta))
    stop("leer_masiva: faltan columnas en ", archivo, ": ",
         paste(falta, collapse = ", "))
  tr <- function(x) trimws(as.character(x))
  d$familia <- tr(d$familia); d$poi <- tr(d$poi)

  ## Una fila con log10LR Y estado vacios no esta informada: mismo criterio
  ## que import_results() (R/genis.R) aplica a resultados.csv, para no leer
  ## una plantilla sin completar como "contestada con NA". Se descarta antes
  ## de validar cualquier otra cosa, incluida la clave duplicada.
  crudo <- tr(d$log10LR)
  est   <- tr(d$estado)
  informada <- nzchar(crudo) | nzchar(est)
  d <- d[informada, , drop = FALSE]
  crudo <- crudo[informada]
  est   <- est[informada]

  ## El centinela de exclusion viaja en tres escrituras. Se normalizan
  ## explicitamente en vez de confiar en el parser: que as.numeric() acepte
  ## "-Infinity" es una propiedad de la plataforma, no del formato de
  ## intercambio.
  norm <- ifelse(tolower(crudo) %in% c("-inf", "-infinity"), "-Inf", crudo)
  x <- suppressWarnings(as.numeric(norm))
  ## Un log10LR que no parsea no puede quedar como SIN_DATO silencioso: se
  ## confundiria con una celda que el sistema declaro no poder calcular.
  malo <- nzchar(crudo) & is.na(x)
  if (any(malo))
    stop("leer_masiva: log10LR no interpretable en ", archivo, ": ",
         paste(utils::head(sprintf("%s/%s ('%s')", d$familia[malo], d$poi[malo],
                                   crudo[malo]), 5), collapse = ", "))

  ## El formato de intercambio define UN SOLO centinela, -Inf, para la exclusion. Un
  ## log10LR +Inf no significa nada -si se aceptara en silencio quedaria como
  ## un valor valido, cuando en realidad es un dato que no deberia poder
  ## salir del sistema evaluado.
  positivo <- is.infinite(x) & x > 0
  if (any(positivo))
    stop("leer_masiva: log10LR infinito positivo invalido (el centinela es ",
         "-Inf, no +Inf) en ", archivo, ": ",
         paste(utils::head(sprintf("%s/%s ('%s')", d$familia[positivo], d$poi[positivo],
                                   crudo[positivo]), 5), collapse = ", "))

  d$log10LR <- x
  d$estado <- ifelse(nzchar(est), est, "ok")

  clave <- paste(d$familia, d$poi)
  if (anyDuplicated(clave))
    stop("leer_masiva: filas duplicadas (familia x poi) en ", archivo, ": ",
         paste(utils::head(unique(clave[duplicated(clave)]), 5), collapse = ", "))
  d
}

#' Reimporta la entrega del sistema evaluado a la matriz que consume el resto
#'
#' Espejo de `import_results()` del bloque 1, a la escala del bloque 2. Devuelve
#' una matriz con los MISMOS margenes y orden que `mass_search()`, de modo que
#' `family_results()`, `search_metrics()` y `search_summary()` la consuman sin
#' cambios.
#'
#' Dos reglas del formato de intercambio, implementadas aca:
#'  - la unidad de entrega es la familia completa (regla 4 del LEEME.txt del
#'    paquete). Una familia con menos de la nomina entera se descarta ENTERA y
#'    se informa por nombre; con
#'    una familia parcialmente informada la deteccion y los falsos positivos de
#'    su tipo quedan indefinidos, porque se miden contra el total de POI.
#'  - entrega parcial valida: una familia ausente da NA e informe de
#'    cobertura, no error. Es el escenario esperado de una validacion por etapas.
#'
#' @param archivo `resultados_genis_masiva.csv`
#' @param esc salida de `escenario_masiva()`
#' @return matriz de log10(LR) con `attr(, "cobertura")`
#' @export
import_masiva <- function(archivo, esc) {
  d <- leer_masiva(archivo)
  fam_ids <- vapply(esc$fams, `[[`, "", "id")
  poi_ids <- rownames(esc$pool$geno)

  desc <- setdiff(unique(d$familia), fam_ids)
  if (length(desc))
    stop("import_masiva: familias desconocidas en ", archivo, ": ",
         paste(utils::head(desc, 5), collapse = ", "))
  desc <- setdiff(unique(d$poi), poi_ids)
  if (length(desc))
    stop("import_masiva: POI desconocidos en ", archivo, ": ",
         paste(utils::head(desc, 5), collapse = ", "))

  n_por_fam   <- table(factor(d$familia, levels = fam_ids))
  informadas  <- names(n_por_fam)[n_por_fam > 0]
  incompletas <- names(n_por_fam)[n_por_fam > 0 & n_por_fam != length(poi_ids)]
  if (length(incompletas)) d <- d[!d$familia %in% incompletas, , drop = FALSE]

  M <- matrix(NA_real_, nrow = length(fam_ids), ncol = length(poi_ids),
              dimnames = list(fam_ids, poi_ids))
  if (nrow(d)) {
    val <- d$log10LR
    val[d$estado != "ok"] <- NA_real_
    M[cbind(match(d$familia, fam_ids), match(d$poi, poi_ids))] <- val
  }
  attr(M, "cobertura") <- list(
    familias        = length(fam_ids),
    informadas      = length(informadas),
    aceptadas       = length(setdiff(informadas, incompletas)),
    incompletas     = incompletas,
    no_informadas   = length(setdiff(fam_ids, informadas)),
    celdas_con_dato = sum(!is.na(M)),
    estados = sort(unique(d$estado[d$estado != "ok"])))
  M
}

## ===========================================================================
## Comparacion celda a celda
##
## Se llama a compare_lr() --la funcion que juzga el catalogo del bloque 1-- una
## vez por celda. No hay reimplementacion vectorizada del criterio: medido, la
## version escalar cuesta 2,4 s por millon de celdas, asi que no hay nada que
## optimizar, y el bloque 1 y el bloque 2 comparten LITERALMENTE la misma
## funcion en vez de dos criterios que se afirman equivalentes.
##
## Lo que si hay que evitar es el do.call(rbind, lapply(...)) que el modulo usa
## en todos lados: correcto con decenas o cientos de filas, fatal con 10^6.
## ===========================================================================

#' Piso del dominio donde `10^x` conserva significado
#' @keywords internal
.PISO_LOG10 <- -300

#' @keywords internal
.guardia_subdesborde <- function(M, lado, piso = .PISO_LOG10) {
  x <- as.numeric(M)
  malo <- is.finite(x) & x < piso
  if (any(malo))
    stop(sprintf(paste("comparar_celdas: %d celdas de la %s tienen log10LR",
                       "finito < %g. 10^x subdesborda a cero ahi, y el criterio",
                       "dejaria de distinguir un LR minusculo de una exclusion."),
                 sum(malo), lado, piso))
  invisible(NULL)
}

#' Compara dos matrices de log10(LR) celda por celda
#'
#' @param M_obs matriz del sistema evaluado (`import_masiva()`)
#' @param M_ref matriz de referencia (`mass_search()`), mismos margenes
#' @param tol tolerancias; por omision las del set
#' @return lista con `celdas` (solo las que NO son PASA), `rel_matriz` (la
#'   matriz completa de `rel`, con los mismos dimnames que `M_obs`/`M_ref`;
#'   `Inf` en un desacuerdo categorico, `NA` donde no hubo dato), `n` y
#'   `conteo`
#' @export
comparar_celdas <- function(M_obs, M_ref, tol = LRVAL_TOL) {
  if (!identical(dimnames(M_obs), dimnames(M_ref)))
    stop("comparar_celdas: las dos matrices tienen que tener los mismos margenes")
  .guardia_subdesborde(M_obs, "entrega")
  .guardia_subdesborde(M_ref, "referencia")

  log_obs <- as.numeric(M_obs)
  log_ref <- as.numeric(M_ref)
  obs <- 10^log_obs
  ref <- 10^log_ref
  n <- length(obs)
  ver <- character(n); rel <- numeric(n)
  for (k in seq_len(n)) {
    r <- compare_lr(obs[k], ref[k], tol)
    ver[k] <- r$veredicto; rel[k] <- r$rel
  }

  i <- which(ver != "PASA")
  fam <- rep(rownames(M_obs), times = ncol(M_obs))
  poi <- rep(colnames(M_obs), each = nrow(M_obs))
  celdas <- data.frame(
    familia = fam[i], poi = poi[i],
    log10LR_obs = log_obs[i], log10LR_ref = log_ref[i],
    LR_obs = obs[i], LR_ref = ref[i], rel = rel[i], veredicto = ver[i],
    stringsAsFactors = FALSE)
  celdas <- celdas[order(-celdas$rel, celdas$familia, celdas$poi), , drop = FALSE]
  rownames(celdas) <- NULL

  ## Matriz completa de `rel`, mismos dimnames que las de entrada. `rel` se
  ## llena en el bucle de arriba en el mismo orden que as.numeric(M_obs)
  ## (column-major), asi que matrix() con ese mismo orden por defecto la
  ## reconstruye sin reordenar nada.
  rel_matriz <- matrix(rel, nrow = nrow(M_obs), ncol = ncol(M_obs),
                       dimnames = dimnames(M_obs))

  conteo <- table(factor(ver, levels = c("PASA","REVISAR","FALLA","SIN_DATO")))
  ## table(factor(...)) descarta en silencio cualquier veredicto que caiga
  ## fuera de los cuatro niveles declarados en `levels`: sum(conteo) daria
  ## menos que `n` sin ningun aviso. `acuerdo_masiva()` DEDUCE `pasa` restando
  ## en vez de contarlo, asi que esta comprobacion es la unica red contra un
  ## veredicto de compare_lr() que no sea ninguno de los cuatro conocidos.
  if (sum(conteo) != n)
    stop(sprintf(paste("comparar_celdas: sum(conteo) = %d no coincide con",
                       "n = %d celdas comparadas; hay veredictos fuera de",
                       "PASA/REVISAR/FALLA/SIN_DATO"), sum(conteo), n))

  list(celdas = celdas, rel_matriz = rel_matriz, n = n, conteo = conteo)
}

#' Resumenes del acuerdo celda a celda
#'
#' La matriz de confusion de exclusiones va en el resumen global porque un
#' desacuerdo categorico --uno excluye y el otro no-- importa mas que cualquier
#' decimal: en una busqueda de personas desaparecidas excluir a quien no
#' corresponde es el peor error posible.
#'
#' `rel_max` y `rel_mediana` se miden sobre las celdas con MAGNITUD
#' comparable, no sobre todas las celdas de `cmp$rel_matriz` ni sobre las
#' que `comparar_celdas()` reporta en `celdas` (esas son solo las que NO son
#' PASA -- ver el comentario en `cuenta()`, mas abajo, para el porque de
#' cada exclusion). `celdas_magnitud` informa el tamano de esa poblacion:
#' sin el, una `rel_mediana` de 3 celdas se lee igual que una de cientos de miles.
#'
#' @param cmp salida de `comparar_celdas()`
#' @param M_obs,M_ref las dos matrices comparadas
#' @param esc escenario (`escenario_masiva()`), aporta el tipo de cada familia
#' @param motor nombre del motor de referencia, para rotular
#' @return lista con `global` (1 fila), `por_familia` y `por_tipo`; las tres
#'   traen `celdas_magnitud`, `rel_max` y `rel_mediana` (estas dos ultimas en
#'   `NA_real_` si `celdas_magnitud` da 0)
#' @export
acuerdo_masiva <- function(cmp, M_obs, M_ref, esc, motor) {
  tipo <- setNames(vapply(esc$fams, `[[`, "", "tipo"),
                   vapply(esc$fams, `[[`, "", "id"))
  ex_o <- !is.finite(M_obs) & !is.na(M_obs)   # -Inf en la entrega
  ex_r <- !is.finite(M_ref) & !is.na(M_ref)

  ## rel_max/rel_mediana se calculan sobre la poblacion de celdas con
  ## MAGNITUD comparable: las dos matrices dan un LR finito Y ninguna de las
  ## dos excluye. No es lo mismo que "todas las celdas de sub_cmp" (esa es
  ## solo la de comparar_celdas(), que por diseno trae unicamente las que NO
  ## son PASA -- `i <- which(ver != "PASA")`): si se calcularan ahi, una
  ## corrida donde TODO pasa (el caso bueno y mas frecuente) daria sub_cmp
  ## vacio y `rel_max` caeria en 0 por construccion, leyendose como
  ## "discrepancia cero" cuando en realidad significa "no hubo celdas que
  ## reportar". Por eso se parte de `rel_sub`, el recorte de `cmp$rel_matriz`
  ## con un valor por celda, PASA incluidas.
  ##
  ## Pero tampoco alcanza con sacar los no-finitos. `rel` vale Inf en un
  ## desacuerdo categorico (una matriz excluye, la otra no) y NA en una celda
  ## sin dato (SIN_DATO): ninguno de los dos es una magnitud, is.finite() los
  ## saca. Lo que ADEMAS hay que sacar es la celda donde LAS DOS excluyen
  ## (excl_ambos): ahi `compare_lr()` devuelve `rel = 0` por la rama "ambos
  ## cero" -un acuerdo categorico, no una medicion- y ese 0 es finito, asi
  ## que is.finite() solo no lo filtra. Meterlo en la poblacion no es
  ## inocuo: en una busqueda la mayoria de las celdas son excl_ambos, y
  ## esa mayoria de ceros-por-definicion arrastra la mediana global a 0
  ## aunque las celdas donde de verdad se comparo un LR tengan una
  ## discrepancia distinta de cero. Un mismo hecho -que las dos
  ## coincidieron en excluir- ya esta contado, y mejor, en `excl_ambos`; aca
  ## contarlo de nuevo disfrazado de "discrepancia" lo confunde con una
  ## magnitud que nunca se midio. De ahi el filtro extra `!(ex_o & ex_r)`
  ## sobre la poblacion, ademas de is.finite(). El filtro puede parecer
  ## redundante con is.finite() y no lo es: sin el, la mediana global leeria
  ## "que fraccion son exclusiones" en vez de "cuan cerca estuvieron los dos
  ## sistemas".
  ##
  ## Sin ninguna celda en esa poblacion (todo Inf, NA o excl_ambos -una
  ## familia enteramente hecha de exclusiones coincidentes es un caso
  ## perfectamente normal) el resultado es NA_real_, no 0: cero es la
  ## afirmacion de que coincidieron exactamente, y no hay que hacerla cuando
  ## no se midio nada.
  cuenta <- function(sub_cmp, n_celdas, ex_o, ex_r, rel_sub) {
    v <- sub_cmp$veredicto
    ## ex_o/ex_r llegan a veces como matriz (el corte por tipo, con
    ## drop = FALSE) y a veces como vector (global aplanado, o el corte por
    ## familia); as.vector() los deja alineados en el mismo orden
    ## column-major que rel_sub, que ya llega aplanado desde el llamador.
    eo <- as.vector(ex_o); er <- as.vector(ex_r)
    magnitud <- is.finite(rel_sub) & !(eo & er)
    rel_fin <- rel_sub[magnitud]
    pasa <- n_celdas - length(v)
    ## na.rm = TRUE a proposito: si `v` trajera algun NA, "v == 'REVISAR'"
    ## devolveria NA en esa posicion y sum() sin na.rm propagaria ese NA,
    ## haciendo que el `if` de abajo aborte con el error generico de R
    ## ("missing value where TRUE/FALSE needed") en vez del stop() informativo
    ## que esta comprobacion existe para dar. Con na.rm = TRUE ese NA
    ## simplemente no cuenta en ningun bucket, la suma da un numero, y es la
    ## comparacion de mas abajo la que dispara con motor/condicion/numeros.
    revisar <- sum(v == "REVISAR", na.rm = TRUE)
    falla <- sum(v == "FALLA", na.rm = TRUE)
    sin_dato <- sum(v == "SIN_DATO", na.rm = TRUE)
    ## `pasa` se DEDUCE (n_celdas menos los veredictos no-PASA de
    ## comparar_celdas()), no se cuenta: es fail-open. Una celda que por algun
    ## motivo no llegara a `v` -un NA que desaparece de `which(ver != "PASA")`,
    ## o un nivel que `table(factor(...))` ya filtro en comparar_celdas()-
    ## se convertiria en un PASA silencioso, y PASA es el numero que publica
    ## el informe. Esta comprobacion es la unica red: si la particion no
    ## cierra, se perdieron celdas por el camino.
    if (pasa + revisar + falla + sin_dato != n_celdas)
      stop(sprintf(paste("acuerdo_masiva/cuenta: la particion no cierra",
                         "(motor %s, condicion %s): pasa=%d + revisar=%d +",
                         "falla=%d + sin_dato=%d = %d, pero celdas=%d"),
                   motor, esc$condicion, pasa, revisar, falla, sin_dato,
                   pasa + revisar + falla + sin_dato, n_celdas))
    data.frame(
      celdas = n_celdas,
      pasa = pasa,
      revisar = revisar, falla = falla,
      sin_dato = sin_dato,
      celdas_magnitud = length(rel_fin),
      rel_max     = if (length(rel_fin)) max(rel_fin) else NA_real_,
      rel_mediana = if (length(rel_fin)) stats::median(rel_fin) else NA_real_,
      excl_ambos     = sum(eo & er),
      excl_solo_obs  = sum(eo & !er),
      excl_solo_ref  = sum(!eo & er),
      stringsAsFactors = FALSE)
  }

  global <- data.frame(condicion = esc$condicion, motor = motor,
                       cuenta(cmp$celdas, cmp$n, ex_o, ex_r,
                              as.numeric(cmp$rel_matriz)),
                       stringsAsFactors = FALSE)

  fams <- rownames(M_obs)
  por_familia <- do.call(rbind, lapply(fams, function(f) {
    s <- cmp$celdas[cmp$celdas$familia == f, , drop = FALSE]
    data.frame(condicion = esc$condicion, motor = motor, familia = f,
               tipo = unname(tipo[f]),
               cuenta(s, ncol(M_obs), ex_o[f, ], ex_r[f, ], cmp$rel_matriz[f, ]),
               stringsAsFactors = FALSE)
  }))

  por_tipo <- do.call(rbind, lapply(sort(unique(tipo)), function(tp) {
    ff <- names(tipo)[tipo == tp]
    s <- cmp$celdas[cmp$celdas$familia %in% ff, , drop = FALSE]
    data.frame(condicion = esc$condicion, motor = motor, tipo = tp,
               n_familias = length(ff),
               cuenta(s, length(ff) * ncol(M_obs),
                      ex_o[ff, , drop = FALSE], ex_r[ff, , drop = FALSE],
                      as.numeric(cmp$rel_matriz[ff, , drop = FALSE])),
               stringsAsFactors = FALSE)
  }))

  list(global = global, por_familia = por_familia, por_tipo = por_tipo)
}

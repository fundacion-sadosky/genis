## ---------------------------------------------------------------------------
## lrval :: los motores del escenario de busqueda masiva
##
## Unico lugar donde el escenario masivo le pide un LR a un motor. Hay dos
## vias, y las dos existen para los dos motores:
##
##   tabla   family_lr_table()  una vez por familia: LR por genotipo y locus
##   par     pair_lr()          un par (familia, POI), por el camino largo
##
## La tabla es lo que hace viable el escenario --reduce 10^6 verosimilitudes
## de pedigri a unos pocos miles-- y la via de par es la que la verifica. Para
## pedsuite la tabla sale de pedprobr::oneMarkerDistribution. Familias no
## expone nada equivalente (el paquete de CRAN exporta cuatro funciones), asi
## que su tabla se arma por barrido: FamiliasPosterior devuelve LRperMarker
## para los 24 loci de una vez, de modo que una llamada llena una entrada de
## cada locus. Que eso sea licito depende de dos hechos verificados: el LR
## total de Familias es el producto de sus LR por locus, y el LR de un locus
## no depende del genotipo del individuo bajo prueba en los otros.
## ---------------------------------------------------------------------------

#' Motores que corren el escenario masivo, el primario primero
#'
#' Familias es el motor de contraste de GENis en la busqueda masiva; pedsuite
#' se sigue corriendo como control interno porque cuesta segundos.
#' @export
MASIVA_ENGINES <- c("familias", "pedsuite")

#' Genotipos que hay que poner en la tabla de una familia
#'
#' Solo los que aparecen en la nomina: son los unicos que se van a consultar.
#' Devuelve, por locus, la clave canonica de `.geno_key()` y el par de alelos
#' en el orden de la escalera.
#'
#' @param pool salida de `build_poi_pool()`
#' @param freqs base de frecuencias
#' @export
poi_genotypes <- function(pool, freqs) {
  out <- lapply(pool$loci, function(nm) {
    a <- names(freqs[[nm]])
    g <- unique(pool$geno[, nm])
    k <- .geno_key(g, a)
    g <- g[!is.na(k)]; k <- k[!is.na(k)]
    ## dos alelos en orden inverso ("17/14" y "14/17") son el mismo genotipo
    ## pero unique() sobre la cadena cruda no lo detecta: dedup por clave
    dup <- duplicated(k)
    g <- g[!dup]; k <- k[!dup]
    pz <- strsplit(g, "/", fixed = TRUE)
    i1 <- match(vapply(pz, `[`, "", 1L), a)
    i2 <- match(vapply(pz, `[`, "", 2L), a)
    data.frame(clave = k, alelo_1 = a[pmin(i1, i2)], alelo_2 = a[pmax(i1, i2)],
               stringsAsFactors = FALSE)
  })
  names(out) <- pool$loci
  out
}

#' Todos los genotipos del panel
#'
#' Alternativa a `poi_genotypes()` cuando no hay nomina: mas cara, pero sirve
#' para pruebas y para uso manual.
#' @export
all_genotypes <- function(freqs) {
  lapply(freqs, function(f) {
    a <- names(f)
    n <- length(a)
    i1 <- rep(seq_len(n), times = n:1)
    i2 <- unlist(lapply(seq_len(n), function(i) i:n))
    data.frame(clave = paste(i1, i2, sep = "-"),
               alelo_1 = a[i1], alelo_2 = a[i2], stringsAsFactors = FALSE)
  })
}

#' Tabla de LR por genotipo para una familia
#'
#' Devuelve, por locus, un vector nombrado clave-de-genotipo -> LR, donde el
#' LR es P(G_MP = g | perfiles de la familia) / P(g): el LR exacto de comparar
#' esa familia con un POI de genotipo g.
#'
#' @param fam familia de `simulate_mp_families()`
#' @param freqs base de frecuencias
#' @param engine motor, ver `MASIVA_ENGINES`
#' @param genos dominio de la tabla (`poi_genotypes()`); solo lo usa Familias,
#'   y si falta se enumeran todos los genotipos del panel
#' @export
family_lr_table <- function(fam, freqs, engine = "familias", genos = NULL) {
  engine <- match.arg(engine, MASIVA_ENGINES)
  if (engine == "pedsuite") return(.lr_table_pedsuite(fam, freqs))
  if (is.null(genos)) genos <- all_genotypes(freqs)
  .lr_table_familias(fam, freqs, genos)
}

#' LR de un par (familia, POI) por el camino largo
#'
#' Arma el pedigri con el POI en el lugar del desaparecido y llama al motor,
#' sin pasar por la tabla de genotipos. Es la via que verifica la tabla.
#'
#' H2 es la misma familia con el desaparecido sin perfil --renombrado, para no
#' chocar con el POI-- mas el POI como individuo aislado. El singleton lleva
#' los marcadores porque `ped_to_familias()` apila los componentes con rbind y
#' falla con "numbers of columns of arguments do not match" si uno de ellos no
#' los tiene.
#'
#' @param fam familia de `simulate_mp_families()`
#' @param geno genotipos del POI, vector nombrado por locus, formato "a/b"
#' @param freqs base de frecuencias
#' @param engine motor, ver `MASIVA_ENGINES`
#' @return LR total, o NA si el motor aborta
#' @export
pair_lr <- function(fam, geno, freqs, engine = "familias") {
  engine <- match.arg(engine, MASIVA_ENGINES)
  H1 <- fam$ped
  for (nm in names(freqs)) {
    al <- strsplit(geno[[nm]], "/", fixed = TRUE)[[1]]
    H1 <- pedtools::setAlleles(H1, ids = "MP", marker = nm, alleles = al)
  }
  H1 <- pedtools::relabel(H1, old = "MP", new = "POI")
  poi_solo <- pedtools::transferMarkers(H1, pedtools::singleton("POI"), ids = "POI")
  fam_sin  <- pedtools::relabel(pedtools::setAlleles(fam$ped, ids = "MP", alleles = 0),
                                old = "MP", new = "MPX")
  H2 <- list(fam_sin, poi_solo)
  if (engine == "pedsuite") {
    r <- try(forrel::kinshipLR(H1, H2, ref = 2, source = 1), silent = TRUE)
    return(if (inherits(r, "try-error")) NA_real_ else unname(r$LRtotal[1]))
  }
  r <- run_familias(list(H1 = H1, H2 = H2))
  if (!identical(r$status, "ok")) NA_real_ else r$LR
}

#' Los dos pedigries de Familias de una familia del escenario masivo
#'
#' No cambian a lo largo del barrido: solo cambia la matriz de datos. H1 tiene
#' al individuo bajo prueba en el lugar del desaparecido; H2 conserva al
#' desaparecido dentro de la familia (sin perfil) y agrega al individuo bajo
#' prueba como fundador aislado.
#'
#' Depende de que el desaparecido no sea progenitor de nadie, que es cierto en
#' las 20 configuraciones de `PEDIGREE_TYPES`.
#' @keywords internal
.familias_masiva_peds <- function(fam) {
  d <- as.data.frame(fam$ped)[, c("id", "fid", "mid", "sex")]
  d$fid[d$fid == "0"] <- NA
  d$mid[d$mid == "0"] <- NA
  sx  <- ifelse(d$sex == 1, "male", "female")
  id1 <- d$id; id1[id1 == "MP"] <- "POI"
  list(
    H1 = Familias::FamiliasPedigree(id = id1, dadid = d$fid, momid = d$mid, sex = sx),
    H2 = Familias::FamiliasPedigree(id = c(d$id, "POI"), dadid = c(d$fid, NA),
                                    momid = c(d$mid, NA), sex = c(sx, "male")))
}

#' Tabla de LR por genotipo, via Familias (barrido)
#'
#' En el paso k se arma un individuo bajo prueba cuyo genotipo en el locus j
#' es el k-esimo de `genos[[j]]`; los loci con menos genotipos repiten el
#' ultimo y su resultado se descarta. Una llamada a FamiliasPosterior devuelve
#' el LR de todos los loci, de modo que el barrido cuesta max_j |G_j| llamadas
#' por familia y no una por par.
#'
#' @param genos dominio de la tabla, salida de `poi_genotypes()`
#' @param loci_obj objetos FamiliasLocus ya armados; dependen del panel y del
#'   modelo de mutacion, no de la familia, asi que conviene armarlos una vez
#'   por escenario y pasarlos
#' @keywords internal
.lr_table_familias <- function(fam, freqs, genos, loci_obj = NULL) {
  loci <- names(freqs)
  ## un dominio vacio en algun locus (p.ej. si poi_genotypes() no encontro
  ## ningun genotipo valido en la nomina) rompe el barrido mas abajo con
  ## "replacement has length zero" -- un error de R generico y desconectado
  ## de la causa real. Este stopifnot() lo convierte en un error explicito.
  n_genos <- vapply(genos[loci], nrow, 0L)
  stopifnot("el dominio de genotipos no puede estar vacio en ningun locus" =
              all(n_genos > 0))
  if (is.null(loci_obj)) loci_obj <- case_to_familias_loci(list(H1 = fam$ped))
  peds <- .familias_masiva_peds(fam)
  ## los parientes tipificados; el desaparecido no tiene perfil y queda afuera
  dm0  <- ped_to_datamatrix(fam$ped)
  K    <- max(n_genos)
  tabs <- lapply(loci, function(nm) numeric(nrow(genos[[nm]])))
  names(tabs) <- loci
  poi <- character(2 * length(loci))
  for (k in seq_len(K)) {
    for (j in seq_along(loci)) {
      gj <- genos[[loci[j]]]
      i  <- min(k, nrow(gj))
      poi[2 * j - 1] <- gj$alelo_1[i]
      poi[2 * j]     <- gj$alelo_2[i]
    }
    r  <- Familias::FamiliasPosterior(peds, loci_obj, rbind(dm0, POI = poi),
                                      prior = c(0.5, 0.5))
    lr <- r$LRperMarker[, 1] / r$LRperMarker[, 2]
    ## realinear por nombre de locus si LRperMarker los trae (los trae: son
    ## las filas de loci_obj). Hoy dm0 y loci_obj ya vienen en el mismo orden
    ## de names(freqs), asi que esto no cambia nada, pero deja de depender de
    ## esa coincidencia el dia que un mismo loci_obj se reuse entre familias
    if (!is.null(names(lr)) && all(loci %in% names(lr))) lr <- lr[loci]
    ## FamiliasPosterior normaliza LRperMarker contra el pedigri de
    ## referencia: una exclusion da columna 1 = NaN y columna 2 = Inf, y el
    ## cociente es NaN en vez del LR = 0 que promete esta tabla
    lr[is.nan(lr)] <- 0
    for (j in seq_along(loci)) if (k <= nrow(genos[[loci[j]]])) tabs[[j]][k] <- lr[j]
  }
  for (j in seq_along(loci)) names(tabs[[j]]) <- genos[[loci[j]]]$clave
  tabs
}

#' Tabla de LR por genotipo, via pedsuite
#'
#' La distribucion condicional del genotipo del desaparecido dados sus
#' parientes, dividida por la probabilidad poblacional del mismo genotipo.
#' @keywords internal
.lr_table_pedsuite <- function(fam, freqs) {
  lapply(names(freqs), function(nm) {
    f <- freqs[[nm]]; a <- names(f)
    d <- pedprobr::oneMarkerDistribution(fam$ped, ids = "MP", marker = nm,
                                         output = "sparse", verbose = FALSE)
    lab <- d[[1]]; pr <- d[["prob"]]
    k <- .geno_key(lab, a)
    pz <- strsplit(lab, "/", fixed = TRUE)
    x1 <- vapply(pz, `[`, "", 1L); x2 <- vapply(pz, `[`, "", 2L)
    hwp <- ifelse(x1 == x2, f[x1]^2, 2 * f[x1] * f[x2])
    setNames(pr / hwp, k)
  })
}

#' Invariante: sin modelo de mutacion los dos motores deben coincidir
#'
#' La unica diferencia de convencion en juego en el escenario masivo es la poda
#' de fundadores no tipificados, y sin mutacion la poda es exacta: no hay
#' alelos silentes en el panel, ni perfiles parciales, ni coancestria. Si el
#' escenario sin mutacion difiere entre motores, es un error de calculo y no
#' una diferencia de convencion.
#'
#' La comparacion es en log10, que es la escala en que se guarda M. Un error
#' relativo e en el LR da una diferencia absoluta de e/ln(10) en log10,
#' independiente de la magnitud, asi que la tolerancia es la misma.
#'
#' @param Ma,Mb matrices de `mass_search()` con los mismos margenes
#' @param motor_a,motor_b nombres de los motores de `Ma` y `Mb`, solo para
#'   rotular las columnas de salida (mismo convenio que `engine_divergence()`)
#' @export
check_mass_engines <- function(Ma, Mb, tol = LRVAL_TOL,
                               motor_a = "familias", motor_b = "pedsuite") {
  stopifnot(identical(dim(Ma), dim(Mb)))
  d <- abs(Ma - Mb)
  ## -Inf menos -Inf da NaN: dos exclusiones no difieren
  d[is.na(d)] <- 0
  m <- max(d)
  data.frame(motor_a = motor_a, motor_b = motor_b, n_pares = length(Ma), max_dif_log10 = m,
             veredicto = if (m <= tol$log10) "PASA" else if (m <= 1e-6) "REVISAR" else "FALLA",
             stringsAsFactors = FALSE)
}

#' Divergencia entre dos motores, por tipo de pedigri
#'
#' Con mutacion activa la diferencia entre Familias y pedsuite es la poda de
#' fundadores no tipificados y no un fallo. Esto la mide sobre el escenario completo, en
#' vez de caso por caso como en el catalogo, e informa cuantos pares cruzan el
#' umbral de reporte en un motor y no en el otro, que es la consecuencia
#' operativa de la diferencia.
#'
#' @param Ma,Mb matrices de `mass_search()` con los mismos margenes
#' @param families lista de familias de `simulate_mp_families()`, en el mismo
#'   orden que las filas de `Ma`/`Mb`; aporta el tipo de pedigri por el que
#'   se agrupa
#' @param escenario etiqueta libre para identificar la corrida en la salida
#' @param umbral log10(LR) que separa un llamado positivo; cuenta cuantos
#'   pares lo cruzan en un motor y no en el otro
#' @param motor_a,motor_b nombres de los motores de `Ma` y `Mb`, solo para
#'   rotular las columnas de salida
#' @return data.frame con una fila por tipo de pedigri: `escenario`, `tipo`,
#'   `motor_a`, `motor_b`, `n_pares`, `mediana`, `p99` y `maximo` (diferencia
#'   absoluta en log10 sobre ese tipo) y `cruzan_umbral` (pares que superan
#'   `umbral` en un motor y no en el otro)
#' @export
engine_divergence <- function(Ma, Mb, families, escenario, umbral = 4,
                              motor_a = "familias", motor_b = "pedsuite") {
  stopifnot(identical(dim(Ma), dim(Mb)))
  tipo <- vapply(families, `[[`, "", "tipo")
  d <- abs(Ma - Mb); d[is.na(d)] <- 0
  cruza <- xor(Ma >= umbral, Mb >= umbral)
  do.call(rbind, lapply(unique(tipo), function(tp) {
    i <- which(tipo == tp)
    v <- as.numeric(d[i, , drop = FALSE])
    data.frame(escenario = escenario, tipo = tp,
               motor_a = motor_a, motor_b = motor_b, n_pares = length(v),
               mediana = stats::median(v), p99 = unname(stats::quantile(v, 0.99)),
               maximo = max(v), cruzan_umbral = sum(cruza[i, , drop = FALSE]),
               stringsAsFactors = FALSE)
  }))
}

#' Motores que corren en una condicion del escenario masivo
#'
#' La corrida de Familias en la condicion CON MUTACION cuesta ~192 h de nucleo
#' (39,6 h de reloj en la corrida medida) y es, por lejos, el paso mas caro de
#' toda la validacion. No emite veredicto: con mutacion el veredicto va contra
#' pedsuite, porque Familias aplica otra convencion para los fundadores no
#' tipificados (los poda) y sus numeros no son comparables celda a celda con
#' los de un motor que los conserva. Lo unico que alimenta es la medida de esa
#' diferencia a escala (`engine_divergence()`).
#'
#' Por eso el default la OMITE y hay que pedirla explicitamente. Sin mutacion,
#' en cambio, Familias es el motor de veredicto y su acuerdo con pedsuite es el
#' contraste cruzado mas grande del set: siempre corre, y cuesta 90 s.
#'
#' No se resuelve con `--solo-pedsuite`, que es global a las dos condiciones y
#' por lo tanto tambien apagaria ese contraste de 90 s.
#'
#' @param motores motores habilitados para la corrida entera
#' @param con_mutacion TRUE si es la condicion con modelo de mutacion
#' @param con_familias_mut TRUE para pagar Familias tambien con mutacion
#' @return los motores que corresponden a esa condicion
#' @export
motores_de_escenario <- function(motores, con_mutacion, con_familias_mut = FALSE) {
  m <- if (isTRUE(con_mutacion) && !isTRUE(con_familias_mut)) {
    setdiff(motores, "familias")
  } else motores
  ## Una condicion sin ningun motor no produce matriz y el resto del script
  ## seguiria como si nada, escribiendo resumenes vacios. Se corta aca.
  if (!length(m))
    stop("motores_de_escenario: la condicion se queda sin ningun motor ",
         "(motores = ", paste(motores, collapse = ", "),
         ", con_mutacion = ", isTRUE(con_mutacion), "). ",
         "Habilitar otro motor o pedir --con-familias-mutacion.")
  m
}

#' Condiciones donde corrieron los dos motores, y que por eso admiten contraste
#'
#' El acuerdo cruzado (`check_mass_engines`, `engine_divergence`) solo se puede
#' medir donde corrieron LOS DOS motores. Como los motores se deciden por
#' condicion (`motores_de_escenario`), no alcanza con mirar
#' `length(MOTORES) > 1`: hay que filtrar condicion por
#' condicion, o se llamaria a `check_mass_engines()` sobre una condicion donde
#' Familias no corrio y su matriz es NULL.
#'
#' El resultado tiene un segundo uso, y es el delicado: sus etiquetas son
#' exactamente las condiciones que esta corrida puede REEMPLAZAR en
#' `acuerdo_motores.csv` y `divergencia_motores.csv`. Si devolviera de mas,
#' `anexar_por_escenario()` borraria justamente la fila que se quiso preservar.
#'
#' @param corridas lista de resultados de `correr()`, cada uno con `$motores`
#' @return la sublista de las que tienen los dos motores de `MASIVA_ENGINES`
#' @export
condiciones_comparables <- function(corridas) {
  Filter(function(r) all(MASIVA_ENGINES %in% r$motores), corridas)
}

## ---------------------------------------------------------------------------
## lrval :: bases de frecuencias alelicas
##
## No se generan frecuencias nuevas: se cargan las que ya existen en el
## ecosistema y se las normaliza a un unico formato (lista nombrada
## locus -> vector nombrado de frecuencias, que es el formato que consumen
## pedtools::setMarkers() y Familias::FamiliasLocus()).
##
## Fuentes soportadas
##   mispitools  : Argentina, Asia, Austria, BosniaHerz, China, Europe,
##                 Japan, USA          (mispitools::get_allele_freqs)
##   forrel      : NorwegianFrequencies (35 loci, incluye SE33 con
##                 abundantes microvariantes)
##   STRidER     : base de referencia de ENFSI, descargable en CSV desde
##                 https://strider.online/frequencies/csv . Es la fuente
##                 primaria que republica leapDNA (https://leapdna.org);
##                 se descarga directamente de STRidER.
## ---------------------------------------------------------------------------

STRIDER_CSV <- "https://strider.online/frequencies/csv"

#' Frecuencias de mispitools en formato pedtools
#'
#' @param region uno de "Argentina","Asia","Austria","BosniaHerz","China",
#'   "Europe","Japan","USA"
#' @param drop_zero elimina los alelos con frecuencia 0 (las bases de
#'   mispitools estan rellenadas con la escalera alelica completa)
#' @export
freqs_mispitools <- function(region = "Argentina", drop_zero = TRUE) {
  df <- get(region, envir = asNamespace("mispitools"))
  f <- mispitools::get_allele_freqs(df)
  if (drop_zero) f <- lapply(f, function(v) v[v > 0])
  lapply(f, renormalize)
}

#' Frecuencias noruegas de forrel
#' @export
freqs_norwegian <- function() {
  f <- get("NorwegianFrequencies", envir = asNamespace("forrel"))
  lapply(f, renormalize)
}

#' Descarga y parsea la base STRidER (ENFSI)
#'
#' El CSV de STRidER viene "apilado": una linea con el nombre del locus,
#' luego una fila de encabezados de poblacion, luego una fila con los N,
#' y luego una fila por alelo.
#'
#' @param population nombre exacto de la columna de poblacion, p.ej.
#'   "Entire Database", "Europe", "ITALY", "SPAIN"
#' @param file ruta a un CSV ya descargado; si es NULL se descarga
#' @param cache ruta donde guardar/leer la descarga
#' @return lista locus -> frecuencias, y atributo "N" con los tamanos muestrales
#' @export
freqs_strider <- function(population = "Entire Database", file = NULL,
                          cache = file.path(LRVAL_DIRS$cache, "strider_freq.csv")) {
  if (is.null(file)) {
    if (!file.exists(cache)) {
      dir.create(dirname(cache), recursive = TRUE, showWarnings = FALSE)
      utils::download.file(STRIDER_CSV, cache, quiet = TRUE)
    }
    file <- cache
  }
  ln <- readLines(file, warn = FALSE)
  ln <- ln[nzchar(trimws(ln))]
  ## Las lineas de locus no tienen comas. Ojo: algunos loci pueden venir sin
  ## datos para ninguna poblacion, en cuyo caso la linea de encabezado tampoco
  ## lleva comas y hay que descartarla.
  esLocus <- !grepl(",", ln) & trimws(ln) != "Allele"
  idx <- which(esLocus)
  out <- list(); ns <- integer(0)
  for (k in seq_along(idx)) {
    i0 <- idx[k]
    i1 <- if (k < length(idx)) idx[k + 1] - 1 else length(ln)
    locus <- trimws(ln[i0])
    if (i1 - i0 < 2) next                       # locus sin datos
    blk <- ln[(i0 + 1):i1]
    blk <- blk[grepl(",", blk)]
    if (length(blk) < 2) next
    tab <- utils::read.csv(text = paste(blk, collapse = "\n"),
                           check.names = FALSE, colClasses = "character")
    if (!population %in% names(tab)) {
      stop("freqs_strider: poblacion '", population, "' no encontrada. Disponibles: ",
           paste(setdiff(names(tab), "Allele"), collapse = ", "))
    }
    ## la primera fila (Allele vacio) contiene los tamanos muestrales
    esN <- !nzchar(trimws(tab$Allele))
    ns[locus] <- suppressWarnings(as.numeric(tab[[population]][esN][1]))
    tab <- tab[!esN, , drop = FALSE]
    v <- suppressWarnings(as.numeric(tab[[population]]))
    names(v) <- trimws(tab$Allele)
    v <- v[!is.na(v) & v > 0]
    if (length(v)) out[[locus]] <- renormalize(v)
  }
  attr(out, "N") <- ns
  attr(out, "population") <- population
  out
}

## --- utilidades -------------------------------------------------------------

#' Renormaliza a suma 1 y ordena los alelos en orden STR canonico
#' @export
renormalize <- function(v) {
  v <- v[v > 0]
  v <- v / sum(v)
  v[order_alleles(names(v))]
}

#' Orden canonico de alelos STR: numerico, con microvariantes en su lugar
#'
#' 9 < 9.3 < 10 < 10.2 < 11.  Los alelos no numericos (p.ej. "silent") van
#' al final, en orden alfabetico.
#' @export
order_alleles <- function(a) {
  n <- suppressWarnings(as.numeric(a))
  order(is.na(n), n, a)
}

#' Es un nombre de alelo STR valido para el modelo stepwise?
#'
#' Familias exige entero o decimal con un unico decimal (microvariante).
#' @export
is_str_allele <- function(a) grepl("^[0-9]+(\\.[0-9])?$", a)

#' Alelos microvariantes de un vector de frecuencias
#' @export
microvariants <- function(v) {
  a <- names(v)
  a[is_str_allele(a) & grepl("\\.", a)]
}

## --- control de calidad de una base -----------------------------------------

#' Control de calidad de una base de frecuencias
#'
#' Chequeos que un laboratorio deberia correr antes de poner una base en uso.
#' Devuelve un data.frame con una fila por locus.
#'
#' @param freqs lista locus -> frecuencias
#' @param minfreq piso de frecuencia esperado (NA para no evaluar)
#' @export
qc_freqs <- function(freqs, minfreq = NA) {
  do.call(rbind, lapply(names(freqs), function(nm) {
    v <- freqs[[nm]]
    a <- names(v)
    data.frame(
      locus       = nm,
      n_alelos    = length(v),
      suma        = sum(v),
      suma_ok     = abs(sum(v) - 1) < 1e-6,
      min_freq    = min(v),
      max_freq    = max(v),
      bajo_piso   = if (is.na(minfreq)) NA_integer_ else sum(v < minfreq),
      n_microvar  = length(microvariants(v)),
      no_str      = sum(!is_str_allele(a)),
      duplicados  = anyDuplicated(a) > 0,
      no_negativos= all(v > 0),
      stringsAsFactors = FALSE)
  }))
}

#' Aplica un piso de frecuencia y renormaliza
#'
#' Es uno de los puntos donde los programas difieren (algunos renormalizan,
#' otros no), por lo que conviene aplicarlo explicitamente aguas arriba y
#' entregar a todos los motores exactamente la misma base.
#' @export
apply_min_freq <- function(v, minfreq) {
  v[v < minfreq] <- minfreq
  v / sum(v)
}

#' Aplica el piso contando los alelos observados que el panel crudo no trae
#'
#' Es la operacion de `apply_min_freq` extendida al caso en que un perfil trae
#' un alelo que no figura en el panel: ese alelo entra valiendo `minfreq`, y
#' despues se pisa y se divide el panel ENTERO por la suma, alelo agregado
#' incluido. Sin alelos nuevos es exactamente `apply_min_freq`.
#' @param v panel crudo (vector nombrado)
#' @param minfreq piso declarado
#' @param observed alelos observados en los perfiles del caso para ese locus
#' @export
apply_min_freq_observed <- function(v, minfreq, observed = character(0)) {
  nuevos <- setdiff(observed, names(v))
  v <- c(v, setNames(rep(minfreq, length(nuevos)), nuevos))
  v[v < minfreq] <- minfreq
  v <- v / sum(v)
  v[order_alleles(names(v))]
}

#' Agrega un alelo silente reescalando el resto
#'
#' Familias trata como silente al alelo llamado "silent"/"Silent", que debe
#' ir ultimo en el vector.
#' @export
add_silent <- function(v, p_silent, name = "silent") {
  stopifnot(p_silent > 0, p_silent < 1)
  c(v * (1 - p_silent), setNames(p_silent, name))
}

#' Agrega alelos (p.ej. una microvariante rara) reescalando el resto
#' @export
add_alleles <- function(v, alleles, freq) {
  freq <- rep_len(freq, length(alleles))
  v2 <- c(v * (1 - sum(freq)), setNames(freq, alleles))
  v2[order_alleles(names(v2))]
}

#' Reduce una base a un subconjunto de loci, verificando que existan
#' @export
pick_loci <- function(freqs, loci) {
  falta <- setdiff(loci, names(freqs))
  if (length(falta))
    stop("pick_loci: loci ausentes de la base: ", paste(falta, collapse = ", "))
  freqs[loci]
}

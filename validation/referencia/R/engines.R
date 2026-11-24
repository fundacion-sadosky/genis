## ---------------------------------------------------------------------------
## lrval :: motores de calculo
##
## Cada motor es un adaptador delgado sobre software ya existente. lrval no
## implementa ningun algoritmo de verosimilitud propio: su funcion es
## presentar EL MISMO caso a cada motor y recoger el LR.
##
##   pedsuite : forrel::kinshipLR()  (pedtools + pedprobr, Elston-Stewart)
##   familias : Familias::FamiliasPosterior()  (paquete R de CRAN, cuyo
##              nucleo es el mismo codigo C++ del Familias de escritorio)
##   genis    : se resuelve fuera de R; lrval exporta el caso y reimporta
##              el resultado (ver genis.R)
##
## Convencion: LR = P(datos | H1) / P(datos | H2), con H1 el pedigri de
## parentesco y H2 el de no parentesco.
## ---------------------------------------------------------------------------

#' Version instalada de un paquete
#'
#' @param p nombre del paquete
#' @return la version como texto, o NA si el paquete no esta instalado
#' @export
pkg_ver <- function(p) {
  tryCatch(as.character(utils::packageVersion(p)), error = function(e) NA_character_)
}

#' Etiqueta "Familias <version>" para los textos de divergencia
#'
#' Una diferencia de convencion es una propiedad de UNA version del programa,
#' no del programa en abstracto. Escribir el numero a mano deja el texto
#' desfasado del stack que fija entorno.txt en cuanto se actualiza el paquete;
#' por eso se lee de la instalacion, igual que el resto de las cifras de la
#' corrida. Si Familias no esta instalado se degrada al nombre solo, para
#' que el texto siga siendo gramatical.
#' @export
familias_label <- function() {
  v <- pkg_ver("Familias")
  if (is.na(v)) "Familias" else paste("Familias", v)
}

#' Corre un caso con el stack pedsuite (forrel/pedtools/pedprobr)
#'
#' @param case caso de `build_catalog()`
#' @return lista con LR, L1, L2, tiempo y estado
#' @export
run_pedsuite <- function(case) {
  t0 <- Sys.time()
  res <- tryCatch({
    r <- forrel::kinshipLR(case$H1, case$H2, ref = 2, source = 1)
    list(LR = unname(r$LRtotal[1]),
         L1 = unname(prod(r$likelihoodsPerMarker[, 1])),
         L2 = unname(prod(r$likelihoodsPerMarker[, 2])),
         per_marker = r$LRperMarker[, 1],
         status = "ok", msg = NA_character_)
  }, error = function(e) list(LR = NA_real_, L1 = NA_real_, L2 = NA_real_,
                              per_marker = NULL, status = "error",
                              msg = conditionMessage(e)))
  res$engine <- "pedsuite"
  res$secs <- as.numeric(difftime(Sys.time(), t0, units = "secs"))
  res
}

#' Convierte un pedigri de pedtools al formato de Familias
#'
#' Acepta un `ped` o una lista de `ped` (pedigri desconectado, como el
#' habitual H2 = {familia con padre desconocido} + {individuo aislado}).
#' @export
ped_to_familias <- function(x) {
  if (pedtools::is.ped(x)) x <- list(x)
  d <- do.call(rbind, lapply(x, as.data.frame))[, c("id", "fid", "mid", "sex")]
  d$fid[d$fid == "0"] <- NA
  d$mid[d$mid == "0"] <- NA
  Familias::FamiliasPedigree(id = d$id, dadid = d$fid, momid = d$mid,
                             sex = ifelse(d$sex == 1, "male", "female"))
}

#' Extrae la matriz de datos (genotipos) de un pedigri de pedtools
#'
#' Formato de Familias: una fila por individuo tipificado, dos columnas por
#' locus. Los individuos sin genotipo se omiten.
#' @export
ped_to_datamatrix <- function(x) {
  if (pedtools::is.ped(x)) x <- list(x)
  ml <- lapply(x, function(p) {
    if (!pedtools::nMarkers(p)) return(NULL)
    d <- as.data.frame(p)
    g <- as.matrix(d[, setdiff(names(d), c("id", "fid", "mid", "sex")), drop = FALSE])
    rownames(g) <- d$id
    g
  })
  ml <- ml[!vapply(ml, is.null, logical(1))]
  g <- do.call(rbind, ml)
  g[g %in% c("-/-", "0/0", "")] <- NA
  loci <- colnames(g)
  dm <- matrix(NA_character_, nrow = nrow(g), ncol = 2 * ncol(g),
               dimnames = list(rownames(g),
                               as.vector(rbind(paste0(loci, ".1"), paste0(loci, ".2")))))
  vacio <- function(z) is.na(z) | z %in% c("-", "0", "")
  for (j in seq_along(loci)) {
    pz <- strsplit(g[, j], "/", fixed = TRUE)
    a1 <- vapply(pz, function(z) if (length(z) == 2) z[1] else NA_character_, "")
    a2 <- vapply(pz, function(z) if (length(z) == 2) z[2] else NA_character_, "")
    ## Un alelo perdido por dropout se pasa como NA: es el unico marcador de
    ## dato ausente que acepta Familias (un "-" o un "0" produce el error
    ## "Allele - is not found in locus").
    a1[vacio(a1)] <- NA_character_
    a2[vacio(a2)] <- NA_character_
    dm[, 2 * j - 1] <- a1
    dm[, 2 * j]     <- a2
  }
  ## Se descartan los individuos sin ningun genotipo: Familias no admite
  ## filas enteramente vacias en la matriz de datos.
  dm[rowSums(!is.na(dm)) > 0, , drop = FALSE]
}

#' Construye los objetos FamiliasLocus de un caso
#'
#' Traduce el modelo de mutacion de pedmut (adjunto a los marcadores de
#' pedtools) al vocabulario de Familias. Los modelos "equal", "proportional"
#' y "stepwise" son numericamente identicos en ambos paquetes (ver
#' `compare_mutation_models()`); para cualquier otro caso se pasa la matriz
#' explicita con MutationModel="Custom", de modo que ambos motores usen
#' exactamente la misma matriz y toda discrepancia sea atribuible al algoritmo
#' de pedigri.
#' @export
case_to_familias_loci <- function(case) {
  src <- if (pedtools::is.ped(case$H1)) case$H1 else case$H1[[1]]
  lapply(seq_len(pedtools::nMarkers(src)), function(i) {
    m  <- pedtools::getMarkers(src, i)[[1]]
    af <- pedtools::afreq(m)
    nm <- pedtools::name(m)
    mm <- pedtools::mutmod(m)
    if (is.null(mm))
      return(Familias::FamiliasLocus(as.numeric(af), names(af), nm))
    ## Ojo: para que Familias use matrices distintas por sexo hay que
    ## declarar tambien maleMutationModel y femaleMutationModel = "Custom";
    ## si solo se pasa MutationModel, aplica la misma matriz a ambos sexos.
    Familias::FamiliasLocus(
      as.numeric(af), names(af), nm,
      maleMutationModel    = "Custom",
      maleMutationMatrix   = as.matrix(mm$male),
      femaleMutationModel  = "Custom",
      femaleMutationMatrix = as.matrix(mm$female))
  })
}

#' Corre un caso con el paquete Familias de CRAN
#'
#' @param case caso de `build_catalog()`
#' @param kinship coeficiente de coancestria theta (parametro `kinship` de
#'   FamiliasPosterior). pedsuite no lo soporta.
#' @export
run_familias <- function(case, kinship = 0) {
  t0 <- Sys.time()
  res <- tryCatch({
    ## Un caso puede traer una representacion propia para Familias (`famH1` y
    ## `famH2`): FamiliasPosterior(kinship > 0) aborta si los pedigries no
    ## tienen exactamente el mismo conjunto de individuos, y pedtools no
    ## admite individuos con un solo progenitor declarado.
    peds <- list(H1 = case$famH1 %||% ped_to_familias(case$H1),
                 H2 = case$famH2 %||% ped_to_familias(case$H2))
    dm   <- ped_to_datamatrix(case$H1)
    loci <- case_to_familias_loci(case)
    r <- Familias::FamiliasPosterior(peds, loci, dm, prior = c(0.5, 0.5),
                                     kinship = kinship)
    list(LR = unname(r$likelihoods[1] / r$likelihoods[2]),
         L1 = unname(r$likelihoods[1]), L2 = unname(r$likelihoods[2]),
         per_marker = r$LRperMarker[, 1] / r$LRperMarker[, 2],
         status = "ok", msg = NA_character_)
  }, error = function(e) list(LR = NA_real_, L1 = NA_real_, L2 = NA_real_,
                              per_marker = NULL, status = "error",
                              msg = conditionMessage(e)))
  res$engine <- "familias"
  res$secs <- as.numeric(difftime(Sys.time(), t0, units = "secs"))
  res
}

#' Verosimilitudes por locus con pedprobr (via de calculo alternativa)
#'
#' `forrel::kinshipLR` ya usa pedprobr, pero llamarlo directamente permite
#' activar opciones (p.ej. `special`, control de lumping) y sirve como
#' control interno del propio stack.
#' @export
run_pedprobr <- function(case, ...) {
  t0 <- Sys.time()
  res <- tryCatch({
    ## Los marcadores viven en H1; hay que transferirlos a H2 igual que hace
    ## kinshipLR(source = 1) antes de poder calcular su verosimilitud.
    H2 <- pedtools::transferMarkers(case$H1, case$H2)
    L1 <- prod(pedprobr::likelihood(case$H1, ...))
    L2 <- prod(pedprobr::likelihood(H2, ...))
    list(LR = L1 / L2, L1 = L1, L2 = L2, per_marker = NULL,
         status = "ok", msg = NA_character_)
  }, error = function(e) list(LR = NA_real_, L1 = NA_real_, L2 = NA_real_,
                              per_marker = NULL, status = "error",
                              msg = conditionMessage(e)))
  res$engine <- "pedprobr"
  res$secs <- as.numeric(difftime(Sys.time(), t0, units = "secs"))
  res
}

#' Corre un caso en todos los motores disponibles
#' @export
run_all_engines <- function(case, engines = c("pedsuite", "familias")) {
  out <- list()
  if ("pedsuite" %in% engines) out$pedsuite <- run_pedsuite(case)
  if ("familias" %in% engines) out$familias <- run_familias(case, kinship = case$theta %||% 0)
  if ("pedprobr" %in% engines) out$pedprobr <- run_pedprobr(case)
  out
}

`%||%` <- function(a, b) if (is.null(a)) b else a

#' Version del motor que produjo un LR
#'
#' El veredicto de una corrida vale para versiones concretas del stack. Los
#' motores internos la toman de la instalacion; un sistema externo la declara
#' en su archivo de resultados. Se guarda por fila, y no solo en entorno.txt,
#' porque una tabla puede mezclar una corrida interna de hoy con resultados
#' que un sistema externo devolvio en otra fecha y con otra version.
#' @param en nombre del motor
#' @export
engine_version <- function(en) {
  v <- switch(en,
              pedsuite = pkg_ver("forrel"),
              familias = pkg_ver("Familias"),
              pedprobr = pkg_ver("pedprobr"),
              NA_character_)
  if (is.na(v)) "" else v
}

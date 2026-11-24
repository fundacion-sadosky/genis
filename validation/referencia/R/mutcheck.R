## ---------------------------------------------------------------------------
## lrval :: verificacion de los MODELOS de mutacion
##
## Antes de comparar LR conviene comparar el objeto que los produce. Si dos
## programas generan matrices de mutacion distintas a partir de los mismos
## parametros, sus LR van a diferir sin que ninguno tenga un error de
## algoritmo. Este archivo separa las dos preguntas:
##
##   (a) dados un modelo, una tasa y una escalera alelica, ¿generan pedmut y
##       Familias la MISMA matriz?
##   (b) ¿cumple esa matriz las propiedades formales exigibles (filas que
##       suman 1, tasa global correcta, estacionariedad, reversibilidad)?
##
## Todo se calcula llamando a pedmut y a Familias; lrval no define ningun
## modelo propio.
## ---------------------------------------------------------------------------

#' Escaleras alelicas de prueba para los modelos de mutacion
#' @export
MUT_LADDERS <- list(
  enteros = list(alleles = c("8", "9", "10", "11", "12"),
                 afreq   = c(0.10, 0.20, 0.30, 0.25, 0.15)),
  microvariantes = list(alleles = c("8", "9", "9.3", "10", "11", "12"),
                        afreq   = c(0.10, 0.15, 0.10, 0.30, 0.20, 0.15)),
  dos_grupos_decimales = list(alleles = c("9", "9.3", "10", "10.3", "11"),
                              afreq   = c(0.20, 0.15, 0.25, 0.20, 0.20)),
  con_hueco = list(alleles = c("8", "9", "10", "12"),
                   afreq   = c(0.25, 0.25, 0.25, 0.25))
)

#' Matriz de mutacion segun Familias
#' @keywords internal
.fam_matrix <- function(model, alleles, afreq, rate, rate2, range) {
  L <- Familias::FamiliasLocus(
    afreq, alleles, "L",
    MutationModel = c(equal = "Equal", proportional = "Proportional",
                      stepwise = "Stepwise")[[model]],
    MutationRate = rate, MutationRate2 = rate2, MutationRange = range)
  as.matrix(L$maleMutationMatrix)
}

#' Matriz de mutacion segun pedmut
#' @keywords internal
.ped_matrix <- function(model, alleles, afreq, rate, rate2, range) {
  as.matrix(pedmut::mutationMatrix(model, alleles = alleles, afreq = afreq,
                                   rate = rate, rate2 = rate2, range = range))
}

#' Compara las matrices de mutacion que generan pedmut y Familias
#'
#' Recorre los modelos con nombre sobre varias escaleras alelicas (incluidas
#' escaleras con microvariantes y con huecos) y devuelve, para cada
#' combinacion, la discrepancia maxima celda a celda y las propiedades
#' formales de la matriz.
#'
#' @param rates tasas a evaluar
#' @export
compare_mutation_models <- function(rates = c(0.001, 0.005, 0.01),
                                    rate2 = 1e-4, range = 0.1, tol = 1e-12) {
  filas <- list()
  for (esc in names(MUT_LADDERS)) {
    lad <- MUT_LADDERS[[esc]]
    a <- lad$alleles
    p <- setNames(lad$afreq / sum(lad$afreq), a)
    for (mod in c("equal", "proportional", "stepwise")) {
      ## el modelo stepwise exige alelos enteros o microvariantes de un decimal
      if (mod == "stepwise" && !all(grepl("^[0-9]+(\\.[0-9])?$", a))) next
      for (r in rates) {
        Mp <- try(.ped_matrix(mod, a, p, r, rate2, range), silent = TRUE)
        Mf <- try(.fam_matrix(mod, a, p, r, rate2, range), silent = TRUE)
        err <- function(x) if (inherits(x, "try-error"))
          conditionMessage(attr(x, "condition")) else NA_character_
        if (inherits(Mp, "try-error") || inherits(Mf, "try-error")) {
          filas[[length(filas) + 1]] <- data.frame(
            escalera = esc, modelo = mod, tasa = r, disc_max = NA_real_,
            iguales = NA, filas_suman_1 = NA, tasa_observada = NA_real_,
            estacionaria = NA, reversible = NA,
            mensaje = paste(na.omit(c(err(Mp), err(Mf))), collapse = " | "),
            stringsAsFactors = FALSE)
          next
        }
        Mf <- Mf[rownames(Mp), colnames(Mp), drop = FALSE]
        disc <- max(abs(Mp - Mf))
        tasa_obs <- sum(p * (1 - diag(Mp)))
        est <- max(abs(as.vector(p %*% Mp) - p))
        A <- p * Mp
        rev <- max(abs(A - t(A)))
        filas[[length(filas) + 1]] <- data.frame(
          escalera = esc, modelo = mod, tasa = r, disc_max = disc,
          iguales = disc <= tol,
          filas_suman_1 = max(abs(rowSums(Mp) - 1)) <= 1e-12,
          tasa_observada = tasa_obs,
          estacionaria = est <= 1e-12, reversible = rev <= 1e-12,
          mensaje = NA_character_, stringsAsFactors = FALSE)
      }
    }
  }
  do.call(rbind, filas)
}

#' Es la matriz agrupable (lumpable) respecto de una particion?
#'
#' Condicion de Kemeny-Snell: para todo alelo i de un bloque B y todo bloque
#' C, la suma de M[i, C] no debe depender de i. Es la condicion que justifica
#' agrupar los alelos no observados para acelerar el calculo. El modelo
#' stepwise no la cumple, de modo que agrupar alelos con stepwise produce LR
#' incorrectos. pedmut expone `isLumpable()`; aqui se usa esa funcion sobre
#' las particiones que aparecen realmente en los casos del catalogo.
#'
#' Con `mats` (un volcado de `import_matrices()`) la condicion se evalua ademas
#' sobre la matriz que el sistema evaluado uso realmente, y no solo sobre la de
#' referencia: agrupar es una optimizacion del sistema, de modo que la pregunta
#' relevante es si SU matriz la admite.
#'
#' @param cs catalogo
#' @param mats volcado de matrices del sistema evaluado, o NULL
#' @export
check_lumpability <- function(cs, mats = NULL) {
  filas <- list()
  for (cid in names(cs)) {
    src <- if (pedtools::is.ped(cs[[cid]]$H1)) cs[[cid]]$H1 else cs[[cid]]$H1[[1]]
    for (j in seq_len(pedtools::nMarkers(src))) {
      m <- pedtools::getMarkers(src, j)[[1]]
      mm <- pedtools::mutmod(m)
      if (is.null(mm)) next
      al <- pedtools::alleles(m)
      obs <- unique(unlist(strsplit(as.data.frame(src)[[pedtools::name(m)]], "/")))
      obs <- intersect(al, obs)
      resto <- setdiff(al, obs)
      if (length(resto) < 2) next
      part <- c(as.list(obs), list(resto))
      agrega <- function(origen, mmx) {
        lp <- try(pedmut::isLumpable(mmx, part), silent = TRUE)
        filas[[length(filas) + 1]] <<- data.frame(
          caso = cid, locus = pedtools::name(m), origen = origen,
          alelos_observados = length(obs), alelos_agrupables = length(resto),
          lumpable = if (inherits(lp, "try-error")) NA else isTRUE(lp),
          stringsAsFactors = FALSE)
      }
      agrega("referencia", mm)
      if (!is.null(mats)) {
        Ms <- find_matrix(mats, "caso", cid, pedtools::name(m), "M")
        if (!is.null(Ms) && setequal(rownames(Ms), al)) {
          Ms <- Ms[al, al, drop = FALSE]
          cst <- try(pedmut::mutationMatrix("custom", matrix = Ms), silent = TRUE)
          if (!inherits(cst, "try-error")) agrega("volcada", cst)
        }
      }
    }
  }
  if (!length(filas)) return(NULL)
  do.call(rbind, filas)
}

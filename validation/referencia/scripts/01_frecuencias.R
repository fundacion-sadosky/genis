#!/usr/bin/env Rscript
## ---------------------------------------------------------------------------
## Bases de frecuencias alelicas: carga, control de calidad y comparacion.
##
##   Rscript scripts/01_frecuencias.R
##
## Sirve para dos cosas:
##   1. verificar una base antes de cargarla en GENis;
##   2. documentar de donde salen las frecuencias que usa el catalogo.
## ---------------------------------------------------------------------------

source("load.R")
dir.create(LRVAL_DIRS$salida, recursive = TRUE, showWarnings = FALSE)

bases <- list()

cat("\n== mispitools ================================================\n")
for (reg in c("Argentina", "Europe", "USA", "Asia")) {
  f <- tryCatch(freqs_mispitools(reg), error = function(e) NULL)
  if (is.null(f)) { cat(reg, ": no disponible\n"); next }
  bases[[paste0("mispitools:", reg)]] <- f
  cat(sprintf("%-12s %2d loci  microvariantes en %2d loci\n",
              reg, length(f), sum(vapply(f, function(v) length(microvariants(v)) > 0, TRUE))))
}

cat("\n== forrel (Noruega) =========================================\n")
fn <- freqs_norwegian()
bases[["forrel:Noruega"]] <- fn
cat(sprintf("%-12s %2d loci  microvariantes en %2d loci\n", "Noruega", length(fn),
            sum(vapply(fn, function(v) length(microvariants(v)) > 0, TRUE))))

cat("\n== STRidER / ENFSI ==========================================\n")
cat("Fuente primaria que republica leapDNA (https://leapdna.org).\n")
fs <- tryCatch(freqs_strider("Entire Database"), error = function(e) {
  cat("No se pudo descargar:", conditionMessage(e), "\n"); NULL })
if (!is.null(fs)) {
  bases[["STRidER:Entire Database"]] <- fs
  cat(sprintf("%-12s %2d loci  microvariantes en %2d loci   N maximo = %s\n",
              "STRidER", length(fs),
              sum(vapply(fs, function(v) length(microvariants(v)) > 0, TRUE)),
              format(max(attr(fs, "N"), na.rm = TRUE))))
}

cat("\n== Control de calidad =======================================\n")
qc <- do.call(rbind, lapply(names(bases), function(nm) {
  d <- qc_freqs(bases[[nm]], minfreq = 1e-4); cbind(base = nm, d) }))
write.csv(qc, file.path(LRVAL_DIRS$salida, "qc_frecuencias_todas.csv"), row.names = FALSE)

resumen <- do.call(rbind, lapply(split(qc, qc$base), function(d) data.frame(
  base = d$base[1], loci = nrow(d),
  suman_1 = all(d$suma_ok),
  sin_duplicados = !any(d$duplicados),
  todas_positivas = all(d$no_negativos),
  nombres_no_STR = sum(d$no_str),
  loci_con_microvariantes = sum(d$n_microvar > 0),
  alelos_bajo_1e4 = sum(d$bajo_piso),
  freq_minima = min(d$min_freq), stringsAsFactors = FALSE)))
print(resumen, row.names = FALSE)

cat("\nDetalle escrito en out/resultados/qc_frecuencias_todas.csv\n")
cat("\nNota: un valor > 0 en 'nombres_no_STR' indica alelos cuyo nombre no es",
    "\nentero ni microvariante de un decimal; el modelo Stepwise no los admite.\n\n")

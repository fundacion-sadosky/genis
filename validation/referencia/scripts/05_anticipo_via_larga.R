#!/usr/bin/env Rscript
## ---------------------------------------------------------------------------
## Anticipo de la comparacion celda a celda, SIN COMPUTAR NINGUNA MATRIZ.
##
##   Rscript scripts/05_anticipo_via_larga.R
##
## verificacion_via_larga.csv guarda 80 pares (familia, POI) distintos con su LR
## calculado por la via larga, 40 por condicion y sobre los dos motores. Estan
## claveados por familia y poi, que son las claves de la entrega del sistema
## evaluado: se pueden comparar sin recalcular nada.
##
## Es una muestra, no el criterio de aceptacion --ese necesita las matrices
## completas, scripts/04-- pero ejercita el circuito entero antes de gastar CPU.
##
## Dos formas del archivo. check_mass_search() escribe su propia columna
## `escenario` (primera columna), y con ella la condicion viene declarada. Un
## archivo sin esa columna deja la condicion implicita
## en el orden del rbind: para ese caso este script la reconstruye cruzando
## (familia, tipo) contra el familias.csv de cada paquete. Esa clave NO
## desambigua siempre: hay 7 pares (familia, tipo) que existen en los dos
## paquetes a la vez (FAM014/hermanos_2, FAM016/abuelos_2_cruzados, FAM076/tio_1,
## FAM092/medios_hermanos_2, FAM095/padre_hermano, FAM116/medio_hermano_1,
## FAM124/hermanos_2); el stop() de mas abajo ("atribucion ambigua") protege
## ese caso.
##
## Por eso las columnas se leen POR NOMBRE y no con un vector de clases por
## posicion: con la columna `escenario` al frente, la quinta posicion es
## `motor` y no LR_tabla, y una lectura por posicion aborta.
## ---------------------------------------------------------------------------

source("load.R")

V <- file.path(LRVAL_DIRS$masiva, "verificacion_via_larga.csv")
if (!file.exists(V)) stop("falta ", V, ": correr antes scripts/02")
vl <- utils::read.csv(V, stringsAsFactors = FALSE, colClasses = "character")
for (col in c("LR_tabla", "LR_via_larga", "disc_rel")) {
  if (!col %in% names(vl)) stop("verificacion_via_larga.csv sin columna ", col)
  vl[[col]] <- as.numeric(vl[[col]])
}
if (!"escenario" %in% names(vl)) {
  cat("El archivo no declara escenario; se atribuye por (familia, tipo).\n")
  vl$escenario <- NA_character_
  for (cond in c("sin_mutacion", "con_mutacion")) {
    fa <- utils::read.csv(file.path(LRVAL_DIRS$masiva, "paquete", cond,
                                    "familias.csv"), colClasses = "character")
    hay <- paste(vl$familia, vl$tipo) %in% paste(fa$familia, fa$tipo)
    if (any(hay & !is.na(vl$escenario)))
      stop("atribucion ambigua: hay filas que encajan en las dos condiciones")
    vl$escenario[hay] <- cond
  }
  if (any(is.na(vl$escenario))) stop("filas sin atribuir a ninguna condicion")
}
vl$escenario <- sub(" ", "_", vl$escenario)

sal <- file.path(LRVAL_DIRS$masiva, "comparacion")
dir.create(sal, recursive = TRUE, showWarnings = FALSE)

filas <- do.call(rbind, lapply(unique(vl$escenario), function(cond) {
  ent <- file.path(LRVAL_DIRS$vuelta, "masiva", cond, "resultados_genis_masiva.csv")
  if (!file.exists(ent)) { cat("  (sin entrega para", cond, ")\n"); return(NULL) }
  g <- leer_masiva(ent)
  k <- paste(g$familia, g$poi)
  d <- vl[vl$escenario == cond, , drop = FALSE]
  j <- match(paste(d$familia, d$poi), k)
  if (any(is.na(j)))
    stop("hay pares de la via larga que no estan en la entrega de ", cond)
  lr_genis <- 10^g$log10LR[j]
  cmp <- lapply(seq_len(nrow(d)), function(i)
    compare_lr(lr_genis[i], d$LR_via_larga[i]))
  data.frame(escenario = cond, familia = d$familia, tipo = d$tipo, poi = d$poi,
             motor_ref = d$motor,
             LR_genis = lr_genis, LR_via_larga = d$LR_via_larga,
             rel = vapply(cmp, `[[`, 0, "rel"),
             veredicto = vapply(cmp, `[[`, "", "veredicto"),
             stringsAsFactors = FALSE)
}))

write.csv(filas, file.path(sal, "anticipo_via_larga.csv"), row.names = FALSE)

cat("\n== Anticipo: GENis contra la via larga de la referencia ==\n\n")
print(table(filas$escenario, filas$motor_ref, filas$veredicto))
cat("\nPeores celdas:\n")
print(utils::head(filas[order(-filas$rel), c("escenario","familia","tipo","poi",
                                             "motor_ref","LR_genis","LR_via_larga",
                                             "rel","veredicto")], 10),
      row.names = FALSE)
cat("\nSalida en", file.path(sal, "anticipo_via_larga.csv"), "\n\n")

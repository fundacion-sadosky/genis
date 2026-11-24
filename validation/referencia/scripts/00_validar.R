#!/usr/bin/env Rscript
## ---------------------------------------------------------------------------
## Corrida completa de la validacion. Se ejecuta desde validation/referencia/.
##
##   Rscript scripts/00_validar.R                 corrida estandar
##   Rscript scripts/00_validar.R --con-genis     incorpora ../out/resultados_*.csv
##
## Produce:
##   ../cases/        paquete de casos para cargar en el sistema a validar, con
##                    la grilla de matrices (grilla.csv / paneles.csv) que el
##                    sistema tiene que contestar volcando SUS matrices
##   out/resultados/  veredicto por caso y motor, resumen, diferencias de
##                    convencion, trazabilidad, el informe de la grilla (matrices.csv) y la
##                    descomposicion del error de cada caso con mutacion
##
## Con --con-genis se leen ademas, de ../out/ y bajo la misma verificacion de
## manifiesto que los resultados, matrices_genis.csv y contexto_matrices.csv:
## es lo que permite medir el nivel 2 sobre el sistema evaluado.
## ---------------------------------------------------------------------------

source("load.R")

args <- commandArgs(trailingOnly = TRUE)
con_genis <- "--con-genis" %in% args

cat("\n== 1. Catalogo ==============================================\n")
cs <- build_catalog()
cat("Casos definidos:", length(cs), "\n")
print(table(vapply(cs, `[[`, "", "grupo")))

cat("\n== 2. Control de la base de frecuencias ======================\n")
qc <- qc_freqs(freqs_mispitools("Argentina"), minfreq = 1e-4)
cat("Loci:", nrow(qc), " | todos suman 1:", all(qc$suma_ok),
    " | loci con microvariantes:", sum(qc$n_microvar > 0), "\n")
dir.create(LRVAL_DIRS$salida, recursive = TRUE, showWarnings = FALSE)
write.csv(qc, file.path(LRVAL_DIRS$salida, "qc_frecuencias.csv"), row.names = FALSE)

cat("\n== 3. Verificacion de los modelos de mutacion ================\n")
## Comprueba que pedmut y Familias generen la MISMA matriz para cada modelo
## con nombre. Es la validacion del modelo, previa a la del algoritmo.
cmp_mut <- compare_mutation_models()
print(cmp_mut)
write.csv(cmp_mut, file.path(LRVAL_DIRS$salida, "modelos_mutacion.csv"), row.names = FALSE)

## Condicion de Kemeny-Snell sobre las particiones que aparecen realmente en el
## catalogo: es lo que justifica agrupar los alelos no observados para acelerar
## el calculo, y el modelo Stepwise no la cumple. Con un volcado del sistema
## evaluado se repite sobre SUS matrices (mas abajo, paso 5b).
lump <- check_lumpability(cs)
if (!is.null(lump))
  cat("Agrupabilidad (Kemeny-Snell):", sum(lump$lumpable, na.rm = TRUE), "de",
      nrow(lump), "combinaciones caso x locus admiten agrupar los no observados\n")

cat("\n== 4. Exportacion del paquete ================================\n")
## Va ANTES de ejecutar el catalogo y de leer cualquier entrega externa, a
## proposito. results_package_ok() (paso 5, mas abajo) verifica una entrega
## contra el CONTENIDO ACTUAL de ../cases/. Si el paquete se exportara recien
## al final, esa verificacion correria contra el paquete VIEJO
## -que por definicion coincide con su propio manifiesto viejo- y una entrega
## que no corresponde al catalogo de hoy se incorporaria igual: exactamente
## el falso verde que existe para evitar. export_bundle() conserva la
## plantilla de resultados.csv si alguien ya la completo in situ, asi que
## exportar temprano no destruye esa entrada.
## Si el dia de manana esto se mueve, que sea con esto en mente.
##
## Esa misma plantilla no puede juzgarse contra el manifiesto que export_bundle()
## esta por escribir: describiria los archivos recien escritos y daria OK
## siempre, con lo cual una plantilla completada sobre el paquete VIEJO se
## incorporaria como si correspondiera al catalogo de hoy. Se guarda antes una
## copia del manifiesto anterior y es contra esa copia que se la juzga.
man_previo <- snapshot_manifest(LRVAL_DIRS$paquete)
export_bundle(cs, LRVAL_DIRS$paquete)
export_fam(cs, file.path(LRVAL_DIRS$paquete, "fam"))
## La grilla y los paneles que acaban de escribirse son tambien lo que consume
## la reimportacion: se reconstruyen una sola vez para que el pedido y su
## lectura no puedan divergir.
paneles <- mutation_panels(cs)
grilla  <- build_mutation_grid(cs, paneles)
cat("Grilla de matrices:", nrow(grilla), "filas sobre", length(paneles), "paneles\n")

cat("\n== 5. Ejecucion del catalogo =================================\n")
res <- run_catalog(cs, verbose = FALSE)

mats <- NULL; motor_mats <- NULL
if (con_genis) {
  ## El sistema evaluado devuelve su archivo aparte y no edita el del paquete:
  ## ../cases/ tiene que seguir coincidiendo con lo que se le entrego, hash
  ## por hash. Se admite igual la plantilla completada in situ por si alguien
  ## la cargo ahi.
  entradas <- sort(Sys.glob(file.path(LRVAL_DIRS$vuelta, "resultados*.csv")))
  if (results_template_filled(file.path(LRVAL_DIRS$paquete, "resultados.csv")))
    entradas <- c(entradas, file.path(LRVAL_DIRS$paquete, "resultados.csv"))
  if (!length(entradas)) {
    cat("Sin resultados externos: no hay ../out/resultados*.csv\n")
  } else for (f in entradas) {
    ## Los resultados valen para la exportacion que el sistema consumio. Si el
    ## paquete se reemitio despues, incorporarlos igual puede dar PASA sobre
    ## casos cuyo contenido cambio: la entrega no entra y se dice por que.
    in_situ <- identical(f, file.path(LRVAL_DIRS$paquete, "resultados.csv"))
    vg <- if (!in_situ) results_package_ok(f, LRVAL_DIRS$paquete)
          else if (is.null(man_previo))
            ## el paquete no tenia manifiesto antes de esta corrida: no hay
            ## contra que juzgar la plantilla, asi que no se incorpora
            list(ok = FALSE, motivo = "sin manifiesto previo a la reexportacion", vm = NULL)
          else results_package_ok(f, LRVAL_DIRS$paquete, manifiesto = man_previo)
    cat("Paquete consumido por ", basename(f), ": ", vg$motivo, "\n", sep = "")
    if (!is.null(vg$vm) && any(vg$vm$veredicto != "OK"))
      print(vg$vm[vg$vm$veredicto != "OK", c("archivo", "veredicto")], row.names = FALSE)
    if (!vg$ok) {
      cat("NO se incorpora: los resultados de ", basename(f),
          " no corresponden al paquete actual\n", sep = "")
      next
    }
    ext <- import_results(f, cs)
    if (!is.null(ext)) {
      res <- rbind(res, ext)
      cat("Resultados externos incorporados desde ", f, ": ", nrow(ext), " filas\n", sep = "")
    }
    ## Las matrices volcadas viajan por el mismo canal y con el mismo
    ## manifiesto, asi que entran bajo la misma verificacion: una matriz
    ## calculada sobre un paquete vencido no puede alimentar la descomposicion.
    fm <- file.path(dirname(f), "matrices_genis.csv")
    fc <- file.path(dirname(f), "contexto_matrices.csv")
    if (file.exists(fm)) {
      mats <- import_matrices(fm, if (file.exists(fc)) fc else NULL, grilla, cs)
      motor_mats <- if (!is.null(ext)) ext$motor[1] else NULL
      cat("Matrices volcadas incorporadas desde ", fm, ": ", length(mats),
          " matrices\n", sep = "")
      if (!file.exists(fc))
        cat("AVISO: sin contexto_matrices.csv no se puede verificar que el ",
            "universo volcado sea el que el sistema uso\n", sep = "")
    }
  }
}

cat("\n== 5a. Cobertura por motor ===================================\n")
cob <- coverage_report(res, cs)
print(cob, row.names = FALSE)

cat("\n== 5b. Nivel 2: modelo de mutacion del sistema evaluado =====\n")
## Se mide directamente sobre las matrices que el sistema volco, y no de rebote
## por la diferencia de LR entre pares de casos: un sistema que no puede
## recibir una matriz daria la misma corrida en los dos miembros del par y la
## diferencia seria cero por construccion.
gr_rep <- grid_report(grilla, paneles, mats)
cat(sprintf("Grilla: %d filas, %d con referencia publicada, %d volcadas por el sistema\n",
            nrow(grilla), sum(gr_rep$referencia != "propiedades"),
            sum(gr_rep$volcada)))
print(table(gr_rep$region, gr_rep$veredicto))

## Invariante de la corrida: toda parametrizacion que use algun caso tiene que
## tener fila en la grilla, porque el veredicto de modelo del caso NO se calcula
## en el caso sino que se busca ahi.
cob_gr <- grid_covers_cases(cs, grilla, paneles)
if (!is.null(cob_gr)) {
  falta <- cob_gr[!cob_gr$cubierto, , drop = FALSE]
  if (nrow(falta)) {
    cat("FALLA: parametrizaciones de caso sin fila en la grilla:\n")
    print(falta[, c("caso", "locus", "modelo", "tasa", "panel")], row.names = FALSE)
  } else cat(sprintf("Cobertura de la grilla: las %d parametrizaciones del catalogo tienen fila\n",
                     nrow(cob_gr)))
}
mod_caso <- model_verdicts(cs, gr_rep, grilla, paneles)

## Descomposicion: entre el LR del sistema y el valor de referencia cambian dos
## cosas a la vez. Con la matriz volcada la diferencia se parte en sumandos que
## se miden por separado.
dec <- mut_decomposition(res, cs, mats, motor_mats)
if (is.null(dec)) cat("Sin casos con mutacion en la corrida.\n") else {
  print(dec[, c("caso", "motor", "matriz", "semantica", "d_total", "d_algoritmo",
                "d_modelo", "d_semantica", "ver_algoritmo", "ver_conformidad")],
        row.names = FALSE, digits = 4)
}
sem <- semantics_coherence(dec)
if (!is.null(sem)) {
  cat("\nConvencion para el fundador no tipificado, detectada:\n")
  print(sem[, c("motor", "n_casos", "n_decididos", "semanticas", "veredicto")],
        row.names = FALSE)
  if (any(sem$veredicto == "FALLA"))
    for (i in which(sem$veredicto == "FALLA")) cat("  ", sem$detalle[i], "\n")
}
if (!is.null(mats)) {
  lump <- check_lumpability(cs, mats)
  cat("\nAgrupabilidad sobre las matrices volcadas:",
      sum(lump$origen == "volcada"), "combinaciones evaluadas\n")
}
res <- annotate_mutation(res, dec, mod_caso)

cat("\n== 5c. Piso de frecuencia declarado ==========================\n")
## Se mide en la corrida principal: el paquete entrega el panel crudo y el
## piso por separado, asi que aplicarlo es del sistema evaluado. El caso sin
## piso es el control, y la diferencia dentro del par mide la logica del piso.
pisos <- min_freq_pairs(res, cs)
if (is.null(pisos)) cat("Sin pares con piso declarado.\n") else
  print(pisos[, c("caso_piso", "caso_sin_piso", "motor", "piso",
                  "LR_piso", "LR_sin_piso", "diagnostico")], row.names = FALSE)

cat("\n== 6. Invariantes ============================================\n")
inv <- check_invariants(cs, res)
print(inv[, c("invariante", "veredicto")], row.names = FALSE)

cat("\n== 7. Acuerdo entre motores ==================================\n")
ag <- engine_agreement(res)
print(table(ag$veredicto))

cat("\n== 8. Resumen ================================================\n")
s <- summarise_run(res)
print(s$por_motor)
if (nrow(s$fallos)) { cat("\nFALLOS:\n"); print(s$fallos, row.names = FALSE) }
if (nrow(s$divergencias)) {
  cat("\nDIFERENCIAS DE CONVENCION:\n")
  print(unique(s$divergencias[, c("caso", "motor", "LR", "gold")]), row.names = FALSE)
}

cat("\n== 9. Resultados ================================================\n")
## ../cases/ ya se exporto en el paso 4; aca solo van los resultados, que
## necesitan res e inv (resultado de correr el catalogo e invariantes).
write_results(res, cs, inv, LRVAL_DIRS$salida,
              mut = list(grilla = grilla, paneles = paneles, informe = gr_rep,
                         descomposicion = dec, semantica = sem, cobertura = cob_gr,
                         modelo = mod_caso, lumpabilidad = lump))
## Sello de procedencia del veredicto: que entrega de GENis se juzgo (SHA-256
## y version) y contra que paquete (identidad del manifiesto). Lo lee
## scripts/06_informe.R para verificar que el veredicto sea de ESTA entrega y
## no de una anterior. Sin --con-genis el veredicto no incluye a GENis, y un
## sello viejo diria lo contrario: se borra.
sello_f <- file.path(LRVAL_DIRS$salida, "PROCEDENCIA.txt")
if (con_genis) {
  escribir_procedencia(sello_f, sello_bloque1(LRVAL_DIRS$vuelta, LRVAL_DIRS$paquete))
} else if (file.exists(sello_f)) {
  unlink(sello_f)
}

## Los casos MUT no declaran una diferencia esperada por poda de fundadores: la
## convencion para el fundador no tipificado se mide, asi que un motor que poda
## informa el veredicto que le corresponde por el valor y la lectura queda en
## la descomposicion.
## Un FALLA cuyo `ver_algoritmo` es PASA no es un error de calculo: el motor
## reproduce la formula cerrada sobre su propia matriz. Pero la causa no es
## siempre la misma, y el mensaje final tiene que separarlas -- si no, un
## error de modelo queda contado como diferencia de convencion.
atrib <- function(f) if (is.null(dec)) 0L else
  sum(dec$ver_conformidad == "FALLA" & dec$ver_algoritmo == "PASA" & f(dec), na.rm = TRUE)
nz <- function(x) !is.na(x) & abs(x) > LRVAL_TOL$log10
por_sem <- atrib(function(d) nz(d$d_semantica))
por_mod <- atrib(function(d) !nz(d$d_semantica))
cat("\nListo. Veredicto global:",
    if (any(res$veredicto == "FALLA")) "HAY FALLOS - revisar out/resultados/resultados.csv"
    else "sin fallos frente a los valores de referencia", "\n")
if (por_sem + por_mod > 0) {
  cat(sprintf("%d diferencias frente a la referencia no son error de calculo: el algoritmo reproduce la formula",
              por_sem + por_mod),
      "cerrada sobre su propia matriz (out/resultados/descomposicion.csv)\n")
  if (por_sem) cat(sprintf("  %d por la convencion para el fundador no tipificado\n", por_sem))
  if (por_mod) cat(sprintf("  %d por la matriz que el motor genero: error de modelo\n", por_mod))
}
cat("\n")

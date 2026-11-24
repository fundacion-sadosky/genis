#!/usr/bin/env Rscript
## ---------------------------------------------------------------------------
## Comparacion celda a celda de la busqueda masiva.
##
##   Rscript scripts/04_comparacion_masiva.R --condicion sin_mutacion --motor ambos
##   Rscript scripts/04_comparacion_masiva.R --condicion con_mutacion --motor pedsuite
##   Rscript scripts/04_comparacion_masiva.R --condicion X --solo-comparar
##
## NUNCA toca el paquete ni llama a export_masiva(): el paquete es la entrada
## contra la cual ya se calcularon las entregas.
##
## Con mutacion el veredicto va contra PEDSUITE, no contra Familias: Familias
## poda los fundadores no tipificados y pedsuite los conserva, y con mutacion
## las dos convenciones dan numeros distintos. Familias entra como diferencia
## de convencion declarada, no como veredicto.
## ---------------------------------------------------------------------------

source("load.R")

args <- commandArgs(trailingOnly = TRUE)

## Flags reconocidos, para no correr en silencio una condicion/motor
## equivocados por un typo (p.ej. --condicon en vez de --condicion).
FLAGS_VALIDOS <- c("--condicion", "--motor", "--hilos", "--solo-comparar")
no_reconocidos <- setdiff(grep("^--", args, value = TRUE), FLAGS_VALIDOS)
if (length(no_reconocidos))
  stop("flag no reconocido: ", paste(no_reconocidos, collapse = ", "),
       ". Validos: ", paste(FLAGS_VALIDOS, collapse = ", "))

val <- function(nombre, def = NULL) {
  i <- match(nombre, args)
  if (is.na(i)) return(def)
  ## el flag aparece pero no trae valor (es el ultimo token, o lo sigue otro
  ## flag): devolver el default en silencio correria otra cosa.
  if (i == length(args) || grepl("^--", args[i + 1]))
    stop("val: el flag ", nombre, " no trae valor")
  args[i + 1]
}
CONDICION <- val("--condicion", "sin_mutacion")
MOTOR     <- val("--motor", "ambos")
HILOS     <- as.integer(val("--hilos", max(1, parallel::detectCores() - 2)))
SOLO_CMP  <- "--solo-comparar" %in% args

MOTORES <- if (MOTOR == "ambos") MASIVA_ENGINES else MOTOR
stopifnot(all(MOTORES %in% MASIVA_ENGINES))

dir_paq <- file.path(LRVAL_DIRS$masiva, "paquete", CONDICION)
dir_ent <- file.path(LRVAL_DIRS$vuelta, "masiva", CONDICION)
dir_mat <- file.path(LRVAL_DIRS$masiva, "matrices")
dir_sal <- file.path(LRVAL_DIRS$masiva, "comparacion")
dir.create(dir_mat, recursive = TRUE, showWarnings = FALSE)
dir.create(dir_sal, recursive = TRUE, showWarnings = FALSE)

cat(sprintf("\n== Comparacion masiva: %s (motores: %s) ==\n\n",
            CONDICION, paste(MOTORES, collapse = ", ")))

## --- 1. El manifiesto -------------------------------------------------------
## Los resultados de un sistema externo valen para la exportacion concreta que
## ese sistema consumio.
man_ent <- file.path(dir_ent, "MANIFIESTO.txt")
man_paq <- file.path(dir_paq, "MANIFIESTO.txt")
if (!file.exists(man_ent))
  stop("la entrega no trae MANIFIESTO.txt: no se puede saber contra que paquete corrio")
if (!identical(readLines(man_ent), readLines(man_paq)))
  stop("el MANIFIESTO.txt de la entrega no coincide con el del paquete actual")
cat("  manifiesto ................. coincide\n")
## condiciones_masiva.txt es lo que deja la corrida cuando termina sola y
## escribe su propio MANIFIESTO.txt. Si falta, el MANIFIESTO.txt de la entrega
## pudo haber sido copiado a mano desde el paquete y entonces "coincide"
## arriba es tautologico, no una verificacion independiente.
manifiesto_independiente <- file.exists(file.path(dir_ent, "condiciones_masiva.txt"))
if (!manifiesto_independiente)
  cat("  ADVERTENCIA ................ no hay condiciones_masiva.txt en la entrega:",
      "el MANIFIESTO.txt pudo haber sido copiado a mano y esta verificacion",
      "no es independiente\n")

## --- 2. El escenario --------------------------------------------------------
t0 <- Sys.time()
esc <- escenario_masiva(CONDICION, dir_paq)
cat(sprintf("  escenario reconstruido ..... %5.1f s  (%d familias x %d POI)\n",
            as.numeric(difftime(Sys.time(), t0, units = "secs")),
            length(esc$fams), nrow(esc$pool$geno)))

## --- 3. La entrega ----------------------------------------------------------
M_genis <- import_masiva(file.path(dir_ent, "resultados_genis_masiva.csv"), esc)
co <- attr(M_genis, "cobertura")
cat(sprintf("  entrega .................... %d de %d familias aceptadas, %s celdas\n",
            co$aceptadas, co$familias, format(co$celdas_con_dato, big.mark = ".")))
if (length(co$incompletas))
  cat("    familias descartadas por incompletas (regla 4):",
      paste(co$incompletas, collapse = ", "), "\n")
write.csv(data.frame(condicion = CONDICION, familias = co$familias,
                     informadas = co$informadas, aceptadas = co$aceptadas,
                     no_informadas = co$no_informadas,
                     incompletas = paste(co$incompletas, collapse = ";"),
                     celdas_con_dato = co$celdas_con_dato,
                     manifiesto_independiente = manifiesto_independiente,
                     stringsAsFactors = FALSE),
          file.path(dir_sal, sprintf("cobertura_%s.csv", CONDICION)),
          row.names = FALSE)

## --- 4. Las matrices de referencia ------------------------------------------
M_ref <- list()
for (mo in MOTORES) {
  f <- file.path(dir_mat, sprintf("%s_%s.csv.gz", CONDICION, mo))
  if (file.exists(f)) {
    d <- leer_masiva(f)
    M <- matrix(NA_real_, length(esc$fams), nrow(esc$pool$geno),
                dimnames = dimnames(M_genis))
    M[cbind(match(d$familia, rownames(M)), match(d$poi, colnames(M)))] <- d$log10LR
    ## Una matriz de referencia COMPLETA no tiene celdas sin cubrir: cubre
    ## todas las familias x POI del escenario. Si faltan, la escritura quedo
    ## truncada o el escenario cambio desde que se cacheo, y dejarlo pasar en
    ## silencio daria SIN_DATO rio abajo sin nada que apunte a la causa real.
    faltan <- sum(is.na(M))
    if (faltan)
      stop(sprintf(paste("la matriz cacheada %s no cubre la grilla completa:",
                         "faltan %d de %d celdas. Borrala para recalcularla."),
                   f, faltan, length(M)))
    M_ref[[mo]] <- M
    cat(sprintf("  matriz %-9s ........... leida de disco\n", mo))
  } else if (SOLO_CMP) {
    stop("--solo-comparar y no existe ", f)
  } else {
    t0 <- Sys.time()
    M_ref[[mo]] <- mass_search(esc$fams, esc$pool, esc$freqs, engine = mo,
                               cores = HILOS)
    cat(sprintf("  matriz %-9s ........... %6.1f s (calculada)\n", mo,
                as.numeric(difftime(Sys.time(), t0, units = "secs"))))
    escribir_matriz_masiva(M_ref[[mo]], f)
  }
}

## Sin mutacion las dos matrices tienen que coincidir celda por celda: es el
## control de que la reconstruccion reprodujo la corrida original y no otra cosa
## parecida.
if (is.null(esc$mut) && length(M_ref) == 2) {
  a <- check_mass_engines(M_ref[["familias"]], M_ref[["pedsuite"]])
  cat(sprintf("  invariante entre motores ... %s (max dif log10 %.2e)\n",
              a$veredicto, a$max_dif_log10))
  if (a$veredicto != "PASA")
    stop("sin mutacion los dos motores tienen que coincidir; la reconstruccion no es fiel")
}

## --- 5. El veredicto --------------------------------------------------------
## Con mutacion, solo pedsuite emite veredicto (ver la cabecera).
## OJO: las llaves NO son estilo. A nivel de script, `x <- if (c) a` es una
## sentencia completa y un `else` en la linea siguiente es un error de sintaxis.
## Adentro de una funcion R sigue leyendo y no pasa; aca no.
motores_veredicto <- if (is.null(esc$mut)) {
  intersect("familias", names(M_ref))
} else {
  intersect("pedsuite", names(M_ref))
}
if (!length(motores_veredicto))
  cat("\n  (sin motor de veredicto para esta condicion: falta su matriz)\n")

glo <- fam <- tip <- cel <- NULL
for (mo in motores_veredicto) {
  cmp <- comparar_celdas(M_genis, M_ref[[mo]])
  ac  <- acuerdo_masiva(cmp, M_genis, M_ref[[mo]], esc, motor = mo)
  glo <- rbind(glo, ac$global); fam <- rbind(fam, ac$por_familia)
  tip <- rbind(tip, ac$por_tipo)
  if (nrow(cmp$celdas))
    cel <- rbind(cel, data.frame(condicion = CONDICION, motor = mo, cmp$celdas,
                                 stringsAsFactors = FALSE))
  cat(sprintf("\n  VEREDICTO contra %s: %s\n", mo,
              paste(sprintf("%s=%d", names(cmp$conteo), as.integer(cmp$conteo)),
                    collapse = "  ")))
}
if (!is.null(glo)) print(glo, row.names = FALSE)

## El borrado de filas viejas va SIEMPRE, este o no `d` vacio: si una
## re-corrida de (CONDICION, motor) pasa de tener filas a cero (p.ej.
## celdas_discrepantes.csv cuando ya no hay ninguna FALLA/REVISAR/SIN_DATO),
## las filas viejas de esa combinacion tienen que desaparecer y no quedar
## como vigentes. Por eso `motores` es un argumento explicito y no se deriva de
## `d$motor` -que no existe cuando `d` es NULL-.
anexar <- function(d, f, motores) {
  ruta <- file.path(dir_sal, f)
  v <- if (file.exists(ruta)) {
    v0 <- utils::read.csv(ruta, stringsAsFactors = FALSE)
    v0[!(v0$condicion == CONDICION & v0$motor %in% motores), , drop = FALSE]
  } else NULL
  out <- rbind(v, d)
  if (is.null(out)) return(invisible(NULL))
  write.csv(out, ruta, row.names = FALSE)
}
anexar(glo, "acuerdo_celdas.csv", motores_veredicto)
anexar(fam, "acuerdo_por_familia.csv", motores_veredicto)
anexar(tip, "acuerdo_por_tipo.csv", motores_veredicto)
anexar(cel, "celdas_discrepantes.csv", motores_veredicto)
## Sello de procedencia del veredicto de esta condicion: que entrega de GENis
## se juzgo (su linea `arrancada`, unica por corrida, y el SHA-256 del archivo)
## y contra que paquete. Lo lee scripts/06_informe.R: sin el, las dos filas de
## acuerdo_celdas.csv pueden ser de corridas distintas sin que nada lo diga.
escribir_procedencia(file.path(dir_sal, sprintf("PROCEDENCIA_%s.txt", CONDICION)),
                     sello_comparacion(CONDICION, dir_ent, dir_paq, motores_veredicto))

## --- 6. La diferencia con Familias a escala ---------------------------------
if (!is.null(esc$mut) && !is.null(M_ref[["familias"]])) {
  dv <- engine_divergence(M_genis, M_ref[["familias"]], esc$fams, CONDICION,
                          motor_a = "genis", motor_b = "familias")
  write.csv(dv, file.path(dir_sal, "divergencia_genis_familias.csv"),
            row.names = FALSE)
  cat("\n  Diferencia GENis vs Familias (convencion de poda), por tipo:\n")
  print(dv[order(-dv$maximo), c("tipo","mediana","p99","maximo","cruzan_umbral")],
        row.names = FALSE)
}

## --- 7. Caracterizacion operativa CONJUNTA ----------------------------------
## No hay tabla nueva: resultados_por_familia y metricas_por_tipo ya tienen
## columna `motor`, y GENis entra como un valor mas. Las tablas no cambian de
## forma, solo crecen en filas.
todas <- c(M_ref, list(genis = M_genis))
op_f <- do.call(rbind, lapply(names(todas), function(mo)
  data.frame(condicion = CONDICION, motor = mo,
             family_results(todas[[mo]], esc$fams, esc$pool),
             stringsAsFactors = FALSE)))
op_t <- do.call(rbind, lapply(names(todas), function(mo)
  data.frame(condicion = CONDICION, motor = mo,
             search_metrics(todas[[mo]], esc$fams, esc$pool,
                            ESCENARIO_MASIVA$umbrales),
             stringsAsFactors = FALSE)))
op_g <- do.call(rbind, lapply(names(todas), function(mo)
  data.frame(condicion = CONDICION, motor = mo,
             search_summary(todas[[mo]], esc$fams, esc$pool,
                            ESCENARIO_MASIVA$umbrales),
             stringsAsFactors = FALSE)))
anexar(op_f, "operativa_por_familia.csv", names(todas))
anexar(op_t, "operativa_por_tipo.csv", names(todas))
anexar(op_g, "operativa_global.csv", names(todas))

cat("\n  Deteccion y falsos positivos, las tres herramientas juntas:\n")
print(op_g[, c("motor","umbral_log10LR","detectados","deteccion",
               "falsos_positivos","tasa_falsos_positivos")], row.names = FALSE)
cat("\nSalidas en", dir_sal, "\n\n")

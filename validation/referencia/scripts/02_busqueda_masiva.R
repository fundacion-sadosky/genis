#!/usr/bin/env Rscript
## ---------------------------------------------------------------------------
## Escenario de busqueda masiva: 5000 POI contra un conjunto de familias de
## personas desaparecidas, con configuraciones de pedigri variadas.
##
##   Rscript scripts/02_busqueda_masiva.R [n_poi] [n_familias]
##   Rscript scripts/02_busqueda_masiva.R [n_poi] [n_familias] --solo-pedsuite
##   Rscript scripts/02_busqueda_masiva.R [n_poi] [n_familias] --con-familias-mutacion
##
## El escenario corre sobre los dos motores de MASIVA_ENGINES: Familias como
## motor primario (es el que se contrasta con GENis) y pedsuite como control
## interno. --solo-pedsuite deja solo pedsuite en LAS DOS condiciones, para
## iterar rapido.
##
## POR OMISION FAMILIAS NO CORRE EN LA CONDICION CON MUTACION, y hay que pedirlo
## con --con-familias-mutacion. Es el paso mas caro de toda la validacion --~192 h
## de nucleo, 39,6 h de reloj-- y no emite veredicto: con mutacion el veredicto
## va contra pedsuite. Lo unico que alimenta es la medida a escala de la
## diferencia de convencion entre Familias y pedsuite. Con el default, el lado
## R del bloque 2 pasa de ~40 h a ~35 min.
## Ver motores_de_escenario() en R/masiva_engines.R.
##
## Lo que una corrida sin Familias con mutacion NO vuelve a medir, y por eso se
## avisa por pantalla al final de la seccion 3: la fila `con mutacion` de
## acuerdo_motores.csv y divergencia_motores.csv --que se PRESERVAN de la corrida
## anterior en vez de borrarse-- y las filas de motor `familias` de
## resultados_por_familia.csv, metricas_por_tipo.csv y resumen_con_mutacion.csv,
## que si se pierden. Respaldar out/masiva/ entero antes de correr.
##
## Se corre en dos condiciones:
##   sin mutacion  screening estricto: un locus discordante excluye
##   con mutacion  la parametrizacion MUTACION_MASIVA: Stepwise sin truncar, tasas
##                 por sexo 0.001584 (M) / 0.000463 (F), rango 0.5, tasa de
##                 microvariante 0 y pasos 22 --que excede la escalera mas
##                 ancha del panel, de modo que no trunca en ningun locus.
##
## OJO: con tasa2 = 0 toda transicion entre un alelo entero y una microvariante
## es un cero ESTRUCTURAL, asi que este modelo SI produce exclusiones duras: su
## matriz no es estrictamente positiva. Medido sobre THO1: 32 de las 100 celdas
## valen exactamente 0, y P(9.3 -> 10) = 0. En este escenario las familias se
## simulan con la misma matriz con la que despues se calcula, asi que ningun
## pariente verdadero simulado necesita una transmision entre clases y los
## ceros no excluyen a los desaparecidos plantados en la nomina.
##
## LAS DOS CONDICIONES NO CORREN A LA MISMA ESCALA, y no es un descuido.
## "Sin mutacion" va completa --N_FAM familias con el tipo sorteado
## uniformemente-- porque cuesta minutos y porque es la que lleva el invariante
## de acuerdo celda por celda entre los dos motores. "Con mutacion" corre sobre
## la asignacion estratificada que se declara mas abajo, porque con Stepwise el
## costo no se reparte parejo: se concentra en los 4 tipos con tios, donde
## el desaparecido esta separado de todo pariente tipificado por dos fundadores
## no tipificados. Medido con scripts/03_costo_mutacion.R, sobre el motor
## primario y una familia por tipo. Las tres filas son SUMAS DEL GRUPO sobre el
## dominio COMPLETO del panel, tal como salen de la sonda:
##
##   progenitores + mixto + abuelos + medios hermanos (13 tipos)    86 s (suma)
##   hermanos (3 tipos)                                            450 s (suma)
##   tios (4 tipos)                                              3.600 s (suma)
##                                    -- las 4 celdas TOPEARON en 900 s cada una
##
## Dos correcciones que la tabla NO trae y que hay que aplicar antes de usarla:
##
##  (a) DOMINIO. El costo de Familias es el LARGO DEL BARRIDO, y el barrido dura
##      el MAXIMO de genotipos distintos entre los loci, no la suma sobre ellos:
##      cada una de las K iteraciones resuelve los 24 loci de una sola llamada
##      (K <- max(n_genos) en .lr_table_familias(), R/masiva_engines.R). La
##      sonda barre el panel completo, 528 genotipos en el locus mas rico; la
##      condicion CON MUTACION tal como corre --180 familias, asignacion 5/10,
##      5000 POI, semillas 2026 y 7-- toca 294 (x0,557) y la SIN MUTACION --200
##      familias, sorteo uniforme, 5000 POI-- toca 290 (x0,549). O sea que las
##      cifras de Familias que da la sonda son ~1,8x el costo real. Las de
##      pedsuite no dependen del dominio.
##  (b) TIOS. Los 3.600 s son cuatro topes sumados: la sonda solo establece
##      >= 900 s por tipo. El piso real POR FAMILIA sale de una medicion aparte
##      de tio_1, sobre el dominio real y sin tope, detenida a 1 h 49 min sin
##      terminar: >= 6.540 s por familia, >= 7,3x el tope de 900 s por celda.
##      Esta medido en UN tipo y EXTENDIDO a los cuatro --comparten la topologia
##      que los vuelve caros--, no son cuatro mediciones, y el presupuesto de
##      aca abajo descansa sobre esa extension. OJO: este piso NO lleva la
##      correccion (a). Ya esta medido sobre el dominio real, y es el ~98% del
##      presupuesto, asi que las horas de aca abajo no dependen del factor.
##
## Con sorteo uniforme caen en tipos con tios unas 40 de las 200 familias en
## promedio (47 con la semilla 2026) y se llevan casi todo el presupuesto:
## 12,7 h de reloj sobre 6 nucleos, contra 6,4 h con la asignacion declarada.
##
## Que las dos condiciones sigan siendo comparables pese a la diferencia de
## escala es responsabilidad de search_summary(), que promedia las tasas por
## tipo sin ponderar en vez de acumular todas las familias juntas.
##
## Sin mutacion la poda de fundadores no tipificados de Familias es exacta,
## asi que los dos motores tienen que coincidir celda por celda; si no
## coinciden el escenario se detiene (seria un error de calculo, no una
## diferencia de convencion). Con mutacion la poda cambia legitimamente el LR:
## esa diferencia se mide y se informa, no se oculta.
##
## Salidas en out/masiva/. La condicion sin mutacion (5000 x 200, sobre los dos
## motores) tarda varios minutos, con Familias como el costo dominante; la
## condicion con mutacion tarda horas incluso a la escala reducida.
## ---------------------------------------------------------------------------

source("load.R")

args     <- commandArgs(trailingOnly = TRUE)

## Banderas reconocidas. Sin esta comprobacion un typo se ignora en silencio
## --`args[!grepl("^--", args)]` los descarta a todos-- y aca eso decide una
## corrida de 39,6 h: escribir --con-familias-mutacio (sin la n final) daria una
## corrida sin Familias creyendo haberla pedido, o al reves. Mismo criterio que
## FLAGS_VALIDOS de scripts/04_comparacion_masiva.R.
FLAGS_VALIDOS <- c("--solo-pedsuite", "--con-familias-mutacion")
no_reconocidos <- setdiff(grep("^--", args, value = TRUE), FLAGS_VALIDOS)
if (length(no_reconocidos))
  stop("bandera no reconocida: ", paste(no_reconocidos, collapse = ", "),
       "\n  validas: ", paste(FLAGS_VALIDOS, collapse = ", "))

SOLO_PED <- "--solo-pedsuite" %in% args      # corrida rapida para iterar
## Paga las ~39,6 h de Familias con mutacion. Ver la cabecera y
## motores_de_escenario() en R/masiva_engines.R.
CON_FAM_MUT <- "--con-familias-mutacion" %in% args
args     <- args[!grepl("^--", args)]
N_FAM_PLENO <- ESCENARIO_MASIVA$n_fam_pleno   # escala de la corrida completa
N_POI    <- if (length(args) >= 1) as.integer(args[1]) else ESCENARIO_MASIVA$n_poi
N_FAM    <- if (length(args) >= 2) as.integer(args[2]) else N_FAM_PLENO
FRAC_VER <- ESCENARIO_MASIVA$frac_ver
UMBRALES <- ESCENARIO_MASIVA$umbrales
NUCLEOS  <- max(1, parallel::detectCores() - 2)
## Motores HABILITADOS para la corrida entera. Cuales corren de verdad se
## decide despues, por condicion, con motores_de_escenario(): el motor primario
## no es uno solo para toda la corrida.
MOTORES  <- if (SOLO_PED) "pedsuite" else MASIVA_ENGINES

## Asignacion de la condicion con mutacion, a escala plena: 5 familias por cada
## tipo con tios --los cuatro que Stepwise vuelve prohibitivos, ver cabecera--
## y 10 por cada uno de los otros dieciseis, es decir 180 familias. Se deriva
## de PEDIGREE_TYPES por `grado` y no escribiendo los tipos a mano: si algun
## dia se agrega una configuracion con tios, la asignacion la cubre sola.
ES_TIOS <- vapply(PEDIGREE_TYPES, function(t) identical(t$grado, "tios"), TRUE)
## En una corrida corta (n_familias por linea de comandos) se reduce en la
## misma proporcion, con piso de una familia por tipo. Sin esto, pedir 20
## familias para iterar correria igual las 180 y la corrida "corta" duraria lo
## mismo que la completa.
ASIGNACION_MUT <- asignacion_masiva(N_FAM)

## Describe cuantas familias le tocan a cada tipo de un grupo SIN afirmar que el
## grupo sea uniforme: hoy lo es, pero imprimir la primera celda como si valiera
## para todo el grupo seria una media verdad el dia que la asignacion deje de
## repartirse pareja adentro de un grado.
.por_tipo <- function(v) {
  if (length(unique(v)) == 1L) format(v[[1]])
  else sprintf("%s a %s", format(min(v)), format(max(v)))
}

dir.create(LRVAL_DIRS$masiva, recursive = TRUE, showWarnings = FALSE)

cat("\n== 1. Escenario =============================================\n")
freqs <- freqs_mispitools("Argentina")
cat(sprintf("Panel        : %d loci (base argentina)\n", length(freqs)))
cat(sprintf("Familias MP  : %d sin mutacion (sorteo uniforme del tipo)\n", N_FAM))
cat(sprintf(paste("               %d con mutacion (asignacion estratificada:",
                  "%s por cada uno de los %d tipos con tios, %s por cada uno",
                  "de los otros %d)\n"),
            sum(ASIGNACION_MUT), .por_tipo(ASIGNACION_MUT[ES_TIOS]), sum(ES_TIOS),
            .por_tipo(ASIGNACION_MUT[!ES_TIOS]), sum(!ES_TIOS)))
cat(sprintf("POI          : %d  (el %g%% de las familias tiene su desaparecido en la nomina)\n",
            N_POI, 100 * FRAC_VER))
cat(sprintf("Comparaciones: %s sin mutacion / %s con mutacion\n",
            formatC(as.numeric(N_FAM) * N_POI, format = "d", big.mark = ".",
                    decimal.mark = ","),
            formatC(sum(ASIGNACION_MUT) * as.numeric(N_POI), format = "d",
                    big.mark = ".", decimal.mark = ",")))
cat(sprintf("Motores      : %s sin mutacion / %s con mutacion\n",
            paste(motores_de_escenario(MOTORES, FALSE), collapse = ", "),
            paste(motores_de_escenario(MOTORES, TRUE, CON_FAM_MUT), collapse = ", ")))
cat(sprintf("Nucleos      : %d\n", NUCLEOS))

cat("\n== 2. Configuraciones de pedigri ============================\n")
tt <- pedigree_types_table()
print(tt[, c("tipo", "grado", "n_tipificados", "tipificados")], row.names = FALSE)
write.csv(tt, file.path(LRVAL_DIRS$masiva, "tipos_pedigri.csv"), row.names = FALSE)

## verificacion estructural: los coeficientes kappa de cada pariente tipificado
## respecto del desaparecido deben ser los que corresponden a la relacion
cat("\nVerificacion de kappa con ribd:\n")
kap <- do.call(rbind, lapply(names(PEDIGREE_TYPES), function(nm) {
  t <- PEDIGREE_TYPES[[nm]]
  do.call(rbind, lapply(t$typed, function(i) {
    k <- ribd::kappaIBD(t$ped, ids = c("MP", i))
    data.frame(tipo = nm, pariente = i, k0 = k[1], k1 = k[2], k2 = k[3],
               stringsAsFactors = FALSE)
  }))
}))
kap$relacion <- with(kap, ifelse(k1 == 1, "progenitor/hijo",
                          ifelse(k2 > 0, "hermano completo", "segundo grado")))
print(table(kap$relacion))
write.csv(kap, file.path(LRVAL_DIRS$masiva, "kappa_por_tipo.csv"), row.names = FALSE)

## ---------------------------------------------------------------------------
## `asignacion = NULL` sortea el tipo de las N_FAM familias uniformemente; un
## vector nombrado declara cuantas familias de cada tipo y fija la escala de la
## condicion por su suma, de modo que N_FAM no interviene.
correr <- function(etiqueta, mut, asignacion = NULL) {
  cat(sprintf("\n== Escenario '%s' ==\n", etiqueta))
  ## Los motores se deciden POR CONDICION, no una vez para toda la corrida:
  ## Familias es el motor de veredicto sin mutacion (90 s) y es prescindible con
  ## mutacion (39,6 h). Ver motores_de_escenario() en R/masiva_engines.R.
  motores <- motores_de_escenario(MOTORES, con_mutacion = !is.null(mut),
                                  con_familias_mut = CON_FAM_MUT)
  cat(sprintf("  motores ..................... %s\n", paste(motores, collapse = ", ")))
  if (!is.null(mut) && !("familias" %in% motores))
    cat("                                (Familias omitida: ~39,6 h; ",
        "--con-familias-mutacion la agrega)\n", sep = "")
  t0 <- Sys.time()
  fams <- if (is.null(asignacion))
    simulate_mp_families(N_FAM, freqs, seed = ESCENARIO_MASIVA$semilla_fam, mut = mut)
  else
    simulate_mp_families(freqs = freqs, seed = ESCENARIO_MASIVA$semilla_fam, mut = mut,
                         asignacion = asignacion)
  ## la fraccion con verdad de referencia es la misma en las dos condiciones,
  ## y se toma sobre las familias que efectivamente se simularon
  n_ver <- max(1, round(length(fams) * FRAC_VER))
  cat(sprintf("  familias simuladas .......... %5.1f s  (%d familias, %d con verdad)\n",
              as.numeric(difftime(Sys.time(), t0, units = "secs")),
              length(fams), n_ver))
  t0 <- Sys.time()
  pool <- build_poi_pool(N_POI, fams, freqs, n_verdaderos = n_ver, seed = ESCENARIO_MASIVA$semilla_poi)
  cat(sprintf("  nomina de POI ............... %5.1f s\n",
              as.numeric(difftime(Sys.time(), t0, units = "secs"))))

  M <- chk <- list()
  for (mo in motores) {
    t0 <- Sys.time()
    M[[mo]] <- mass_search(fams, pool, freqs, engine = mo, cores = NUCLEOS)
    cat(sprintf("  %s comparaciones [%-9s] %6.1f s\n", format(length(M[[mo]]),
                big.mark = "."), mo, as.numeric(difftime(Sys.time(), t0, units = "secs"))))
    ## La matriz es el resultado caro de la corrida y el unico insumo con el que
    ## despues se compara celda a celda contra el sistema evaluado (scripts/04).
    ## Se escribe a disco apenas se calcula: si quedara solo en RAM se perderia
    ## al terminar el proceso.
    dir.create(file.path(LRVAL_DIRS$masiva, "matrices"), recursive = TRUE,
               showWarnings = FALSE)
    escribir_matriz_masiva(
      M[[mo]], file.path(LRVAL_DIRS$masiva, "matrices",
                         sprintf("%s_%s.csv.gz", gsub(" ", "_", etiqueta), mo)))
    chk[[mo]] <- check_mass_search(M[[mo]], fams, pool, freqs, engine = mo, n = 40,
                                   escenario = etiqueta)
    cat(sprintf("    verificacion contra su via larga: %d/%d PASA (max %.2e)\n",
                sum(chk[[mo]]$veredicto == "PASA"), nrow(chk[[mo]]),
                max(chk[[mo]]$disc_rel[is.finite(chk[[mo]]$disc_rel)], na.rm = TRUE)))
  }
  list(fams = fams, pool = pool, M = M, chk = chk, etiqueta = etiqueta,
       mut = mut, motores = motores, primario = motores[1])
}

r_sin <- correr("sin mutacion", NULL)
r_con <- correr("con mutacion", MUTACION_MASIVA, ASIGNACION_MUT)

## ---------------------------------------------------------------------------
cat("\n== 3. Acuerdo entre motores ==================================\n")

## El acuerdo cruzado solo se puede medir en las condiciones donde corrieron LOS
## DOS motores. Los motores se deciden por condicion, asi que no alcanza con
## mirar `length(MOTORES) > 1`: se filtra condicion por condicion. Con el
## default eso deja solo `sin mutacion`.
comparables    <- condiciones_comparables(list(r_sin, r_con))
etiquetas_comp <- vapply(comparables, `[[`, "", "etiqueta")
div <- acu <- NULL
if (length(comparables)) {
  ## Sin mutacion la poda de Familias es exacta: las dos matrices tienen que
  ## coincidir celda por celda. Es el contraste cruzado mas grande del set.
  acu <- do.call(rbind, lapply(comparables, function(r) {
    a <- check_mass_engines(r$M[["familias"]], r$M[["pedsuite"]])
    data.frame(escenario = r$etiqueta, a, exigido = is.null(r$mut),
               stringsAsFactors = FALSE)
  }))
  ## Sin mutacion la diferencia tiene que caer dentro de tolerancia (PASA) o el
  ## escenario se detiene mas abajo. Con mutacion la diferencia esperada es la
  ## poda de fundadores no tipificados, una diferencia de convencion
  ## documentada y no un fallo: el veredicto por tolerancia no
  ## aplica ahi, asi que se reclasifica a CONVENCION en vez de FALLA/REVISAR.
  acu$veredicto <- ifelse(!acu$exigido & acu$veredicto != "PASA",
                           "CONVENCION", acu$veredicto)
  print(acu, row.names = FALSE)
  if (any(acu$exigido & acu$veredicto != "PASA"))
    stop("sin mutacion los motores tienen que coincidir; revisar antes de seguir")
  div <- do.call(rbind, lapply(comparables, function(r)
    engine_divergence(r$M[["familias"]], r$M[["pedsuite"]], r$fams, r$etiqueta)))
}

## Las condiciones que ESTA corrida no comparo conservan sus filas anteriores en
## vez de perderlas. Con write.csv, una corrida sin Familias con mutacion --el
## default-- borraria la fila `con mutacion`: 900.000 pares y 39,6 h de computo,
## terminando bien y sin avisar nada. Ver anexar_por_escenario() y
## sellar_procedencia() en R/resultados.R: el sello de fecha y commit es lo que
## despues distingue una fila preservada de una recien medida.
anexar_por_escenario(sellar_procedencia(acu),
                     file.path(LRVAL_DIRS$masiva, "acuerdo_motores.csv"),
                     etiquetas_comp)
anexar_por_escenario(sellar_procedencia(div),
                     file.path(LRVAL_DIRS$masiva, "divergencia_motores.csv"),
                     etiquetas_comp)

if (!is.null(div) && any(div$escenario == "con mutacion")) {
  cat("\nDivergencia con mutacion, por tipo (log10 LR):\n")
  print(div[div$escenario == "con mutacion",
            c("tipo", "mediana", "p99", "maximo", "cruzan_umbral")], row.names = FALSE)
}

## Que esta corrida NO volvio a medir. Se dice fuerte y por nombre de archivo:
## el modo de falla de omitir Familias es justamente que no se note.
if (!("con mutacion" %in% etiquetas_comp)) {
  cat("\n  Familias NO corrio en la condicion con mutacion (~39,6 h ahorradas).\n")
  cat("  PRESERVADO de la corrida anterior, con su sello de procedencia:\n")
  cat("    acuerdo_motores.csv, divergencia_motores.csv   (fila `con mutacion`)\n")
  cat("  NO se vuelve a medir, y las filas de motor `familias` se PIERDEN en:\n")
  cat("    resultados_por_familia.csv, metricas_por_tipo.csv, resumen_con_mutacion.csv\n")
  cat("  Para volver a medirlo: --con-familias-mutacion\n")
}

## ---------------------------------------------------------------------------
cat("\n== 4. Deteccion y poder por tipo, por motor =================\n")
## deteccion y falsos positivos, por escenario y motor
for (r in list(r_sin, r_con)) {
  cat(sprintf("\n--- %s ---\n", r$etiqueta))
  s <- do.call(rbind, lapply(r$motores, function(mo)
    data.frame(motor = mo, search_summary(r$M[[mo]], r$fams, r$pool, UMBRALES),
               stringsAsFactors = FALSE)))
  print(s[, c("motor", "umbral_log10LR", "detectados", "deteccion",
              "falsos_positivos", "tasa_falsos_positivos")], row.names = FALSE)
  ## Las dos clases de columna no se leen igual y se imprimen juntas, asi que la
  ## leyenda no es decorativa: `detectados` y `falsos_positivos` son totales
  ## crudos de la corrida, mientras que `deteccion` y `tasa_falsos_positivos`
  ## promedian los tipos de pedigri SIN PONDERAR (search_summary()). O sea que
  ## deteccion NO es detectados / de_un_total_de, y solo coincide con ese
  ## cociente si la asignacion de tipos es pareja -- que es justo lo que la
  ## condicion con mutacion no es.
  cat("  detectados y falsos_positivos: totales crudos de la corrida.\n")
  cat("  deteccion y tasa_falsos_positivos: promedio por tipo de pedigri, sin ponderar\n")
  cat("  (deteccion no es detectados / de_un_total_de).\n")
  write.csv(s, sprintf(file.path(LRVAL_DIRS$masiva, "resumen_%s.csv"), gsub(" ", "_", r$etiqueta)),
            row.names = FALSE)
}

## poder por tipo, escenario con mutacion, motor primario primero
met <- do.call(rbind, lapply(r_con$motores, function(mo)
  data.frame(motor = mo, search_metrics(r_con$M[[mo]], r_con$fams, r_con$pool, UMBRALES),
             stringsAsFactors = FALSE)))
met <- met[order(-met$mediana_log10LR_verdadero), ]
write.csv(met, file.path(LRVAL_DIRS$masiva, "metricas_por_tipo.csv"), row.names = FALSE)

fr <- do.call(rbind, lapply(r_con$motores, function(mo)
  data.frame(motor = mo, family_results(r_con$M[[mo]], r_con$fams, r_con$pool),
             stringsAsFactors = FALSE)))
write.csv(fr, file.path(LRVAL_DIRS$masiva, "resultados_por_familia.csv"), row.names = FALSE)

write.csv(do.call(rbind, c(r_sin$chk, r_con$chk)),
          file.path(LRVAL_DIRS$masiva, "verificacion_via_larga.csv"), row.names = FALSE)


cat("\nResultados en out/masiva/\n")


cat("\n== 5. Paquete de intercambio ================================\n")
for (r in list(r_sin, r_con)) {
  d <- file.path(LRVAL_DIRS$masiva, "paquete", gsub(" ", "_", r$etiqueta))
  export_masiva(r$fams, r$pool, freqs, d, escenario = r$etiqueta, mut = r$mut)
  ce <- check_export_masiva(d, r$fams, r$pool, freqs)
  cat(sprintf("  %-14s %d de %d verificaciones PASA\n", r$etiqueta,
              sum(ce$veredicto == "PASA"), nrow(ce)))
  if (any(ce$veredicto != "PASA")) print(ce[ce$veredicto != "PASA", ], row.names = FALSE)
}

cat("\nListo.\n\n")

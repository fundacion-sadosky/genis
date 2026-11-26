## ---------------------------------------------------------------------------
## lrval :: el informe de la corrida
##
## Junta lo que ya esta en disco --el veredicto del catalogo, la entrega de
## GENis, la comparacion masiva-- y lo presenta en dos niveles para alguien que
## corre la validacion sin haberla disenado:
##
##   out/VEREDICTO.txt   el nivel corto; es lo que scripts/06 imprime
##   out/INFORME.md      el nivel largo, con el detalle por caso y por tipo
##
## No recalcula nada. Toda cifra sale de los archivos de la corrida; la unica
## prosa que no sale de ahi es la de hallazgos_genis.csv, que explica las
## FALLA ya analizadas y se cruza con los veredictos en los dos sentidos. De
## esa tabla se imprimen el titulo y la explicacion; las columnas `codigo` y
## `documento` no se imprimen.
## ---------------------------------------------------------------------------

#' Lee la tabla hallazgos_genis.csv
#' @export
leer_hallazgos <- function(ruta = LRVAL_DIRS$hallazgos) {
  if (!file.exists(ruta)) stop("leer_hallazgos: no existe ", ruta)
  h <- utils::read.csv(ruta, colClasses = "character", encoding = "UTF-8")
  falta <- setdiff(c("ambito", "clave", "codigo", "esperado", "titulo", "explicacion", "documento"),
                   names(h))
  if (length(falta))
    stop("leer_hallazgos: faltan columnas en ", ruta, ": ", paste(falta, collapse = ", "))
  h
}

#' Cruza los veredictos de GENis con hallazgos_genis.csv, en los dos sentidos
#'
#' @param res filas de GENis de resultados.csv: al menos caso, veredicto, estado
#' @param hallazgos tabla de `leer_hallazgos()`
#' @return `res` mas `codigo` y `situacion`:
#'   conocido              FALLA/REVISAR con fila en la tabla
#'   no_previsto           FALLA/REVISAR sin fila: hay que mirarla
#'   declarado             no calculado (no_representable/no_soportado/error) con fila
#'   sin_explicacion       no calculado sin fila
#'   posiblemente_cerrado  PASA con una fila que esperaba otra cosa: sacarla de la tabla
#'   sin_referencia        SIN_DATO con estado ok: el caso no tiene valor de referencia
#'   ok                    PASA sin fila
#' @export
cruzar_hallazgos <- function(res, hallazgos) {
  h <- hallazgos[hallazgos$ambito == "bloque1", , drop = FALSE]
  i <- match(res$caso, h$clave)
  codigo <- ifelse(is.na(i), "", h$codigo[i])
  titulo <- ifelse(is.na(i), "", h$titulo[i])
  esperado <- ifelse(is.na(i), "", h$esperado[i])
  calculado <- res$estado %in% c("ok", "")
  situacion <- ifelse(res$veredicto == "CONVENCION", "convencion",
               ifelse(res$veredicto %in% c("FALLA", "REVISAR"),
                      ifelse(is.na(i), "no_previsto", "conocido"),
               ifelse(res$veredicto == "PASA",
                      ifelse(is.na(i), "ok", "posiblemente_cerrado"),
               ifelse(!calculado,
                      ifelse(is.na(i), "sin_explicacion", "declarado"),
                      "sin_referencia"))))
  res$codigo <- codigo
  res$titulo_hallazgo <- titulo
  res$esperado <- esperado
  res$situacion <- situacion
  res
}

## --- lectura de disco ------------------------------------------------------

#' Lee un CSV si existe; NULL si no
leer_csv_si <- function(ruta, ...) {
  if (!file.exists(ruta)) return(NULL)
  utils::read.csv(ruta, stringsAsFactors = FALSE, ...)
}

#' Commit del arnes a partir de la columna `version` de la entrega
#'
#' `5.1.15-abc1234-dirty-motor-directo` -> `abc1234-dirty`. Si la forma no es
#' esa, se devuelve la version entera: mejor un dato raro que uno inventado.
#' @export
commit_de_version <- function(v) {
  if (is.null(v) || is.na(v) || !nzchar(v)) return("desconocido")
  m <- regmatches(v, regexec("^[0-9][0-9.]*-(.+)-motor-[a-z]+$", v))[[1]]
  if (length(m) == 2L) m[2] else v
}

#' Parsea contraste.txt, el veredicto propio del arnes de GENis
#'
#' Columnas alineadas con espacios, un guion donde no hay valor, y un pie
#' `Totales:` tras una linea en blanco. Solo los casos calculados.
#' @export
leer_contraste <- function(ruta) {
  if (!file.exists(ruta)) return(NULL)
  ln <- readLines(ruta, warn = FALSE)
  fin <- which(!nzchar(trimws(ln)))
  cuerpo <- if (length(fin)) ln[seq_len(fin[1] - 1)] else ln
  if (length(cuerpo) < 2L) return(NULL)
  d <- utils::read.table(text = cuerpo, header = TRUE, stringsAsFactors = FALSE,
                         na.strings = "-", colClasses = "character")
  for (k in c("LR_obtenido", "LR_referencia", "disc_rel"))
    if (k %in% names(d)) d[[k]] <- suppressWarnings(as.numeric(d[[k]]))
  d
}

#' Lee del disco todo lo que el informe necesita del bloque 1
#'
#' `hay` es FALSE si no hay veredicto de la referencia (resultados.csv). Cada
#' pieza que falte queda en NULL: el informe dice que falta, no aborta.
#' @export
leer_bloque1 <- function(dirs = LRVAL_DIRS) {
  f_res <- file.path(dirs$salida, "resultados.csv")
  if (!file.exists(f_res)) return(list(hay = FALSE))
  res <- utils::read.csv(f_res, stringsAsFactors = FALSE)
  res <- res[res$motor == "GENis", , drop = FALSE]
  if (!"estado" %in% names(res)) res$estado <- "ok"
  res$estado[is.na(res$estado) | !nzchar(res$estado)] <- "ok"
  f_ent <- file.path(dirs$vuelta, "resultados_genis.csv")
  ver <- if (file.exists(f_ent)) {
    e <- utils::read.csv(f_ent, colClasses = "character", nrows = 1)
    if ("version" %in% names(e) && nrow(e)) e$version[1] else NA_character_
  } else NA_character_
  grilla <- leer_csv_si(file.path(dirs$salida, "matrices.csv"))
  entorno <- if (file.exists(file.path(dirs$salida, "entorno.txt")))
    readLines(file.path(dirs$salida, "entorno.txt"), warn = FALSE) else NULL
  list(
    hay = TRUE,
    res = res,
    resumen = leer_csv_si(file.path(dirs$salida, "resumen.csv")),
    invariantes = leer_csv_si(file.path(dirs$salida, "invariantes.csv")),
    semantica = leer_csv_si(file.path(dirs$salida, "semantica.csv")),
    piso = leer_csv_si(file.path(dirs$salida, "pares_piso.csv")),
    descomposicion = leer_csv_si(file.path(dirs$salida, "descomposicion.csv")),
    convenciones = leer_csv_si(file.path(dirs$salida, "convenciones.csv")),
    grilla = if (!is.null(grilla) && "veredicto" %in% names(grilla))
      table(grilla$veredicto) else NULL,
    contraste = leer_contraste(file.path(dirs$vuelta, "contraste.txt")),
    entrega_version = ver,
    entrega_commit = commit_de_version(ver),
    entrega_sha256 = if (file.exists(f_ent)) sha256_file(f_ent) else NA_character_,
    sello = leer_procedencia(file.path(dirs$salida, "PROCEDENCIA.txt")),
    entorno_fecha = if (!is.null(entorno) && length(entorno))
      trimws(sub("^Fecha *: *", "", entorno[1])) else NA_character_,
    man_paquete = hash_manifiesto(file.path(dirs$paquete, "MANIFIESTO.txt")),
    man_entrega = hash_manifiesto(file.path(dirs$vuelta, "MANIFIESTO.txt")),
    fecha_paquete = fecha_manifiesto(file.path(dirs$paquete, "MANIFIESTO.txt"))
  )
}

#' Fecha de la cabecera de un manifiesto, o NA
fecha_manifiesto <- function(ruta) {
  if (!file.exists(ruta)) return(NA_character_)
  ln <- readLines(ruta, n = 1, warn = FALSE)
  m <- regmatches(ln, regexpr("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9:]{8}", ln))
  if (length(m)) m else NA_character_
}

## --- la cadena ---------------------------------------------------------------

eslabon <- function(nombre, ok, detalle)
  data.frame(eslabon = nombre, ok = isTRUE(ok), detalle = detalle, stringsAsFactors = FALSE)

#' Verifica que el veredicto del bloque 1 sea de ESTA entrega y ESTE paquete
#'
#' Dos eslabones: el paquete actual es el que la entrega dice haber consumido
#' (identidad del manifiesto por contenido), y la entrega en disco es la que el
#' veredicto sello (SHA-256 del archivo). Las fechas no cuentan: se pierden
#' con un checkout o una copia.
#' @export
cadena_bloque1 <- function(b) {
  e1 <- if (is.na(b$man_paquete)) {
    eslabon("paquete -> entrega", FALSE, "no hay manifiesto del paquete en ../cases/")
  } else if (is.na(b$man_entrega)) {
    eslabon("paquete -> entrega", FALSE,
            "la entrega no trae copia del manifiesto: no se sabe contra qué paquete corrió")
  } else if (identical(b$man_paquete, b$man_entrega)) {
    eslabon("paquete -> entrega", TRUE, "la entrega consumió el paquete actual (manifiesto por contenido)")
  } else {
    eslabon("paquete -> entrega", FALSE,
            "el manifiesto del paquete actual no es el que consumió la entrega: el paquete se reemitió después, o la entrega es de otro paquete")
  }
  s <- b$sello
  e2 <- if (is.null(s)) {
    eslabon("entrega -> veredicto", FALSE,
            "veredicto sin sello de procedencia: correr `validation/correr bloque1 veredicto`")
  } else if (is.na(b$entrega_sha256)) {
    eslabon("entrega -> veredicto", FALSE, "no hay resultados_genis.csv en ../out/")
  } else if (identical(unname(s["entrega_sha256"]), b$entrega_sha256)) {
    eslabon("entrega -> veredicto", TRUE, "el veredicto se emitió sobre la entrega que está en disco (SHA-256)")
  } else {
    eslabon("entrega -> veredicto", FALSE,
            "la entrega en disco no es la que el veredicto juzgó: se volvió a correr GENis sin volver a emitir el veredicto, o al revés")
  }
  rbind(e1, e2)
}

#' TRUE si todos los eslabones cierran
#' @export
cadena_cierra <- function(ca) !is.null(ca) && nrow(ca) > 0 && all(ca$ok)

#' Avisos del bloque 1 que no rompen la cadena pero hay que leer
#' @export
advertencias_bloque1 <- function(b) {
  av <- character(0)
  s <- b$sello
  c_ent <- b$entrega_commit
  c_ver <- if (!is.null(s) && "commit" %in% names(s)) unname(s["commit"]) else NA_character_
  if (!is.na(c_ver) && !identical(c_ent, "desconocido") && !identical(c_ent, c_ver))
    av <- c(av, sprintf("la entrega de GENis es del commit %s y el veredicto de la referencia del commit %s: las dos mitades corrieron sobre código distinto",
                        c_ent, c_ver))
  sucios <- unique(c(c_ent, c_ver)); sucios <- sucios[!is.na(sucios) & grepl("-dirty$", sucios)]
  if (length(sucios))
    av <- c(av, sprintf("commit %s: el árbol tenía cambios sin commitear; la corrida no se reproduce haciendo checkout de ese commit",
                        paste(sucios, collapse = " y ")))
  av
}

## --- bloque 2: una condicion de la busqueda masiva ----------------------------

#' Resume puente_masiva.csv: cuantas celdas, diferencia maxima, errores
resumir_puente <- function(ruta, estado_declarado) {
  if (!file.exists(ruta)) return(NULL)
  p <- utils::read.csv(ruta, stringsAsFactors = FALSE)
  dif <- suppressWarnings(as.numeric(p$diferencia))
  list(estado = estado_declarado, n = nrow(p),
       max_dif = if (any(is.finite(dif))) max(abs(dif[is.finite(dif)])) else NA_real_,
       errores = sum(!(p$estado %in% "ok")))
}

#' Lee del disco todo lo que el informe necesita de una condicion masiva
#'
#' `hay_entrega`: GENis dejo resultados_genis_masiva.csv. `hay_veredicto`: la
#' comparacion celda a celda tiene una fila para esta condicion. Cada pieza
#' que falte queda en NULL.
#' @export
leer_condicion_masiva <- function(dirs = LRVAL_DIRS, cond) {
  ent <- file.path(dirs$vuelta, "masiva", cond)
  paq <- file.path(dirs$masiva, "paquete", cond)
  cmp <- file.path(dirs$masiva, "comparacion")
  f_ent <- file.path(ent, "resultados_genis_masiva.csv")
  filas_de <- function(d, col = "condicion", valor = cond)
    if (is.null(d) || !col %in% names(d)) NULL else {
      x <- d[d[[col]] == valor, , drop = FALSE]; if (nrow(x)) x else NULL }
  acuerdo <- filas_de(leer_csv_si(file.path(cmp, "acuerdo_celdas.csv")))
  condiciones <- leer_procedencia(file.path(ent, "condiciones_masiva.txt"))
  campo <- function(k) if (!is.null(condiciones) && k %in% names(condiciones))
    unname(condiciones[k][1]) else NA_character_
  escenario <- gsub("_", " ", cond)
  list(
    cond = cond,
    hay_entrega = file.exists(f_ent),
    hay_veredicto = !is.null(acuerdo),
    condiciones = condiciones,
    procedencia = leer_procedencia(file.path(ent, "parcial", "PROCEDENCIA.txt")),
    sello = leer_procedencia(file.path(cmp, sprintf("PROCEDENCIA_%s.txt", cond))),
    acuerdo = acuerdo,
    por_tipo = filas_de(leer_csv_si(file.path(cmp, "acuerdo_por_tipo.csv"))),
    cobertura = leer_csv_si(file.path(cmp, sprintf("cobertura_%s.csv", cond))),
    discrepantes = filas_de(leer_csv_si(file.path(cmp, "celdas_discrepantes.csv"))),
    operativa = filas_de(leer_csv_si(file.path(cmp, "operativa_global.csv"))),
    acuerdo_motores = filas_de(leer_csv_si(file.path(dirs$masiva, "acuerdo_motores.csv")),
                               "escenario", escenario),
    divergencia_motores = filas_de(leer_csv_si(file.path(dirs$masiva, "divergencia_motores.csv")),
                                   "escenario", escenario),
    puente = resumir_puente(file.path(ent, "puente_masiva.csv"), campo("puente")),
    resumen_genis = leer_csv_si(file.path(ent, "resumen_masiva.csv")),
    costo = leer_csv_si(file.path(dirs$vuelta, "masiva", "costo_masiva.csv")),
    entrega_sha256 = if (file.exists(f_ent)) sha256_file(f_ent) else NA_character_,
    man_paquete = hash_manifiesto(file.path(paq, "MANIFIESTO.txt")),
    man_entrega = hash_manifiesto(file.path(ent, "MANIFIESTO.txt")),
    fecha_paquete = fecha_manifiesto(file.path(paq, "MANIFIESTO.txt"))
  )
}

#' Commit de una linea `<fecha> en <commit>` de condiciones_masiva.txt
commit_de_linea <- function(x) {
  if (is.null(x) || is.na(x) || !grepl(" en ", x)) return(NA_character_)
  trimws(sub("^.* en ([^ ]+).*$", "\\1", x))
}

#' Verifica que el veredicto de una condicion masiva sea de ESTA entrega y ESTE paquete
#'
#' Eslabon 1: el paquete actual es el que la entrega copio (manifiesto por
#' contenido). Eslabon 2: la entrega en disco es la que la comparacion sello,
#' por su linea `arrancada` (unica por corrida de GENis) y por el SHA-256 del
#' archivo de resultados.
#' @export
cadena_masiva <- function(m) {
  e1 <- if (!m$hay_entrega) {
    eslabon("paquete -> entrega", FALSE, "no hay entrega de GENis para esta condición")
  } else if (is.na(m$man_paquete)) {
    eslabon("paquete -> entrega", FALSE, "no hay manifiesto del paquete de esta condición")
  } else if (is.na(m$man_entrega)) {
    eslabon("paquete -> entrega", FALSE,
            "la entrega no trae copia del manifiesto: no se sabe contra qué paquete corrió")
  } else if (identical(m$man_paquete, m$man_entrega)) {
    eslabon("paquete -> entrega", TRUE, "la entrega consumió el paquete actual (manifiesto por contenido)")
  } else {
    eslabon("paquete -> entrega", FALSE,
            "el manifiesto del paquete actual no es el que consumió la entrega: el paquete se regeneró después, o la entrega es de otro paquete")
  }
  s <- m$sello
  arr_ent <- if (!is.null(m$condiciones) && "arrancada" %in% names(m$condiciones))
    unname(m$condiciones["arrancada"][1]) else NA_character_
  arr_sel <- if (!is.null(s) && "entrega_arrancada" %in% names(s)) unname(s["entrega_arrancada"]) else NA_character_
  sha_sel <- if (!is.null(s) && "entrega_sha256" %in% names(s)) unname(s["entrega_sha256"]) else NA_character_
  e2 <- if (!m$hay_veredicto) {
    eslabon("entrega -> veredicto", FALSE,
            "no hay veredicto celda a celda para esta condición: correr `validation/correr masiva comparar`")
  } else if (is.null(s)) {
    eslabon("entrega -> veredicto", FALSE,
            "veredicto sin sello de procedencia: correr `validation/correr masiva comparar`")
  } else if (!is.na(arr_ent) && !identical(arr_sel, "desconocida") && !identical(arr_ent, arr_sel)) {
    eslabon("entrega -> veredicto", FALSE,
            sprintf("la entrega en disco es de la corrida arrancada %s y el veredicto juzgó la arrancada %s: se volvió a correr GENis sin volver a comparar, o al revés",
                    arr_ent, arr_sel))
  } else if (!is.na(sha_sel) && !identical(sha_sel, m$entrega_sha256)) {
    eslabon("entrega -> veredicto", FALSE,
            "el archivo de resultados en disco no es el que el veredicto juzgó (SHA-256 distinto)")
  } else {
    eslabon("entrega -> veredicto", TRUE,
            "el veredicto se emitió sobre la entrega que está en disco (misma `arrancada`, mismo SHA-256)")
  }
  rbind(e1, e2)
}

#' Avisos de una condicion masiva que no rompen la cadena pero hay que leer
#' @export
advertencias_masiva <- function(m) {
  av <- character(0)
  co <- m$condiciones
  campo <- function(k) if (!is.null(co) && k %in% names(co)) unname(co[k][1]) else NA_character_
  if (m$hay_entrega && is.null(co))
    av <- c(av, "la entrega no trae condiciones_masiva.txt: la corrida de GENis no cerró sola y su MANIFIESTO.txt pudo copiarse a mano, así que la verificación del paquete no es independiente")
  pu <- campo("puente")
  if (!is.na(pu) && grepl("pendiente", pu))
    av <- c(av, sprintf("el puente quedó pendiente (puente = %s): la entrega sirve para el veredicto, pero falta el contraste interno de GENis; volver a lanzar `validation/correr masiva genis %s` lo corre solo",
                        pu, m$cond))
  if (!is.na(pu) && grepl("no corrido", pu))
    av <- c(av, "el puente no se corrió: no hay contraste interno entre los dos caminos de cálculo de GENis")
  arr <- commit_de_linea(campo("arrancada"))
  rean <- if (!is.null(co)) unname(co[names(co) == "reanudada"]) else character(0)
  otros <- unique(vapply(rean, commit_de_linea, "")); otros <- otros[!is.na(otros) & otros != arr]
  if (length(otros))
    av <- c(av, sprintf("la entrega mezcla familias calculadas por dos versiones: arrancada en %s y reanudada en %s",
                        arr, paste(otros, collapse = ", ")))
  c_sel <- if (!is.null(m$sello) && "commit" %in% names(m$sello)) unname(m$sello["commit"]) else NA_character_
  c_ent <- campo("commit")
  if (!is.na(c_sel) && !is.na(c_ent) && !identical(c_sel, c_ent))
    av <- c(av, sprintf("la entrega de GENis es del commit %s y la comparación del commit %s: las dos mitades corrieron sobre código distinto",
                        c_ent, c_sel))
  sucios <- unique(c(c_ent, c_sel, arr)); sucios <- sucios[!is.na(sucios) & grepl("-dirty$", sucios)]
  if (length(sucios))
    av <- c(av, sprintf("commit %s: el árbol tenía cambios sin commitear; la corrida no se reproduce haciendo checkout de ese commit",
                        paste(sucios, collapse = " y ")))
  cb <- m$cobertura
  if (!is.null(cb) && nrow(cb) && cb$aceptadas[1] < cb$familias[1])
    av <- c(av, sprintf("%d de %d familias no entraron al veredicto (no informadas o incompletas): %s",
                        cb$familias[1] - cb$aceptadas[1], cb$familias[1],
                        if (nzchar(cb$incompletas[1])) cb$incompletas[1] else "ver cobertura"))
  if (!is.null(m$puente) && m$puente$errores > 0)
    av <- c(av, sprintf("%d celdas del puente terminaron en error", m$puente$errores))
  av
}

## --- armado ------------------------------------------------------------------

#' Cruza contraste.txt (veredicto propio del arnes) con el de la referencia
#'
#' Los dos miden lo mismo por separado; aca se comprueba que coincidan. Un
#' SIN_REFERENCIA del arnes equivale a un SIN_DATO de la referencia.
#' @export
cruzar_contraste <- function(b) {
  ct <- b$contraste
  if (is.null(ct) || is.null(b$res) || !nrow(b$res)) return(NULL)
  i <- match(ct$caso, b$res$caso)
  arnes <- ifelse(ct$veredicto == "SIN_REFERENCIA", "SIN_DATO", ct$veredicto)
  ref <- ifelse(is.na(i), NA_character_, b$res$veredicto[i])
  dif <- which(is.na(ref) | arnes != ref)
  list(n = nrow(ct), coinciden = nrow(ct) - length(dif),
       difieren = data.frame(caso = ct$caso[dif], arnes = ct$veredicto[dif],
                             referencia = ifelse(is.na(ref[dif]), "(ausente)", ref[dif]),
                             stringsAsFactors = FALSE))
}

#' Junta todo lo que hay en disco, con su cadena y sus advertencias
#' @export
armar_informe <- function(dirs = LRVAL_DIRS) {
  hz <- leer_hallazgos(dirs$hallazgos)
  b <- leer_bloque1(dirs)
  if (b$hay) {
    b$cruce <- cruzar_hallazgos(b$res, hz)
    b$cadena <- cadena_bloque1(b)
    b$advertencias <- advertencias_bloque1(b)
    b$contraste_cruce <- cruzar_contraste(b)
  }
  conds <- c("sin_mutacion", "con_mutacion")
  ms <- lapply(conds, function(cond) {
    m <- leer_condicion_masiva(dirs, cond)
    if (m$hay_entrega || m$hay_veredicto) {
      m$cadena <- cadena_masiva(m)
      m$advertencias <- advertencias_masiva(m)
    }
    m
  })
  names(ms) <- conds
  list(generado = list(fecha = format(Sys.time(), "%Y-%m-%d %H:%M:%S"), commit = commit_actual()),
       hallazgos = hz, bloque1 = b, masiva = ms,
       tol = if (exists("LRVAL_TOL") && !is.null(LRVAL_TOL$rel)) LRVAL_TOL$rel else 1e-9)
}

## --- texto: ayudantes ----------------------------------------------------------

miles <- function(n) {
  if (is.null(n) || is.na(n)) return("?")
  gsub("(\\d)(?=(\\d{3})+$)", "\\1.", as.character(as.integer(round(n))), perl = TRUE)
}
cientifico <- function(x) if (is.null(x) || is.na(x)) "NA" else formatC(x, format = "e", digits = 2)
o_nada <- function(x, nada = "ninguno") if (!length(x)) nada else paste(x, collapse = " ")
valor <- function(v, k, si_no = "?") if (!is.null(v) && k %in% names(v)) unname(v[k][1]) else si_no

## Una sublinea por titulo de hallazgos_genis.csv, con sus casos. La columna
## `codigo` no se imprime.
##     MUT-04 MUT-05 MUT-06  <titulo de la fila>
lineas_por_titulo <- function(x) {
  if (is.null(x) || !nrow(x)) return(character(0))
  tit <- ifelse(nzchar(x$titulo_hallazgo), x$titulo_hallazgo, "(sin título en la tabla)")
  grupos <- split(x$caso, tit)
  grupos <- grupos[order(vapply(grupos, `[`, "", 1))]
  sprintf("    %-22s %s", vapply(grupos, paste, "", collapse = " "), names(grupos))
}
## "titulo (n): ninguno" en una linea, o el titulo y las sublineas
bloque_de_casos <- function(titulo, x, nada = "ninguno") {
  if (is.null(x) || !nrow(x)) return(sprintf("  %s (0): %s", titulo, nada))
  c(sprintf("  %s (%d):", titulo, nrow(x)), lineas_por_titulo(x))
}

conteo_veredictos <- function(v) {
  niveles <- c("PASA", "CONVENCION", "REVISAR", "FALLA", "SIN_DATO")
  tb <- table(factor(v, levels = niveles))
  paste(sprintf("%s=%d", niveles, as.integer(tb)), collapse = "  ")
}

## El texto de una celda, el mismo en Markdown y en LaTeX: numeros con pocos
## digitos significativos; NA y vacios, en blanco.
celda_texto <- function(x, digitos = 6) {
  if (is.numeric(x)) ifelse(is.na(x), "", trimws(formatC(x, digits = digitos, format = "g")))
  else ifelse(is.na(x) | !nzchar(as.character(x)), "", as.character(x))
}

## Una tabla Markdown a partir de un data.frame
md_tabla <- function(d, cols = names(d), etiquetas = cols, digitos = 6) {
  if (is.null(d) || !nrow(d)) return("(sin filas)")
  filas <- do.call(paste, c(lapply(cols, function(k) celda_texto(d[[k]], digitos)), sep = " | "))
  c(paste("|", paste(etiquetas, collapse = " | "), "|"),
    paste("|", paste(rep("---", length(cols)), collapse = " | "), "|"),
    paste("|", filas, "|"))
}


## Una linea por mensaje, con las piezas que lo comparten: sin agrupar, el
## aviso de arbol sucio saldria tres veces casi iguales, una por pieza.
advertencias_agrupadas <- function(b, ms) {
  piezas <- character(0); msgs <- character(0)
  if (b$hay && length(b$advertencias)) {
    piezas <- c(piezas, rep("bloque 1", length(b$advertencias))); msgs <- c(msgs, b$advertencias)
  }
  for (m in ms) if (length(m$advertencias)) {
    piezas <- c(piezas, rep(m$cond, length(m$advertencias))); msgs <- c(msgs, m$advertencias)
  }
  if (!length(msgs)) return(character(0))
  vapply(unique(msgs), function(x)
    sprintf("%s [%s]", x, paste(piezas[msgs == x], collapse = ", ")), "", USE.NAMES = FALSE)
}

## --- texto: el nivel corto --------------------------------------------------------

linea_procedencia_b1 <- function(b) {
  s <- b$sello
  sprintf("  bloque 1              paquete %s | entrega de GENis commit %s | veredicto de la referencia commit %s, %s",
          if (is.na(b$fecha_paquete)) "?" else b$fecha_paquete,
          b$entrega_commit, valor(s, "commit", "sin sello"), valor(s, "fecha", "?"))
}
linea_procedencia_ma <- function(m) {
  co <- m$condiciones; s <- m$sello
  sprintf("  masiva %-13s paquete %s | entrega de GENis commit %s, arrancada %s | comparación commit %s, %s",
          m$cond, if (is.na(m$fecha_paquete)) "?" else m$fecha_paquete,
          valor(co, "commit", "sin condiciones"), valor(co, "arrancada", "?"),
          valor(s, "commit", "sin sello"), valor(s, "fecha", "?"))
}
lineas_cadena <- function(ca) {
  if (cadena_cierra(ca))
    return("                        cadena verificada: el veredicto es de esta entrega y la entrega es de este paquete")
  rotos <- ca[!ca$ok, , drop = FALSE]
  sprintf("                        ATENCIÓN, eslabón %s: %s", rotos$eslabon, rotos$detalle)
}

resumen_bloque1 <- function(b, tol) {
  if (!b$hay)
    return("Bloque 1, el catálogo: sin corrida en disco; correr `validation/correr bloque1`")
  x <- b$cruce
  if (!nrow(x))
    return("Bloque 1, el catálogo: el veredicto en disco no incluye a GENis; correr `validation/correr bloque1 genis` y después `validation/correr bloque1 veredicto`")
  if (!cadena_cierra(b$cadena))
    return("Bloque 1, el catálogo: SIN VEREDICTO, piezas de corridas distintas (ver PROCEDENCIA); volver a correr el paso que la cadena nombra")
  sit <- function(s) x[x$situacion == s, , drop = FALSE]
  con <- sit("conocido"); nop <- sit("no_previsto"); dec <- sit("declarado")
  sinx <- sit("sin_explicacion"); cer <- sit("posiblemente_cerrado"); sinr <- sit("sin_referencia")
  cv <- sit("convencion")
  ## Las lineas de lo que no ocurrio no se imprimen, salvo dos que son la
  ## respuesta que se viene a buscar: las diferencias de convencion y las
  ## FALLA no previstas.
  out <- c(
    sprintf("Bloque 1, el catálogo: %d casos, GENis calculó %d | %s", nrow(x), sum(x$estado == "ok"), conteo_veredictos(x$veredicto)),
    sprintf("  diferencias de convención (%d): %s", nrow(cv), o_nada(cv$caso, "ninguna")))
  if (nrow(con)) out <- c(out, bloque_de_casos("FALLA conocidas", con, "ninguna"))
  out <- c(out,
    sprintf("  FALLA no previstas (%d): %s", nrow(nop),
            if (nrow(nop)) paste(o_nada(nop$caso), "<- REVISAR: no están en hallazgos_genis.csv") else "ninguna"))
  if (nrow(dec)) out <- c(out, bloque_de_casos("no calculados, declarados", dec))
  if (nrow(sinx))
    out <- c(out, sprintf("  no calculados SIN explicación registrada (%d): %s <- REVISAR", nrow(sinx), o_nada(sinx$caso)))
  if (nrow(cer))
    out <- c(out, sprintf("  filas de hallazgos_genis.csv posiblemente cerradas (%d): salieron PASA; sacar la fila", nrow(cer)),
             lineas_por_titulo(cer))
  if (nrow(sinr)) out <- c(out, sprintf("  sin valor de referencia (%d): %s", nrow(sinr), o_nada(sinr$caso, "ninguno")))
  cc <- b$contraste_cruce
  if (!is.null(cc))
    out <- c(out, if (!nrow(cc$difieren))
      sprintf("  contraste del arnés vs referencia: coinciden los %d casos calculados", cc$n)
      else sprintf("  contraste del arnés vs referencia: NO coinciden %d de %d: %s <- uno de los dos lados está mal",
                   nrow(cc$difieren), cc$n,
                   paste(sprintf("%s (arnés %s, referencia %s)", cc$difieren$caso, cc$difieren$arnes, cc$difieren$referencia), collapse = ", ")))
  inv <- b$invariantes
  if (!is.null(inv)) inv <- inv[grepl("/GENis$", inv$invariante), , drop = FALSE]
  extras <- c(
    if (!is.null(inv) && nrow(inv)) sprintf("invariantes %s", paste(sprintf("%d %s", as.integer(table(inv$veredicto)), names(table(inv$veredicto))), collapse = ", ")),
    if (!is.null(b$grilla)) sprintf("grilla de matrices %s", paste(sprintf("%d %s", as.integer(b$grilla), names(b$grilla)), collapse = ", ")))
  if (length(extras)) out <- c(out, paste0("  ", paste(extras, collapse = " | ")))
  cola <- if (nrow(cv)) sprintf("; %d diferencia%s de convención, explicada%s en el informe",
                                nrow(cv), if (nrow(cv) > 1) "s" else "", if (nrow(cv) > 1) "s" else "") else ""
  veredicto <- if (nrow(nop) || nrow(sinx))
    sprintf("  => bloque 1: %d FALLA, %d no previstas: HAY QUE MIRARLAS", nrow(con) + nrow(nop), nrow(nop) + nrow(sinx))
  else if (nrow(con))
    sprintf("  => bloque 1: %d FALLA y las %d están explicadas en hallazgos_genis.csv; ningún desvío no previsto%s", nrow(con), nrow(con), cola)
  else paste0("  => bloque 1: sin FALLA frente a la referencia", cola)
  c(out, veredicto)
}

resumen_condicion <- function(m, tol) {
  et <- sprintf("  %-13s", m$cond)
  if (!m$hay_entrega && !m$hay_veredicto)
    return(paste0(et, " sin entrega ni veredicto en disco: correr `validation/correr masiva`"))
  if (!m$hay_veredicto)
    return(paste0(et, " hay entrega de GENis pero sin veredicto: correr `validation/correr masiva comparar`"))
  if (!cadena_cierra(m$cadena))
    return(paste0(et, " SIN VEREDICTO: piezas de corridas distintas (ver PROCEDENCIA)"))
  a <- m$acuerdo
  out <- character(0)
  for (i in seq_len(nrow(a))) {
    r <- a[i, ]
    out <- c(out,
      sprintf("%s %s celdas contra %s | PASA=%s  REVISAR=%d  FALLA=%d  SIN_DATO=%d",
              if (i == 1) et else sprintf("  %-13s", ""), miles(r$celdas), r$motor, miles(r$pasa), r$revisar, r$falla, r$sin_dato),
      sprintf("                rel_max %s (tolerancia %s) sobre %s celdas con magnitud | exclusiones sólo GENis %d, sólo referencia %d",
              cientifico(r$rel_max), cientifico(tol), miles(r$celdas_magnitud), r$excl_solo_obs, r$excl_solo_ref))
  }
  p <- m$puente
  if (!is.null(p))
    out <- c(out, sprintf("                puente (los dos caminos de GENis): %d celdas, diferencia máxima %s%s",
                          p$n, cientifico(p$max_dif), if (p$errores) sprintf(", %d en error", p$errores) else ""))
  out
}

veredicto_masiva <- function(ms) {
  con <- Filter(function(m) m$hay_veredicto && cadena_cierra(m$cadena), ms)
  if (!length(con)) return("  => bloque 2: sin veredicto")
  a <- do.call(rbind, lapply(con, `[[`, "acuerdo"))
  malas <- sum(a$falla) + sum(a$revisar) + sum(a$sin_dato)
  cat_ <- sum(a$excl_solo_obs) + sum(a$excl_solo_ref)
  sprintf("  => bloque 2: %s celdas en %d %s, %s%s",
          miles(sum(a$celdas)), length(con), if (length(con) > 1) "condiciones" else "condición",
          if (malas == 0) "todas PASA" else sprintf("%s que no son PASA", miles(malas)),
          if (cat_ == 0) ", cero desacuerdos categóricos" else sprintf(", %d DESACUERDOS CATEGÓRICOS (uno excluye y el otro no)", cat_))
}

#' El nivel corto: lo que se imprime al final de `validation/correr`
#' @export
texto_veredicto <- function(inf) {
  b <- inf$bloque1; ms <- inf$masiva
  out <- c("VEREDICTO DE LA VALIDACIÓN DEL LR POR PEDIGRÍ DE GENis",
           sprintf("generado %s en el commit %s | tolerancia de aceptación: discrepancia relativa <= %s",
                   inf$generado$fecha, inf$generado$commit, cientifico(inf$tol)),
           "", "PROCEDENCIA (la cadena se verifica por contenido: manifiestos y SHA-256, no por fechas)")
  if (b$hay && !is.null(b$cadena)) out <- c(out, linea_procedencia_b1(b), lineas_cadena(b$cadena))
  else out <- c(out, "  bloque 1              sin corrida en disco")
  for (m in ms) {
    if (!is.null(m$cadena)) out <- c(out, linea_procedencia_ma(m), lineas_cadena(m$cadena))
    else out <- c(out, sprintf("  masiva %-13s sin corrida en disco", m$cond))
  }
  out <- c(out, "", resumen_bloque1(b, inf$tol), "", "Bloque 2, la búsqueda masiva:")
  for (m in ms) out <- c(out, resumen_condicion(m, inf$tol))
  out <- c(out, veredicto_masiva(ms))
  av <- advertencias_agrupadas(b, ms)
  out <- c(out, "", "ADVERTENCIAS", if (length(av)) paste("  -", av) else "  ninguna")
  c(out, "", "Detalle por caso y por tipo: validation/referencia/out/INFORME.pdf (fuente INFORME.tex; el mismo contenido en INFORME.md)",
    "Este texto: validation/referencia/out/VEREDICTO.txt")
}

## --- el nivel largo: un documento y dos renderizadores ---------------------------
##
## El nivel largo se arma UNA vez como documento (titulos, parrafos, listas,
## tablas y un bloque literal) y se renderiza a Markdown (INFORME.md) y a
## LaTeX (INFORME.tex -> INFORME.pdf, en R/informe_tex.R). Al salir del mismo
## documento, los dos no pueden contar cosas distintas. La prosa lleva un
## subconjunto minimo de Markdown, **negrita** y `codigo`, que cada
## renderizador traduce a lo suyo. Cada constructor devuelve una lista de un
## nodo, asi que `c()` los encadena en un documento plano.

nodo_h <- function(nivel, texto) list(list(tipo = "h", nivel = nivel, texto = texto))
nodo_p <- function(texto) list(list(tipo = "p", texto = texto))
nodo_lista <- function(items) list(list(tipo = "lista", items = items))
nodo_tabla <- function(d, cols = names(d), etiquetas = cols, digitos = 6)
  list(list(tipo = "tabla", d = d, cols = cols, etiquetas = etiquetas, digitos = digitos))
nodo_literal <- function(lineas) list(list(tipo = "literal", lineas = lineas))
nodos <- function(...) do.call(c, list(...))

doc_procedencia_b1 <- function(b) {
  s <- b$sello
  d <- data.frame(pieza = c("paquete de casos", "entrega de GENis", "veredicto de la referencia"),
                  commit = c("", b$entrega_commit, valor(s, "commit", "sin sello")),
                  fecha = c(if (is.na(b$fecha_paquete)) "" else b$fecha_paquete, "", valor(s, "fecha", "")),
                  identidad = c(if (is.na(b$man_paquete)) "" else substr(b$man_paquete, 1, 12),
                                if (is.na(b$entrega_sha256)) "" else substr(b$entrega_sha256, 1, 12),
                                substr(valor(s, "entrega_sha256", ""), 1, 12)),
                  stringsAsFactors = FALSE)
  nodos(nodo_tabla(d, etiquetas = c("pieza", "commit", "fecha", "identidad (prefijo del hash)")),
        nodo_tabla(transform(b$cadena, ok = ifelse(ok, "cierra", "ROTO")), c("eslabon", "ok", "detalle"), c("eslabón", "ok", "detalle")))
}
doc_procedencia_ma <- function(m) {
  co <- m$condiciones; s <- m$sello
  d <- data.frame(pieza = c("paquete", "entrega de GENis", "comparación celda a celda"),
                  commit = c("", valor(co, "commit", "sin condiciones"), valor(s, "commit", "sin sello")),
                  fecha = c(if (is.na(m$fecha_paquete)) "" else m$fecha_paquete, valor(co, "arrancada", ""), valor(s, "fecha", "")),
                  identidad = c(if (is.na(m$man_paquete)) "" else substr(m$man_paquete, 1, 12),
                                if (is.na(m$entrega_sha256)) "" else substr(m$entrega_sha256, 1, 12),
                                substr(valor(s, "entrega_sha256", ""), 1, 12)),
                  stringsAsFactors = FALSE)
  nodos(nodo_tabla(d, etiquetas = c("pieza", "commit", "fecha", "identidad (prefijo del hash)")),
        if (!is.null(m$cadena)) nodo_tabla(transform(m$cadena, ok = ifelse(ok, "cierra", "ROTO")), c("eslabon", "ok", "detalle"), c("eslabón", "ok", "detalle")))
}

## Un titulo por fila de hallazgos_genis.csv, con sus casos, el resultado
## esperado y la prosa de la tabla. La explicacion puede variar por caso: se
## listan las distintas.
doc_hallazgos <- function(x, hz, ambito = "bloque1") {
  if (is.null(x) || !nrow(x)) return(nodo_p("(ninguno)"))
  tits <- unique(x$titulo_hallazgo[nzchar(x$titulo_hallazgo)])
  do.call(c, lapply(tits, function(tit) {
    filas <- hz[hz$ambito == ambito & hz$titulo == tit, , drop = FALSE]
    casos <- x$caso[x$titulo_hallazgo == tit]
    expl <- unique(filas$explicacion[filas$clave %in% casos])
    if (!length(expl)) expl <- unique(filas$explicacion)
    nodos(nodo_h(3, tit),
          nodo_p(sprintf("Casos: %s. Resultado esperado: %s.", paste(casos, collapse = ", "),
                         paste(unique(filas$esperado), collapse = "/"))),
          do.call(c, lapply(expl, nodo_p)))
  }))
}

## Una entrada por caso con veredicto CONVENCION: la prosa del catalogo y, al
## lado, lo medido: el LR de GENis, el de referencia y el de la otra convencion.
doc_convenciones <- function(cv, res) {
  do.call(c, lapply(cv$caso, function(id) {
    r <- res[res$caso == id, , drop = FALSE][1, ]
    alt <- if ("gold_alt" %in% names(r)) r$gold_alt else NA
    nodos(nodo_p(sprintf("**%s**: %s", id, r$titulo)),
          if (!is.na(r$divergencia) && nzchar(r$divergencia)) nodo_p(r$divergencia),
          nodo_p(sprintf("Medido: GENis da %s; el valor de referencia es %s%s.",
                         celda_texto(r$LR, 10), celda_texto(r$gold, 10),
                         if (!is.na(alt)) sprintf(" y el de la otra convención, %s, que es el que GENis reproduce dentro de la tolerancia", celda_texto(alt, 10)) else "")))
  }))
}

doc_bloque1 <- function(b, hz, tol) {
  out <- nodo_h(2, "Bloque 1: el catálogo de casos")
  if (!b$hay) return(nodos(out, nodo_p("Sin corrida en disco: correr `validation/correr bloque1`.")))
  x <- b$cruce
  if (!nrow(x)) return(nodos(out, nodo_p("El veredicto en disco no incluye a GENis: correr `validation/correr bloque1 genis` y después `validation/correr bloque1 veredicto`.")))
  out <- nodos(out, nodo_h(3, "Procedencia y cadena"), doc_procedencia_b1(b))
  if (!cadena_cierra(b$cadena))
    out <- nodos(out, nodo_p("**SIN VEREDICTO: piezas de corridas distintas.** La tabla de abajo se muestra sólo para diagnosticar; no es un veredicto."))
  resumen <- c(
    sprintf("casos del catálogo: %d; calculados por GENis: %d; %s", nrow(x), sum(x$estado == "ok"), conteo_veredictos(x$veredicto)),
    sprintf("tolerancia de aceptación: discrepancia relativa <= %s (REVISAR hasta 1e-6)", cientifico(tol)))
  cc <- b$contraste_cruce
  if (!is.null(cc))
    resumen <- c(resumen, if (!nrow(cc$difieren)) sprintf("el veredicto propio del arnés (`contraste.txt`) coincide con el de la referencia en los %d casos calculados", cc$n)
                          else sprintf("el veredicto propio del arnés NO coincide con el de la referencia en: %s", paste(cc$difieren$caso, collapse = ", ")))
  out <- nodos(out, nodo_h(3, "Resumen"), nodo_lista(resumen), nodo_h(3, "Veredicto por caso"))
  res <- b$res
  i <- match(x$caso, res$caso)
  d <- data.frame(caso = x$caso, titulo = if ("titulo" %in% names(res)) res$titulo[i] else "",
                  LR_GENis = if ("LR" %in% names(res)) res$LR[i] else NA,
                  LR_referencia = if ("gold" %in% names(res)) res$gold[i] else NA,
                  disc_rel = if ("disc_rel" %in% names(res)) res$disc_rel[i] else NA,
                  veredicto = x$veredicto, estado = x$estado,
                  hallazgo = ifelse(nzchar(x$titulo_hallazgo), x$titulo_hallazgo,
                                    ifelse(x$situacion %in% c("ok", "sin_referencia"), "",
                                    ifelse(x$situacion == "convencion", "diferencia de convención",
                                           gsub("_", " ", x$situacion)))),
                  stringsAsFactors = FALSE)
  out <- nodos(out, nodo_tabla(d, etiquetas = c("caso", "título", "LR GENis", "LR referencia", "disc. rel.", "veredicto", "estado", "nota"), digitos = 10))
  sit <- function(s) x[x$situacion == s, , drop = FALSE]
  cv <- sit("convencion")
  if (nrow(cv)) out <- nodos(out, nodo_h(3, "Diferencias de convención"), doc_convenciones(cv, res))
  if (nrow(sit("conocido")))
    out <- nodos(out, nodo_h(3, "FALLA explicadas en hallazgos_genis.csv"), doc_hallazgos(sit("conocido"), hz))
  if (nrow(sit("declarado")))
    out <- nodos(out, nodo_h(3, "Casos no calculados, declarados"), doc_hallazgos(sit("declarado"), hz))
  nop <- sit("no_previsto"); sinx <- sit("sin_explicacion"); cer <- sit("posiblemente_cerrado")
  if (nrow(nop) || nrow(sinx))
    out <- nodos(out, nodo_h(3, "Desvíos NO previstos: hay que mirarlos"),
                 nodo_lista(sprintf("%s: %s (%s), sin fila en `hallazgos_genis.csv`",
                                    c(nop$caso, sinx$caso), c(nop$veredicto, sinx$veredicto), c(nop$estado, sinx$estado))))
  if (nrow(cer))
    out <- nodos(out, nodo_h(3, "Filas de hallazgos_genis.csv posiblemente cerradas"),
                 nodo_lista(sprintf("%s salió PASA pero `hallazgos_genis.csv` esperaba %s (%s): si el arreglo es real, sacar la fila",
                                    cer$caso, cer$esperado, cer$titulo_hallazgo)))
  out <- nodos(out, nodo_h(3, "Otros indicadores"))
  inv <- b$invariantes
  if (!is.null(inv) && nrow(inv))
    out <- nodos(out, nodo_p("Invariantes entre casos, sobre los LR de GENis:"),
                 nodo_tabla(inv[grepl("/GENis$", inv$invariante), , drop = FALSE], c("invariante", "descripcion", "veredicto", "detalle"), c("invariante", "descripción", "veredicto", "detalle")))
  if (!is.null(b$grilla))
    out <- nodos(out, nodo_p(sprintf("Grilla de matrices de mutación volcadas por GENis: %s.",
                                     paste(sprintf("%d %s", as.integer(b$grilla), names(b$grilla)), collapse = ", "))))
  if (!is.null(b$semantica)) { sg <- b$semantica[b$semantica$motor == "GENis", , drop = FALSE]
    if (nrow(sg)) out <- nodos(out, nodo_p(sprintf("Convención para el fundador no tipificado, medida sobre los casos con mutación: GENis lo **%s** (la misma en todos los casos: %s).", sg$semanticas[1], sg$veredicto[1]))) }
  if (!is.null(b$piso)) { pg <- b$piso[b$piso$motor == "GENis", , drop = FALSE]
    if (nrow(pg)) out <- nodos(out, nodo_p("Piso de frecuencia: el caso con piso declarado junto a su control sin piso:"),
                               nodo_tabla(pg, c("caso_piso", "caso_sin_piso", "piso", "LR_piso", "LR_sin_piso", "diagnostico"),
                                          c("caso_piso", "caso_sin_piso", "piso", "LR_piso", "LR_sin_piso", "diagnóstico"), digitos = 10)) }
  if (!is.null(b$descomposicion)) { dg <- b$descomposicion[b$descomposicion$motor == "GENis", , drop = FALSE]
    if (nrow(dg)) out <- nodos(out, nodo_p("Descomposición de la diferencia en los casos con mutación (d_algoritmo mide el cálculo sobre la matriz propia; d_modelo, la matriz; d_semantica, la convención para el fundador no tipificado):"),
                               nodo_tabla(dg, c("caso", "matriz", "semantica", "d_total", "d_algoritmo", "d_modelo", "d_semantica", "ver_algoritmo", "ver_conformidad"),
                                          c("caso", "matriz", "semántica", "d_total", "d_algoritmo", "d_modelo", "d_semantica", "ver_algoritmo", "ver_conformidad"), digitos = 3)) }
  out
}

doc_condicion <- function(m, hz, tol) {
  out <- nodo_h(3, sprintf("Condición %s", m$cond))
  if (!m$hay_entrega && !m$hay_veredicto) return(nodos(out, nodo_p("Sin entrega ni veredicto en disco: correr `validation/correr masiva`.")))
  out <- nodos(out, nodo_h(4, "Procedencia y cadena"), doc_procedencia_ma(m))
  co <- m$condiciones
  if (!is.null(co))
    out <- nodos(out, nodo_p(sprintf("Condiciones de la corrida de GENis: %s.", paste(sprintf("%s = %s", names(co), unname(co)), collapse = "; "))))
  if (!m$hay_veredicto) return(nodos(out, nodo_p("Hay entrega de GENis pero no hay veredicto celda a celda: correr `validation/correr masiva comparar`.")))
  if (!cadena_cierra(m$cadena)) out <- nodos(out, nodo_p("**SIN VEREDICTO: piezas de corridas distintas.** Las tablas de abajo se muestran sólo para diagnosticar."))
  out <- nodos(out, nodo_h(4, "Veredicto celda a celda"),
               nodo_tabla(m$acuerdo, c("motor", "celdas", "pasa", "revisar", "falla", "sin_dato", "celdas_magnitud", "rel_max", "rel_mediana", "excl_ambos", "excl_solo_obs", "excl_solo_ref"), digitos = 3),
               nodo_p(sprintf("`rel_max` se calcula sobre las celdas con magnitud comparable (sin las que ambos lados excluyen); tolerancia %s. `excl_solo_obs` y `excl_solo_ref` son desacuerdos categóricos: uno excluye y el otro no.", cientifico(tol))))
  cb <- m$cobertura
  if (!is.null(cb) && nrow(cb))
    out <- nodos(out, nodo_p(sprintf("Cobertura: %d de %d familias aceptadas, %s celdas con dato; verificación del manifiesto independiente: %s.",
                                     cb$aceptadas[1], cb$familias[1], miles(cb$celdas_con_dato[1]), if (isTRUE(cb$manifiesto_independiente[1])) "sí" else "NO")))
  if (!is.null(m$por_tipo))
    out <- nodos(out, nodo_h(4, "Por tipo de pedigrí"),
                 nodo_tabla(m$por_tipo, c("motor", "tipo", "n_familias", "celdas", "pasa", "revisar", "falla", "sin_dato", "rel_max", "excl_solo_obs", "excl_solo_ref"), digitos = 3))
  if (!is.null(m$discrepantes)) {
    dd <- m$discrepantes[order(-m$discrepantes$rel), , drop = FALSE]
    out <- nodos(out, nodo_h(4, sprintf("Celdas que no son PASA (%d; se muestran hasta 30, por discrepancia)", nrow(dd))),
                 nodo_tabla(utils::head(dd, 30), intersect(c("motor", "familia", "poi", "log10LR_obs", "log10LR_ref", "rel", "veredicto"), names(dd)), digitos = 6))
  } else out <- nodos(out, nodo_p("No hay celdas discrepantes: `celdas_discrepantes.csv` no existe porque no hubo ninguna."))
  p <- m$puente
  if (!is.null(p))
    out <- nodos(out, nodo_h(4, "Puente: los dos caminos de cálculo de GENis"),
                 nodo_p(sprintf("Muestra pedida (celdas por tipo de pedigrí y decil): %s. %d celdas muestreadas, diferencia máxima en log10 %s%s. Los dos caminos tienen que coincidir a precisión de máquina.",
                                p$estado, p$n, cientifico(p$max_dif), if (p$errores) sprintf(", %d en error", p$errores) else "")))
  if (!is.null(m$acuerdo_motores))
    out <- nodos(out, nodo_h(4, "Acuerdo entre los motores de referencia"),
                 nodo_tabla(m$acuerdo_motores, intersect(c("motor_a", "motor_b", "n_pares", "max_dif_log10", "veredicto", "exigido", "medido", "commit"), names(m$acuerdo_motores)), digitos = 3),
                 nodo_p("Una fila con `medido` vacío o con otra fecha es una medición preservada de otra corrida, no de esta."))
  if (identical(m$cond, "con_mutacion"))
    out <- nodos(out, nodo_p("Con modelo de mutación el veredicto de GENis se emite contra pedsuite, porque Familias aplica otra convención para los fundadores no tipificados (los poda); es la misma diferencia de convención que muestran PRU-01 y PRU-02 en el catálogo."))
  if (length(m$advertencias)) out <- nodos(out, nodo_h(4, "Advertencias de esta condición"), nodo_lista(m$advertencias))
  out
}

#' El nivel largo como documento: la unica fuente de INFORME.md e INFORME.tex
#' @export
documento_informe <- function(inf) {
  b <- inf$bloque1; ms <- inf$masiva; hz <- inf$hallazgos
  out <- nodos(
    nodo_h(1, "Informe de la validación del LR por pedigrí de GENis"),
    nodo_p(sprintf("Generado el %s en el commit %s por `validation/correr informe`. No recalcula nada: toda cifra sale de los archivos de la corrida que están en disco.", inf$generado$fecha, inf$generado$commit)),
    nodo_h(2, "Veredicto"), nodo_literal(texto_veredicto(inf)),
    nodo_h(2, "Cómo leer este informe"),
    nodo_lista(c(
      "**Alcance**: se valida el cálculo de LR del motor de pedigrí de GENis (`BayesianNetwork`), el que usa la identificación por parentesco. El motor forense de mezclas no forma parte de esta validación.",
      "**PASA**: la discrepancia relativa entre el LR de GENis y el de la referencia está dentro de la tolerancia. **REVISAR**: hasta 1e-6. **FALLA**: mayor.",
      "**CONVENCION**: el caso admite dos convenciones documentadas que dan números distintos, y trae el valor de las dos en forma cerrada. GENis no da el valor de referencia pero sí, dentro de la misma tolerancia, el de la otra convención. No es un error de cálculo: abajo se explica cada una. Un número que no es ninguno de los dos sigue siendo FALLA.",
      "Una **FALLA no prevista** hay que mirarla antes de dar por buena la corrida. `validation/referencia/hallazgos_genis.csv` es la tabla donde se anota, una vez analizada, la explicación de una FALLA; hoy no tiene filas.",
      "La **cadena** paquete -> entrega -> veredicto se verifica por contenido (identidad del manifiesto y SHA-256 de la entrega), no por fechas. Si un eslabón está roto, el bloque no recibe veredicto: hay que volver a correr el paso que la cadena nombra.",
      "El bloque 1 es el catálogo caso por caso, contra valores en forma cerrada. El bloque 2 es la búsqueda masiva: una familia contra toda la nómina de personas de interés, celda por celda, contra los motores de referencia; ahí lo que más importa son los desacuerdos categóricos (`excl_solo_obs`, `excl_solo_ref`), en los que un lado excluye y el otro no.")),
    doc_bloque1(b, hz, inf$tol),
    nodo_h(2, "Bloque 2: la búsqueda masiva"))
  for (m in ms) out <- nodos(out, doc_condicion(m, hz, inf$tol))
  av <- advertencias_agrupadas(b, ms)
  nodos(out, nodo_h(2, "Advertencias"), if (length(av)) nodo_lista(av) else nodo_p("Ninguna."))
}

#' Renderiza el documento a Markdown
#' @export
render_md <- function(doc) {
  unlist(lapply(doc, function(n) switch(n$tipo,
    h = c(paste(strrep("#", n$nivel), n$texto), ""),
    p = c(n$texto, ""),
    lista = c(paste("-", n$items), ""),
    tabla = c(md_tabla(n$d, n$cols, n$etiquetas, n$digitos), ""),
    literal = c("```", n$lineas, "```", ""))))
}

#' El nivel largo: Markdown con el detalle por caso y por tipo
#' @export
texto_informe <- function(inf) render_md(documento_informe(inf))

#' El nivel largo en LaTeX, listo para pdflatex (R/informe_tex.R)
#' @export
texto_informe_tex <- function(inf) render_tex(documento_informe(inf), inf$generado)

#' Genera y escribe los niveles; devuelve las lineas de cada uno
#'
#' Escribe VEREDICTO.txt, INFORME.md e INFORME.tex y, si `pdf` y hay pdflatex,
#' compila INFORME.pdf. `pdf` es la ruta del PDF o NULL, con `pdf_motivo`
#' diciendo por que no esta; el informe nunca aborta por el PDF.
#' @export
generar_informe <- function(dirs = LRVAL_DIRS, pdf = TRUE) {
  inf <- armar_informe(dirs)
  doc <- documento_informe(inf)
  v <- texto_veredicto(inf); m <- render_md(doc); t <- render_tex(doc, inf$generado)
  dir.create(dirs$informe, recursive = TRUE, showWarnings = FALSE)
  writeLines(v, file.path(dirs$informe, "VEREDICTO.txt"))
  writeLines(m, file.path(dirs$informe, "INFORME.md"))
  f_tex <- file.path(dirs$informe, "INFORME.tex")
  writeLines(t, f_tex)
  p <- if (pdf) compilar_pdf(f_tex) else list(pdf = NULL, motivo = "no se pidió el PDF")
  invisible(list(veredicto = v, informe = m, tex = t, pdf = p$pdf, pdf_motivo = p$motivo, datos = inf))
}

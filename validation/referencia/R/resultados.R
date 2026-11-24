## ---------------------------------------------------------------------------
## lrval :: resultados de la corrida
##
## Escribe el veredicto en CSV. Estan versionados a proposito: el resultado de
## una corrida de validacion es parte del entregable, y si solo se imprimiera
## por consola se perderia con el scrollback.
##
##   resultados.csv        tabla completa, un renglon por caso y motor
##   resumen.csv           recuento de veredictos por motor
##   convenciones.csv      diferencias de convencion, con su explicacion
##   entorno.txt           versiones exactas de todo el stack (trazabilidad)
##
## write_results() escribe ademas cobertura.csv, catalogo.csv y
## trazabilidad.csv en toda corrida, e invariantes.csv y los productos del
## nivel 2 sobre el sistema evaluado (matrices, descomposicion, semantica,
## cobertura de grilla, modelo por caso, lumpabilidad, pares de piso) solo
## cuando la corrida los produjo.
## ---------------------------------------------------------------------------

#' Registro del entorno de ejecucion
#'
#' Sin esto una corrida de validacion no es reproducible: el veredicto vale
#' para versiones concretas de cada paquete.
#' @export
env_record <- function() {
  pk <- c("pedtools", "pedprobr", "pedmut", "forrel", "ribd", "Familias",
          "mispitools", "verbalisr", "pedFamilias")
  v <- vapply(pk, function(p) {
    x <- pkg_ver(p)
    if (is.na(x)) "no instalado" else x
  }, "")
  c(paste("Fecha              :", format(Sys.time(), "%Y-%m-%d %H:%M:%S %Z")),
    paste("R                  :", R.version.string),
    paste("Plataforma         :", R.version$platform),
    paste("Sistema operativo  :", paste(Sys.info()[c("sysname", "release")], collapse = " ")),
    "", "Paquetes:",
    sprintf("  %-14s %s", names(v), v))
}

#' Escribe los resultados de una corrida
#'
#' @param res tabla de `run_catalog()` (opcionalmente con filas importadas)
#' @param cs catalogo
#' @param inv tabla de `check_invariants()`
#' @param dir directorio de salida
#' @param mut productos del nivel 2: lista con `grilla`, `informe` (de
#'   `grid_report()`), `descomposicion`, `semantica`, `cobertura`, `modelo` y
#'   `lumpabilidad`. `NULL` si la corrida no los produjo.
#' @export
write_results <- function(res, cs, inv = NULL, dir = LRVAL_DIRS$salida, mut = NULL) {
  dir.create(dir, recursive = TRUE, showWarnings = FALSE)
  utils::write.csv(res, file.path(dir, "resultados.csv"), row.names = FALSE, na = "")

  resumen <- as.data.frame.matrix(table(res$motor, res$veredicto))
  resumen <- cbind(motor = rownames(resumen), resumen)
  utils::write.csv(resumen, file.path(dir, "resumen.csv"), row.names = FALSE)

  div <- unique(divergence_rows(res)[, c("caso", "titulo", "motor", "LR", "gold",
                                         "gold_alt", "motor_ref", "veredicto", "divergencia")])
  utils::write.csv(div, file.path(dir, "convenciones.csv"), row.names = FALSE, na = "")

  ## Nivel 2 medido sobre el sistema evaluado: la grilla de matrices, la
  ## descomposicion del error de cada caso con mutacion y la coherencia de la
  ## convencion detectada para el fundador no tipificado. Son resultados de la
  ## corrida como cualquier otro.
  if (!is.null(mut)) {
    esc <- function(x, f) if (!is.null(x) && nrow(x))
      utils::write.csv(x, file.path(dir, f), row.names = FALSE, na = "")
    esc(mut$informe,       "matrices.csv")
    esc(mut$descomposicion,"descomposicion.csv")
    esc(mut$semantica,     "semantica.csv")
    esc(mut$cobertura,     "cobertura_grilla.csv")
    esc(mut$modelo,        "modelo_por_caso.csv")
    esc(mut$lumpabilidad,  "lumpabilidad.csv")
  }
  ## pares_modelo.csv no es un producto de esta corrida: si hay uno en out/, se
  ## borra para que no pase por resultado de esta corrida.
  for (viejo in c("pares_modelo.csv"))
    if (file.exists(file.path(dir, viejo))) unlink(file.path(dir, viejo))

  ## La lectura del piso de frecuencia es un resultado de la corrida como cualquier otro: si
  ## solo se imprimiera por consola se perderia con el scrollback, y out/ esta
  ## versionado justamente porque los resultados son parte del entregable.
  pisos <- min_freq_pairs(res, cs)
  if (!is.null(pisos))
    utils::write.csv(pisos, file.path(dir, "pares_piso.csv"), row.names = FALSE, na = "")

  utils::write.csv(coverage_report(res, cs), file.path(dir, "cobertura.csv"), row.names = FALSE)
  utils::write.csv(catalog_table(cs), file.path(dir, "catalogo.csv"), row.names = FALSE, na = "")
  utils::write.csv(traceability_matrix(cs), file.path(dir, "trazabilidad.csv"), row.names = FALSE)
  if (!is.null(inv)) utils::write.csv(inv, file.path(dir, "invariantes.csv"), row.names = FALSE)
  writeLines(env_record(), file.path(dir, "entorno.txt"))

  message("Resultados escritos en ", normalizePath(dir))
  invisible(dir)
}

#' Matriz de trazabilidad: caracteristica -> casos que la ejercitan
#'
#' Es lo que exige una auditoria: demostrar que cada requisito funcional
#' esta cubierto por al menos un caso de prueba identificable.
#' @export
traceability_matrix <- function(cs) {
  fs <- sort(unique(unlist(lapply(cs, `[[`, "features"))))
  do.call(rbind, lapply(fs, function(f) {
    ids <- names(cs)[vapply(cs, function(x) f %in% x$features, logical(1))]
    data.frame(caracteristica = f, n_casos = length(ids),
               casos = paste(ids, collapse = " "), stringsAsFactors = FALSE)
  }))
}

## ---------------------------------------------------------------------------
## Escritura que no destruye lo que esta corrida no calculo
##
## Varias tablas del escenario masivo llevan una fila por CONDICION
## (`sin mutacion` / `con mutacion`). Familias con mutacion es opt-in
## (`motores_de_escenario`), asi que una corrida normal calcula una sola; con
## write.csv, o sea sobreescritura completa, borraria la
## otra --900.000 pares y 39,6 h de computo-- terminando bien y sin avisar
## nada.
## ---------------------------------------------------------------------------

#' Marca un hash como no reproducible si el arbol de trabajo estaba sucio
#'
#' Separado de la consulta a git para que la REGLA se pueda probar sin
#' fabricar repositorios. La razon de que exista: sin la marca, una corrida
#' sobre un commit limpio y otra sobre ese mismo commit con archivos
#' modificados llevarian el mismo hash, y la segunda no se reproduce haciendo
#' checkout de ese commit. Mismo criterio que Runner.scala.
#'
#' Sin repositorio no se puede afirmar nada sobre suciedad, asi que
#' "sin-commit" no se sufija: seria una afirmacion sin respaldo.
#' @export
marcar_sucio <- function(hash, sucio) {
  if (identical(hash, "sin-commit") || !isTRUE(sucio)) hash
  else paste0(hash, "-dirty")
}

#' Hash corto del commit actual, con -dirty si el arbol no esta limpio
#'
#' Read-only (`git rev-parse` y `git status`, ninguno toca el repositorio) y
#' tolerante a fallos: el set tiene que poder correr desde un tarball sin
#' repositorio o en una maquina sin `git`. Si el hash se obtuvo pero el estado
#' no, se devuelve el hash solo: mejor un hash sin la advertencia que ninguna
#' procedencia.
#' @export
commit_actual <- function() {
  git <- function(args) tryCatch(
    suppressWarnings(system2("git", args, stdout = TRUE, stderr = FALSE)),
    error = function(e) character(0))
  h <- git(c("rev-parse", "--short", "HEAD"))
  if (!length(h) || is.na(h[1]) || !nzchar(h[1])) return("sin-commit")
  ## Solo archivos YA trackeados: contar los que estan sin trackear de forma
  ## permanente dejaria la marca encendida para siempre, sin distinguir nada.
  ## Mismo criterio que Runner.scala.
  est <- git(c("status", "--porcelain", "--untracked-files=no"))
  marcar_sucio(h[1], length(est) > 0 && any(nzchar(est)))
}

#' Sella una tabla con la fecha y el commit que la produjeron
#'
#' En un mismo archivo conviven filas recien calculadas y filas preservadas de
#' una corrida anterior: sin fecha son indistinguibles.
#' @export
sellar_procedencia <- function(d) {
  if (is.null(d) || !nrow(d)) return(d)
  d$medido <- format(Sys.Date())
  d$commit <- commit_actual()
  d
}

#' rbind que tolera que las dos tablas no tengan las mismas columnas
#'
#' Una tabla escrita sin pasar por `sellar_procedencia` no trae
#' `medido` ni `commit`; unirla con una que si las trae no puede romperse.
#' Las columnas que faltan de un lado quedan en NA, que es justamente lo que
#' se quiere leer: esa fila no es de esta corrida.
#' @export
unir_filas <- function(a, b) {
  if (is.null(a) || !nrow(a)) return(b)
  if (is.null(b) || !nrow(b)) return(a)
  for (n in setdiff(names(b), names(a))) a[[n]] <- NA
  for (n in setdiff(names(a), names(b))) b[[n]] <- NA
  rbind(a, b[, names(a), drop = FALSE])
}

#' Escribe una tabla reemplazando solo las condiciones que esta corrida calculo
#'
#' Las filas de `escenarios` se reemplazan por las de `nuevo`; las de cualquier
#' otra condicion quedan intactas. Si `nuevo` viene vacio, las filas viejas de
#' esas condiciones se BORRAN igual: una re-corrida que pasa de tener filas a
#' cero no puede dejar las viejas como vigentes. Mismo criterio que `anexar()` de
#' scripts/04_comparacion_masiva.R, y por eso `escenarios` es un argumento
#' explicito y no se deriva de `nuevo$escenario`, que no existe cuando `nuevo`
#' es NULL.
#'
#' @param nuevo filas calculadas por esta corrida (puede ser NULL)
#' @param ruta archivo CSV destino
#' @param escenarios condiciones que esta corrida calculo de verdad
#' @export
anexar_por_escenario <- function(nuevo, ruta, escenarios) {
  viejo <- if (file.exists(ruta)) {
    v <- utils::read.csv(ruta, stringsAsFactors = FALSE)
    v[!(v$escenario %in% escenarios), , drop = FALSE]
  } else NULL
  out <- unir_filas(viejo, nuevo)
  if (is.null(out) || !nrow(out)) return(invisible(NULL))
  utils::write.csv(out, ruta, row.names = FALSE)
  invisible(out)
}

## ---------------------------------------------------------------------------
## Sello de procedencia de los veredictos de R
##
## El arnes de GENis deja PROCEDENCIA.txt y condiciones_masiva.txt en formato
## clave = valor, y `validation/correr` los lee con grep/sed. Los dos
## veredictos que emite R --el del catalogo (scripts/00 --con-genis) y la
## comparacion masiva (scripts/04)-- llevan un sello en ese mismo formato,
## para que un informe que los junte pueda saber si son de la misma corrida.
## Estas funciones lo escriben y lo leen del lado R.
## ---------------------------------------------------------------------------

#' Escribe un archivo clave = valor
#'
#' Un valor de largo mayor que uno se escribe como lineas repetidas con la
#' misma clave (como `reanudada` en condiciones_masiva.txt).
#' @export
escribir_procedencia <- function(ruta, campos) {
  ln <- unlist(lapply(names(campos), function(k)
    paste0(k, " = ", as.character(campos[[k]]))), use.names = FALSE)
  dir.create(dirname(ruta), recursive = TRUE, showWarnings = FALSE)
  writeLines(ln, ruta)
  invisible(ruta)
}

#' Lee un archivo clave = valor como vector con nombres
#'
#' Las claves repetidas se conservan todas (los nombres del vector se repiten).
#' Las lineas de comentario (`#`) y las vacias se ignoran. El valor es todo lo
#' que sigue al primer ` = `, sin recortar: `3 (pendiente: ...)` es un valor.
#' Devuelve NULL si el archivo no existe.
#' @export
leer_procedencia <- function(ruta) {
  if (!file.exists(ruta)) return(NULL)
  ln <- readLines(ruta, warn = FALSE)
  ln <- ln[nzchar(trimws(ln)) & !startsWith(trimws(ln), "#")]
  ln <- ln[grepl(" = ", ln, fixed = TRUE)]
  if (!length(ln)) return(character(0))
  k <- trimws(sub(" = .*$", "", ln))
  v <- sub("^[^=]*? = ", "", ln)
  stats::setNames(v, k)
}

#' Entradas de un manifiesto: hash y archivo, sin la cabecera
#' @export
entradas_manifiesto <- function(ruta) {
  if (!file.exists(ruta)) return(NULL)
  ln <- readLines(ruta, warn = FALSE)
  ln <- trimws(ln[nzchar(trimws(ln)) & !startsWith(trimws(ln), "#")])
  if (!length(ln)) return(NULL)
  campos <- strsplit(ln, "[[:space:]]+")
  data.frame(hash = vapply(campos, `[`, "", 1), archivo = vapply(campos, `[`, "", 2),
             stringsAsFactors = FALSE)
}

#' Identidad de un manifiesto por su contenido
#'
#' SHA-256 de sus entradas ordenadas. Dos manifiestos del mismo paquete escritos
#' en momentos distintos tienen cabeceras distintas (la fecha) y la misma
#' identidad; compararlos byte a byte diria que son paquetes distintos.
#' NA si el manifiesto no existe o no tiene entradas.
#' @export
hash_manifiesto <- function(ruta) {
  e <- entradas_manifiesto(ruta)
  if (is.null(e) || !nrow(e)) return(NA_character_)
  claves <- sort(paste(e$hash, e$archivo))
  if (!requireNamespace("digest", quietly = TRUE)) return("digest-no-instalado")
  digest::digest(paste(claves, collapse = "\n"), algo = "sha256", serialize = FALSE)
}

#' Sello del veredicto del bloque 1: que entrega de GENis se juzgo
#'
#' La identidad de la entrega es el SHA-256 de `resultados_genis.csv` mas la
#' `version` que ese archivo declara (artefacto, commit y camino de calculo).
#' @export
sello_bloque1 <- function(dir_vuelta = LRVAL_DIRS$vuelta, dir_paquete = LRVAL_DIRS$paquete) {
  f <- file.path(dir_vuelta, "resultados_genis.csv")
  ver <- if (file.exists(f)) {
    d <- utils::read.csv(f, colClasses = "character", nrows = 1)
    if ("version" %in% names(d) && nrow(d)) d$version[1] else "desconocida"
  } else "desconocida"
  list(commit = commit_actual(),
       fecha = format(Sys.time(), "%Y-%m-%d %H:%M:%S"),
       manifiesto = hash_manifiesto(file.path(dir_paquete, "MANIFIESTO.txt")),
       entrega = "resultados_genis.csv",
       entrega_version = ver,
       entrega_sha256 = if (file.exists(f)) sha256_file(f) else NA_character_)
}

#' Sello del veredicto de la comparacion masiva de una condicion
#'
#' La identidad de la entrega de GENis es su linea `arrancada`, unica por
#' corrida, mas el commit que la calculo y el SHA-256 del archivo de
#' resultados. Sin `condiciones_masiva.txt` no se inventa: queda `desconocida`.
#' @export
sello_comparacion <- function(condicion, dir_entrega, dir_paquete, motores) {
  cond <- leer_procedencia(file.path(dir_entrega, "condiciones_masiva.txt"))
  campo <- function(k) if (!is.null(cond) && k %in% names(cond)) unname(cond[k][1]) else "desconocida"
  f <- file.path(dir_entrega, "resultados_genis_masiva.csv")
  list(commit = commit_actual(),
       fecha = format(Sys.time(), "%Y-%m-%d %H:%M:%S"),
       condicion = condicion,
       motores = paste(motores, collapse = " "),
       manifiesto = hash_manifiesto(file.path(dir_paquete, "MANIFIESTO.txt")),
       entrega = "resultados_genis_masiva.csv",
       entrega_arrancada = campo("arrancada"),
       entrega_commit = campo("commit"),
       entrega_sha256 = if (file.exists(f)) sha256_file(f) else NA_character_)
}

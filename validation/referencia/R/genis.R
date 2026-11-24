## ---------------------------------------------------------------------------
## lrval :: intercambio con software externo (GENis y Familias de escritorio)
##
## Exporta cada caso a archivos planos, sin dependencias de R, y reimporta los
## resultados del sistema externo a la misma tabla de comparacion.
##
## Las frecuencias van alelo por alelo y los modelos de mutacion van por
## parametros: modelo, tasa, tasa2, rango, escalera y pasos. La matriz no
## viaja de ida --el sistema evaluado no tiene donde recibirla-- y viaja de
## vuelta: es el sistema el que vuelca las que uso, y es contra ellas que se
## mide el modelo (ver mutgrid.R).
## ---------------------------------------------------------------------------

#' Parametrizacion del modelo de mutacion de un caso
#'
#' Devuelve, por locus y sexo, el nombre del modelo y los parametros con que se
#' construyo la matriz (`pedmut` los conserva como atributos de la matriz). Es
#' lo que se le entrega al sistema evaluado en `modelos.csv` para TODO caso con
#' mutacion: la matriz no viaja. La escalera alelica va incluida porque el
#' modelo Stepwise depende de ella.
#'
#' `pasos` es la cantidad maxima de pasos que el modelo cubre antes de truncar.
#' Es un parametro real del modelo del sistema evaluado que ningun motor de
#' referencia expone. Un parametro del modelo bajo validacion no se infiere,
#' asi que se declara: un Stepwise sin truncamiento lo lleva igual a la
#' amplitud de la escalera, y el modelo de un paso lo lleva en 1. `equal` no
#' trunca y lo deja vacio.
#'
#' @param case caso del catalogo
#' @return data.frame, o NULL si el caso no tiene modelo de mutacion
#' @export
mut_params <- function(case) {
  src <- if (pedtools::is.ped(case$H1)) case$H1 else case$H1[[1]]
  loci <- pedtools::name(src)
  filas <- list()
  for (j in seq_along(loci)) {
    m  <- pedtools::getMarkers(src, j)[[1]]
    mm <- pedtools::mutmod(m)
    if (is.null(mm)) next
    al <- pedtools::alleles(m)
    for (sx in c("male", "female")) {
      a <- attributes(mm[[sx]])
      ## El nombre que viaja es el del formato de intercambio, no el de
      ## pedmut: pedmut llama "stepwise" tanto al modelo sin tasa2 como al
      ## extendido, y el arnes traduce el nombre al tipo de modelo de GENis
      ## (2 o 3). Misma regla que la grilla (modelo_contrato): extendido solo
      ## si rate2 > 0 y la escalera tiene mas de un grupo decimal.
      mod <- modelo_contrato(a$model %||% NA_character_, a$rate2 %||% NA_real_, al)
      filas[[length(filas) + 1]] <- data.frame(
        caso = case$id, locus = loci[j],
        sexo_progenitor = if (sx == "male") "M" else "F",
        modelo = mod,
        tasa   = a$rate  %||% NA_real_,
        tasa2  = a$rate2 %||% NA_real_,
        rango  = a$range %||% NA_real_,
        alelos = paste(al, collapse = " "),
        ## pedmut no trunca: la matriz cubre toda la escalera
        pasos  = if (identical(mod, "equal")) NA_real_ else ladder_span(al),
        stringsAsFactors = FALSE)
    }
  }
  if (!length(filas)) return(NULL)
  do.call(rbind, filas)
}

#' Formatea un numero con la representacion mas corta que reconstruye el mismo
#' valor al leerlo
#'
#' El criterio de aceptacion esta en 1e-9, de modo que un archivo escrito con
#' seis decimales mediria el formato y no el modelo. Se busca el menor numero
#' de digitos significativos cuyo ida y vuelta devuelva el mismo double.
#' @export
num_exact <- function(x) {
  vapply(x, function(v) {
    if (is.na(v)) return("")
    if (!is.finite(v)) return(as.character(v))
    for (d in 1:17) {
      s <- sprintf("%.*g", d, v)
      if (suppressWarnings(as.numeric(s)) == v) return(s)
    }
    sprintf("%.17g", v)
  }, "", USE.NAMES = FALSE)
}

#' Escribe un CSV del paquete con precision de ida y vuelta
#'
#' Mismo formato que `write.csv(row.names = FALSE, na = "")` --UTF-8, LF, coma,
#' punto decimal, texto entre comillas dobles-- pero con las columnas numericas
#' escritas por `num_exact()`. Se admite notacion cientifica.
#'
#' @param d data.frame; @param file destino
#' @param num nombres de columnas a tratar como numericas aunque lleguen como
#'   texto
#' @export
write_csv_exact <- function(d, file, num = character(0)) {
  d <- as.data.frame(d, stringsAsFactors = FALSE)
  esnum <- names(d) %in% num | vapply(d, is.numeric, TRUE)
  out <- d
  for (k in seq_along(out)) {
    if (esnum[k]) out[[k]] <- num_exact(suppressWarnings(as.numeric(out[[k]])))
    else { v <- as.character(out[[k]]); v[is.na(v)] <- ""; out[[k]] <- v }
  }
  utils::write.table(out, file, sep = ",", row.names = FALSE, na = "",
                     qmethod = "double", quote = which(!esnum))
  invisible(file)
}

#' Exporta el catalogo a un paquete de archivos planos
#'
#' Genera, en `dir`:
#'   casos.csv          un renglon por caso, con el LR de referencia
#'   pedigries.csv      estructura de H1 y H2
#'   perfiles.csv       genotipos observados
#'   frecuencias.csv    base de frecuencias de cada caso y locus; en los casos
#'                      que declaran `piso` el panel va crudo, sin pisar
#'   modelos.csv        modelo y parametros de TODO caso con mutacion
#'   grilla.csv         pedido de matrices, independiente de todo caso
#'   paneles.csv        alelos y frecuencias de los paneles de la grilla
#'   resultados.csv     plantilla vacia para que la complete el sistema
#'   MANIFIESTO.txt     inventario con hash SHA-256 de cada archivo
#'
#' `mutacion.csv` --las matrices celda por celda-- no se entrega. No lo
#' consumiria nadie: el sistema evaluado no tiene ningun punto de entrada que
#' acepte una matriz arbitraria, y los motores internos la generan. Las
#' matrices de referencia existen dentro de lrval; lo que no hay es su
#' entrega. En su lugar el sistema evaluado devuelve LAS SUYAS, y es contra
#' ellas que se mide el modelo (ver `mutgrid.R`).
#'
#' @param cs catalogo
#' @param dir directorio de salida
#' @export
export_bundle <- function(cs, dir = LRVAL_DIRS$paquete) {
  dir.create(dir, recursive = TRUE, showWarnings = FALSE)
  casos <- ped_df <- perf <- frec <- mod <- list()

  for (cid in names(cs)) {
    ca <- cs[[cid]]
    src <- if (pedtools::is.ped(ca$H1)) ca$H1 else ca$H1[[1]]

    casos[[cid]] <- data.frame(
      caso = cid, grupo = ca$grupo, titulo = ca$titulo,
      LR_referencia = ca$gold, fuente_referencia = ca$gold_src,
      LR_otra_convencion = ca$gold_alt %||% NA_real_,
      theta = ca$theta, piso = ca$piso, motor_referencia = ca$motor_ref,
      diferencia_de_convencion = ca$divergencia,
      caracteristicas = paste(ca$features, collapse = ";"),
      bibliografia = ca$ref, nota = ca$nota, stringsAsFactors = FALSE)

    for (h in c("H1", "H2")) {
      x <- ca[[h]]
      if (pedtools::is.ped(x)) x <- list(x)
      d <- do.call(rbind, lapply(x, function(q) as.data.frame(q)[, c("id","fid","mid","sex")]))
      ped_df[[paste(cid, h)]] <- data.frame(
        caso = cid, hipotesis = h, individuo = d$id,
        padre = ifelse(d$fid == "0", "", d$fid),
        madre = ifelse(d$mid == "0", "", d$mid),
        sexo = ifelse(d$sex == 1, "M", "F"), stringsAsFactors = FALSE)
    }

    dm <- ped_to_datamatrix(ca$H1)
    loci <- pedtools::name(src)
    if (nrow(dm)) perf[[cid]] <- do.call(rbind, lapply(seq_along(loci), function(j)
      data.frame(caso = cid, individuo = rownames(dm), locus = loci[j],
                 alelo_1 = dm[, 2*j-1], alelo_2 = dm[, 2*j],
                 stringsAsFactors = FALSE)))

    for (j in seq_along(loci)) {
      m  <- pedtools::getMarkers(src, j)[[1]]
      ## En los casos que declaran piso el panel viaja CRUDO: aplicarlo es del
      ## sistema bajo validacion, y es lo que se mide. El panel pisado es el
      ## que consumen los motores de referencia y el que va al .fam.
      af <- ca$freqs_crudas[[loci[j]]]
      if (is.null(af)) af <- pedtools::afreq(m)
      frec[[paste(cid, j)]] <- data.frame(
        caso = cid, locus = loci[j], alelo = names(af),
        frecuencia = as.numeric(af), stringsAsFactors = FALSE)
    }
    mp <- mut_params(ca)
    if (!is.null(mp)) mod[[cid]] <- mp
  }

  esc <- function(x, f, num = character(0)) {
    d <- do.call(rbind, x)
    write_csv_exact(d, file.path(dir, f), num = num)
    d
  }
  ca_df <- esc(casos, "casos.csv")
  esc(ped_df, "pedigries.csv"); esc(perf, "perfiles.csv"); esc(frec, "frecuencias.csv")
  if (length(mod)) esc(mod, "modelos.csv") else
    write_csv_exact(data.frame(caso = character(0), locus = character(0),
                               sexo_progenitor = character(0), modelo = character(0),
                               tasa = numeric(0), tasa2 = numeric(0),
                               rango = numeric(0), alelos = character(0),
                               pasos = numeric(0)),
                    file.path(dir, "modelos.csv"))

  ## Pedido de matrices: no depende de ningun caso ni de ningun pedigri, y no
  ## lleva valor de referencia. El sistema evaluado recibe la pregunta y
  ## devuelve el numero y la matriz; nunca recibe la respuesta esperada.
  export_grid(cs, dir)

  ## Plantilla de resultados: es el unico archivo que el sistema debe devolver.
  ## No se pisa si ya viene completada: la exportacion corre despues de la
  ## importacion dentro de la misma corrida, y reescribirla en blanco
  ## incondicionalmente destruiria la entrada de esa corrida. La via
  ## recomendada es devolver el archivo aparte
  ## (`../out/resultados_genis.csv`) y no editar el del paquete.
  f_res <- file.path(dir, "resultados.csv")
  if (results_template_filled(f_res)) {
    message("export_bundle: ", f_res, " ya tiene resultados; se conserva")
  } else {
    utils::write.csv(data.frame(
      caso = ca_df$caso, sistema = "", version = "", LR = "", log10LR = "",
      estado = "", observaciones = "", stringsAsFactors = FALSE),
      f_res, row.names = FALSE)
  }

  ## El directorio puede traer un mutacion.csv de otra exportacion. Si se
  ## quedara, el manifiesto lo inventariaria y el sistema evaluado veria un
  ## archivo que no forma parte del paquete.
  viejo <- file.path(dir, "mutacion.csv")
  if (file.exists(viejo)) unlink(viejo)

  write_manifest(dir)
  message("Paquete exportado en ", normalizePath(dir))
  invisible(dir)
}


#' Indica si una plantilla de resultados ya trae algo cargado
#' @export
results_template_filled <- function(file) {
  if (!file.exists(file)) return(FALSE)
  d <- try(utils::read.csv(file, colClasses = "character"), silent = TRUE)
  if (inherits(d, "try-error") || !nrow(d)) return(FALSE)
  cols <- intersect(c("sistema", "LR", "estado"), names(d))
  any(vapply(cols, function(k) any(nzchar(trimws(d[[k]]))), TRUE))
}

#' SHA-256 de un archivo
#'
#' Mismo calculo al escribir el manifiesto y al verificarlo, para que las dos
#' operaciones no puedan divergir.
#' @export
sha256_file <- function(f) {
  if (requireNamespace("digest", quietly = TRUE))
    digest::digest(file = f, algo = "sha256") else "digest-no-instalado"
}

#' Escribe el manifiesto con hash SHA-256 de cada archivo del paquete
#'
#' Da trazabilidad: el veredicto de la corrida queda ligado a un conjunto de
#' datos de entrada verificable.
#' @export
write_manifest <- function(dir) {
  ## Solo archivos regulares del nivel superior: los subdirectorios (p.ej.
  ## fam/) llevan su propio inventario.
  fs <- setdiff(list.files(dir), "MANIFIESTO.txt")
  fs <- fs[!dir.exists(file.path(dir, fs))]
  h <- vapply(file.path(dir, fs), sha256_file, "")
  writeLines(c(
    paste("# Paquete de validacion lrval -", format(Sys.time(), "%Y-%m-%d %H:%M:%S")),
    paste("# R", getRversion(), "| pedtools", as.character(utils::packageVersion("pedtools")),
          "| Familias", as.character(utils::packageVersion("Familias"))),
    "", sprintf("%-64s  %s", h, fs)),
    file.path(dir, "MANIFIESTO.txt"))
  invisible(TRUE)
}

#' Lee los resultados devueltos por un sistema externo
#'
#' Se importa toda fila que el sistema haya *informado*, tenga o no un LR.
#' Una fila con `estado` distinto de vacio y sin LR --`no_representable`,
#' `no_soportado`, `error`-- es un resultado de la validacion, no una ausencia:
#' dice que el sistema no puede expresar esa entrada o no implementa esa
#' funcion. Descartarla haria que la corrida informara menos casos de los que
#' el sistema contesto, y justo los que documentan un limite del sistema.
#'
#' Lo que se descarta es la fila intacta de la plantilla (sin LR y sin estado):
#' esa si es un caso sobre el que el sistema no dijo nada. Un `SIN_DATO` por
#' fila informada y una fila ausente son cosas distintas y se cuentan aparte;
#' ver `coverage_report()`.
#'
#' @param file ruta al archivo de resultados devuelto (`resultados_genis.csv`)
#' @param cs catalogo
#' @param tol tolerancias
#' @return data.frame homogeneo con los mismos campos que `run_catalog()`
#' @export
import_results <- function(file, cs, tol = LRVAL_TOL) {
  d <- utils::read.csv(file, colClasses = "character")
  falta_col <- setdiff(c("caso", "sistema", "version", "LR", "estado", "observaciones"),
                       names(d))
  if (length(falta_col))
    stop("import_results: faltan columnas en ", file, ": ",
         paste(falta_col, collapse = ", "))
  tr <- function(x) trimws(as.character(x))
  informada <- nzchar(tr(d$LR)) | nzchar(tr(d$estado))
  d <- d[informada, , drop = FALSE]
  if (!nrow(d)) return(NULL)

  falta <- setdiff(d$caso, names(cs))
  if (length(falta))
    stop("import_results: casos desconocidos en el archivo: ", paste(falta, collapse = ", "))
  if (any(!nzchar(tr(d$sistema))))
    stop("import_results: filas sin declarar 'sistema': ",
         paste(d$caso[!nzchar(tr(d$sistema))], collapse = ", "))
  dup <- paste(d$caso, tr(d$sistema))
  if (anyDuplicated(dup))
    stop("import_results: filas duplicadas (caso x sistema): ",
         paste(unique(dup[duplicated(dup)]), collapse = ", "))

  ## Un LR que no parsea no puede quedar como SIN_DATO silencioso: se
  ## confundiria con un caso que el sistema declaro no poder calcular.
  crudo <- tr(d$LR)
  lr <- suppressWarnings(as.numeric(crudo))
  malo <- nzchar(crudo) & is.na(lr)
  if (any(malo))
    stop("import_results: LR no interpretable como numero en ",
         paste(sprintf("%s ('%s')", d$caso[malo], crudo[malo]), collapse = ", "))

  do.call(rbind, lapply(seq_len(nrow(d)), function(i) {
    ca <- cs[[d$caso[i]]]
    sis <- tr(d$sistema[i])
    cmp <- compare_lr(lr[i], ca$gold, tol)
    est <- if (nzchar(tr(d$estado[i]))) tr(d$estado[i]) else "ok"
    cmp$veredicto <- apply_divergence(ca, sis, cmp$veredicto, est, lr[i], tol)
    data.frame(caso = d$caso[i], grupo = ca$grupo, titulo = ca$titulo,
               motor = sis, version = tr(d$version[i]),
               LR = lr[i], log10LR = suppressWarnings(log10(lr[i])),
               L1 = NA_real_, L2 = NA_real_,
               gold = ca$gold, gold_src = ca$gold_src, gold_alt = ca$gold_alt %||% NA_real_,
               motor_ref = ca$motor_ref, divergencia = ca$divergencia,
               disc_rel = cmp$rel, disc_log10 = cmp$dlog10, veredicto = cmp$veredicto,
               estado = est, mensaje = d$observaciones[i], segundos = NA_real_,
               features = paste(ca$features, collapse = ";"), stringsAsFactors = FALSE)
  }))
}

#' Cobertura de un sistema sobre el catalogo
#'
#' Una entrega parcial es un escenario valido de una validacion por etapas, y
#' por eso hay que poder leerla como tal. Separa tres situaciones que de otro
#' modo se confunden bajo un mismo recuento: el caso que el sistema calculo, el
#' que informo declarando que no puede resolverlo, y el que no contesto.
#'
#' @param res tabla de resultados; @param cs catalogo
#' @return data.frame con una fila por motor
#' @export
coverage_report <- function(res, cs) {
  n <- length(cs)
  do.call(rbind, lapply(sort(unique(res$motor)), function(mo) {
    d <- res[res$motor == mo, , drop = FALSE]
    calculados <- sum(!is.na(d$LR))
    data.frame(motor = mo, catalogo = n,
               informados = nrow(d), calculados = calculados,
               sin_calcular = nrow(d) - calculados,
               no_informados = n - nrow(d),
               estados = paste(sort(unique(d$estado[is.na(d$LR)])), collapse = ";"),
               stringsAsFactors = FALSE)
  }))
}

#' Verifica un paquete contra un MANIFIESTO.txt
#'
#' Los resultados de un sistema externo valen para la exportacion concreta que
#' ese sistema consumio. Sin esta comprobacion nada impide comparar contra el
#' catalogo de hoy unos resultados calculados sobre una exportacion anterior,
#' con otros genotipos o otras frecuencias, y la corrida no lo notaria.
#'
#' `excluir` deja fuera de la comparacion archivos que el manifiesto inventaria
#' igual. El manifiesto sigue cubriendo el paquete completo --de eso depende que
#' el sistema evaluado pueda verificar de su lado lo que recibio-- pero no todo
#' archivo del paquete es una entrada del calculo: `resultados.csv` es el canal
#' de vuelta, y que su hash difiera no dice nada sobre si las entradas
#' cambiaron. Ver `results_package_ok()`.
#'
#' @param dir directorio del paquete
#' @param manifiesto ruta al manifiesto a verificar (por defecto el del paquete)
#' @param excluir nombres de archivo a omitir de la comparacion
#' @return data.frame con una fila por archivo comparado, o NULL si `excluir`
#'   deja fuera a todos
#' @export
verify_manifest <- function(dir = LRVAL_DIRS$paquete, manifiesto = file.path(dir, "MANIFIESTO.txt"),
                            excluir = character(0)) {
  if (!file.exists(manifiesto)) stop("verify_manifest: no existe ", manifiesto)
  ln <- readLines(manifiesto, warn = FALSE)
  ln <- ln[nzchar(trimws(ln)) & !startsWith(ln, "#")]
  if (!length(ln)) stop("verify_manifest: manifiesto sin entradas: ", manifiesto)
  campos <- strsplit(trimws(ln), "[[:space:]]+")
  if (length(excluir))
    campos <- campos[!vapply(campos, function(x) x[2] %in% excluir, TRUE)]
  if (!length(campos)) return(NULL)
  do.call(rbind, lapply(campos, function(x) {
    f <- file.path(dir, x[2])
    act <- if (file.exists(f)) sha256_file(f) else NA_character_
    data.frame(archivo = x[2], esperado = x[1], obtenido = act,
               veredicto = if (identical(act, x[1])) "OK"
                           else if (is.na(act)) "FALTA" else "DIFIERE",
               stringsAsFactors = FALSE)
  }))
}

#' Verifica que una entrega externa corresponda al paquete actual
#'
#' Los resultados de un sistema evaluado valen para la exportacion que ese
#' sistema consumio, y nada mas. Si el paquete se reemitio despues de la
#' entrega, comparar esas cifras contra el catalogo actual puede dar PASA sobre
#' casos que cambiaron de contenido: es el falso verde que el set existe para
#' evitar. Sin manifiesto no hay forma de saberlo, y entonces tampoco se
#' incorpora.
#'
#' La plantilla completada in situ es la excepcion: no viaja a ningun lado, asi
#' que su paquete es `dir` mismo. Pero verificarla contra el `MANIFIESTO.txt`
#' que hay en el directorio DESPUES de reexportar seria una tautologia --el
#' manifiesto recien escrito describe los archivos recien escritos, y da OK
#' siempre--, asi que quien la juzga tiene que pasar por `manifiesto` la copia
#' del manifiesto ANTERIOR a la reexportacion (ver `snapshot_manifest()`).
#'
#' `resultados.csv` queda fuera de la comparacion en todos los casos: es el
#' canal de vuelta, no una entrada del calculo. Compararlo rechazaria una
#' entrega legitima solo porque alguien completo la plantilla del paquete, y
#' aceptaria una vencida si el manifiesto se reescribio con la plantilla ya
#' completada.
#'
#' Del directorio de vuelta se consideran los dos nombres que puede llevar la
#' copia del manifiesto: `MANIFIESTO-paquete.txt` --como lo nombra lrval-- y
#' `MANIFIESTO.txt` --como lo nombra el arnes del sistema evaluado--. Si
#' existe uno solo, se usa ese. Si existen los dos, se comparan por contenido:
#' iguales, se usa cualquiera; distintos, ninguno de los dos nombres es
#' autoritativo sobre cual describe la entrega real, asi que se rechaza en vez
#' de preferir uno por orden --preferir por orden es exactamente lo que
#' aceptaria un `MANIFIESTO-paquete.txt` viejo, olvidado de un intercambio
#' anterior, por encima del `MANIFIESTO.txt` que el arnes acaba de escribir.
#'
#' @param f archivo de resultados de la entrega
#' @param dir paquete contra el que se verifica
#' @param manifiesto manifiesto contra el que se compara; NULL lo deriva de `f`
#' @return lista con `ok`, `motivo` y la tabla de `verify_manifest()` o NULL
#' @export
results_package_ok <- function(f, dir = LRVAL_DIRS$paquete, manifiesto = NULL) {
  npath <- function(p) normalizePath(p, winslash = "/", mustWork = FALSE)
  if (!is.null(manifiesto)) {
    man <- manifiesto
  } else if (identical(npath(dirname(f)), npath(dir))) {
    man <- file.path(dir, "MANIFIESTO.txt")
  } else {
    ## El sistema evaluado deja en su directorio de salida una copia del
    ## manifiesto del paquete que consumio. lrval la escribe como
    ## MANIFIESTO-paquete.txt y el arnes de GENis como MANIFIESTO.txt. Ninguno
    ## de los dos nombres es autoritativo, asi que preferir uno por orden
    ## aceptaria un MANIFIESTO-paquete.txt viejo -- olvidado de un intercambio
    ## anterior -- por encima del MANIFIESTO.txt que el arnes acaba de
    ## escribir, y una entrega calculada contra otro paquete pasaria en
    ## silencio. Si existen los dos, se comparan por contenido: iguales,
    ## cualquiera sirve; distintos, no hay forma de saber cual describe la
    ## entrega real y se rechaza.
    cand <- file.path(dirname(f), c("MANIFIESTO-paquete.txt", "MANIFIESTO.txt"))
    hay <- cand[file.exists(cand)]
    if (length(hay) == 2L && !identical(sha256_file(hay[1]), sha256_file(hay[2])))
      return(list(ok = FALSE,
                  motivo = paste0("los dos manifiestos de la entrega no coinciden: ",
                                   hay[1], " y ", hay[2]),
                  vm = NULL))
    man <- if (length(hay)) hay[1] else cand[1]
  }
  if (!file.exists(man))
    return(list(ok = FALSE, motivo = paste0("sin manifiesto (", man, ")"), vm = NULL))
  vm <- verify_manifest(dir, man, excluir = "resultados.csv")
  if (is.null(vm))
    return(list(ok = FALSE, motivo = paste0("manifiesto sin entradas comparables (", man, ")"),
                vm = NULL))
  if (all(vm$veredicto == "OK"))
    return(list(ok = TRUE, motivo = "coincide con el paquete actual", vm = vm))
  list(ok = FALSE, motivo = "el paquete que consumio no es el actual", vm = vm)
}

#' Guarda una copia del manifiesto del paquete antes de reexportarlo
#'
#' La plantilla de resultados completada in situ solo puede juzgarse contra el
#' manifiesto del paquete que quien la completo tenia delante, es decir el
#' anterior a la reexportacion de esta corrida. Como `export_bundle()` reescribe
#' `MANIFIESTO.txt`, hay que sacar la copia antes.
#'
#' @param dir directorio del paquete
#' @param dest destino de la copia
#' @return la ruta de la copia, o NULL si el paquete todavia no tiene manifiesto
#' @export
snapshot_manifest <- function(dir = LRVAL_DIRS$paquete,
                              dest = tempfile("MANIFIESTO-previo-", fileext = ".txt")) {
  src <- file.path(dir, "MANIFIESTO.txt")
  if (!file.exists(src)) return(NULL)
  if (!file.copy(src, dest, overwrite = TRUE)) return(NULL)
  dest
}

#' Exporta cada caso como archivo .fam de Familias
#'
#' Permite abrir los casos en el Familias de escritorio y es la via para
#' validar versiones nuevas de Familias contra las mismas entradas.
#' Requiere el paquete pedFamilias.
#'
#' @export
export_fam <- function(cs, dir = file.path(LRVAL_DIRS$paquete, "fam")) {
  if (!requireNamespace("pedFamilias", quietly = TRUE)) {
    warning("export_fam: se necesita el paquete pedFamilias; se omite este paso")
    return(invisible(NULL))
  }
  dir.create(dir, recursive = TRUE, showWarnings = FALSE)
  ok <- character(0)
  for (cid in names(cs)) {
    ca <- cs[[cid]]
    f <- file.path(dir, paste0(cid, ".fam"))
    r <- try(pedFamilias::writeFam(ca$H1, ca$H2, famfile = f, verbose = FALSE), silent = TRUE)
    if (!inherits(r, "try-error")) ok <- c(ok, cid)
  }
  message("Archivos .fam escritos: ", length(ok), " de ", length(cs))
  invisible(ok)
}

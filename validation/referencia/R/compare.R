## ---------------------------------------------------------------------------
## lrval :: comparacion y criterio de aceptacion
##
## Criterio primario  : LR de cada motor contra el valor de referencia.
## Criterio secundario: acuerdo entre motores. No sustituye al primario, porque
##                      dos programas pueden coincidir y estar ambos mal.
##
## Metrica: discrepancia relativa |LR - LRref| / max(|LRref|, tol_abs), y
## log10(LR), que es la escala en que se informa el LR en un dictamen.
## ---------------------------------------------------------------------------

#' Tolerancias por defecto
#'
#' `rel` es la discrepancia relativa maxima admisible. 1e-9 es holgado frente
#' a la precision doble (~1e-16) y estricto frente a cualquier error de
#' modelo real, que siempre se manifiesta con discrepancias >> 1e-6.
#' @export
LRVAL_TOL <- list(rel = 1e-9, abs = 1e-300, log10 = 1e-9)

#' Motores internos del set
#'
#' Son los dos motores que corren dentro de R y para los que cada diferencia
#' de convencion del catalogo esta analizada y documentada caso por caso: el
#' campo `divergencia` describe la diferencia y `motor_ref` indica cual de los
#' dos corresponde al valor de referencia.
#' @export
INTERNAL_ENGINES <- c("pedsuite", "familias")

#' Reclasifica una discrepancia como CONVENCION cuando corresponde
#'
#' Una diferencia de convencion NO es un fallo del motor: dos programas que
#' siguen convenciones distintas para un mismo registro dan numeros distintos
#' sin que ninguno calcule mal. Se informa aparte y se lista siempre.
#'
#' Hay dos caminos, y el segundo vale solo para los motores internos:
#'
#'  - MEDIDO, para cualquier motor: el caso trae `gold_alt`, el LR bajo la otra
#'    convencion en forma cerrada, y el motor da exactamente ese valor (misma
#'    tolerancia que PASA). Un numero que no es ni el de referencia ni el de la
#'    otra convencion sigue siendo FALLA: asi un error de calculo no queda
#'    absorbido como diferencia de convencion.
#'  - DECLARADO, solo motores internos: el caso declara `divergencia` y el
#'    motor no es el `motor_ref`. Para esos dos motores la diferencia esta
#'    analizada caso por caso; para un sistema externo, no.
#'
#' @param ca caso del catalogo
#' @param motor nombre del motor que reporta
#' @param veredicto veredicto emitido por `compare_lr`
#' @param estado estado devuelto por el motor ("ok", "error", ...)
#' @param lr el LR que informo el motor
#' @return el veredicto, reclasificado o tal cual
#' @export
apply_divergence <- function(ca, motor, veredicto, estado = "ok", lr = NA_real_,
                             tol = LRVAL_TOL) {
  alt <- ca$gold_alt
  if (veredicto %in% c("FALLA", "REVISAR") && !is.null(alt) && !is.na(alt) &&
      identical(compare_lr(lr, alt, tol)$veredicto, "PASA"))
    return("CONVENCION")
  if (!is.na(ca$divergencia) && motor %in% INTERNAL_ENGINES &&
      !identical(ca$motor_ref, motor) &&
      (veredicto %in% c("FALLA", "REVISAR") || identical(estado, "error")))
    "CONVENCION" else veredicto
}

#' Compara dos LR y emite un veredicto
#' @export
compare_lr <- function(obs, ref, tol = LRVAL_TOL) {
  if (is.na(obs) || is.na(ref))
    return(list(rel = NA_real_, dlog10 = NA_real_, veredicto = "SIN_DATO"))
  ## ambos cero: coincidencia exacta esperada (exclusion)
  if (ref == 0 && obs == 0)
    return(list(rel = 0, dlog10 = 0, veredicto = "PASA"))
  if (ref == 0 || obs == 0)
    return(list(rel = Inf, dlog10 = Inf, veredicto = "FALLA"))
  rel <- abs(obs - ref) / max(abs(ref), tol$abs)
  dl  <- abs(log10(obs) - log10(ref))
  list(rel = rel, dlog10 = dl,
       veredicto = if (rel <= tol$rel) "PASA" else if (rel <= 1e-6) "REVISAR" else "FALLA")
}

#' Ejecuta el catalogo completo en todos los motores y arma la tabla de resultados
#'
#' @param cs catalogo (`build_catalog()`)
#' @param engines motores a usar
#' @param tol tolerancias
#' @return data.frame con una fila por caso y motor
#' @export
run_catalog <- function(cs, engines = INTERNAL_ENGINES, tol = LRVAL_TOL,
                        verbose = TRUE) {
  filas <- list()
  for (cid in names(cs)) {
    ca <- cs[[cid]]
    if (verbose) message("  ", cid, " ", ca$titulo)
    res <- run_all_engines(ca, engines)
    for (en in names(res)) {
      r <- res[[en]]
      cmp <- compare_lr(r$LR, ca$gold, tol)
      cmp$veredicto <- apply_divergence(ca, en, cmp$veredicto, r$status, r$LR, tol)
      filas[[length(filas) + 1]] <- data.frame(
        caso = cid, grupo = ca$grupo, titulo = ca$titulo, motor = en,
        version = engine_version(en),
        LR = r$LR, log10LR = suppressWarnings(log10(r$LR)),
        L1 = r$L1, L2 = r$L2,
        gold = ca$gold, gold_src = ca$gold_src, gold_alt = ca$gold_alt %||% NA_real_,
        motor_ref = ca$motor_ref, divergencia = ca$divergencia,
        disc_rel = cmp$rel, disc_log10 = cmp$dlog10, veredicto = cmp$veredicto,
        estado = r$status, mensaje = r$msg, segundos = r$secs,
        features = paste(ca$features, collapse = ";"),
        stringsAsFactors = FALSE)
    }
  }
  do.call(rbind, filas)
}

#' Acuerdo entre motores para cada caso
#'
#' Criterio secundario.
#' @export
engine_agreement <- function(res, tol = LRVAL_TOL) {
  sp <- split(res, res$caso)
  do.call(rbind, lapply(names(sp), function(cid) {
    d <- sp[[cid]]
    lrs <- d$LR
    ok  <- !is.na(lrs)
    if (sum(ok) < 2)
      return(data.frame(caso = cid, n_motores = sum(ok), disc_max = NA_real_,
                        veredicto = "SIN_DATO", stringsAsFactors = FALSE))
    v <- lrs[ok]
    disc <- if (all(v == 0)) 0 else if (any(v == 0)) Inf else
      (max(v) - min(v)) / max(abs(v))
    data.frame(caso = cid, n_motores = sum(ok), disc_max = disc,
               veredicto = if (disc <= tol$rel) "PASA"
                           else if (disc <= 1e-6) "REVISAR" else "FALLA",
               stringsAsFactors = FALSE)
  }))
}

#' Matrices de referencia del locus con mutacion de un caso
#'
#' Devuelve la matriz masculina y la femenina del primer locus que declara
#' modelo de mutacion. Es `M_ref`: la que genera pedmut a partir de los
#' parametros del caso, y la que consumen los dos motores internos (pedsuite
#' directamente, Familias por `MutationModel = "Custom"`).
#'
#' @param ca caso del catalogo
#' @return lista con `locus`, `male` y `female`, o NULL
#' @export
case_ref_matrices <- function(ca) {
  src <- if (pedtools::is.ped(ca$H1)) ca$H1 else ca$H1[[1]]
  for (j in seq_len(pedtools::nMarkers(src))) {
    m  <- pedtools::getMarkers(src, j)[[1]]
    mm <- pedtools::mutmod(m)
    if (is.null(mm)) next
    return(list(locus = pedtools::name(m),
                male = as.matrix(mm$male), female = as.matrix(mm$female)))
  }
  NULL
}

#' Las cuatro referencias de un caso con mutacion
#'
#' La expresion cerrada del caso (`gold_mut`) recibe la matriz y la convencion
#' para el fundador no tipificado como argumentos, de modo que las cuatro salen
#' de la misma formula:
#'
#'                    con M_ref                 con M_sis
#'   conserva     el `gold` del catalogo     referencia de algoritmo
#'   poda         segunda convencion         referencia de algoritmo, podada
#'
#' Pasarle `M_sis` a la formula cerrada no compromete la independencia del
#' patron primario: se le cambia una entrada, no un pedazo del razonamiento, y
#' la expresion sigue siendo la de la publicacion. La matriz entra como dato.
#'
#' Fijar la matriz NO fija la convencion para el fundador no tipificado --entra
#' en la formula por q = M^t p contra p-- y por eso la variable que queda
#' suelta se enumera en vez de dejarse abierta.
#'
#' @param ca caso; @param M_sis lista con `male`/`female` volcada por el
#'   sistema, o NULL
#' @return lista de cuatro valores, o NULL si el caso no tiene forma cerrada
#' @export
mut_references <- function(ca, M_sis = NULL) {
  if (is.null(ca$gold_mut)) return(NULL)
  Mr <- case_ref_matrices(ca)
  if (is.null(Mr)) return(NULL)
  ev <- function(M, podado) {
    v <- try(ca$gold_mut(M$male, M$female, podado), silent = TRUE)
    if (inherits(v, "try-error")) NA_real_ else as.numeric(v)
  }
  out <- list(ref_conserva = ev(Mr, FALSE), ref_poda = ev(Mr, TRUE),
              sis_conserva = NA_real_, sis_poda = NA_real_)
  if (!is.null(M_sis)) {
    out$sis_conserva <- ev(M_sis, FALSE)
    out$sis_poda     <- ev(M_sis, TRUE)
  }
  out
}

#' Matrices que el sistema evaluado volco para un caso
#'
#' Se exige que el universo de la matriz volcada coincida con el de la matriz
#' de referencia: una matriz sobre otro conjunto de alelos no es la misma
#' pregunta, y evaluarla en la formula cerrada daria un numero que no compara
#' con nada.
#'
#' @param ca caso; @param mats volcado de `import_matrices()`
#' @export
case_sys_matrices <- function(ca, mats) {
  Mr <- case_ref_matrices(ca)
  if (is.null(Mr) || is.null(mats)) return(NULL)
  M <- find_matrix(mats, "caso", ca$id, Mr$locus, "M")
  F <- find_matrix(mats, "caso", ca$id, Mr$locus, "F")
  if (is.null(M) && is.null(F)) {
    ## sexo indistinto: una sola matriz para los dos progenitores
    I <- find_matrix(mats, "caso", ca$id, Mr$locus, "I")
    if (is.null(I)) return(NULL)
    M <- I; F <- I
  }
  if (is.null(M)) M <- F
  if (is.null(F)) F <- M
  if (!setequal(rownames(M), rownames(Mr$male)) ||
      !setequal(rownames(F), rownames(Mr$female)))
    stop("case_sys_matrices: el universo volcado para ", ca$id,
         " no es el de la escalera del caso")
  list(locus = Mr$locus,
       male   = M[rownames(Mr$male),   colnames(Mr$male),   drop = FALSE],
       female = F[rownames(Mr$female), colnames(Mr$female), drop = FALSE])
}

#' Descomposicion del error de un caso con mutacion
#'
#' Entre el LR que informa el sistema, `A(M_sis)`, y el valor de referencia del
#' catalogo, `G(M_ref)`, cambian dos cosas a la vez y una diferencia no se puede
#' atribuir. Agregando el tercer numero, `G(M_sis)` --la misma formula cerrada
#' con la matriz que el sistema uso-- la diferencia total se parte en sumandos
#' exactos, en escala logaritmica:
#'
#'   A(M_sis) - G_c(M_ref) = [A(M_sis) - G_s(M_sis)]   error de algoritmo
#'                         + [G_s(M_sis) - G_s(M_ref)] error de modelo
#'                         + [G_s(M_ref) - G_c(M_ref)] efecto de la convencion
#'
#' con `s` la convencion detectada para el fundador no tipificado (columna
#' `semantica`) y `c` la que conserva el fundador. El primer
#' sumando compara dos caminos de calculo sobre LA MISMA matriz: no puede
#' contener error de modelo. El segundo es la formula cerrada consigo misma
#' cambiando solo la matriz: no puede contener error de algoritmo. El tercero
#' es cero cuando el sistema conserva el fundador, y entonces la identidad se
#' reduce a los dos primeros sumandos. La identidad es aritmetica.
#'
#' Que `A(M_sis)` coincida con `G(M_sis)` NO dice que la respuesta sea correcta:
#' dice que el sistema calculo bien con la matriz que tiene. Un sistema que
#' ignorase el modelo tendria matriz identidad y coincidiria trivialmente. Por
#' eso el veredicto de algoritmo nunca se informa solo, y la conformidad sigue
#' siendo `A(M_sis)` contra `G(M_ref)`.
#'
#' Para los dos motores internos `M_sis` ES `M_ref` --pedsuite consume la matriz
#' del caso y a Familias se le pasa explicita como "Custom"--, de modo que el
#' error de modelo es cero por construccion y la lectura util es la convencion.
#'
#' @param res tabla de `run_catalog()` o de `import_results()`
#' @param cs catalogo
#' @param mats volcado de `import_matrices()`, o NULL
#' @param motor_mats nombre del motor al que pertenece el volcado
#' @export
mut_decomposition <- function(res, cs, mats = NULL, motor_mats = NULL,
                              tol = LRVAL_TOL) {
  filas <- list()
  for (i in seq_len(nrow(res))) {
    ca <- cs[[res$caso[i]]]
    if (is.null(ca) || is.null(ca$gold_mut)) next
    mo <- res$motor[i]
    interno <- mo %in% INTERNAL_ENGINES
    Msis <- NULL; origen <- "ausente"
    if (interno) {
      Msis <- case_ref_matrices(ca); origen <- "referencia"
    } else if (!is.null(mats) && (is.null(motor_mats) || identical(mo, motor_mats))) {
      Msis <- try(case_sys_matrices(ca, mats), silent = TRUE)
      if (inherits(Msis, "try-error")) Msis <- NULL
      if (!is.null(Msis)) origen <- "volcada"
    }
    rf <- mut_references(ca, Msis)
    lr <- res$LR[i]

    ## Convencion: cual de las dos filas explica el numero. Se elige por
    ## distancia relativa sobre la columna de M_sis, que es la unica que puede
    ## contener el LR del sistema sin arrastrar error de modelo.
    ##
    ## Primero se pregunta si el caso puede discriminar: cuando las dos
    ## convenciones predicen el MISMO numero --incluido el caso en que las dos
    ## predicen cero, que es lo que pasa cuando la matriz del sistema anula la
    ## transmision-- no hay nada que elegir, y decidirlo por distancia
    ## logaritmica devolveria NA y perderia una referencia de algoritmo que esta
    ## perfectamente determinada.
    igual_ref <- !is.na(rf$sis_conserva) && !is.na(rf$sis_poda) &&
      (rf$sis_conserva == rf$sis_poda ||
         abs(rf$sis_conserva - rf$sis_poda) <=
           tol$rel * max(abs(rf$sis_conserva), abs(rf$sis_poda)))
    dist <- function(x) if (is.na(x) || is.na(lr) || x == 0 || lr == 0) Inf
                        else abs(log10(lr) - log10(x))
    dc <- dist(rf$sis_conserva); dp <- dist(rf$sis_poda)
    sem <- if (igual_ref) "indistinguible"
           else if (!is.finite(dc) && !is.finite(dp)) NA_character_
           else if (is.finite(dc) && is.finite(dp) &&
                    abs(dc - dp) <= tol$log10) "indistinguible"
           else if (dc <= dp) "conserva" else "poda"
    g_sis <- switch(sem %||% "", conserva = rf$sis_conserva, poda = rf$sis_poda,
                    indistinguible = rf$sis_conserva, NA_real_)
    g_ref <- switch(sem %||% "", conserva = rf$ref_conserva, poda = rf$ref_poda,
                    indistinguible = rf$ref_conserva, NA_real_)

    l10 <- function(a, b) if (is.na(a) || is.na(b) || a <= 0 || b <= 0) NA_real_
                          else log10(a) - log10(b)
    filas[[length(filas) + 1]] <- data.frame(
      caso = res$caso[i], motor = mo, matriz = origen, LR = lr,
      ref_conformidad = rf$ref_conserva, ref_segunda_semantica = rf$ref_poda,
      ref_algoritmo = g_sis, semantica = sem %||% NA_character_,
      ver_algoritmo = compare_lr(lr, g_sis, tol)$veredicto,
      ver_conformidad = compare_lr(lr, rf$ref_conserva, tol)$veredicto,
      d_total     = l10(lr, rf$ref_conserva),
      d_algoritmo = l10(lr, g_sis),
      d_modelo    = l10(g_sis, g_ref),
      d_semantica = l10(g_ref, rf$ref_conserva),
      stringsAsFactors = FALSE)
  }
  if (!length(filas)) return(NULL)
  do.call(rbind, filas)
}

#' Coherencia de la convencion detectada entre casos
#'
#' Un sistema que en un caso se explica podando el fundador y en otro
#' conservandolo no esta implementando ninguna de las dos: elegir en cada caso
#' la que mejor cierra seria fabricar el resultado. Por eso la convencion se
#' determina caso por caso pero se exige coherente, y la verificacion es
#' explicita en la corrida.
#'
#' @param dec tabla de `mut_decomposition()`
#' @export
semantics_coherence <- function(dec) {
  if (is.null(dec) || !nrow(dec)) return(NULL)
  do.call(rbind, lapply(sort(unique(dec$motor)), function(mo) {
    d <- dec[dec$motor == mo, , drop = FALSE]
    v <- d$semantica[!is.na(d$semantica) & d$semantica != "indistinguible"]
    data.frame(motor = mo, n_casos = nrow(d), n_decididos = length(v),
               semanticas = paste(sort(unique(v)), collapse = "/"),
               veredicto = if (!length(v)) "SIN_DATO"
                           else if (length(unique(v)) == 1) "PASA" else "FALLA",
               detalle = if (length(unique(v)) > 1)
                 paste("casos que no coinciden:",
                       paste(sprintf("%s=%s", d$caso, d$semantica)[
                         !is.na(d$semantica) & d$semantica != "indistinguible"],
                         collapse = " ")) else "",
               stringsAsFactors = FALSE)
  }))
}

#' Toda parametrizacion de mutacion del catalogo tiene fila en la grilla?
#'
#' El veredicto de modelo de un caso no se calcula en el caso: se busca en la
#' grilla. La parametrizacion del caso identifica una fila y el veredicto de esa
#' fila es el veredicto de modelo del caso, de modo que una parametrizacion sin
#' fila deja al caso sin veredicto de modelo. Se verifica en cada corrida.
#'
#' `proportional` queda fuera a proposito: el sistema evaluado no lo implementa
#' y la grilla no lo pide. Es una diferencia de capacidad, no un hueco.
#'
#' @param cs catalogo; @param g grilla; @param pn paneles
#' @export
grid_covers_cases <- function(cs, g, pn = mutation_panels(cs)) {
  cp <- case_mut_params(cs, pn)
  if (is.null(cp)) return(NULL)
  gk <- param_key(g$modelo, g$tasa, g$tasa2, g$rango, g$pasos, g$panel_universo)
  gk[g$ki_redondeado != "no"] <- NA_character_
  cp$clave <- param_key(cp$modelo, cp$tasa, cp$tasa2, cp$rango, cp$pasos, cp$panel)
  cp$id_grilla <- g$id[match(cp$clave, gk)]
  cp$cubierto <- !is.na(cp$id_grilla) | cp$modelo == "proportional"
  cp$motivo <- ifelse(cp$modelo == "proportional",
                      "no se pide: el sistema evaluado no implementa Proportional",
                      ifelse(is.na(cp$id_grilla), "sin fila en la grilla", ""))
  cp
}

#' Veredicto de modelo de cada caso, leido de la grilla
#'
#' No se calcula en el caso. Si un caso usa dos parametrizaciones --dos loci, o
#' dos matrices por sexo-- se queda con la peor, que es la que manda sobre el
#' LR del caso.
#'
#' @param cs catalogo; @param gr informe de `grid_report()`; @param g grilla
#' @export
model_verdicts <- function(cs, gr, g, pn = mutation_panels(cs)) {
  cov <- grid_covers_cases(cs, g, pn)
  if (is.null(cov)) return(NULL)
  orden <- c("PASA", "REVISAR", "SIN_DATO", "FALLA")
  cov$veredicto_modelo <- gr$veredicto[match(cov$id_grilla, gr$id)]
  cov$veredicto_modelo[is.na(cov$veredicto_modelo)] <- "SIN_DATO"
  do.call(rbind, lapply(sort(unique(cov$caso)), function(cid) {
    d <- cov[cov$caso == cid, , drop = FALSE]
    peor <- d$veredicto_modelo[which.max(match(d$veredicto_modelo, orden))]
    ## Un SIN_DATO tiene que decir por que: "todavia no volco esa matriz" y "esa
    ## parametrizacion no se le pide porque no implementa el modelo" son cosas
    ## distintas, y la segunda es una diferencia de capacidad que no se cierra
    ## con una entrega mas.
    mot <- unique(d$motivo[nzchar(d$motivo)])
    data.frame(caso = cid, filas_grilla = paste(stats::na.omit(unique(d$id_grilla)),
                                                collapse = " "),
               veredicto_modelo = peor,
               ## El caso declara una tasa de microvariante que su propia
               ## escalera vuelve inaplicable. Va al informe porque sin eso su
               ## veredicto se leeria como evidencia sobre un parametro que el
               ## caso no ejercita.
               tasa2_inerte = any(d$tasa2_inerte),
               motivo = if (length(mot)) paste(mot, collapse = "; ")
                        else if (peor == "SIN_DATO") "el sistema no volco esa matriz" else "",
               stringsAsFactors = FALSE)
  }))
}

#' Agrega a la tabla de resultados las columnas de atribucion
#'
#' El vocabulario de veredictos es el mismo del resto de la corrida y
#' `veredicto` es el de conformidad, que es el que alimenta el veredicto de
#' la corrida. Lo que se agrega al lado son dos lecturas
#' mas --algoritmo y modelo-- y la convencion detectada, para que una diferencia
#' se pueda atribuir sin revisar todo el sistema.
#'
#' El veredicto de modelo no se calcula en el caso: se busca en la grilla por
#' la parametrizacion (ver `model_verdicts`).
#'
#' @param res tabla de resultados
#' @param dec tabla de `mut_decomposition()`; @param mv tabla de `model_verdicts()`
#' @export
annotate_mutation <- function(res, dec = NULL, mv = NULL) {
  res$semantica <- NA_character_
  res$ver_algoritmo <- NA_character_
  res$ver_modelo <- NA_character_
  if (!is.null(dec) && nrow(dec)) {
    k <- match(paste(res$caso, res$motor), paste(dec$caso, dec$motor))
    res$semantica     <- dec$semantica[k]
    res$ver_algoritmo <- dec$ver_algoritmo[k]
  }
  if (!is.null(mv) && nrow(mv))
    res$ver_modelo <- mv$veredicto_modelo[match(res$caso, mv$caso)]
  res
}

#' Lectura del par con piso / sin piso
#'
#' El gemelo `*S` es identico a su caso base salvo por el piso declarado, asi
#' que la diferencia entre los dos LR es atribuible a la logica del piso y a
#' nada mas. Es un diferencial DENTRO del sistema evaluado, y el par se
#' resuelve por sufijo del identificador.
#'
#' Los cuatro comportamientos posibles estan separados por magnitud. Llamando
#' `S` a la masa del marcador despues de aplicar el piso sin renormalizar, la
#' referencia queda un factor `S` por encima de la cuenta ingenua `1/(4p)` y un
#' sistema que no renormaliza queda un factor `S` por debajo: el desvio total es
#' `S^2`, asi que se reconoce en vez de conjeturarse.
#'
#' @param res tabla de resultados de `run_catalog()`
#' @param cs catalogo
#' @param tol tolerancias
#' @return data.frame con una fila por par y motor, o NULL
#' @export
min_freq_pairs <- function(res, cs, tol = LRVAL_TOL) {
  gem <- grep("S$", names(cs), value = TRUE)
  if (!length(gem)) return(NULL)
  filas <- list()
  for (g in gem) {
    b <- sub("S$", "", g)
    if (is.null(cs[[b]]) || is.na(cs[[b]]$piso)) next
    ## un par sobre el que el sistema no dijo nada no se informa
    if (!any(res$caso %in% c(b, g))) next
    ## Masa del marcador tras aplicar el piso y no renormalizar. Con varios loci
    ## el LR es el producto de los LR por locus, asi que el desvio total es el
    ## producto de las masas: tomar el primer locus alcanzaria para los dos casos
    ## de hoy (los dos son de un solo locus) y seria silenciosamente falso el dia
    ## que un caso multi-locus declare piso.
    S <- prod(vapply(cs[[b]]$freqs_crudas,
                     function(v) sum(pmax(v, cs[[b]]$piso)), 0))
    for (mo in unique(res$motor)) {
      uno <- function(cid) {
        x <- res$LR[res$caso == cid & res$motor == mo]
        if (length(x)) x[1] else NA_real_
      }
      lb <- uno(b); lg <- uno(g)
      pasa <- function(obs, ref) !is.na(obs) && !is.na(ref) &&
        compare_lr(obs, ref, tol)$veredicto == "PASA"
      ## Un piso aplicado donde no se declaro solo puede SUBIR la frecuencia del
      ## alelo raro, y por lo tanto solo puede BAJAR el LR del gemelo: nunca a 0
      ## ni por encima de su propio valor de referencia. Un gemelo fuera de ese
      ## intervalo es otra cosa --un fallo del motor, un caso mal cargado-- y
      ## etiquetarlo PISO_NO_SOLICITADO afirmaria mas de lo que el par sabe.
      ## `mkcase()` admite un caso sin valor de referencia cerrado, asi que `ref`
      ## puede ser NA: sin referencia no hay intervalo contra el cual leer la
      ## diferencia, y el par tampoco puede atribuirla al piso.
      piso_no_pedido <- function(x, ref)
        !is.na(x) && !is.na(ref) && x > 0 && x < ref
      dg <- if (is.na(lb) || is.na(lg)) "SIN_DATO"
            else if (pasa(lb, cs[[b]]$gold) && pasa(lg, cs[[g]]$gold)) "PISO_OK"
            else if (!pasa(lg, cs[[g]]$gold))
              (if (piso_no_pedido(lg, cs[[g]]$gold)) "PISO_NO_SOLICITADO" else "PISO_OTRO")
            else if (pasa(lb, cs[[b]]$gold / S^2)) "SIN_RENORMALIZAR"
            else if (pasa(lb, lg)) "PISO_IGNORADO"
            else "PISO_OTRO"
      filas[[length(filas) + 1]] <- data.frame(
        caso_piso = b, caso_sin_piso = g, motor = mo, piso = cs[[b]]$piso,
        LR_piso = lb, LR_sin_piso = lg, gold_piso = cs[[b]]$gold,
        diagnostico = dg, stringsAsFactors = FALSE)
    }
  }
  if (!length(filas)) return(NULL)
  do.call(rbind, filas)
}

#' Verificacion de invariantes matematicas
#'
#' Propiedades que deben cumplirse con independencia de cualquier valor de
#' referencia externo.
#'
#' Todos los invariantes se evaluan DESDE `res`, es decir desde lo que cada
#' motor informo, y nunca recalculando con otro motor: un invariante que se
#' resuelve con un calculo ajeno no dice nada del motor al que se le atribuye.
#'
#' Un invariante que cruza varios casos solo es evaluable si el motor informo
#' todos. Con una entrega parcial --el escenario normal de una validacion por
#' etapas-- el caso faltante daria `FALLA` sin que nada haya fallado; por eso
#' la falta de un insumo produce `SIN_DATO` y el detalle nombra lo que falta.
#'
#' @param cs catalogo; @param res tabla de `run_catalog()`
#' @export
check_invariants <- function(cs, res, tol = LRVAL_TOL) {
  inv <- list()
  add <- function(id, descripcion, veredicto, detalle = "")
    inv[[length(inv) + 1]] <<- data.frame(invariante = id, descripcion = descripcion,
                                          veredicto = veredicto,
                                          detalle = detalle, stringsAsFactors = FALSE)
  getLR <- function(cid, motor) {
    v <- res$LR[res$caso == cid & res$motor == motor]
    if (!length(v)) NA_real_ else v[1]
  }
  igual <- function(a, b) !is.na(a) && !is.na(b) &&
    (abs(a - b) <= tol$rel * max(abs(a), abs(b)) || (a == 0 && b == 0))

  ## Un insumo ausente no es una falla: es un invariante no evaluable.
  faltan <- function(casos, vals) paste(casos[is.na(vals)], collapse = ", ")
  juzgar <- function(casos, vals, ok)
    if (anyNA(vals)) list(v = "SIN_DATO", d = paste("sin LR para:", faltan(casos, vals)))
    else list(v = if (isTRUE(ok)) "PASA" else "FALLA", d = NULL)

  for (motor in unique(res$motor)) {
    ## CRUCE-01 kappa identicos => LR identico (medio hermano, tio, abuelo)
    cids <- c("PAR-08", "PAR-09", "PAR-12")
    v <- vapply(cids, getLR, 0, motor = motor)
    j <- juzgar(cids, v, igual(v[1], v[2]) && igual(v[2], v[3]))
    add(paste0("CRUCE-01/", motor),
        "Abuelo-nieto, tío-sobrino y medio hermano comparten kappa=(1/2,1/2,0): mismo LR",
        j$v, j$d %||% sprintf("abuelo=%.10g tío=%.10g medio_hermano=%.10g", v[1], v[2], v[3]))

    ## CRUCE-02 primos y bisabuelo comparten kappa=(3/4,1/4,0)
    cids <- c("PAR-13", "PAR-14")
    v <- vapply(cids, getLR, 0, motor = motor)
    j <- juzgar(cids, v, igual(v[1], v[2]))
    add(paste0("CRUCE-02/", motor),
        "Primos hermanos y bisabuelo-bisnieto comparten kappa=(3/4,1/4,0): mismo LR",
        j$v, j$d %||% sprintf("primos=%.10g bisabuelo=%.10g", v[1], v[2]))

    ## CRUCE-03 exclusion sin mutacion => LR exactamente 0
    cids <- c("PAR-17", "MUT-08")
    v <- vapply(cids, getLR, 0, motor = motor)
    j <- juzgar(cids, v, all(v == 0))
    add(paste0("CRUCE-03/", motor),
        "Exclusión sin modelo de mutación: LR exactamente 0",
        j$v, j$d %||% sprintf("PAR-17=%.10g MUT-08=%.10g", v[1], v[2]))

    ## CRUCE-04 el LR total es el producto de los LR por locus.
    ## Necesita el LR locus por locus, que solo exponen los motores internos:
    ## la plantilla de resultados pide un unico LR por caso. Recalcularlo con
    ## un motor interno y atribuirselo al externo produciria un PASA ajeno.
    ## Un motor externo no recibe esta fila: no hay nada que juzgarle.
    if (motor %in% INTERNAL_ENGINES) {
      ca <- cs[["FRQ-04"]]
      r  <- if (motor == "pedsuite") run_pedsuite(ca) else run_familias(ca)
      add(paste0("CRUCE-04/", motor),
          "El LR total es el producto de los LR por locus (FRQ-04, 5 loci)",
          if (!is.null(r$per_marker) && igual(prod(r$per_marker), r$LR)) "PASA" else "FALLA",
          sprintf("prod=%.10g total=%.10g", prod(r$per_marker %||% NA), r$LR))
    }
  }
  do.call(rbind, inv)
}

#' Filas de la corrida que son, o podrian ser, una diferencia de convencion
#'
#' Las que recibieron CONVENCION, mas las que quedaron en FALLA o REVISAR sobre
#' un caso que declara una diferencia de convencion. Se listan juntas y con su
#' veredicto: para una FALLA la diferencia declarada del caso es una
#' explicacion candidata, no una atribucion.
#' @export
divergence_rows <- function(res) {
  declara <- !is.na(res$divergencia) & nzchar(res$divergencia)
  sel <- res$veredicto == "CONVENCION" | (declara & res$veredicto %in% c("FALLA", "REVISAR"))
  res[sel, , drop = FALSE]
}

#' Resumen ejecutivo de una corrida
#' @export
summarise_run <- function(res) {
  tab <- table(res$motor, res$veredicto)
  list(por_motor = as.data.frame.matrix(tab),
       fallos = res[res$veredicto == "FALLA",
                    c("caso", "motor", "LR", "gold", "disc_rel", "mensaje")],
       divergencias = res[res$veredicto == "CONVENCION",
                          c("caso", "motor", "LR", "gold", "motor_ref", "divergencia")],
       errores = res[res$estado == "error", c("caso", "motor", "mensaje")])
}

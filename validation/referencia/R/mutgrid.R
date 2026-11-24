## ---------------------------------------------------------------------------
## lrval :: modelo de mutacion medido sobre el sistema evaluado (grilla de
## matrices)
##
## Hermana de mutcheck.R. Aquella compara pedmut contra Familias llamandolos
## desde R; esta mide el generador de matrices de un sistema que NO corre en R,
## y lo hace pidiendole que vuelque las matrices que uso.
##
## La medicion es directa: se compara la matriz volcada contra la de pedmut,
## celda por celda.
##
## Que NO se hace aca, a proposito: reimplementar la formula del sistema
## evaluado para regenerar la matriz y compararla. Una reimplementacion escrita
## leyendo el mismo texto comparte los errores de concepcion con el original.
## Donde no hay referencia publicada, la matriz se juzga por propiedades que
## salen de la definicion del modelo como cadena de Markov y son independientes
## de toda implementacion (ver `matrix_properties`).
## ---------------------------------------------------------------------------

## --- panel real versionado ---------------------------------------------------

#' Marcadores del panel real que entran en la grilla
#'
#' Las cuatro escaleras de `MUT_LADDERS` son sinteticas y estan elegidas para
#' discriminar implementaciones de Stepwise. Estos tres marcadores son lo
#' contrario: marcadores de un panel real, con su escalera real (D8S1179 solo
#' enteros, THO1 con microvariante y hueco, D21S11 con tres grupos decimales y
#' 32 alelos).
#' @export
GRID_LOCI_REALES <- c("D8S1179", "THO1", "D21S11")

#' Tasas de mutacion por sexo del escenario
#'
#' Son parametros del escenario de validacion: una tasa para el progenitor
#' masculino (M), una para el femenino (F) y una indistinta (I). Van en la
#' grilla para que no se midan solo tasas redondas.
#' @export
GRID_TASAS_SEXO <- c(M = 0.001584, F = 0.000463, I = 0.001628)

## --- algebra de escaleras alelicas -------------------------------------------

#' Cantidad de repeticiones completas entre dos alelos
#'
#' `NA` cuando la diferencia no es un numero entero de repeticiones, es decir
#' cuando los dos alelos pertenecen a grupos decimales distintos (9 y 9.3). El
#' modelo stepwise no define esa transicion: es lo que cubre `tasa2`.
#' @export
allele_steps <- function(a, b) {
  x <- suppressWarnings(as.numeric(a)); y <- suppressWarnings(as.numeric(b))
  if (is.na(x) || is.na(y)) return(NA_real_)
  d <- abs(x - y)
  if (abs(d - round(d)) > 1e-9) return(NA_real_)
  round(d)
}

#' Amplitud de una escalera: mayor cantidad de pasos entre dos de sus alelos
#'
#' Es el valor de `pasos` con el que un Stepwise no trunca nada.
#' @export
ladder_span <- function(alleles) {
  n <- length(alleles)
  if (n < 2) return(0)
  s <- 0
  for (i in seq_len(n)) for (j in seq_len(n)) {
    d <- allele_steps(alleles[i], alleles[j])
    if (!is.na(d) && d > s) s <- d
  }
  s
}

#' Grupos decimales presentes en una escalera (0, .1, .2, .3 ...)
#' @keywords internal
.decimal_groups <- function(alleles) {
  x <- suppressWarnings(as.numeric(alleles))
  sort(unique(round((x - floor(x)) * 10)[!is.na(x)]))
}

#' Tiene el alelo `i` algun destino admisible bajo el modelo de esta fila?
#'
#' En `equal` lo tiene siempre que la escalera tenga mas de un alelo. En
#' `stepwise` el destino tiene que estar a un numero entero de repeticiones --el
#' modelo no define las transiciones entre grupos decimales-- y dentro de
#' `pasos`. Un alelo sin destino admisible es un callejon sin salida: su fila
#' tiene que ser la identidad, y la masa que le correspondia queda fuera del
#' alcance del modelo.
#'
#' Existe porque la tasa efectiva no puede exigirse igual a la declarada sin
#' descontar esos alelos: el 9.3 aislado de una escalera con microvariantes, el
#' alelo superior de una escalera con hueco cuando `pasos = 1`, o el 33.3 de
#' D21S11 no tienen adonde mutar, y `pedmut` --la referencia-- los deja en la
#' identidad igual que cualquier otra implementacion correcta.
#' @export
allele_has_target <- function(i, alleles, row) {
  if (identical(row$modelo, "equal")) return(length(alleles) > 1)
  for (j in alleles) {
    if (identical(i, j)) next
    d <- allele_steps(i, j)
    if (!is.na(d) && (is.na(row$pasos) || d <= row$pasos)) return(TRUE)
  }
  FALSE
}

#' Nombre del modelo en el vocabulario del formato de intercambio
#'
#' `stepwise_extendido` es un stepwise con tasa de microvariante activa sobre
#' una escalera que efectivamente tiene mas de un grupo decimal. Sobre una
#' escalera de enteros `tasa2` no tiene donde aplicarse y la matriz es la misma
#' que con `tasa2 = 0`: llamarla extendida diria que se midio algo que no.
#' @export
modelo_contrato <- function(model, rate2, alleles) {
  if (identical(model, "stepwise") && !is.na(rate2) && rate2 > 0 &&
      length(.decimal_groups(alleles)) > 1) "stepwise_extendido" else model
}

## --- paneles -----------------------------------------------------------------

#' Paneles de frecuencias que consume la grilla
#'
#' Un panel es un identificador, una lista de alelos y sus frecuencias. El
#' sistema evaluado solo necesita de aca la lista de alelos --su modelo no usa
#' frecuencias para armar la matriz--; las frecuencias viajan porque lrval las
#' necesita para evaluar tasa efectiva, estacionariedad y reversibilidad, que
#' son propiedades respecto de una distribucion poblacional.
#'
#' Los paneles con sufijo `-AMP` son la version ampliada del panel homonimo:
#' los mismos alelos mas los que el sistema agrega en tiempo de calculo al
#' observar un genotipo fuera de la tabla poblacional. Existen porque en el
#' sistema evaluado el dominio de calibracion de la constante de normalizacion
#' y el universo de la matriz NO son el mismo conjunto.
#'
#' @param cs catalogo; sus escaleras entran como paneles si no coinciden con
#'   ninguna de las escaleras de prueba
#' @return lista panel -> vector nombrado de frecuencias
#' @export
mutation_panels <- function(cs = NULL) {
  pn <- list()
  nm <- c(enteros = "P-ENT5", microvariantes = "P-MV6",
          dos_grupos_decimales = "P-DEC5", con_hueco = "P-HUE4")
  for (k in names(MUT_LADDERS)) {
    l <- MUT_LADDERS[[k]]
    pn[[nm[[k]]]] <- setNames(l$afreq / sum(l$afreq), l$alleles)
  }

  ## panel real: se toma de la base versionada, no se transcribe a mano
  fr <- try(freqs_mispitools("Argentina"), silent = TRUE)
  if (!inherits(fr, "try-error"))
    for (lo in GRID_LOCI_REALES)
      if (!is.null(fr[[lo]])) pn[[paste0("P-", lo)]] <- fr[[lo]]

  ## versiones ampliadas: dos alelos mas, uno por debajo y uno por encima de la
  ## escalera, con la frecuencia menor del panel. Renormalizan a 1 porque una
  ## tabla poblacional que no suma 1 mide otra cosa.
  ampliar <- function(v) {
    x <- suppressWarnings(as.numeric(names(v)))
    ent <- names(v)[!is.na(x) & abs(x - round(x)) < 1e-9]
    if (length(ent) < 2) return(NULL)
    xe <- as.numeric(ent)
    nuevos <- as.character(c(min(xe) - 1, max(xe) + 1))
    w <- c(v, setNames(rep(min(v), 2), nuevos))
    renormalize(w)
  }
  for (b in c("P-ENT5", "P-MV6", "P-DEC5", "P-HUE4", paste0("P-", GRID_LOCI_REALES))) {
    if (is.null(pn[[b]])) next
    a <- ampliar(pn[[b]])
    if (!is.null(a)) pn[[paste0(b, "-AMP")]] <- a
  }

  ## escaleras del catalogo que no coincidan con ninguna de las anteriores
  if (!is.null(cs)) {
    k <- 0L
    for (cid in names(cs)) {
      for (pf in .case_ladders(cs[[cid]])) {
        if (any(vapply(pn, function(q) .same_panel(q, pf), TRUE))) next
        k <- k + 1L
        pn[[sprintf("P-CASO-%02d", k)]] <- pf
      }
    }
  }
  pn
}

#' Escaleras (alelos + frecuencias) de los loci con mutacion de un caso
#' @keywords internal
.case_ladders <- function(ca) {
  src <- if (pedtools::is.ped(ca$H1)) ca$H1 else ca$H1[[1]]
  out <- list()
  for (j in seq_len(pedtools::nMarkers(src))) {
    m <- pedtools::getMarkers(src, j)[[1]]
    if (is.null(pedtools::mutmod(m))) next
    out[[length(out) + 1]] <- pedtools::afreq(m)
  }
  out
}

#' Dos paneles son el mismo (mismos alelos y mismas frecuencias)?
#' @keywords internal
.same_panel <- function(a, b, tol = 1e-12) {
  if (!setequal(names(a), names(b))) return(FALSE)
  max(abs(a[names(b)] - b)) <= tol
}

#' Busca el identificador del panel que corresponde a una escalera
#' @keywords internal
.panel_id <- function(pn, af) {
  hit <- names(pn)[vapply(pn, function(q) .same_panel(q, af), TRUE)]
  ## el panel base gana sobre su version ampliada, que nunca puede coincidir
  hit <- hit[!grepl("-AMP$", hit)]
  if (!length(hit)) NA_character_ else hit[1]
}

## --- construccion de la grilla -----------------------------------------------

#' Parametrizaciones de mutacion que usa el catalogo
#'
#' Una fila por combinacion distinta de (modelo, tasa, tasa2, rango, pasos,
#' panel) que aparece en algun caso, con los casos que la usan. Es lo que
#' obliga a que la grilla cubra toda parametrizacion del catalogo: sin eso, el
#' veredicto de modelo de un caso no tendria fila donde buscarse (ver
#' `grid_covers_cases`).
#'
#' El sexo no participa de la clave: la formula del modelo no depende de el,
#' solo sus parametros, de modo que dos sexos con la misma parametrizacion son
#' la misma matriz.
#' @export
case_mut_params <- function(cs, pn = mutation_panels(cs)) {
  filas <- list()
  for (cid in names(cs)) {
    pr <- mut_params(cs[[cid]])
    if (is.null(pr)) next
    lads <- .case_ladders(cs[[cid]])
    src <- if (pedtools::is.ped(cs[[cid]]$H1)) cs[[cid]]$H1 else cs[[cid]]$H1[[1]]
    loci_mut <- vapply(seq_len(pedtools::nMarkers(src)), function(j) {
      m <- pedtools::getMarkers(src, j)[[1]]
      if (is.null(pedtools::mutmod(m))) NA_character_ else pedtools::name(m)
    }, "")
    loci_mut <- loci_mut[!is.na(loci_mut)]
    for (i in seq_len(nrow(pr))) {
      k <- match(pr$locus[i], loci_mut)
      af <- lads[[k]]
      filas[[length(filas) + 1]] <- data.frame(
        caso = cid, locus = pr$locus[i], sexo = pr$sexo_progenitor[i],
        modelo = modelo_contrato(pr$modelo[i], pr$tasa2[i], names(af)),
        tasa = pr$tasa[i], tasa2 = pr$tasa2[i], rango = pr$rango[i],
        pasos = pr$pasos[i], panel = .panel_id(pn, af),
        ## Una tasa de microvariante declarada sobre una escalera de un solo
        ## grupo decimal no tiene donde aplicarse: no hay transicion entre
        ## grupos, y la matriz de referencia es identica bit a bit con tasa2 = 0.
        ## El caso declara el parametro y no lo ejercita, de modo que ni un PASA
        ## ni un FALLA suyo dicen nada sobre ese parametro.
        tasa2_inerte = !is.na(pr$tasa2[i]) && pr$tasa2[i] > 0 &&
          length(.decimal_groups(names(af))) <= 1,
        stringsAsFactors = FALSE)
    }
  }
  if (!length(filas)) return(NULL)
  do.call(rbind, filas)
}

#' Clave de una parametrizacion, para cruzar caso contra grilla
#' @export
param_key <- function(modelo, tasa, tasa2, rango, pasos, panel) {
  n <- function(x) ifelse(is.na(x), "", formatC(as.numeric(x), format = "g", digits = 15))
  paste(modelo, n(tasa), n(tasa2), n(rango), n(pasos), panel, sep = "|")
}

#' Arma la grilla de matrices que se le pide al sistema evaluado
#'
#' La grilla es independiente de todo caso y de todo pedigri: es un pedido de
#' matrices. Se compone de cinco bloques, y el primero es el que garantiza que
#' cada caso del catalogo tenga donde buscar su veredicto de modelo.
#'
#'   catalogo   una fila por parametrizacion distinta que usa algun caso
#'   barrido    las cuatro escaleras de prueba x modelos x tasas x rangos x pasos
#'   por sexo   las tres tasas del escenario (`GRID_TASAS_SEXO`)
#'   panel real los marcadores del panel versionado
#'   universo   filas donde el dominio de calibracion y el universo difieren
#'
#' y cada bloque se duplica en una seleccion de filas con la constante de
#' normalizacion redondeada a ocho decimales. Ese par de filas es lo que mide
#' el techo de precision de la base de datos del sistema evaluado, y es lo que
#' evita tener que correr cada caso de LR dos veces.
#'
#' `proportional` no se pide: el sistema evaluado no lo implementa. Queda como
#' diferencia de capacidad, no como fila sin contestar.
#'
#' @param cs catalogo
#' @param pn paneles (`mutation_panels()`)
#' @return data.frame con las columnas del formato de intercambio
#' @export
build_mutation_grid <- function(cs = NULL, pn = mutation_panels(cs)) {
  filas <- list()
  add <- function(pcal, puni, modelo, tasa, tasa2, rango, pasos, sexo, nota) {
    filas[[length(filas) + 1]] <<- data.frame(
      panel_calibracion = pcal, panel_universo = puni, modelo = modelo,
      tasa = tasa, tasa2 = tasa2, rango = rango, pasos = pasos, sexo = sexo,
      ki_redondeado = "no", nota = nota, stringsAsFactors = FALSE)
  }
  esc <- c("P-ENT5", "P-MV6", "P-DEC5", "P-HUE4")
  esc <- esc[esc %in% names(pn)]

  ## --- bloque 1: parametrizaciones del catalogo ----------------------------
  cp <- if (is.null(cs)) NULL else case_mut_params(cs, pn)
  if (!is.null(cp)) {
    cp <- cp[!is.na(cp$panel) & cp$modelo != "proportional", , drop = FALSE]
    kk <- param_key(cp$modelo, cp$tasa, cp$tasa2, cp$rango, cp$pasos, cp$panel)
    for (u in unique(kk)) {
      d <- cp[kk == u, , drop = FALSE][1, ]
      usos <- sort(unique(cp$caso[kk == u]))
      add(d$panel, d$panel, d$modelo, d$tasa, d$tasa2, d$rango, d$pasos, d$sexo,
          paste("Parametrizacion del catalogo, usada por", paste(usos, collapse = " ")))
    }
  }

  ## --- bloque 2: barrido sobre las escaleras de prueba ---------------------
  for (p in esc) {
    a <- names(pn[[p]])
    span <- ladder_span(a)
    for (r in c(0.001, 0.005, 0.01)) {
      add(p, p, "equal", r, 0, NA_real_, NA_real_, "I",
          "Equal sobre escalera de prueba")
      for (rg in c(0.1, 0.5)) for (ps in unique(c(1, 2, span))) {
        if (ps > span) next
        add(p, p, "stepwise", r, 0, rg, ps, "I",
            sprintf("Stepwise %s sobre escalera de prueba",
                    if (ps >= span) "sin truncar" else sprintf("truncado a %d paso(s)", ps)))
      }
    }
  }

  ## --- bloque 3: sonda del modelo extendido --------------------------------
  for (p in intersect(c("P-MV6", "P-DEC5"), names(pn)))
    add(p, p, "stepwise_extendido", 0.005, 1e-4, 0.1, ladder_span(names(pn[[p]])), "I",
        "Sonda: stepwise con tasa de microvariante activa")

  ## --- bloque 4: tasas del escenario, por sexo -----------------------------
  if ("P-ENT5" %in% names(pn)) {
    span <- ladder_span(names(pn[["P-ENT5"]]))
    for (sx in names(GRID_TASAS_SEXO)) {
      tt <- GRID_TASAS_SEXO[[sx]]
      add("P-ENT5", "P-ENT5", "stepwise", tt, 0, 0.5, 1, sx,
          "Stepwise de un paso, rango 0.5")
      add("P-ENT5", "P-ENT5", "stepwise", tt, 0, 0.1, 1, sx,
          "Idem con rango 0.1: con un paso el rango se cancela y no debe cambiar nada")
      add("P-ENT5", "P-ENT5", "equal", tt, 0, NA_real_, NA_real_, sx,
          "Equal con la misma tasa")
    }
  }

  ## --- bloque 5: panel real versionado -------------------------------------
  for (lo in GRID_LOCI_REALES) {
    p <- paste0("P-", lo)
    if (is.null(pn[[p]])) next
    span <- ladder_span(names(pn[[p]]))
    for (sx in names(GRID_TASAS_SEXO)) {
      tt <- GRID_TASAS_SEXO[[sx]]
      add(p, p, "stepwise", tt, 0, 0.5, 1, sx,
          paste("Panel real", lo, "con stepwise de un paso"))
    }
    add(p, p, "equal", 0.001628, 0, NA_real_, NA_real_, "I",
        paste("Panel real", lo, "con modelo Equal"))
    add(p, p, "stepwise", 0.001628, 0, 0.1, span, "I",
        paste("Panel real", lo, "con stepwise sin truncar"))
  }

  ## --- bloque 6: universo ampliado -----------------------------------------
  ## El dominio sobre el que se calibra la constante y el universo de la matriz
  ## no son el mismo conjunto: el segundo se amplia con los alelos observados.
  ## La tasa efectiva se corre, y estas filas miden exactamente cuanto.
  for (p in c(esc, paste0("P-", GRID_LOCI_REALES))) {
    amp <- paste0(p, "-AMP")
    if (is.null(pn[[p]]) || is.null(pn[[amp]])) next
    add(p, amp, "equal", 0.005, 0, NA_real_, NA_real_, "I",
        "Universo ampliado: la calibracion no cubre los alelos agregados")
    add(p, amp, "stepwise", 0.005, 0, 0.5, 1, "I",
        "Universo ampliado, stepwise de un paso")
  }

  g <- do.call(rbind, filas)

  ## --- duplicado con la constante redondeada -------------------------------
  ## No sobre toda la grilla: alcanza con una fila por combinacion de panel y
  ## modelo, porque lo que mide es el redondeo, no la parametrizacion.
  clave <- paste(g$panel_calibracion, g$panel_universo, g$modelo, g$pasos)
  sel <- !duplicated(clave)
  gr <- g[sel, , drop = FALSE]
  gr$ki_redondeado <- "si"
  gr$nota <- paste(gr$nota, "- constante redondeada a ocho decimales")
  g <- rbind(g, gr)

  g <- cbind(id = sprintf("GRL-%03d", seq_len(nrow(g))), g, stringsAsFactors = FALSE)
  rownames(g) <- NULL
  g[, c("id", "panel_calibracion", "panel_universo", "modelo", "tasa", "tasa2",
        "rango", "pasos", "sexo", "ki_redondeado", "nota")]
}

## --- referencia de cada fila --------------------------------------------------

#' Region y matriz de referencia de una fila de la grilla
#'
#' Clasifica la fila en una region. Donde hay referencia publicada se
#' devuelve la matriz de `pedmut`; donde no la hay se devuelve `NULL` y la
#' fila se juzga por propiedades. Ninguna region se cubre reimplementando la
#' formula del sistema evaluado.
#'
#' @param row una fila de `build_mutation_grid()`
#' @param pn paneles
#' @return lista con `region`, `referencia`, `M` (o NULL) y `motivo`
#' @export
grid_reference <- function(row, pn) {
  a  <- names(pn[[row$panel_universo]])
  ac <- names(pn[[row$panel_calibracion]])
  p  <- pn[[row$panel_universo]]
  span <- ladder_span(a)
  sin <- function(region, motivo)
    list(region = region, referencia = "propiedades", M = NULL, motivo = motivo)

  if (!identical(row$panel_calibracion, row$panel_universo))
    return(sin("universo ampliado",
               "el universo excede el dominio de calibracion: la tasa efectiva se corre"))

  if (identical(row$modelo, "equal")) {
    M <- try(.ped_matrix("equal", a, p, row$tasa, 0, NA), silent = TRUE)
    if (inherits(M, "try-error")) return(sin("equal", "pedmut no genero la referencia"))
    return(list(region = "equal", referencia = "pedmut Equal", M = as.matrix(M),
                motivo = NA_character_))
  }

  if (identical(row$modelo, "stepwise_extendido"))
    return(sin("stepwise extendido",
               "sin referencia publicada: sonda de la tasa de microvariante"))

  ## stepwise
  enteros <- all(vapply(a, function(x) {
    v <- suppressWarnings(as.numeric(x)); !is.na(v) && abs(v - round(v)) < 1e-9 }, TRUE))
  if (!is.na(row$pasos) && row$pasos >= span) {
    M <- try(.ped_matrix("stepwise", a, p, row$tasa, 0, row$rango), silent = TRUE)
    if (inherits(M, "try-error")) return(sin("stepwise sin truncar", "pedmut no genero la referencia"))
    return(list(region = "stepwise sin truncar", referencia = "pedmut Stepwise (rate2=0)",
                M = as.matrix(M), motivo = NA_character_))
  }
  if (!is.na(row$pasos) && row$pasos == 1) {
    if (!enteros)
      return(sin("stepwise de un paso, escalera no entera",
                 "sin referencia publicada: el modelo onestep exige alelos enteros"))
    M <- try(as.matrix(pedmut::mutationMatrix("onestep", alleles = a, rate = row$tasa)),
             silent = TRUE)
    if (inherits(M, "try-error")) return(sin("stepwise de un paso", "pedmut no genero la referencia"))
    return(list(region = "stepwise de un paso", referencia = "pedmut onestep",
                M = M, motivo = NA_character_))
  }
  sin("stepwise truncado", "sin referencia publicada: 2 <= pasos < amplitud de la escalera")
}

## --- propiedades formales de una matriz ---------------------------------------

#' Propiedades formales de una matriz de mutacion
#'
#' Separa dos clases que el informe no debe mezclar:
#'
#'   exigencias   tienen que valer: filas que suman 1,
#'                tasa efectiva igual a la declarada, razon entre
#'                pasos consecutivos igual al rango, soporte nulo mas alla de
#'                `pasos` y en toda transicion que no sea de repeticiones
#'                completas, masa igual hacia dos alelos a la misma cantidad de
#'                pasos, y --solo en el modelo extendido-- masa entre grupos
#'                decimales igual a `tasa2` y repartida por igual entre los
#'                alelos de otra clase (la tasa entera se mide aparte, sobre
#'                las transiciones del mismo grupo).
#'   descriptores no tienen por que valer: estacionariedad y reversibilidad.
#'                Una matriz Stepwise no es estacionaria en general, de modo
#'                que informarlas como requisito produciria un falso positivo.
#'                Se comparan contra el valor de la referencia, no contra cero.
#'
#' Sobre la escalera de enteros con `pasos = 2` las cuatro exigencias
#' determinan la matriz por completo: una matriz que las cumpla todas no puede
#' ser otra que la correcta. Es lo que permite juzgar las regiones sin
#' referencia publicada sin reimplementar la formula del sistema evaluado.
#'
#' @param M matriz (dimnames = alelos)
#' @param p frecuencias del universo
#' @param row fila de la grilla (aporta modelo, tasa, rango y pasos)
#' @param tol tolerancia de las exigencias
#' @export
matrix_properties <- function(M, p, row, tol = 1e-9) {
  a <- rownames(M)
  p <- p[a]
  filas1 <- max(abs(rowSums(M) - 1))
  ## Dos mascaras sobre la matriz alineada con `a`: transiciones dentro del
  ## mismo grupo decimal (`misma`) y entre grupos distintos (`otra`).
  Mm <- M[a, a, drop = FALSE]
  misma <- outer(a, a, Vectorize(function(i, j) !is.na(allele_steps(i, j))))
  diag(misma) <- FALSE
  otra <- !misma; diag(otra) <- FALSE
  ## Masa de mutacion ENTERA, ponderada por frecuencia: solo las transiciones
  ## dentro del mismo grupo decimal, y no toda la masa fuera de la diagonal. Da
  ## lo mismo mientras el modelo no reparta nada entre grupos decimales, pero en
  ## el modelo extendido Familias y pedmut declaran DOS tasas para dos tipos de
  ## mutacion --entera y de microvariante-- y la diagonal vale 1 - tasa - tasa2.
  ## Cada masa se exige contra su tasa; `equal` no distingue grupos y mide todo.
  tasa_ef <- if (identical(row$modelo, "equal")) sum(p * (1 - diag(Mm)))
             else sum(p * rowSums(Mm * misma))
  est <- max(abs(as.vector(p %*% M) - p))
  A <- p * M
  rev <- max(abs(A - t(A)))

  ## soporte: cero mas alla de `pasos` y en toda transicion que no sea de
  ## repeticiones completas (salvo que la fila declare tasa de microvariante)
  sop <- TRUE
  extendido <- identical(row$modelo, "stepwise_extendido")
  if (!identical(row$modelo, "equal")) {
    for (i in a) for (j in a) {
      if (i == j) next
      d <- allele_steps(i, j)
      fuera <- if (is.na(d)) !extendido else (!is.na(row$pasos) && d > row$pasos)
      if (fuera && abs(M[i, j]) > tol) sop <- FALSE
    }
  }

  ## Simetria: dos alelos a la misma cantidad de pasos de `i` tienen que recibir
  ## la misma masa. El modelo la fija en ki * rango^d, que depende de la distancia
  ## y no de la direccion. Es la exigencia que cubre `pasos = 1`: ahi la de la
  ## razon entre pasos queda vacia --no hay dos pasos consecutivos dentro del
  ## soporte-- y sin esta un reparto asimetrico entre los dos vecinos, 0.004 y 0.001 en vez de 0.0025 y 0.0025, cumpliria todo lo
  ## demas. Vale en toda la grilla, no solo con un paso.
  sim <- NA
  if (!identical(row$modelo, "equal")) {
    pares <- 0L; sim_ok <- TRUE
    for (i in a) {
      dd <- vapply(a, function(j) allele_steps(i, j), 0)
      for (d in unique(dd[!is.na(dd) & dd > 0])) {
        js <- a[!is.na(dd) & dd == d]
        if (length(js) < 2) next
        v <- M[i, js]
        pares <- pares + 1L
        if (max(v) - min(v) > 1e-9 * max(1, max(v))) sim_ok <- FALSE
      }
    }
    if (pares > 0L) sim <- sim_ok
  }

  ## Razon entre celdas a pasos consecutivos: solo tiene sentido en stepwise, y
  ## solo entre dos pasos que esten AMBOS dentro de `pasos`. Con `pasos = 1` no
  ## hay ningun par consecutivo dentro del soporte y la exigencia es vacia:
  ## compararla contra la celda del paso 2 --que el truncamiento fuerza a cero--
  ## la hace fallar sobre toda matriz correcta, incluidas las de `pedmut`. Que el
  ## soporte se corte en `pasos` ya lo verifica `sop`.
  raz <- NA
  if (!identical(row$modelo, "equal") && !is.na(row$rango) && !is.na(row$pasos)) {
    comparados <- 0L
    for (i in a) {
      dd <- vapply(a, function(j) allele_steps(i, j), 0)
      for (n in seq_len(max(0, row$pasos - 1))) {
        j1 <- a[!is.na(dd) & dd == n]; j2 <- a[!is.na(dd) & dd == n + 1]
        if (!length(j1) || !length(j2)) next
        v1 <- M[i, j1[1]]; v2 <- M[i, j2[1]]
        if (v1 <= 0) next
        comparados <- comparados + 1L
        if (abs(v2 / v1 - row$rango) > 1e-6) raz <- FALSE
      }
    }
    if (comparados > 0L && is.na(raz)) raz <- TRUE
  }

  ## Masa que sale hacia otro grupo decimal, ponderada por las frecuencias. Es el
  ## analogo de la tasa efectiva para `tasa2`, y existe porque sin ella la sonda
  ## del modelo extendido no distinguiria una matriz que reparte esa masa de una
  ## que le asigna cero: las otras exigencias se cumplen igual en las dos, y la
  ## region no tiene referencia publicada contra la cual notarlo. Un sistema que
  ## no implementa la tasa de microvariante da exactamente cero aca.
  ## Ademas se exige, como cantidad y como reparto: la masa
  ## tiene que valer `tasa2` por la masa de los alelos que tienen algun destino
  ## de otra clase (el analogo de `masa_alcanzable`), y dentro de cada fila
  ## tiene que repartirse por igual entre esos destinos, que es como Familias
  ## y pedmut definen el modelo. Sin lo segundo, un reparto pesado por
  ## frecuencia conserva la cantidad y pasaria.
  tasa2_ef <- NA_real_; tasa2_esp <- NA_real_; masa2 <- NA_real_
  tasa2_ok <- NA; reparto2 <- NA
  if (extendido) {
    tasa2_ef <- sum(p * rowSums(Mm * otra))
    masa2 <- sum(p[rowSums(otra) > 0])
    t2 <- row$tasa2
    if (!is.null(t2) && !is.na(t2)) {
      tasa2_esp <- t2 * masa2
      tasa2_ok <- abs(tasa2_ef - tasa2_esp) <= 1e-9 * max(1, tasa2_esp)
    }
    filas2 <- which(rowSums(otra) > 1)
    if (length(filas2))
      reparto2 <- all(vapply(filas2, function(i) {
        v <- Mm[i, otra[i, ]]
        max(v) - min(v) <= 1e-9 * max(1, max(v))
      }, TRUE))
  }

  ## Tasa efectiva. NO se exige igual a la tasa declarada a secas: un alelo sin
  ## destino admisible tiene que quedar en la identidad, y su frecuencia sale del
  ## alcance del modelo. La forma correcta descuenta esa masa, y es la que cumple
  ## `pedmut`; la forma literal no la cumple la propia referencia.
  ##
  ## `masa_alcanzable` es esa suma de frecuencias, y va al informe: con el
  ## universo ampliado la tasa efectiva se corre a proposito y lo que importa es
  ## cuanto, no un booleano.
  alcanzables <- vapply(a, function(i) allele_has_target(i, a, row), TRUE)
  masa <- sum(p[alcanzables])
  esperada <- row$tasa * masa
  list(filas_suman_1 = filas1 <= tol, desvio_filas = filas1,
       tasa_efectiva = tasa_ef, tasa_esperada = esperada, masa_alcanzable = masa,
       tasa_efectiva_ok = abs(tasa_ef - esperada) <= 1e-9 * max(1, esperada),
       tasa2_efectiva = tasa2_ef, tasa2_esperada = tasa2_esp,
       masa2_alcanzable = masa2, tasa2_efectiva_ok = tasa2_ok,
       reparto_tasa2_ok = reparto2,
       microvariante_ok = if (!extendido) NA else tasa2_ef > 0,
       razon_pasos_ok = raz, simetria_ok = sim, soporte_ok = sop,
       estacionariedad = est, reversibilidad = rev,
       nula = max(abs(M)) == 0)
}

#' Discrepancia celda a celda entre dos matrices
#'
#' Es `compare_mutation_models()` abierto para recibir una matriz de afuera en
#' vez de generar las dos: la comparacion es la misma, cambia de donde sale uno
#' de los dos operandos.
#'
#' @param A,B matrices con dimnames
#' @return lista con discrepancia absoluta y relativa maximas y la celda
#' @export
compare_matrices <- function(A, B) {
  if (is.null(A) || is.null(B))
    return(list(abs = NA_real_, rel = NA_real_, celda = NA_character_))
  if (!setequal(rownames(A), rownames(B)))
    return(list(abs = NA_real_, rel = NA_real_, celda = "universos distintos"))
  B <- B[rownames(A), colnames(A), drop = FALSE]
  D <- abs(A - B)
  R <- D / pmax(abs(B), 1e-300)
  i <- which(R == max(R), arr.ind = TRUE)[1, ]
  list(abs = max(D), rel = max(R),
       celda = paste0(rownames(A)[i[1]], "->", colnames(A)[i[2]]))
}

## --- exportacion del pedido ---------------------------------------------------

#' Escribe la grilla y los paneles del paquete de intercambio
#'
#' El paquete de ida no lleva ningun valor de referencia: el sistema evaluado
#' recibe la pregunta y devuelve el numero y la matriz, nunca la respuesta
#' esperada.
#' @export
export_grid <- function(cs, dir = LRVAL_DIRS$paquete) {
  dir.create(dir, recursive = TRUE, showWarnings = FALSE)
  pn <- mutation_panels(cs)
  g  <- build_mutation_grid(cs, pn)
  usados <- unique(c(g$panel_calibracion, g$panel_universo))
  pd <- do.call(rbind, lapply(usados, function(k) data.frame(
    panel = k, alelo = names(pn[[k]]), frecuencia = as.numeric(pn[[k]]),
    stringsAsFactors = FALSE)))
  write_csv_exact(g,  file.path(dir, "grilla.csv"),  num = c("tasa", "tasa2", "rango", "pasos"))
  write_csv_exact(pd, file.path(dir, "paneles.csv"), num = "frecuencia")
  invisible(list(grilla = g, paneles = pn))
}

## --- reimportacion del volcado ------------------------------------------------

#' Lee las matrices volcadas por el sistema evaluado
#'
#' Espejo de `import_results()`. Un archivo incompleto es un error ruidoso y no
#' una matriz con ceros implicitos: que una transicion valga cero es un
#' resultado, y darlo por supuesto convertiria una capacidad faltante en un
#' acuerdo.
#'
#' Se exige, ademas de las columnas del formato, que cada matriz este completa
#' --tantas celdas como el cuadrado de su universo-- y que el universo de la
#' matriz coincida con el que declara el archivo de contexto. Sin esa segunda
#' comprobacion nada distingue un volcado de una reconstruccion posterior, y la
#' separacion entre error de modelo y error de algoritmo se cae en silencio.
#'
#' @param file matrices volcadas (`matrices_genis.csv`)
#' @param contexto constantes de normalizacion (`contexto_matrices.csv`)
#' @param grilla grilla exportada; `NULL` omite la verificacion de ids de grilla
#' @param cs catalogo; `NULL` omite la verificacion de ids de caso
#' @return lista de matrices, cada una con atributos `origen`, `id`, `locus`,
#'   `sexo` y `ki`
#' @export
import_matrices <- function(file, contexto = NULL, grilla = NULL, cs = NULL) {
  d <- utils::read.csv(file, colClasses = "character")
  falta <- setdiff(c("origen", "id", "locus", "sexo", "alelo_origen",
                     "alelo_destino", "probabilidad"), names(d))
  if (length(falta))
    stop("import_matrices: faltan columnas en ", file, ": ", paste(falta, collapse = ", "))
  tr <- function(x) trimws(as.character(x))
  for (k in c("origen", "id", "locus", "sexo", "alelo_origen", "alelo_destino"))
    d[[k]] <- tr(d[[k]])
  if (!all(d$origen %in% c("grilla", "caso")))
    stop("import_matrices: 'origen' admite solo 'grilla' o 'caso'; hay: ",
         paste(unique(setdiff(d$origen, c("grilla", "caso"))), collapse = ", "))
  pr <- suppressWarnings(as.numeric(tr(d$probabilidad)))
  if (any(is.na(pr)))
    stop("import_matrices: probabilidad no interpretable en ",
         paste(unique(d$id[is.na(pr)]), collapse = ", "))
  d$probabilidad <- pr

  if (!is.null(grilla)) {
    des <- setdiff(d$id[d$origen == "grilla"], grilla$id)
    if (length(des))
      stop("import_matrices: ids de grilla desconocidos: ", paste(des, collapse = ", "))
  }
  if (!is.null(cs)) {
    des <- setdiff(d$id[d$origen == "caso"], names(cs))
    if (length(des))
      stop("import_matrices: casos desconocidos: ", paste(des, collapse = ", "))
  }

  ctx <- NULL
  if (!is.null(contexto) && file.exists(contexto)) {
    ctx <- utils::read.csv(contexto, colClasses = "character")
    faltac <- setdiff(c("origen", "id", "locus", "sexo", "alelo", "ki",
                        "en_calibracion", "en_universo"), names(ctx))
    if (length(faltac))
      stop("import_matrices: faltan columnas en ", contexto, ": ",
           paste(faltac, collapse = ", "))
    for (k in c("origen", "id", "locus", "sexo", "alelo", "en_calibracion", "en_universo"))
      ctx[[k]] <- tr(ctx[[k]])
    ctx$ki <- suppressWarnings(as.numeric(tr(ctx$ki)))
  }

  clave <- paste(d$origen, d$id, d$locus, d$sexo, sep = "\r")
  out <- list()
  for (u in unique(clave)) {
    b <- d[clave == u, , drop = FALSE]
    al <- unique(c(b$alelo_origen, b$alelo_destino))
    al <- al[order_alleles(al)]
    if (nrow(b) != length(al)^2)
      stop("import_matrices: matriz incompleta en ", b$id[1], " (", b$sexo[1], "): ",
           nrow(b), " celdas para un universo de ", length(al),
           " alelos; se esperan ", length(al)^2,
           ". Una transicion nula es un resultado y viaja explicita.")
    M <- matrix(NA_real_, length(al), length(al), dimnames = list(al, al))
    M[cbind(match(b$alelo_origen, al), match(b$alelo_destino, al))] <- b$probabilidad
    if (anyNA(M))
      stop("import_matrices: celdas duplicadas o faltantes en ", b$id[1])
    ki <- NULL
    if (!is.null(ctx)) {
      cb <- ctx[ctx$origen == b$origen[1] & ctx$id == b$id[1] &
                  ctx$locus == b$locus[1] & ctx$sexo == b$sexo[1], , drop = FALSE]
      if (nrow(cb)) {
        uni <- sort(cb$alelo[cb$en_universo == "si"])
        if (!setequal(uni, al))
          stop("import_matrices: el universo de la matriz ", b$id[1],
               " no coincide con el que declara el contexto (matriz: ",
               length(al), " alelos; contexto: ", length(uni), ")")
        ki <- setNames(cb$ki, cb$alelo)
      }
    }
    attr(M, "origen") <- b$origen[1]; attr(M, "id") <- b$id[1]
    attr(M, "locus")  <- b$locus[1];  attr(M, "sexo") <- b$sexo[1]
    attr(M, "ki") <- ki
    out[[u]] <- M
  }
  out
}

#' Busca en un volcado la matriz de una fila de grilla o de un caso
#' @export
find_matrix <- function(mats, origen, id, locus = "", sexo = NULL) {
  if (is.null(mats) || !length(mats)) return(NULL)
  k <- vapply(mats, function(M)
    identical(attr(M, "origen"), origen) && identical(attr(M, "id"), id) &&
      identical(attr(M, "locus"), locus) &&
      (is.null(sexo) || identical(attr(M, "sexo"), sexo)), TRUE)
  if (!any(k)) return(NULL)
  mats[[which(k)[1]]]
}

## --- informe de la grilla -----------------------------------------------------

#' Informe de la grilla: una fila por combinacion pedida
#'
#' Region, referencia usada, discrepancia contra ella, celda del maximo, y el
#' estado de cada exigencia y cada descriptor. El veredicto sale de la
#' discrepancia relativa donde hay referencia publicada y de las exigencias
#' donde no la hay; nunca de una reimplementacion de la formula del sistema.
#'
#' @param g grilla; @param pn paneles; @param mats volcado (`import_matrices()`)
#' @export
grid_report <- function(g, pn, mats = NULL, tol = LRVAL_TOL) {
  do.call(rbind, lapply(seq_len(nrow(g)), function(i) {
    row <- g[i, ]
    rf <- grid_reference(row, pn)
    M  <- find_matrix(mats, "grilla", row$id, "")
    base <- data.frame(
      id = row$id, panel_calibracion = row$panel_calibracion,
      panel_universo = row$panel_universo, modelo = row$modelo,
      tasa = row$tasa, tasa2 = row$tasa2, rango = row$rango, pasos = row$pasos,
      sexo = row$sexo, ki_redondeado = row$ki_redondeado,
      region = rf$region, referencia = rf$referencia,
      volcada = !is.null(M), stringsAsFactors = FALSE)
    if (is.null(M)) {
      return(cbind(base, data.frame(
        disc_abs = NA_real_, disc_rel = NA_real_, celda_max = NA_character_,
        veredicto = "SIN_DATO", filas_suman_1 = NA, desvio_filas = NA_real_,
        tasa_efectiva = NA_real_, tasa_esperada = NA_real_, masa_alcanzable = NA_real_,
        tasa_efectiva_ok = NA, tasa2_efectiva = NA_real_, tasa2_esperada = NA_real_,
        masa2_alcanzable = NA_real_, tasa2_efectiva_ok = NA, reparto_tasa2_ok = NA,
        microvariante_ok = NA, razon_pasos_ok = NA, simetria_ok = NA,
        soporte_ok = NA, estacionariedad = NA_real_, estacionariedad_ref = NA_real_,
        reversibilidad = NA_real_, reversibilidad_ref = NA_real_,
        nota = "el sistema no volco esta matriz", stringsAsFactors = FALSE)))
    }
    p <- pn[[row$panel_universo]]
    if (!setequal(names(p), rownames(M)))
      return(cbind(base, data.frame(
        disc_abs = NA_real_, disc_rel = NA_real_, celda_max = NA_character_,
        veredicto = "FALLA", filas_suman_1 = NA, desvio_filas = NA_real_,
        tasa_efectiva = NA_real_, tasa_esperada = NA_real_, masa_alcanzable = NA_real_,
        tasa_efectiva_ok = NA, tasa2_efectiva = NA_real_, tasa2_esperada = NA_real_,
        masa2_alcanzable = NA_real_, tasa2_efectiva_ok = NA, reparto_tasa2_ok = NA,
        microvariante_ok = NA, razon_pasos_ok = NA, simetria_ok = NA,
        soporte_ok = NA, estacionariedad = NA_real_, estacionariedad_ref = NA_real_,
        reversibilidad = NA_real_, reversibilidad_ref = NA_real_,
        nota = paste0("el universo volcado (", nrow(M), " alelos) no es el del panel ",
                      row$panel_universo, " (", length(p), " alelos)"),
        stringsAsFactors = FALSE)))
    pp <- matrix_properties(M, p, row)
    cm <- compare_matrices(M, rf$M)
    pr <- if (is.null(rf$M)) NULL else matrix_properties(rf$M, p, row)
    exig <- c(pp$filas_suman_1, pp$tasa_efectiva_ok, pp$razon_pasos_ok, pp$soporte_ok,
              pp$tasa2_efectiva_ok, pp$reparto_tasa2_ok,
              pp$microvariante_ok, pp$simetria_ok)
    exig <- exig[!is.na(exig)]
    ver <- if (!is.null(rf$M)) {
      if (cm$rel <= tol$rel) "PASA" else if (cm$rel <= 1e-6) "REVISAR" else "FALLA"
    } else if (pp$nula) "SIN_DATO"
      else if (all(exig)) "PASA" else "FALLA"
    nota <- if (pp$nula)
      "el motor devuelve la matriz identicamente nula: el modelo no esta disponible"
      else if (isFALSE(pp$microvariante_ok))
      "no reparte masa entre grupos decimales pese a declarar tasa2"
      else if (is.null(rf$M)) rf$motivo else NA_character_
    cbind(base, data.frame(
      disc_abs = cm$abs, disc_rel = cm$rel, celda_max = cm$celda, veredicto = ver,
      filas_suman_1 = pp$filas_suman_1, desvio_filas = pp$desvio_filas,
      tasa_efectiva = pp$tasa_efectiva, tasa_esperada = pp$tasa_esperada,
      masa_alcanzable = pp$masa_alcanzable, tasa_efectiva_ok = pp$tasa_efectiva_ok,
      tasa2_efectiva = pp$tasa2_efectiva, tasa2_esperada = pp$tasa2_esperada,
      masa2_alcanzable = pp$masa2_alcanzable, tasa2_efectiva_ok = pp$tasa2_efectiva_ok,
      reparto_tasa2_ok = pp$reparto_tasa2_ok, microvariante_ok = pp$microvariante_ok,
      razon_pasos_ok = pp$razon_pasos_ok, simetria_ok = pp$simetria_ok,
      soporte_ok = pp$soporte_ok,
      estacionariedad = pp$estacionariedad,
      estacionariedad_ref = if (is.null(pr)) NA_real_ else pr$estacionariedad,
      reversibilidad = pp$reversibilidad,
      reversibilidad_ref = if (is.null(pr)) NA_real_ else pr$reversibilidad,
      nota = nota, stringsAsFactors = FALSE))
  }))
}

## ---------------------------------------------------------------------------
## lrval :: catalogo de casos de validacion
##
## Cada caso se define UNA sola vez, con las primitivas de pedtools, y se
## traduce despues a cada motor (ver engines.R). Un caso consta de:
##
##   H1        pedigri bajo la hipotesis de parentesco (lleva los marcadores)
##   H2        pedigri bajo la hipotesis alternativa (habitualmente el
##             individuo no identificado como singleton no emparentado)
##   gold      LR esperado, calculado con una expresion cerrada publicada
##             (gold.R); NA cuando no existe forma cerrada
##   gold_src  cita exacta de donde sale ese valor
##   features  caracteristicas que el caso ejercita (matriz de trazabilidad)
##
## Convencion de identificadores de caso:
##   PAR-*  parentesco basico sin mutacion       MUT-*  modelos de mutacion
##   SIL-*  alelo silente                        FRQ-*  frecuencias/microvariantes
##   PRU-*  poda del pedigri                     INV-*  invariantes matematicas
##   SLO-*  casos del articulo de Slooten (2011)
## ---------------------------------------------------------------------------

## Frecuencias sinteticas, elegidas para que las expresiones algebraicas se
## evaluen sin ambiguedad y para que ningun alelo tenga frecuencia extrema.
P4 <- c("1" = 0.10, "2" = 0.20, "3" = 0.30, "4" = 0.40)
P5 <- c("1" = 0.05, "2" = 0.15, "3" = 0.25, "4" = 0.25, "5" = 0.30)

#' Adjunta un marcador a un pedigri
#' @keywords internal
setgt <- function(ped, geno, afreq, name = "L1", mutmod = NULL) {
  args <- c(list(ped), geno, list(afreq = afreq, name = name))
  if (!is.null(mutmod)) args$mutmod <- mutmod
  pedtools::setMarkers(ped, do.call(pedtools::marker, args))
}

#' Constructor de un caso del catalogo
#'
#' `gold_mut` es la expresion cerrada del caso dejada abierta en la matriz de
#' mutacion y en la convencion para el fundador no tipificado:
#' `function(Mm, Mf, podado)`. La lleva
#' todo caso con mutacion que tenga forma cerrada, y es lo que permite calcular
#' las cuatro referencias del caso --las dos matrices por las dos convenciones--
#' sin volver a escribir la formula. `gold` es esa misma funcion evaluada en la
#' matriz de referencia y en la convencion que conserva el fundador.
#'
#' `piso` y `freqs_crudas` van juntos y solo los usan los casos que declaran un
#' piso de frecuencia: el paquete entrega `freqs_crudas` y deja que el sistema
#' bajo validacion aplique `piso`, mientras que los marcadores de `H1` llevan el
#' panel ya pisado, que es el que consumen los motores de referencia y el .fam.
#'
#' `gold_alt` es el LR del mismo caso bajo la OTRA convencion documentada, en
#' forma cerrada (pedigri podado en vez de completo, genotipo literal en vez de
#' banda). Solo lo llevan los casos donde las convenciones dan numeros
#' distintos. Sirve para medir, y no declarar, a cual de las dos responde un
#' motor: quien da exactamente `gold_alt` recibe CONVENCION y no FALLA.
#' @keywords internal
mkcase <- function(id, titulo, grupo, H1, H2, gold = NA_real_,
                   gold_src = NA_character_, features = character(0),
                   ref = NA_character_, theta = 0, nota = NA_character_,
                   motor_ref = NA_character_, divergencia = NA_character_,
                   famH1 = NULL, famH2 = NULL, gold_mut = NULL,
                   piso = NA_real_, freqs_crudas = NULL, gold_alt = NA_real_) {
  structure(list(id = id, titulo = titulo, grupo = grupo, H1 = H1, H2 = H2,
                 gold = gold, gold_src = gold_src, gold_alt = gold_alt,
                 features = features,
                 ref = ref, theta = theta, nota = nota,
                 motor_ref = motor_ref, divergencia = divergencia,
                 famH1 = famH1, famH2 = famH2, gold_mut = gold_mut,
                 piso = piso, freqs_crudas = freqs_crudas),
            class = "lrcase")
}

#' @export
print.lrcase <- function(x, ...) {
  cat("<lrcase>", x$id, "|", x$titulo, "\n")
  cat("  grupo :", x$grupo, "\n")
  cat("  gold  :", if (is.na(x$gold)) "-" else format(x$gold, digits = 10),
      if (!is.na(x$gold_src)) paste0("  [", x$gold_src, "]") else "", "\n")
  cat("  rasgos:", paste(x$features, collapse = ", "), "\n")
  invisible(x)
}

## ===========================================================================
## GRUPO PAR : parentesco basico, sin mutacion
## ===========================================================================

cases_parentesco <- function(p = P4) {
  cs <- list()
  add <- function(x) cs[[length(cs) + 1]] <<- x

  ## --- PAR-01 duo paterno (paternidad sin madre) ---------------------------
  AF <- c("1", "2"); CH <- c("1", "3")
  H1 <- setgt(pedtools::nuclearPed(fa = "AF", mo = "MO", children = "CH"),
              list(AF = AF, CH = CH), p)
  H2 <- list(pedtools::singleton("AF"), pedtools::singleton("CH"))
  add(mkcase("PAR-01", "Dúo paterno: padre 1/2, hijo 1/3", "parentesco", H1, H2,
             gold = gold_duo(CH, AF, p), gold_src = "Evett & Weir (1998), indice de paternidad sin madre",
             features = c("duo", "heterocigoto", "sin_mutacion")))

  ## --- PAR-02 duo paterno con hijo homocigoto ------------------------------
  AF <- c("1", "2"); CH <- c("1", "1")
  H1 <- setgt(pedtools::nuclearPed(fa = "AF", mo = "MO", children = "CH"),
              list(AF = AF, CH = CH), p)
  add(mkcase("PAR-02", "Dúo paterno: padre 1/2, hijo 1/1", "parentesco", H1,
             list(pedtools::singleton("AF"), pedtools::singleton("CH")),
             gold = gold_duo(CH, AF, p), gold_src = "Evett & Weir (1998)",
             features = c("duo", "homocigoto", "sin_mutacion")))

  ## --- PAR-03 trio completo ------------------------------------------------
  AF <- c("1", "2"); MO <- c("3", "4"); CH <- c("1", "3")
  H1 <- setgt(pedtools::nuclearPed(fa = "AF", mo = "MO", children = "CH"),
              list(AF = AF, MO = MO, CH = CH), p)
  H2 <- list(pedtools::nuclearPed(fa = "NN", mo = "MO", children = "CH"),
             pedtools::singleton("AF"))
  H2[[1]] <- setgt(H2[[1]], list(MO = MO, CH = CH), p)
  add(mkcase("PAR-03", "Trío: padre 1/2, madre 3/4, hijo 1/3", "parentesco", H1,
             list(pedtools::nuclearPed(fa = "NN", mo = "MO", children = "CH"),
                  pedtools::singleton("AF")),
             gold = gold_trio(CH, MO, AF, p), gold_src = "Evett & Weir (1998), trio",
             features = c("trio", "madre_tipificada", "sin_mutacion")))

  ## --- PAR-04 trio, hijo homocigoto, madre comparte alelo ------------------
  AF <- c("1", "1"); MO <- c("1", "2"); CH <- c("1", "1")
  H1 <- setgt(pedtools::nuclearPed(fa = "AF", mo = "MO", children = "CH"),
              list(AF = AF, MO = MO, CH = CH), p)
  add(mkcase("PAR-04", "Trío: padre 1/1, madre 1/2, hijo 1/1", "parentesco", H1,
             list(pedtools::nuclearPed(fa = "NN", mo = "MO", children = "CH"),
                  pedtools::singleton("AF")),
             gold = gold_trio(CH, MO, AF, p), gold_src = "Evett & Weir (1998), trio",
             features = c("trio", "homocigoto", "sin_mutacion")))

  ## --- PAR-05 maternidad (duo materno) -------------------------------------
  MO <- c("2", "3"); CH <- c("2", "4")
  H1 <- setgt(pedtools::nuclearPed(fa = "FA", mo = "MO", children = "CH"),
              list(MO = MO, CH = CH), p)
  add(mkcase("PAR-05", "Dúo materno: madre 2/3, hijo 2/4", "parentesco", H1,
             list(pedtools::singleton("MO", sex = 2), pedtools::singleton("CH")),
             gold = gold_duo(CH, MO, p), gold_src = "Evett & Weir (1998)",
             features = c("duo", "materno", "sin_mutacion")))

  ## --- PAR-06 hijo faltante, ambos padres tipificados ----------------------
  AF <- c("1", "2"); MO <- c("3", "4"); UI <- c("2", "4")
  H1 <- setgt(pedtools::nuclearPed(fa = "AF", mo = "MO", children = "UI"),
              list(AF = AF, MO = MO, UI = UI), p)
  H2 <- list(pedtools::nuclearPed(fa = "AF", mo = "MO", children = "MP"),
             pedtools::singleton("UI"))
  add(mkcase("PAR-06", "Hijo desaparecido con ambos padres tipificados", "parentesco",
             H1, H2, gold = gold_child_of_couple(UI, AF, MO, p),
             gold_src = "Evett & Weir (1998): P(C|F,M)/P(C)",
             features = c("hijo_faltante", "dos_padres", "sin_mutacion")))

  ## --- PAR-07 hijo faltante, un solo padre tipificado ----------------------
  AF <- c("1", "2"); UI <- c("2", "4")
  H1 <- setgt(pedtools::nuclearPed(fa = "AF", mo = "MO", children = "UI"),
              list(AF = AF, UI = UI), p)
  H2 <- list(pedtools::nuclearPed(fa = "AF", mo = "MO", children = "MP"),
             pedtools::singleton("UI"))
  add(mkcase("PAR-07", "Hijo desaparecido con un solo padre tipificado", "parentesco",
             H1, H2, gold = gold_duo(UI, AF, p), gold_src = "Evett & Weir (1998)",
             features = c("hijo_faltante", "un_padre", "sin_mutacion")))

  ## --- PAR-08 un abuelo paterno -------------------------------------------
  AB <- c("1", "2"); UI <- c("1", "3")
  H1 <- pedtools::linearPed(2)                      # 1,2 -> 3 ; 3,4 -> 5
  H1 <- pedtools::relabel(H1, new = c("AB", "ABX", "PAD", "PADX", "UI"))
  H1 <- setgt(H1, list(AB = AB, UI = UI), p)
  H2 <- list(pedtools::relabel(pedtools::linearPed(2),
                               new = c("AB", "ABX", "PAD", "PADX", "MP")),
             pedtools::singleton("UI"))
  add(mkcase("PAR-08", "Un abuelo paterno tipificado, nieto desaparecido", "parentesco",
             H1, H2, gold = gold_pairwise(AB, UI, KAPPA$abuelo_nieto, p),
             gold_src = "Evett & Weir (1998), kappa = (1/2,1/2,0)",
             features = c("abuelo", "un_abuelo", "sin_mutacion")))

  ## --- PAR-09 tio-sobrino ---------------------------------------------------
  TI <- c("1", "2"); UI <- c("1", "3")
  H1 <- pedtools::avuncularPed()
  H1 <- pedtools::relabel(H1, new = c("AB1", "AB2", "TIO", "PAD", "PADX", "UI"))
  H1 <- setgt(H1, list(TIO = TI, UI = UI), p)
  H2 <- list(pedtools::relabel(pedtools::avuncularPed(),
                               new = c("AB1", "AB2", "TIO", "PAD", "PADX", "MP")),
             pedtools::singleton("UI"))
  add(mkcase("PAR-09", "Tío paterno y sobrino desaparecido", "parentesco", H1, H2,
             gold = gold_pairwise(TI, UI, KAPPA$tio_sobrino, p),
             gold_src = "Evett & Weir (1998), kappa = (1/2,1/2,0)",
             features = c("tio", "sin_mutacion")))

  ## --- PAR-10 un hermano completo ------------------------------------------
  S1 <- c("1", "2"); UI <- c("1", "3")
  H1 <- setgt(pedtools::nuclearPed(children = c("S1", "UI")),
              list(S1 = S1, UI = UI), p)
  H2 <- list(pedtools::nuclearPed(children = c("S1", "MP")),
             pedtools::singleton("UI"))
  add(mkcase("PAR-10", "Un hermano completo", "parentesco", H1, H2,
             gold = gold_pairwise(S1, UI, KAPPA$hermanos, p),
             gold_src = "Evett & Weir (1998), kappa = (1/4,1/2,1/4)",
             features = c("hermanos", "un_hermano", "sin_mutacion")))

  ## --- PAR-11 dos hermanos completos ---------------------------------------
  S1 <- c("1", "2"); S2 <- c("1", "3"); UI <- c("2", "3")
  H1 <- setgt(pedtools::nuclearPed(children = c("S1", "S2", "UI")),
              list(S1 = S1, S2 = S2, UI = UI), p)
  H2 <- list(pedtools::nuclearPed(children = c("S1", "S2", "MP")),
             pedtools::singleton("UI"))
  add(mkcase("PAR-11", "Dos hermanos completos, tercer hermano desaparecido",
             "parentesco", H1, H2,
             gold = sl_3hermanos_A1(p[["1"]], p[["2"]], p[["3"]]),
             gold_src = "Slooten (2011), Ejemplo A.1",
             features = c("hermanos", "dos_hermanos", "sin_mutacion"),
             ref = "Slooten 2011 A.1"))

  ## --- PAR-12 medio hermano -------------------------------------------------
  S1 <- c("1", "2"); UI <- c("1", "3")
  H1 <- pedtools::relabel(pedtools::halfSibPed(), new = c("MA1", "PAD", "MA2", "S1", "UI"))
  H1 <- setgt(H1, list(S1 = S1, UI = UI), p)
  H2 <- list(pedtools::relabel(pedtools::halfSibPed(), new = c("MA1", "PAD", "MA2", "S1", "MP")),
             pedtools::singleton("UI"))
  add(mkcase("PAR-12", "Medio hermano paterno", "parentesco", H1, H2,
             gold = gold_pairwise(S1, UI, KAPPA$medio_hermanos, p),
             gold_src = "Evett & Weir (1998), kappa = (1/2,1/2,0)",
             features = c("medio_hermano", "sin_mutacion")))

  ## --- PAR-13 primos hermanos ----------------------------------------------
  C1 <- c("1", "2"); UI <- c("1", "3")
  H1 <- pedtools::cousinPed(1)
  H1 <- pedtools::relabel(H1, new = c("AB1","AB2","PA1","PX1","PA2","PX2","C1","UI"))
  H1 <- setgt(H1, list(C1 = C1, UI = UI), p)
  H2 <- list(pedtools::relabel(pedtools::cousinPed(1),
                               new = c("AB1","AB2","PA1","PX1","PA2","PX2","C1","MP")),
             pedtools::singleton("UI"))
  add(mkcase("PAR-13", "Primos hermanos", "parentesco", H1, H2,
             gold = gold_pairwise(C1, UI, KAPPA$primos, p),
             gold_src = "Evett & Weir (1998), kappa = (3/4,1/4,0)",
             features = c("primos", "parentesco_lejano", "sin_mutacion")))

  ## --- PAR-14 bisabuelo ----------------------------------------------------
  BA <- c("1", "2"); UI <- c("1", "3")
  H1 <- pedtools::relabel(pedtools::linearPed(3),
                          new = c("BIS","BISX","AB","ABX","PAD","PADX","UI"))
  H1 <- setgt(H1, list(BIS = BA, UI = UI), p)
  H2 <- list(pedtools::relabel(pedtools::linearPed(3),
                               new = c("BIS","BISX","AB","ABX","PAD","PADX","MP")),
             pedtools::singleton("UI"))
  add(mkcase("PAR-14", "Bisabuelo y bisnieto desaparecido", "parentesco", H1, H2,
             gold = gold_pairwise(BA, UI, KAPPA$bisabuelo, p),
             gold_src = "Evett & Weir (1998), kappa = (3/4,1/4,0)",
             features = c("bisabuelo", "parentesco_lejano", "sin_mutacion")))

  ## --- PAR-15 dos tias del mismo lado (Slooten 4.5) ------------------------
  ## S1 y S2 son hermanas del progenitor del desaparecido
  H1 <- pedtools::nuclearPed(children = c("S1", "S2", "PAD"),
                             sex = c(2, 2, 1))
  H1 <- pedtools::addSon(H1, parents = c("PAD", "PADX"), id = "UI")
  H1 <- setgt(H1, list(S1 = c("1","2"), S2 = c("1","2"), UI = c("1","3")), p)
  H2b <- pedtools::nuclearPed(children = c("S1", "S2", "PAD"), sex = c(2, 2, 1))
  H2b <- pedtools::addSon(H2b, parents = c("PAD", "PADX"), id = "MP")
  H2 <- list(H2b, pedtools::singleton("UI"))
  add(mkcase("PAR-15", "Dos tías del mismo lado (S1=S2=x/y, UI=x/z)", "parentesco",
             H1, H2, gold = sl_2tias_A3(p[["1"]], p[["2"]]),
             gold_src = "Slooten (2011), Ejemplo A.3",
             features = c("tias", "dos_tios", "sin_mutacion"),
             ref = "Slooten 2011 A.3"))

  ## --- PAR-16 identidad directa (mismo individuo) --------------------------
  G <- c("1", "3")
  H1 <- setgt(pedtools::nuclearPed(children = c("A", "UI")), list(A = G, UI = G), p)
  add(mkcase("PAR-16", "Perfil idéntico comparado como hermanos", "parentesco", H1,
             list(pedtools::nuclearPed(children = c("A", "MP")), pedtools::singleton("UI")),
             gold = gold_pairwise(G, G, KAPPA$hermanos, p),
             gold_src = "Evett & Weir (1998), kappa = (1/4,1/2,1/4)",
             features = c("hermanos", "perfiles_identicos", "sin_mutacion")))

  ## --- PAR-17 exclusion: duo sin alelo compartido --------------------------
  AF <- c("1", "2"); CH <- c("3", "4")
  H1 <- setgt(pedtools::nuclearPed(fa = "AF", mo = "MO", children = "CH"),
              list(AF = AF, CH = CH), p)
  add(mkcase("PAR-17", "Dúo paterno sin alelo compartido (exclusión, LR=0)", "parentesco",
             H1, list(pedtools::singleton("AF"), pedtools::singleton("CH")),
             gold = 0, gold_src = "Exclusion sin mutacion: P(datos|H1)=0",
             features = c("duo", "exclusion", "sin_mutacion")))

  cs
}

## ===========================================================================
## GRUPO SLO : casos del articulo de Slooten (2011)
## ===========================================================================

cases_slooten <- function(p = P4) {
  cs <- list(); add <- function(x) cs[[length(cs) + 1]] <<- x

  ## --- SLO-41 Ejemplo 4.1 : trio clasico, todos (a,a) ----------------------
  a <- "1"; G <- c(a, a)
  H1 <- setgt(pedtools::nuclearPed(fa = "UI", mo = "MO", children = "CH"),
              list(UI = G, MO = G, CH = G), p)
  H2 <- list(pedtools::nuclearPed(fa = "NN", mo = "MO", children = "CH"),
             pedtools::singleton("UI"))
  add(mkcase("SLO-41", "Trío clásico UI=MO=CH=(1,1): LR = 1/p1", "slooten", H1, H2,
             gold = sl_ex41(p[[a]]), gold_src = "Slooten (2011), Ejemplo 4.1",
             features = c("trio", "sin_mutacion", "verificacion_frecuencias"),
             ref = "Slooten 2011 Ex 4.1"))

  ## --- SLO-A1 Ejemplo A.1 : tercer hermano ---------------------------------
  x <- "1"; y <- "2"; z <- "3"
  H1 <- setgt(pedtools::nuclearPed(children = c("S1", "S2", "UI")),
              list(S1 = c(x, y), S2 = c(x, z), UI = c(y, z)), p)
  add(mkcase("SLO-A1", "Tercer hermano: S1=(x,y), S2=(x,z), UI=(y,z)", "slooten", H1,
             list(pedtools::nuclearPed(children = c("S1", "S2", "MP")),
                  pedtools::singleton("UI")),
             gold = sl_3hermanos_A1(p[[x]], p[[y]], p[[z]]),
             gold_src = "Slooten (2011), Ejemplo A.1",
             features = c("hermanos", "dos_hermanos", "sin_mutacion"),
             ref = "Slooten 2011 A.1"))

  ## --- SLO-A3 Ejemplo A.3 : dos tias ---------------------------------------
  H1 <- pedtools::nuclearPed(children = c("S1", "S2", "PAD"), sex = c(2, 2, 1))
  H1 <- pedtools::addSon(H1, parents = c("PAD", "PADX"), id = "UI")
  H1 <- setgt(H1, list(S1 = c("1","2"), S2 = c("1","2"), UI = c("1","3")), p)
  H2b <- pedtools::nuclearPed(children = c("S1", "S2", "PAD"), sex = c(2, 2, 1))
  H2b <- pedtools::addSon(H2b, parents = c("PAD", "PADX"), id = "MP")
  add(mkcase("SLO-A3", "Dos tías: S1=S2=(x,y), UI=(x,z)", "slooten", H1,
             list(H2b, pedtools::singleton("UI")),
             gold = sl_2tias_A3(p[["1"]], p[["2"]]),
             gold_src = "Slooten (2011), Ejemplo A.3",
             features = c("tias", "dos_tios", "sin_mutacion"),
             ref = "Slooten 2011 A.3"))

  ## --- SLO-INC caso incestuoso (sec. 4.2) ----------------------------------
  ## F es padre de D, y F con D tienen al desaparecido MP. D es la unica
  ## tipificada ademas del UI.
  inc <- pedtools::nuclearPed(fa = "F", mo = "M", children = "D", sex = 2)
  inc <- pedtools::addSon(inc, parents = c("F", "D"), id = "UI")
  D <- c("1", "2"); U <- c("1", "3")
  H1 <- setgt(inc, list(D = D, UI = U), p)
  incb <- pedtools::nuclearPed(fa = "F", mo = "M", children = "D", sex = 2)
  incb <- pedtools::addSon(incb, parents = c("F", "D"), id = "MP")
  Mid <- diag(length(p)); dimnames(Mid) <- list(names(p), names(p))
  add(mkcase("SLO-INC", "Pedigrí incestuoso: el padre del desaparecido es el padre de la madre",
             "slooten", H1, list(incb, pedtools::singleton("UI")),
             gold = sl_incesto(U, D, Mid, p),
             gold_src = "Slooten (2011), ec. (4.2) con M = I",
             features = c("consanguinidad", "incesto", "sin_mutacion"),
             ref = "Slooten 2011 sec 4.2"))

  ## --- SLO-3G tres generaciones, pedigri completo (ec. 4.3) ----------------
  A <- "1"; B <- "2"; C <- "1"; D <- "3"; E <- "1"; Fa <- "4"
  g3 <- pedtools::relabel(pedtools::linearPed(2),
                          new = c("PROG", "PROGX", "UI", "UIX", "HIJO"))
  H1 <- setgt(g3, list(PROG = c(A, B), UI = c(C, D), HIJO = c(E, Fa)), p)
  H2 <- list(pedtools::relabel(pedtools::linearPed(2),
                               new = c("PROG", "PROGX", "MP", "MPX", "HIJO")),
             pedtools::singleton("UI"))
  add(mkcase("SLO-3G", "Tres generaciones (progenitor e hijo del desaparecido), sin mutación",
             "slooten", H1, H2,
             gold = sl_3gen_full(A, B, C, D, E, Fa, Mid <- {
               M <- diag(length(p)); dimnames(M) <- list(names(p), names(p)); M }, p),
             gold_src = "Slooten (2011), ec. (4.3) con M = I",
             features = c("tres_generaciones", "sin_mutacion"),
             ref = "Slooten 2011 sec 4.3"))

  cs
}

## ===========================================================================
## GRUPO MUT : modelos de mutacion
## ===========================================================================

MUT_ALLELES <- c("8", "9", "10", "11", "12")
PMUT <- setNames(c(0.10, 0.20, 0.30, 0.25, 0.15), MUT_ALLELES)
## base con microvariante, para el modelo stepwise extendido
MUT_ALLELES_MV <- c("8", "9", "9.3", "10", "11", "12")
PMUT_MV <- setNames(c(0.10, 0.15, 0.10, 0.30, 0.20, 0.15), MUT_ALLELES_MV)

cases_mutacion <- function() {
  cs <- list(); add <- function(x) cs[[length(cs) + 1]] <<- x

  ## Estos casos NO declaran diferencia de convencion por poda de fundadores:
  ## se mide. La diferencia entre conservar el fundador no tipificado (el alelo
  ## no heredado se distribuye segun q = M^t p) y podarlo (se distribuye segun
  ## p) solo se manifiesta con mutacion, y se determina caso por caso
  ## --`mut_references()` evalua la expresion cerrada en las dos convenciones y
  ## `mut_decomposition()` dice cual explico el numero--, asi que cada motor
  ## recibe el veredicto que le corresponde por el valor, con la convencion
  ## detectada al lado.
  ##
  ## `gold_mut` es la expresion cerrada del caso con la matriz y la convencion
  ## como argumentos. De ahi salen las cuatro referencias del caso.

  duo <- function(af, ch, afreq, mutmod, id, titulo, feats, gold = NA_real_, src = NA) {
    force(af); force(ch); force(afreq)
    H1 <- setgt(pedtools::nuclearPed(fa = "AF", mo = "MO", children = "CH"),
                list(AF = af, CH = ch), afreq, mutmod = mutmod)
    gm <- if (is.null(mutmod)) NULL else
      function(Mm, Mf, podado) gold_duo_mut(af, ch, Mm, afreq, podado = podado)
    ## Valor de referencia calculado con la expresion cerrada, en la
    ## convencion de pedigri completo (sin podar).
    if (is.na(gold) && !is.null(mutmod)) {
      M <- as.matrix(pedmut::mutationModel(mutmod)$male)
      gold <- gm(M, M, FALSE)
      src  <- "Expresion cerrada del duo con mutacion, pedigri completo (analogo de Slooten 2011 ec. 4.3)"
    }
    alt <- if (is.null(gm)) NA_real_ else {
      M <- as.matrix(pedmut::mutationModel(mutmod)$male); gm(M, M, TRUE) }
    mkcase(id, titulo, "mutacion", H1,
           list(pedtools::singleton("AF"), pedtools::singleton("CH")),
           gold = gold, gold_src = src, features = feats, gold_mut = gm,
           gold_alt = alt)
  }

  eq  <- pedmut::mutationModel("equal", alleles = MUT_ALLELES, rate = 0.005)
  stw <- pedmut::mutationModel("stepwise", alleles = MUT_ALLELES, afreq = PMUT,
                               rate = 0.005, rate2 = 1e-6, range = 0.1)

  ## --- MUT-01..02 duo con una mutacion de un paso, dos modelos --------------
  add(duo(c("10", "10"), c("11", "12"), PMUT, eq, "MUT-01",
          "Dúo con mutación de un paso, modelo Equal",
          c("duo", "mutacion", "modelo_equal", "un_paso")))
  add(duo(c("10", "10"), c("11", "12"), PMUT, stw, "MUT-02",
          "Dúo con mutación de un paso, modelo Stepwise",
          c("duo", "mutacion", "modelo_stepwise", "un_paso")))

  ## --- MUT-03 mutacion de dos pasos (discrimina stepwise de equal) ---------
  add(duo(c("10", "10"), c("12", "8"), PMUT, stw, "MUT-03",
          "Dúo con mutación de dos pasos, modelo Stepwise",
          c("duo", "mutacion", "modelo_stepwise", "dos_pasos")))

  ## --- MUT-04 mutacion a microvariante (stepwise extendido) ----------------
  stw_mv <- pedmut::mutationModel("stepwise", alleles = MUT_ALLELES_MV,
                                  afreq = PMUT_MV, rate = 0.005,
                                  rate2 = 1e-4, range = 0.1)
  add(duo(c("9", "9"), c("9.3", "10"), PMUT_MV, stw_mv, "MUT-04",
          "Dúo con mutación entera->microvariante (9 -> 9.3), Stepwise extendido",
          c("duo", "mutacion", "microvariante", "modelo_stepwise", "mutacion_fraccional")))

  ## --- MUT-05 mutacion desde microvariante ---------------------------------
  add(duo(c("9.3", "9.3"), c("10", "11"), PMUT_MV, stw_mv, "MUT-05",
          "Dúo con mutación microvariante->entera (9.3 -> 10), Stepwise extendido",
          c("duo", "mutacion", "microvariante", "modelo_stepwise", "mutacion_fraccional")))

  ## --- MUT-06 mutacion entre microvariantes de la misma clase --------------
  P_MV2 <- setNames(c(0.20, 0.15, 0.25, 0.20, 0.20),
                    c("9", "9.3", "10", "10.3", "11"))
  stw_mv2 <- pedmut::mutationModel("stepwise", alleles = names(P_MV2),
                                   afreq = P_MV2, rate = 0.005,
                                   rate2 = 1e-4, range = 0.1)
  add(duo(c("9.3", "9.3"), c("10.3", "11"), P_MV2, stw_mv2, "MUT-06",
          "Dúo con mutación 9.3 -> 10.3 (mismo grupo decimal, un paso)",
          c("duo", "mutacion", "microvariante", "modelo_stepwise", "mismo_grupo_decimal")))

  ## --- MUT-07 mutacion sexo-especifica -------------------------------------
  msex <- pedmut::mutationModel(
    list(male   = pedmut::mutationMatrix("equal", alleles = MUT_ALLELES, rate = 0.01),
         female = pedmut::mutationMatrix("equal", alleles = MUT_ALLELES, rate = 0.001)))
  H1 <- setgt(pedtools::nuclearPed(fa = "AF", mo = "MO", children = "CH"),
              list(AF = c("10","10"), MO = c("9","9"), CH = c("11","9")),
              PMUT, mutmod = msex)
  gm_08 <- function(Mm, Mf, podado)
    gold_trio_mut(c("11","9"), c("9","9"), c("10","10"), Mm, Mf, PMUT, podado = podado)
  add(mkcase("MUT-07", "Trío con tasas de mutación distintas por sexo (paterna 10x)",
             "mutacion", H1,
             list(pedtools::nuclearPed(fa = "NN", mo = "MO", children = "CH"),
                  pedtools::singleton("AF")),
             gold = gm_08(as.matrix(msex$male), as.matrix(msex$female), FALSE),
             gold_src = "Expresion cerrada del trio con mutacion dependiente del sexo, pedigri completo",
             features = c("trio", "mutacion", "sexo_especifica"),
             gold_mut = gm_08,
             gold_alt = gm_08(as.matrix(msex$male), as.matrix(msex$female), TRUE)))

  ## --- MUT-08 exclusion sin mutacion vs con mutacion -----------------------
  add(duo(c("10", "10"), c("11", "12"), PMUT, NULL, "MUT-08",
          "Dúo excluyente SIN modelo de mutación (LR debe ser 0)",
          c("duo", "exclusion", "sin_mutacion"), gold = 0,
          src = "P(datos|H1) = 0 sin mutacion"))

  ## --- MUT-09 trio consistente con mutacion activa -------------------------
  ## El LR debe ser practicamente igual al del mismo trio sin mutacion.
  H1 <- setgt(pedtools::nuclearPed(fa = "AF", mo = "MO", children = "CH"),
              list(AF = c("10","11"), MO = c("9","12"), CH = c("10","9")),
              PMUT, mutmod = eq)
  gm_11 <- function(Mm, Mf, podado)
    gold_trio_mut(c("10","9"), c("9","12"), c("10","11"), Mm, Mf, PMUT, podado = podado)
  add(mkcase("MUT-09", "Trío consistente con modelo de mutación activo", "mutacion", H1,
             list(pedtools::nuclearPed(fa = "NN", mo = "MO", children = "CH"),
                  pedtools::singleton("AF")),
             gold = gm_11(as.matrix(eq$male), as.matrix(eq$female), FALSE),
             gold_src = "Expresion cerrada del trio con mutacion, pedigri completo",
             features = c("trio", "mutacion", "sin_inconsistencia"),
             gold_mut = gm_11,
             gold_alt = gm_11(as.matrix(eq$male), as.matrix(eq$female), TRUE)))

  ## --- MUT-10 tasa alta (sensibilidad) --------------------------------------
  eq_alta <- pedmut::mutationModel("equal", alleles = MUT_ALLELES, rate = 0.10)
  add(duo(c("10", "10"), c("11", "12"), PMUT, eq_alta, "MUT-10",
          "Dúo con tasa de mutación alta (0.10), modelo Equal",
          c("duo", "mutacion", "tasa_alta", "modelo_equal")))

  ## El generador de matrices del sistema evaluado no se mide con estos casos:
  ## se mide directamente, sobre las matrices que el sistema vuelca
  ## (`mutgrid.R`).

  cs
}

## ===========================================================================
## GRUPO SIL : alelo silente
## ===========================================================================

cases_silente <- function() {
  cs <- list(); add <- function(x) cs[[length(cs) + 1]] <<- x
  p <- add_silent(P4, 0.05)     # 1,2,3,4 + silent

  ## Enumeracion explicita del LR de un duo cuando lo que se registra son
  ## BANDAS y no genotipos. Una banda unica `a` es compatible con los
  ## genotipos a/a y a/silent; el LR se obtiene sumando sobre ambos.
  ## Es algebra escrita a mano, no un motor: cuatro terminos.
  gold_duo_bandas <- function(banda_af, banda_ch, p, s = "silent") {
    gt <- function(b) list(c(b, b), c(b, s))                  # genotipos compatibles
    pr <- function(g) if (g[1] == g[2]) p[[g[1]]]^2 else 2 * p[[g[1]]] * p[[g[2]]]
    tr <- function(x, g) sum(g == x) / 2                      # transmision sin mutacion
    pc <- function(C, F) if (C[1] == C[2]) tr(C[1], F) * p[[C[1]]]
                         else tr(C[1], F) * p[[C[2]]] + tr(C[2], F) * p[[C[1]]]
    L1 <- 0; L2 <- 0
    for (gf in gt(banda_af)) for (gc in gt(banda_ch)) {
      L1 <- L1 + pr(gf) * pc(gc, gf)
      L2 <- L2 + pr(gf) * pr(gc)
    }
    L1 / L2
  }

  div_sil <- paste(
    "El caso registra una sola banda en cada individuo (1 en el padre, 3 en el",
    "hijo), escrita como genotipo 1/1 y 3/3. El valor de referencia sigue la",
    "convención de Familias: una banda única puede ser a/a o a/silente, y se suma",
    "sobre las dos posibilidades. La otra convención, la de pedsuite, toma el",
    "genotipo tal como está escrito: con 1/1 y 3/3 el dúo es una exclusión y el",
    "LR es 0.")

  ## --- SIL-01 banda unica: dos convenciones de lectura ---------------------
  ## Padre con banda unica 1, hijo con banda unica 3. Sin alelo silente es
  ## una exclusion; con alelo silente ambos pueden ser heterocigotos con el
  ## silente y la exclusion desaparece.
  H1 <- setgt(pedtools::nuclearPed(fa = "AF", mo = "MO", children = "CH"),
              list(AF = c("1", "1"), CH = c("3", "3")), p)
  add(mkcase("SIL-01", "Dúo con banda única en ambos, leída como a/a o a/silente",
             "silente", H1,
             list(pedtools::singleton("AF"), pedtools::singleton("CH")),
             gold = gold_duo_bandas("1", "3", p),
             gold_src = "Enumeracion sobre los genotipos compatibles con cada banda observada",
             features = c("duo", "alelo_silente", "exclusion_aparente", "banda_unica"),
             motor_ref = "familias", divergencia = div_sil, gold_alt = 0))

  ## --- SIL-02 heterocigoto explicito con el alelo silente ------------------
  ## Aqui el genotipo es inequivoco (1/silent no es una banda unica sino una
  ## asignacion explicita), de modo que ambos motores deben coincidir.
  H1 <- setgt(pedtools::nuclearPed(fa = "AF", mo = "MO", children = "CH"),
              list(AF = c("1", "2"), MO = c("3", "silent"), CH = c("1", "silent")), p)
  add(mkcase("SIL-02", "Trío con genotipos explícitos madre 3/silent e hijo 1/silent",
             "silente", H1,
             list(pedtools::nuclearPed(fa = "NN", mo = "MO", children = "CH"),
                  pedtools::singleton("AF")),
             gold = gold_trio(c("1","silent"), c("3","silent"), c("1","2"), p),
             gold_src = "Evett & Weir (1998), tratando 'silent' como un alelo mas",
             features = c("trio", "alelo_silente", "genotipo_explicito")))

  ## --- SIL-03 locus nulo (ninguna banda) -----------------------------------
  ## No existe una forma portable de registrar "el locus amplifico y no
  ## mostro ninguna banda". Escribir silent/silent no significa lo mismo en
  ## los dos programas.
  H1 <- setgt(pedtools::nuclearPed(fa = "AF", mo = "MO", children = "CH"),
              list(AF = c("silent", "silent"), CH = c("silent", "silent")), p)
  add(mkcase("SIL-03", "Dúo con locus nulo registrado como silent/silent", "silente", H1,
             list(pedtools::singleton("AF"), pedtools::singleton("CH")),
             gold = gold_duo(c("silent","silent"), c("silent","silent"), p),
             gold_src = "Evett & Weir (1998) con 'silent' como alelo ordinario: LR = 1/p_silent",
             features = c("duo", "alelo_silente", "locus_nulo"),
             motor_ref = "pedsuite",
             divergencia = paste(familias_label(),
                                 "lee el registro silent/silent con otra convención",
                                 "(al menos un alelo silente); el valor de referencia",
                                 "es el del genotipo silente homocigoto.")))

  cs
}

## ===========================================================================
## GRUPO FRQ : frecuencias, microvariantes, alelos raros
## ===========================================================================

cases_frecuencias <- function() {
  cs <- list(); add <- function(x) cs[[length(cs) + 1]] <<- x

  ## --- FRQ-01 microvariante comun ------------------------------------------
  pmv <- setNames(c(0.20, 0.10, 0.30, 0.25, 0.15), c("14", "15.2", "16", "17", "18"))
  H1 <- setgt(pedtools::nuclearPed(fa = "AF", mo = "MO", children = "CH"),
              list(AF = c("15.2", "16"), CH = c("15.2", "17")), pmv)
  add(mkcase("FRQ-01", "Dúo con microvariante 15.2", "frecuencias", H1,
             list(pedtools::singleton("AF"), pedtools::singleton("CH")),
             gold = gold_duo(c("15.2","17"), c("15.2","16"), pmv),
             gold_src = "Evett & Weir (1998)",
             features = c("duo", "microvariante", "sin_mutacion")))

  ## --- FRQ-02 microvariante rara (frecuencia minima) -----------------------
  ## El panel se declara crudo y el piso aparte. El paquete entrega el crudo:
  ## si viajara ya pisado, el sistema bajo validacion no tendria donde aplicar
  ## el piso sin contarlo dos veces.
  crudo_mv <- add_alleles(pmv, "15.3", 1e-6)
  praro    <- apply_min_freq(crudo_mv, 0.001)
  H1 <- setgt(pedtools::nuclearPed(fa = "AF", mo = "MO", children = "CH"),
              list(AF = c("15.3", "16"), CH = c("15.3", "17")), praro)
  add(mkcase("FRQ-02", "Dúo con microvariante rara sujeta a frecuencia mínima 0.001",
             "frecuencias", H1,
             list(pedtools::singleton("AF"), pedtools::singleton("CH")),
             gold = gold_duo(c("15.3","17"), c("15.3","16"), praro),
             gold_src = "Evett & Weir (1998) con frecuencias tras aplicar el piso",
             piso = 0.001, freqs_crudas = list(L1 = crudo_mv),
             features = c("duo", "microvariante", "frecuencia_minima",
                          "alelo_raro", "piso_declarado")))

  ## --- FRQ-02S gemelo de FRQ-02 sin piso declarado -------------------------
  ## Mismo pedigri, mismos genotipos y el MISMO panel crudo que FRQ-02, pero sin
  ## piso: todo lo demas se cancela y la diferencia entre los dos mide la logica
  ## del piso y nada mas. Lo que se pide es el piso *declarado*: sin este caso,
  ## un sistema que aplica un piso propio no solicitado no se distinguiria,
  ## porque el alelo mas raro del resto del catalogo vale 0.05. El par S es un
  ## contraste (250.24975 contra 250000, por construccion).
  H1 <- setgt(pedtools::nuclearPed(fa = "AF", mo = "MO", children = "CH"),
              list(AF = c("15.3", "16"), CH = c("15.3", "17")), crudo_mv)
  add(mkcase("FRQ-02S", "Gemelo de FRQ-02 sin piso declarado (alelo raro a 1e-6)",
             "frecuencias", H1,
             list(pedtools::singleton("AF"), pedtools::singleton("CH")),
             gold = gold_duo(c("15.3","17"), c("15.3","16"), crudo_mv),
             gold_src = "Evett & Weir (1998), sobre el panel sin piso",
             features = c("duo", "microvariante", "alelo_raro",
                          "piso_no_declarado", "control_pareado")))

  ## --- FRQ-03 alelo de frecuencia muy baja (LR muy grande) -----------------
  crudo_bj <- add_alleles(P4, "9", 1e-8)
  pbaja    <- apply_min_freq(crudo_bj, 5e-4)
  H1 <- setgt(pedtools::nuclearPed(fa = "AF", mo = "MO", children = "CH"),
              list(AF = c("9", "2"), CH = c("9", "3")), pbaja)
  add(mkcase("FRQ-03", "Dúo sobre un alelo en el piso de frecuencia (5e-4)", "frecuencias", H1,
             list(pedtools::singleton("AF"), pedtools::singleton("CH")),
             gold = gold_duo(c("9","3"), c("9","2"), pbaja),
             gold_src = "Evett & Weir (1998)",
             piso = 5e-4, freqs_crudas = list(L1 = crudo_bj),
             features = c("duo", "frecuencia_minima", "LR_grande",
                          "estabilidad_numerica", "piso_declarado")))

  ## --- FRQ-04 base real: trio multi-locus con frecuencias argentinas -------
  fr <- pick_loci(freqs_mispitools("Argentina"),
                  c("D3S1358", "VWA", "FGA", "THO1", "D18S51"))
  gt <- list(
    D3S1358 = list(AF = c("15","16"), MO = c("14","17"), CH = c("15","14")),
    VWA     = list(AF = c("16","17"), MO = c("15","18"), CH = c("17","15")),
    FGA     = list(AF = c("22","24"), MO = c("21","23"), CH = c("24","21")),
    THO1    = list(AF = c("6","9.3"), MO = c("7","8"),   CH = c("9.3","7")),
    D18S51  = list(AF = c("14","16"), MO = c("13","15"), CH = c("16","13")))
  add(mkcase("FRQ-04", "Trío multi-locus con base argentina (5 loci, incluye THO1 9.3)",
             "frecuencias",
             build_multilocus(pedtools::nuclearPed(fa = "AF", mo = "MO", children = "CH"), gt, fr),
             list(pedtools::nuclearPed(fa = "NN", mo = "MO", children = "CH"),
                  pedtools::singleton("AF")),
             gold = prod(mapply(function(nm) gold_trio(gt[[nm]]$CH, gt[[nm]]$MO, gt[[nm]]$AF, fr[[nm]]),
                                names(gt))),
             gold_src = "Producto de indices de paternidad por locus [Evett & Weir 1998]",
             features = c("trio", "multi_locus", "base_real", "microvariante")))

  ## --- FRQ-05..09 alelo observado que el panel crudo no trae ----------------
  ## El panel viaja crudo y SIN el alelo; el caso declara el piso. La operacion
  ## que se le pide al sistema es el piso con el alelo observado adentro: el
  ## alelo entra valiendo el piso y se divide el panel entero por la suma. Los
  ## motores de referencia consumen el panel ya armado (`apply_min_freq_observed`).
  duo_obs <- function(id, titulo, af, ch, crudo, piso, feats) {
    pc <- apply_min_freq_observed(crudo, piso, unique(c(af, ch)))
    H1 <- setgt(pedtools::nuclearPed(fa = "AF", mo = "MO", children = "CH"),
                list(AF = af, CH = ch), pc)
    mkcase(id, titulo, "frecuencias", H1,
           list(pedtools::singleton("AF"), pedtools::singleton("CH")),
           gold = gold_duo(ch, af, pc),
           gold_src = "Evett & Weir (1998), sobre el panel con el alelo observado agregado al piso y renormalizado",
           piso = piso, freqs_crudas = list(L1 = crudo),
           features = c("duo", "frecuencia_minima", "piso_declarado",
                        "alelo_fuera_del_panel", feats))
  }
  add(duo_obs("FRQ-05", "Dúo: el padre alegado trae un alelo que el panel no tiene (piso 0.01)",
              c("2", "9"), c("2", "3"), P4, 0.01, "alelo_nuevo_en_PI"))
  add(duo_obs("FRQ-06", "Dúo: el hijo trae un alelo que el panel no tiene (piso 0.01)",
              c("2", "3"), c("2", "9"), P4, 0.01, "alelo_nuevo_en_familia"))
  add(duo_obs("FRQ-07", "Dúo: el alelo compartido es uno que el panel no tiene (piso 0.01)",
              c("9", "2"), c("9", "3"), P4, 0.01, c("alelo_nuevo_compartido", "LR_grande")))
  ## dos alelos nuevos en el mismo locus, uno en cada progenitor
  AF <- c("1", "9"); MO <- c("3", "8"); CH <- c("1", "3")
  pc2 <- apply_min_freq_observed(P4, 0.01, unique(c(AF, MO, CH)))
  H1 <- setgt(pedtools::nuclearPed(fa = "AF", mo = "MO", children = "CH"),
              list(AF = AF, MO = MO, CH = CH), pc2)
  add(mkcase("FRQ-08", "Trío con dos alelos que el panel no tiene, uno en cada progenitor (piso 0.01)",
             "frecuencias", H1,
             list(pedtools::nuclearPed(fa = "NN", mo = "MO", children = "CH"),
                  pedtools::singleton("AF")),
             gold = gold_trio(CH, MO, AF, pc2),
             gold_src = "Evett & Weir (1998), trio, sobre el panel con los alelos observados agregados al piso y renormalizado",
             piso = 0.01, freqs_crudas = list(L1 = P4),
             features = c("trio", "madre_tipificada", "frecuencia_minima", "piso_declarado",
                          "alelo_fuera_del_panel", "dos_alelos_nuevos")))
  ## el alelo nuevo convive con un alelo del panel por debajo del piso
  crudo_mx <- add_alleles(P4, "7", 1e-6)
  add(duo_obs("FRQ-09", "Dúo: alelo que el panel no tiene, sobre un panel con otro alelo bajo el piso (0.01)",
              c("9", "2"), c("9", "3"), crudo_mx, 0.01,
              c("alelo_nuevo_compartido", "alelo_raro", "LR_grande")))

  cs
}

#' Adjunta varios marcadores a un pedigri
#' @keywords internal
build_multilocus <- function(ped, gt, freqs, mutmods = NULL) {
  for (nm in names(gt)) {
    args <- c(list(ped), gt[[nm]], list(afreq = freqs[[nm]], name = nm))
    if (!is.null(mutmods[[nm]])) args$mutmod <- mutmods[[nm]]
    ped <- do.call(pedtools::addMarker, args)
  }
  ped
}

## ===========================================================================
## Ensamblado
## ===========================================================================

#' Construye el catalogo completo de casos de validacion
#' @export
build_catalog <- function() {
  cs <- c(cases_parentesco(), cases_slooten(), cases_mutacion(),
          cases_silente(), cases_frecuencias(), cases_poda(),
          cases_invariantes())
  names(cs) <- vapply(cs, function(x) x$id, "")
  if (anyDuplicated(names(cs)))
    stop("build_catalog: identificadores de caso duplicados: ",
         paste(names(cs)[duplicated(names(cs))], collapse = ", "))
  cs
}

#' Resumen tabular del catalogo
#' @export
catalog_table <- function(cs) {
  do.call(rbind, lapply(cs, function(x) data.frame(
    id = x$id, grupo = x$grupo, titulo = x$titulo,
    n_loci = pedtools::nMarkers(if (pedtools::is.ped(x$H1)) x$H1 else x$H1[[1]]),
    tiene_gold = !is.na(x$gold),
    gold = x$gold, gold_src = x$gold_src,
    features = paste(x$features, collapse = ";"),
    ref = x$ref, nota = x$nota,
    motor_ref = x$motor_ref, divergencia = x$divergencia,
    stringsAsFactors = FALSE)))
}

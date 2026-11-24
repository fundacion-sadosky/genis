## ---------------------------------------------------------------------------
## lrval :: casos que exponen diferencias de convencion, e invariantes
##
## cases.R comprueba que cada motor calcule bien el LR de una pregunta que
## todos entienden igual. Aca se comprueba que la pregunta sea la misma: si dos
## programas siguen convenciones distintas para un mismo registro (un pedigri
## con fundadores no tipificados, bajo mutacion), el LR difiere sin que ninguno
## tenga un error.
##
## Campos propios de estos casos:
##   motor_ref    motor cuya convencion corresponde al valor de referencia
##   divergencia  la diferencia de convencion, en prosa
##   gold_alt     el LR bajo la otra convencion, en forma cerrada
##
## El comparador las informa como CONVENCION y no como FALLA, y las lista.
## ---------------------------------------------------------------------------

## ===========================================================================
## GRUPO PRU : poda del pedigri (Slooten 2011, sec. 4.3)
## ===========================================================================

cases_poda <- function() {
  cs <- list(); add <- function(x) cs[[length(cs) + 1]] <<- x
  p <- PMUT
  M <- pedmut::mutationMatrix("equal", alleles = names(p), rate = 0.01)
  mm <- pedmut::mutationModel(list(male = M, female = M))
  A <- "10"; B <- "11"; C <- "10"; D <- "9"; E <- "10"; Fa <- "12"

  ## Pedigri COMPLETO: el desaparecido y su hijo tienen ambos progenitores
  ## declarados (los no tipificados son fundadores del pedigri).
  g3 <- pedtools::relabel(pedtools::linearPed(2),
                          new = c("PROG", "PROGX", "UI", "UIX", "HIJO"))
  H1 <- pedtools::setMarkers(g3, pedtools::marker(
    g3, geno = c(PROG = paste(A, B, sep = "/"), PROGX = "-/-",
                 UI = paste(C, D, sep = "/"), UIX = "-/-",
                 HIJO = paste(E, Fa, sep = "/")),
    afreq = p, name = "L1", mutmod = mm))
  H2 <- list(pedtools::relabel(pedtools::linearPed(2),
                               new = c("PROG", "PROGX", "MP", "MPX", "HIJO")),
             pedtools::singleton("UI"))
  add(mkcase("PRU-01", "Tres generaciones CON mutación, pedigrí completo (sin podar)",
             "poda", H1, H2,
             gold = sl_3gen_full(A, B, C, D, E, Fa, as.matrix(M), p),
             gold_src = "Slooten (2011), ec. (4.3)",
             features = c("tres_generaciones", "mutacion", "pedigri_completo"),
             ref = "Slooten 2011 sec 4.3",
             motor_ref = "pedsuite",
             gold_alt = sl_3gen_pruned(A, B, C, D, E, Fa, as.matrix(M), p),
             divergencia = paste(
               "Con modelo de mutación hay dos convenciones para un fundador no",
               "tipificado con un solo hijo: conservarlo en el pedigrí (Slooten 2011,",
               "ec. 4.3) o podarlo (ec. 4.4). Sin mutación dan el mismo LR; con",
               "mutación, no. Este caso lleva el valor del pedigrí completo, que es la",
               "convención de pedsuite;", familias_label(), "poda.")))

  ## Valor de referencia del pedigri PODADO (ec. 4.4). pedtools no admite
  ## individuos con un solo progenitor, de modo que no puede representar este
  ## pedigri; Familias si. La diferencia entre PRU-01 y PRU-02 es exactamente
  ## el efecto de podar fundadores no tipificados EN PRESENCIA de mutacion.
  add(mkcase("PRU-02", "Tres generaciones CON mutación, pedigrí podado (valor de referencia)",
             "poda", H1, H2,
             gold = sl_3gen_pruned(A, B, C, D, E, Fa, as.matrix(M), p),
             gold_src = "Slooten (2011), ec. (4.4)",
             features = c("tres_generaciones", "mutacion", "pedigri_podado"),
             ref = "Slooten 2011 sec 4.3",
             motor_ref = "familias",
             gold_alt = sl_3gen_full(A, B, C, D, E, Fa, as.matrix(M), p),
             divergencia = paste(
               "El mismo pedigrí que PRU-01, con el valor del pedigrí podado (Slooten",
               "2011, ec. 4.4), que es la convención de", familias_label(), "con",
               "modelo de mutación. Un motor que conserva los fundadores no",
               "tipificados, como pedsuite, da aquí el valor de PRU-01.")))
  cs
}

## ===========================================================================
## GRUPO INV : invariantes que no requieren valor de referencia externo
## ===========================================================================

cases_invariantes <- function(p = P4) {
  cs <- list(); add <- function(x) cs[[length(cs) + 1]] <<- x

  ## --- INV-01 el duo de base contra el que se leen los otros tres ---------
  AF <- c("1", "2"); CH <- c("1", "3")
  H1 <- setgt(pedtools::nuclearPed(fa = "AF", mo = "MO", children = "CH"),
              list(AF = AF, CH = CH), p)
  H2 <- list(pedtools::singleton("AF"), pedtools::singleton("CH"))
  add(mkcase("INV-01", "Dúo paterno de base: padre 1/2, hijo 1/3",
             "invariantes",
             setgt(pedtools::nuclearPed(fa = "AF", mo = "MO", children = "CH"),
                   list(AF = AF, CH = CH), p),
             H2, gold = gold_duo(CH, AF, p),
             gold_src = "Evett & Weir (1998)",
             features = c("duo", "invariante")))

  ## --- INV-02 individuo no informativo: LR = 1 ------------------------------
  ## Un pariente sin genotipo no puede modificar el LR.
  ped <- pedtools::nuclearPed(fa = "AF", mo = "MO", children = c("CH", "HNO"))
  ped <- setgt(ped, list(AF = AF, CH = CH), p)
  add(mkcase("INV-02", "Un hermano no tipificado no altera el LR del dúo paterno",
             "invariantes", ped,
             list(pedtools::singleton("AF"), pedtools::singleton("CH")),
             gold = gold_duo(CH, AF, p),
             gold_src = "Evett & Weir (1998); un individuo sin datos se integra a 1",
             features = c("duo", "individuo_no_tipificado", "invariante")))

  ## --- INV-03 permutacion de alelos: el orden no importa -------------------
  add(mkcase("INV-03", "El orden de los alelos dentro del genotipo no altera el LR",
             "invariantes",
             setgt(pedtools::nuclearPed(fa = "AF", mo = "MO", children = "CH"),
                   list(AF = rev(AF), CH = rev(CH)), p),
             list(pedtools::singleton("AF"), pedtools::singleton("CH")),
             gold = gold_duo(CH, AF, p),
             gold_src = "Debe coincidir exactamente con INV-01",
             features = c("duo", "orden_alelos", "invariante")))

  ## --- INV-04 locus ausente en el perfil -----------------------------------
  ## Un locus no tipificado no debe aportar informacion: LR = 1 exactamente.
  H1 <- pedtools::nuclearPed(fa = "AF", mo = "MO", children = "CH")
  H1 <- pedtools::setMarkers(H1, pedtools::marker(
    H1, geno = c(AF = "1/2", MO = "-/-", CH = "-/-"), afreq = p, name = "L1"))
  add(mkcase("INV-04", "Locus no tipificado en el individuo cuestionado (LR debe ser 1)",
             "invariantes", H1,
             list(pedtools::singleton("AF"), pedtools::singleton("CH")),
             gold = 1, gold_src = "Un locus sin datos no modifica la verosimilitud",
             features = c("duo", "locus_ausente", "invariante")))

  cs
}

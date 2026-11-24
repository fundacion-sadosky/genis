#!/usr/bin/env Rscript
## ---------------------------------------------------------------------------
## El informe de la corrida, en dos niveles, para alguien que corre la
## validacion sin haberla disenado.
##
##   Rscript scripts/06_informe.R
##
## No recalcula nada: lee lo que ya esta en disco (el veredicto del catalogo,
## la entrega de GENis, la comparacion masiva), verifica que las piezas sean
## de la misma corrida, cruza las FALLA con hallazgos_genis.csv y escribe
##
##   out/VEREDICTO.txt   el nivel corto; es lo que este script imprime
##   out/INFORME.md      el nivel largo, con el detalle por caso y por tipo
##   out/INFORME.pdf     el mismo nivel largo, compilado con pdflatex desde
##                       out/INFORME.tex (sin pdflatex queda solo el .tex)
##
## `validation/correr` lo corre como ultimo paso de bloque1, masiva y todo, y
## suelto con `validation/correr informe`. Termina en 0 aunque haya FALLA:
## el veredicto se lee, no se deduce del codigo de salida.
## ---------------------------------------------------------------------------

source("load.R")

g <- generar_informe(LRVAL_DIRS)
cat("\n")
writeLines(g$veredicto)
if (is.null(g$pdf)) cat("INFORME.pdf no generado:", g$pdf_motivo, "\n")
cat("\n")

## ---------------------------------------------------------------------------
## Carga lrval sin instalarlo.
##
##   source("load.R")
##
## Se usa asi (y no como paquete instalado) a proposito: un set de validacion
## debe poder ejecutarse desde una copia del repositorio, sin pasos de
## instalacion que puedan introducir versiones distintas de las declaradas.
## ---------------------------------------------------------------------------

suppressPackageStartupMessages({
  library(pedtools)
  library(pedprobr)
  library(pedmut)
  library(forrel)
  library(ribd)
  library(Familias)
  library(mispitools)
})

local({
  cand <- c("lrval/R", "R", file.path(dirname(getwd()), "lrval", "R"))
  d <- cand[dir.exists(cand)][1]
  if (is.na(d))
    stop("load.R: no se encuentra el directorio lrval/R. ",
         "Ejecute desde validation/referencia/.")
  for (f in sort(list.files(d, pattern = "[.]R$", full.names = TRUE)))
    sys.source(f, envir = globalenv())
})

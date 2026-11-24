## ---------------------------------------------------------------------------
## lrval :: rutas de intercambio
##
## Unico lugar donde se declara donde entra y donde sale cada cosa. El modulo
## vive al lado del arnes del sistema evaluado, asi que el intercambio es por
## directorios hermanos y no por copias:
##
##   ../cases   lo que este modulo ENTREGA (paquete de casos + grilla)
##   ../out     lo que el sistema DEVUELVE; de solo lectura para el modulo
##
## Si estuvieran repartidas entre defaults de funciones y literales de scripts,
## mover el modulo exigiria encontrarlas una por una.
## ---------------------------------------------------------------------------

#' Directorios de intercambio y de salida
#' @export
LRVAL_DIRS <- list(
  paquete = "../cases",
  vuelta  = "../out",
  salida  = "out/resultados",
  masiva  = "out/masiva",
  ## descargas cacheadas (p.ej. STRidER): interna al modulo, git-ignorada
  cache   = "out/cache",
  ## el informe de la corrida (VEREDICTO.txt e INFORME.md) y la tabla
  ## hallazgos_genis.csv que lo alimenta (R/informe.R)
  informe   = "out",
  hallazgos = "hallazgos_genis.csv"
)

#!/usr/bin/env Rscript
## ---------------------------------------------------------------------------
## Sonda de costo del modelo de mutacion.
##
##   Rscript scripts/03_costo_mutacion.R [tope_segundos]
##
## Stepwise no cumple Kemeny-Snell, asi que pedprobr deja de podar alelos no
## observados y los tipos con fundadores no tipificados entre el desaparecido y
## los parientes tipificados se vuelven ordenes de magnitud mas caros. Esta
## sonda mide cuanto, por tipo y por motor, sobre family_lr_table(), que es la
## unidad real de costo: el pedigri se resuelve una vez por familia y locus, no
## una vez por comparacion.
##
## De aca sale el presupuesto con el que se fija la escala del escenario "con
## mutacion". Salida en out/masiva/costo_mutacion.csv.
##
## Tope por celda. Tiene que ser duro, porque el tope es lo unico que acota la
## corrida entera y una sola llamada a FamiliasPosterior sobre un pedigri con
## tios tarda minutos. No sirve withTimeout() de R.utils: es una envoltura
## sobre setTimeLimit(), que solo se controla entre evaluaciones de R y no
## interrumpe una llamada compilada en curso. Asi que cada celda se evalua en un proceso hijo (fork, Unix) y el
## corte es una senal al hijo, que no depende del motor.
##
## Estado de cada celda: "ok" es una medicion; "TOPE" dice que la celda se
## corto sin terminar --su tiempo es el tope, no un costo-- y "ERROR" que el
## motor no entrego resultado, sea porque aborto en R o porque el hijo murio
## sin entregar nada (OOM, aborto en C), con el motivo impreso en consola. Las
## celdas TOPE y ERROR no son costos: la suma que se extrapola es una cota
## inferior y el pie de la salida lo avisa.
##
## Para Familias la sonda mide el barrido sobre TODOS los genotipos del panel
## (family_lr_table() sin `genos`), mientras que mass_search() lo restringe a
## los que aparecen en la nomina. El costo de Familias es lineal en el mayor
## de esos dominios, asi que lo que mide la sonda es una cota superior de la
## corrida real por ese factor; pedsuite no depende del dominio.
## ---------------------------------------------------------------------------

source("load.R")

args <- commandArgs(trailingOnly = TRUE)
TOPE <- if (length(args) >= 1) as.numeric(args[1]) else 900
## un argumento no numerico da NA, y mccollect(timeout = NA) espera al hijo sin
## tope: un error de tipeo en la linea de lanzamiento dejaria la corrida sin lo
## unico que la acota, y en silencio
stopifnot(is.finite(TOPE), TOPE > 0)

dir.create(LRVAL_DIRS$masiva, recursive = TRUE, showWarnings = FALSE)
freqs <- freqs_mispitools("Argentina")

#' Evalua una expresion en un proceso hijo, con tope de tiempo de pared
#'
#' @param expr expresion a evaluar
#' @param tope segundos de tiempo transcurrido
#' @return el resultado; un error de clase "tope" si se agoto el tiempo, o el
#'   error del motor si aborto por su cuenta
con_tope <- function(expr, tope) {
  ## sin fork no hay tope duro, y aca el tope no es una comodidad sino lo unico
  ## que acota la corrida: mejor abortar que prometer un tope que no se cumple
  if (.Platform$OS.type != "unix")
    stop("la sonda necesita fork (unix) para que el tope por celda sea duro")
  hijo <- parallel::mcparallel(expr)
  r    <- parallel::mccollect(hijo, wait = FALSE, timeout = tope)
  if (is.null(r)) {
    tools::pskill(hijo$pid, tools::SIGKILL)
    ## wait = TRUE es lo que cosecha al hijo: espera su salida, cierra el pipe
    ## y lo saca de la lista de hijos. Con wait = FALSE queda un zombi y un
    ## descriptor abierto por celda cortada. El aviso "did not deliver a
    ## result" es justamente lo que se acaba de matar.
    suppressWarnings(parallel::mccollect(hijo, wait = TRUE))
    return(structure(class = c("tope", "error", "condition"),
                     list(message = "tope de tiempo alcanzado", call = NULL)))
  }
  r <- r[[1]]
  ## un hijo que muere sin entregar resultado --lo mata el OOM killer, o el
  ## motor aborta a nivel C-- vuelve como list(<pid> = NULL): no es NULL y no
  ## es un error, de modo que sin esta guarda la celda se anotaria como una
  ## medicion valida de unas centesimas. family_lr_table() nunca devuelve NULL.
  if (is.null(r))
    return(simpleError("el hijo murio sin entregar resultado"))
  ## mcparallel devuelve los errores del hijo como "try-error", no como
  ## condicion; el texto trae saltos de linea que ensucian la consola
  if (inherits(r, "try-error"))
    simpleError(gsub("[[:space:]]+", " ", trimws(as.character(r)))) else r
}

cat(sprintf("\nPanel: %d loci  |  modelo: %s, tasas %g/%g, rango %g, pasos %d\n",
            length(freqs), MUTACION_MASIVA$modelo,
            MUTACION_MASIVA$tasa[["M"]], MUTACION_MASIVA$tasa[["F"]],
            MUTACION_MASIVA$rango, MUTACION_MASIVA$pasos))
cat(sprintf("Tope por celda: %g s\n\n", TOPE))

filas <- list()
for (tp in names(PEDIGREE_TYPES)) {
  fam <- simulate_mp_families(1, freqs, types = PEDIGREE_TYPES[tp], seed = 11,
                              mut = MUTACION_MASIVA)[[1]]
  for (mo in MASIVA_ENGINES) {
    t0  <- Sys.time()
    r   <- con_tope(family_lr_table(fam, freqs, engine = mo), TOPE)
    seg <- as.numeric(difftime(Sys.time(), t0, units = "secs"))
    est <- if (inherits(r, "tope")) "TOPE"
           else if (inherits(r, "error")) "ERROR" else "ok"
    filas[[length(filas) + 1]] <- data.frame(
      tipo = tp, grado = PEDIGREE_TYPES[[tp]]$grado, motor = mo,
      segundos = round(seg, 2), estado = est, stringsAsFactors = FALSE)
    cat(sprintf("  %-20s %-9s %8.2f s  %s\n", tp, mo, seg, est))
    if (est == "ERROR") cat(sprintf("      %s\n", conditionMessage(r)))
  }
}

d <- do.call(rbind, filas)
write.csv(d, file.path(LRVAL_DIRS$masiva, "costo_mutacion.csv"), row.names = FALSE)

cat("\n== Costo por grado (motor primario) ==\n")
pri <- d[d$motor == MASIVA_ENGINES[1], ]
print(aggregate(segundos ~ grado, pri, function(x) c(n = length(x), total = sum(x))),
      row.names = FALSE)
## 200 familias repartidas entre los 20 tipos dan 10 por tipo en esperanza:
## simulate_mp_families() sortea el tipo con probabilidad uniforme. De ahi el
## factor 10 sobre la suma de una familia por tipo. Es tiempo de una sola
## tarea; mass_search() reparte las familias con mclapply() sobre NUCLEOS.
cat(sprintf("\nExtrapolacion a 200 familias (asignacion uniforme, %s): %.1f h\n",
            MASIVA_ENGINES[1], sum(pri$segundos) * 10 / 3600))
n_corte <- sum(pri$estado != "ok")
if (n_corte > 0)
  cat(sprintf("  cota INFERIOR: %d de %d celdas del motor primario no terminaron\n",
              n_corte, nrow(pri)))
cat("\nResultados en out/masiva/costo_mutacion.csv\n\n")

## ---------------------------------------------------------------------------
## lrval :: el informe en LaTeX
##
## Renderiza el documento de `documento_informe()` (R/informe.R) a un .tex
## completo y lo compila a PDF con pdflatex. El contenido es el de INFORME.md,
## mismas secciones, tablas, cifras y prosa; aca solo se decide como se ve.
##
## Las estrategias: escape con marcadores, booktabs + longtable con
## encabezado repetido, fancyhdr, hyperref sin cajas, gris para lo que falta,
## columnas p{} para la prosa, el color por veredicto, el bloque literal con
## corte de linea y el tamano de letra o el apaisado automaticos segun el
## ancho de cada tabla.
##
## pdflatex con inputenc/T1 alcanza y no hace falta ninguna fuente del
## sistema.
## ---------------------------------------------------------------------------

#' Escapa los caracteres especiales de LaTeX en texto plano
#'
#' Primero marcadores para la barra, el circunflejo y la tilde, para no volver
#' a escapar las llaves que introducen sus propios comandos.
#' @export
tex_escapar <- function(x) {
  x <- as.character(x); x[is.na(x)] <- ""
  x <- gsub("\\", "<<BSL>>", x, fixed = TRUE)
  x <- gsub("^", "<<CIR>>", x, fixed = TRUE)
  x <- gsub("~", "<<TIL>>", x, fixed = TRUE)
  for (ch in c("&", "%", "$", "#", "_", "{", "}"))
    x <- gsub(ch, paste0("\\", ch), x, fixed = TRUE)
  x <- gsub("<<BSL>>", "\\textbackslash{}", x, fixed = TRUE)
  x <- gsub("<<CIR>>", "\\textasciicircum{}", x, fixed = TRUE)
  x <- gsub("<<TIL>>", "\\textasciitilde{}", x, fixed = TRUE)
  x
}

#' Prosa del documento a LaTeX: escape, **negrita**, `codigo` y flechas
#'
#' Los tramos entre acentos graves van en monoespaciado y pueden cortar linea
#' en las barras: una ruta larga no se parte sola y se sale del margen.
#' @export
tex_prosa <- function(x) vapply(x, function(s) {
  partes <- strsplit(s, "`", fixed = TRUE)[[1]]
  if (!length(partes)) return("")
  codigo <- seq_along(partes) %% 2L == 0L
  partes[codigo] <- sprintf("\\texttt{%s}", gsub("/", "/\\\\allowbreak{}", tex_escapar(partes[codigo])))
  p <- tex_escapar(partes[!codigo])
  p <- gsub("\\*\\*(.+?)\\*\\*", "\\\\textbf{\\1}", p, perl = TRUE)
  p <- gsub("->", "$\\\\rightarrow$", p)
  p <- gsub("<-", "$\\\\leftarrow$", p)
  p <- gsub("<=", "$\\\\le$", p)
  partes[!codigo] <- p
  paste(partes, collapse = "")
}, "", USE.NAMES = FALSE)

## Los veredictos y los estados de la cadena, en color y a maquina: el ojo
## encuentra una FALLA en una tabla larga sin leerla entera.
tex_colores <- c(PASA = "pasa", cierra = "pasa", PISO_OK = "pasa",
                 FALLA = "falla", ROTO = "falla",
                 REVISAR = "revisar", CONVENCION = "revisar",
                 SIN_DATO = "sindato")
tex_celda <- function(x) {
  e <- tex_escapar(x)
  con_color <- x %in% names(tex_colores)
  e[con_color] <- sprintf("\\textcolor{%s}{\\texttt{%s}}", tex_colores[x[con_color]], e[con_color])
  e
}

## --- disposicion de una tabla ---------------------------------------------------
##
## Ancho de linea en puntos: A4 con margen de 2,2 cm. Cada columna se estima
## por su celda mas larga (0,52 em por caracter) y por su encabezado, que va
## en negrita y es mas ancho (0,62 em). Se prueban en orden small,
## footnotesize y scriptsize, cada uno primero con el encabezado entero y
## despues partido en dos lineas, en vertical y recien despues en apaisado
## (donde small no se prueba).
## Las columnas de prosa (texto de mas de 40 caracteres) van en p{} y se
## reparten lo que sobra, proporcional a su largo; la disposicion cierra
## cuando cada una recibe al menos 3 cm.
TEX_LINEA <- c(vertical = 472, apaisado = 717)
TEX_PT <- c(small = 9, footnotesize = 8, scriptsize = 7)
TEX_SEP <- 3   # \tabcolsep en puntos

## Un encabezado largo se parte en dos por el `_` o el espacio mas cercano al
## medio; el separador queda en la primera linea para que se lea como uno.
tex_partir <- function(et) lapply(et, function(e) {
  if (nchar(e) <= 9) return(e)
  cortes <- gregexpr("[_ ]", e)[[1]]
  if (cortes[1] < 0) return(e)
  k <- cortes[which.min(abs(cortes - nchar(e) / 2))]
  c(trimws(substr(e, 1, k)), trimws(substring(e, k + 1)))
})

tex_medidas <- function(n) {
  celdas <- lapply(n$cols, function(k) celda_texto(n$d[[k]], n$digitos))
  list(cuerpo = vapply(celdas, function(x) max(nchar(x)), 0),
       encabezado = nchar(n$etiquetas),
       encabezado_partido = vapply(tex_partir(n$etiquetas), function(x) max(nchar(x)), 0),
       numerica = vapply(n$cols, function(k) is.numeric(n$d[[k]]), TRUE))
}

tex_disposicion <- function(n) {
  if (is.null(n$d) || !nrow(n$d)) return(NULL)
  m <- tex_medidas(n)
  prosa <- !m$numerica & m$cuerpo > 40
  nc <- length(n$cols)
  intentos <- expand.grid(partir = c(FALSE, TRUE), tam = names(TEX_PT), apaisado = c(FALSE, TRUE),
                          stringsAsFactors = FALSE)
  for (i in seq_len(nrow(intentos))) {
    tam <- intentos$tam[i]; ap <- intentos$apaisado[i]; partir <- intentos$partir[i]
    ## una pagina apaisada es cara: ahi se arranca de footnotesize, que entra
    ## el doble de filas y evita una tercera pagina con dos renglones sueltos
    if (ap && tam == "small") next
    pt <- TEX_PT[[tam]]
    enc <- if (partir) m$encabezado_partido else m$encabezado
    col <- pmax(m$cuerpo * 0.52, enc * 0.62) * pt
    fijo <- sum(col[!prosa]) + 2 * TEX_SEP * nc
    libre <- TEX_LINEA[[if (ap) "apaisado" else "vertical"]] - 8 - fijo
    if (!any(prosa)) {
      if (libre >= 0) return(list(tam = tam, apaisado = ap, partir = partir, anchos = NULL))
      next
    }
    peso <- pmin(m$cuerpo[prosa], 80)
    reparto <- pmin(libre * peso / sum(peso), m$cuerpo[prosa] * 0.52 * pt + 6)
    if (all(reparto >= 85)) return(list(tam = tam, apaisado = ap, partir = partir, anchos = reparto))
  }
  ## nada cierra: apaisado, letra chica, encabezado partido, reparto en partes iguales
  pt <- TEX_PT[["scriptsize"]]
  col <- pmax(m$cuerpo * 0.52, m$encabezado_partido * 0.62) * pt
  libre <- max(TEX_LINEA[["apaisado"]] - 8 - sum(col[!prosa]) - 2 * TEX_SEP * nc, 60 * sum(prosa))
  list(tam = "scriptsize", apaisado = TRUE, partir = TRUE,
       anchos = if (any(prosa)) rep(libre / sum(prosa), sum(prosa)))
}

## Una longtable de booktabs con el encabezado repetido en cada pagina,
## dentro de un grupo que fija el tamano de letra y el tabcolsep. El apaisado lo abre quien llama,
## para poder meter adentro el titulo que la precede.
tex_tabla <- function(n, disp) {
  d <- n$d
  if (is.null(d) || !nrow(d)) return("\\textcolor{gris}{(sin filas)}")
  m <- tex_medidas(n)
  prosa <- !m$numerica & m$cuerpo > 40
  al <- ifelse(m$numerica, "r", "l")
  if (any(prosa)) al[prosa] <- sprintf("p{%.0fpt}", disp$anchos)
  et <- if (disp$partir) tex_partir(n$etiquetas) else as.list(n$etiquetas)
  enc <- mapply(function(partes, alin) {
    partes <- tex_escapar(partes)
    if (length(partes) == 1L) sprintf("\\textbf{%s}", partes)
    else sprintf("\\textbf{\\makecell[%s]{%s}}", alin, paste(partes, collapse = "\\\\"))
  }, et, ifelse(m$numerica, "r", "l"))
  enc <- paste(paste(enc, collapse = " & "), "\\\\")
  celdas <- lapply(n$cols, function(k) tex_celda(celda_texto(d[[k]], n$digitos)))
  filas <- paste(do.call(paste, c(celdas, sep = " & ")), "\\\\")
  c("\\begingroup", sprintf("\\%s", disp$tam), sprintf("\\setlength{\\tabcolsep}{%dpt}", TEX_SEP),
    sprintf("\\begin{longtable}{@{}%s@{}}", paste(al, collapse = "")),
    "\\toprule", enc, "\\midrule", "\\endfirsthead",
    sprintf("\\multicolumn{%d}{@{}l}{\\itshape (continuación)}\\\\", length(n$cols)),
    "\\toprule", enc, "\\midrule", "\\endhead",
    "\\bottomrule", "\\endlastfoot",
    filas, "\\end{longtable}", "\\endgroup")
}

tex_preambulo <- function(generado) c(
  "% Generado por lrval (R/informe_tex.R). Compilar con: pdflatex INFORME.tex (dos veces)",
  "\\documentclass[11pt,a4paper]{article}",
  "\\usepackage[utf8]{inputenc}",
  "\\usepackage[T1]{fontenc}",
  "\\usepackage{lmodern}",
  "\\usepackage[spanish,es-noshorthands]{babel}",
  "\\usepackage[margin=2.2cm]{geometry}",
  "\\usepackage{microtype}",
  "\\usepackage{booktabs}",
  "\\usepackage{longtable}",
  "\\usepackage{array}",
  "\\usepackage{makecell}",
  "\\usepackage{pdflscape}",
  "\\usepackage{xcolor}",
  "\\usepackage{enumitem}",
  "\\usepackage{fancyhdr}",
  "\\usepackage{fvextra}",
  "\\usepackage[hidelinks]{hyperref}",
  "",
  "\\definecolor{gris}{gray}{0.45}",
  "\\definecolor{pasa}{RGB}{27,120,55}",
  "\\definecolor{falla}{RGB}{178,24,43}",
  "\\definecolor{revisar}{RGB}{191,105,0}",
  "\\definecolor{sindato}{gray}{0.5}",
  "\\setlist{itemsep=2pt,parsep=0pt,topsep=3pt}",
  "\\setcounter{secnumdepth}{2}",
  "\\setcounter{tocdepth}{2}",
  "\\setlength{\\parindent}{0pt}",
  "\\setlength{\\parskip}{5pt plus 1pt}",
  "\\setlength{\\LTpre}{6pt}\\setlength{\\LTpost}{8pt}\\setlength{\\LTleft}{0pt}",
  "\\fvset{fontsize=\\scriptsize,breaklines=true,breakindent=2em,breaksymbolleft={},frame=single,rulecolor=\\color{gris},framesep=2mm}",
  "",
  "\\pagestyle{fancy}",
  "\\fancyhf{}",
  "\\lhead{\\small Informe de la validación forense de GENis}",
  sprintf("\\rhead{\\small commit %s}", tex_escapar(generado$commit)),
  "\\cfoot{\\small\\thepage}",
  "\\renewcommand{\\headrulewidth}{0.4pt}",
  "",
  "\\begin{document}",
  "\\thispagestyle{fancy}")

#' Renderiza el documento a un .tex completo
#'
#' El titulo de nivel 1 es el del documento; el indice va antes de la primera
#' seccion; los niveles 2, 3 y 4 son section, subsection y subsubsection. Una
#' tabla apaisada arrastra al apaisado el titulo que la precede.
#' @export
render_tex <- function(doc, generado = list(fecha = "", commit = "")) {
  disp <- lapply(doc, function(n) if (n$tipo == "tabla") tex_disposicion(n) else NULL)
  apaisada <- vapply(disp, function(x) isTRUE(x$apaisado), TRUE)
  out <- tex_preambulo(generado)
  indice_puesto <- FALSE
  for (i in seq_along(doc)) {
    n <- doc[[i]]
    if (n$tipo == "h" && n$nivel == 1) {
      out <- c(out, sprintf("{\\LARGE\\bfseries %s\\par}", tex_prosa(n$texto)), "\\vspace{1.2em}")
      next
    }
    if (n$tipo == "h" && n$nivel == 2 && !indice_puesto) {
      out <- c(out, "\\tableofcontents", ""); indice_puesto <- TRUE
    }
    ## el titulo entra al apaisado junto con la tabla que le sigue
    if (n$tipo == "h" && i < length(doc) && apaisada[i + 1]) out <- c(out, "\\begin{landscape}")
    if (n$tipo == "tabla" && apaisada[i] && !(i > 1 && doc[[i - 1]]$tipo == "h")) out <- c(out, "\\begin{landscape}")
    out <- c(out, switch(n$tipo,
      h = sprintf("\\%s{%s}", c("", "section", "subsection", "subsubsection")[n$nivel], tex_prosa(n$texto)),
      p = c(tex_prosa(n$texto), ""),
      lista = c("\\begin{itemize}", paste("  \\item", tex_prosa(n$items)), "\\end{itemize}", ""),
      tabla = tex_tabla(n, disp[[i]]),
      literal = c("\\begin{Verbatim}", n$lineas, "\\end{Verbatim}", "")))
    if (n$tipo == "tabla" && apaisada[i]) out <- c(out, "\\end{landscape}")
  }
  c(out, "\\end{document}")
}

#' Compila un .tex con pdflatex, en su directorio, las pasadas que haga falta
#'
#' Devuelve `pdf` (la ruta, o NULL) y `motivo`. Sin pdflatex no es un error:
#' queda el .tex y se dice. Si falla, queda el .log al lado para leerlo.
#' @export
compilar_pdf <- function(tex) {
  pdflatex <- Sys.which("pdflatex")
  if (!nzchar(pdflatex))
    return(list(pdf = NULL, motivo = "pdflatex no está instalado; queda INFORME.tex para compilarlo en otra máquina"))
  dir <- dirname(tex); base <- sub("[.]tex$", "", basename(tex))
  viejo <- setwd(dir); on.exit(setwd(viejo))
  aux <- paste0(base, c(".aux", ".toc", ".out", ".log"))
  pdf <- paste0(base, ".pdf")
  unlink(c(pdf, aux))
  for (pasada in 1:4) {
    st <- system2(pdflatex, c("-interaction=batchmode", "-halt-on-error", basename(tex)),
                  stdout = FALSE, stderr = FALSE)
    if (st != 0) {
      unlink(setdiff(aux, paste0(base, ".log")))
      return(list(pdf = NULL, motivo = sprintf("pdflatex terminó con código %d; ver %s", st, file.path(dir, paste0(base, ".log")))))
    }
    log <- readLines(paste0(base, ".log"), warn = FALSE)
    ## el indice y las columnas de longtable tardan una pasada mas en asentarse
    if (!any(grepl("Rerun to get|Table widths have changed|Rerun LaTeX", log))) break
  }
  if (!file.exists(pdf)) return(list(pdf = NULL, motivo = "pdflatex no dejó el PDF"))
  unlink(aux)
  list(pdf = file.path(dir, pdf), motivo = "")
}

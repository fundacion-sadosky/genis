## ---------------------------------------------------------------------------
## lrval :: valores de referencia (gold standard) en forma cerrada
##
## Transcripcion de expresiones algebraicas publicadas, evaluadas en las
## frecuencias del caso. Es la unica pieza independiente de todo el software
## bajo validacion, y por eso el patron primario. El acuerdo entre dos
## programas no es evidencia de correccion; el acuerdo con estas expresiones
## si lo es.
##
## Fuentes
##   [EW]  Evett & Weir (1998) "Interpreting DNA Evidence", cap. 6-8.
##         Formulas de indices de parentesco por coeficientes kappa.
##   [SL]  Slooten K. (2011) "Validation of DNA-based identification software
##         by computation of pedigree likelihood ratios". Forensic Sci Int
##         Genet 5:308-315.  Ecuaciones (4.2)-(4.9) y apendice A.
## ---------------------------------------------------------------------------

## --- bloques basicos (sin mutacion) -----------------------------------------

#' Probabilidad de un genotipo bajo equilibrio de Hardy-Weinberg
#' @param g genotipo, vector de dos alelos
#' @param p vector nombrado de frecuencias
#' @export
hw <- function(g, p) {
  if (g[1] == g[2]) p[[g[1]]]^2 else 2 * p[[g[1]]] * p[[g[2]]]
}

#' Probabilidad de transmitir el alelo `x` desde el genotipo `G` (sin mutacion)
#' @export
transmit <- function(x, G) sum(G == x) / 2

#' Probabilidad de G2 dado que comparte exactamente un alelo IBD con G1 [EW]
#' @export
p_one_ibd <- function(G2, G1, p) {
  a <- G1[1]; b <- G1[2]
  c <- G2[1]; d <- G2[2]
  het2 <- c != d
  if (a == b) {
    ## G1 homocigoto a/a: el alelo IBD es necesariamente `a`
    if (!het2) return(if (c == a) p[[a]] else 0)
    if (c == a) return(p[[d]])
    if (d == a) return(p[[c]])
    return(0)
  }
  ## G1 heterocigoto a/b: el alelo IBD es a o b, cada uno con probabilidad 1/2
  cont <- function(ibd) {
    if (!het2) return(if (c == ibd) p[[ibd]] else 0)
    if (c == ibd) return(p[[d]])
    if (d == ibd) return(p[[c]])
    0
  }
  0.5 * cont(a) + 0.5 * cont(b)
}

#' LR de un par de individuos con coeficientes IBD kappa (sin mutacion) [EW]
#'
#' LR = P(G2|G1)/P(G2), con
#' P(G2|G1) = k0 P(G2) + k1 P(G2 | 1 alelo IBD) + k2 1{G1 = G2}
#'
#' @param G1,G2 genotipos (vectores de dos alelos)
#' @param kappa vector c(k0,k1,k2)
#' @param p frecuencias
#' @export
gold_pairwise <- function(G1, G2, kappa, p) {
  num <- kappa[1] * hw(G2, p) +
         kappa[2] * p_one_ibd(G2, G1, p) +
         kappa[3] * as.numeric(setequal(G1, G2) && length(unique(G1)) == length(unique(G2)))
  num / hw(G2, p)
}

#' Coeficientes kappa de las relaciones estandar
#' @export
KAPPA <- list(
  identicos      = c(0,    1,   0)[c(1,3,2)][c(1,3,2)],  # placeholder, ver abajo
  padre_hijo     = c(0,    1,    0),
  hermanos       = c(1/4,  1/2,  1/4),
  medio_hermanos = c(1/2,  1/2,  0),
  abuelo_nieto   = c(1/2,  1/2,  0),
  tio_sobrino    = c(1/2,  1/2,  0),
  primos         = c(3/4,  1/4,  0),
  bisabuelo      = c(3/4,  1/4,  0),
  no_emparentados= c(1,    0,    0)
)
KAPPA$identicos <- c(0, 0, 1)

#' LR de paternidad con madre tipificada (trio) [EW]
#'
#' LR = P(C | M, F) / P(C | M)
#' @param C,M,F genotipos de hijo, madre y presunto padre
#' @export
gold_trio <- function(C, M, F, p) {
  num <- if (C[1] == C[2]) transmit(C[1], M) * transmit(C[1], F)
         else transmit(C[1], M) * transmit(C[2], F) +
              transmit(C[2], M) * transmit(C[1], F)
  den <- if (C[1] == C[2]) transmit(C[1], M) * p[[C[1]]]
         else transmit(C[1], M) * p[[C[2]]] + transmit(C[2], M) * p[[C[1]]]
  num / den
}

#' LR de paternidad sin madre (duo) [EW]
#'
#' LR = P(C | F) / P(C)
#' @export
gold_duo <- function(C, F, p) {
  num <- if (C[1] == C[2]) transmit(C[1], F) * p[[C[1]]]
         else transmit(C[1], F) * p[[C[2]]] + transmit(C[2], F) * p[[C[1]]]
  num / hw(C, p)
}

#' LR de hijo desaparecido con AMBOS padres tipificados
#'
#' Ojo: no es el indice de paternidad. Aqui la hipotesis alternativa es que
#' el individuo no identificado no pertenece a la familia, de modo que los
#' genotipos de ambos padres estan en el numerador y en el denominador:
#'
#'   LR = P(C | F, M) / P(C)
#'
#' mientras que en el trio de paternidad (`gold_trio`) el genotipo de la
#' madre condiciona tambien el denominador. Confundir ambas expresiones es
#' un error frecuente al armar sets de validacion.
#' @export
gold_child_of_couple <- function(C, F, M, p) {
  num <- if (C[1] == C[2]) transmit(C[1], F) * transmit(C[1], M)
         else transmit(C[1], F) * transmit(C[2], M) +
              transmit(C[2], F) * transmit(C[1], M)
  num / hw(C, p)
}

## --- algebra de mutacion (para las formulas de Slooten) ---------------------

#' Vector q: probabilidad de que un alelo fundador no observado se transmita
#' como alelo a.  q = M^t p   [SL sec. 3.1]
#' @export
q_vec <- function(M, p) as.vector(t(M) %*% p[rownames(M)])

#' Matriz Q: alelo fundador no observado transmitido como `a` a un hijo y
#' como `b` a otro.  Q = M^t diag(p) M   [SL ec. 3.3]
#' @export
Q_mat <- function(M, p) t(M) %*% diag(p[rownames(M)]) %*% M

#' T3: alelo no observado transmitido como a1, a2, a3 a tres hijos [SL A.3]
#' @export
T3 <- function(M, p, a1, a2, a3) sum(M[, a1] * M[, a2] * M[, a3] * p[rownames(M)])

## --- casos de Slooten (2011) ------------------------------------------------

#' [SL Ejemplo 4.1] Trio clasico sin mutacion, todos (a,a): LR = 1/p_a
#' @export
sl_ex41 <- function(pa) 1 / pa

#' [SL Ejemplo 4.2] Trio con mutacion madre-hijo, modelo uniforme
#'
#' LR(m) = ((1-m) + m/(k-1)) / (2 p_b)
#' con limite 1/(2 p_b) cuando m -> 0.
#' @param m tasa de mutacion; @param k numero de alelos; @param pb frecuencia
#' @export
sl_ex42 <- function(m, k, pb) ((1 - m) + m / (k - 1)) / (2 * pb)

#' [SL sec. 4.1.2] Trio con alelos F (dropout), modelo uniforme
#'
#' GC=(b,F), GM=(a,F), GUI=(b,F), padre faltante
#' LR = (2 q_b + 1 - m + m/(k-1)) / (3 q_b + m/(k-1))
#' Para m = 0 se reduce a (1 + 2 p_b)/(3 p_b).
#' @export
sl_falelo <- function(qb, m = 0, k = NA) {
  if (m == 0) return((1 + 2 * qb) / (3 * qb))
  (2 * qb + 1 - m + m / (k - 1)) / (3 * qb + m / (k - 1))
}

#' [SL ec. 4.3] Tres generaciones, pedigri COMPLETO (con padres no tipificados)
#'
#' F=(a,b) progenitor del desaparecido, UI=(c,d), C=(e,f) hijo del desaparecido.
#' @param M matriz de mutacion (dimnames = alelos); @param p frecuencias
#' @export
sl_3gen_full <- function(a, b, c, d, e, f, M, p) {
  q  <- setNames(q_vec(M, p), rownames(M))
  M2 <- M %*% M
  Mtq <- setNames(as.vector(t(M) %*% q[rownames(M)]), rownames(M))
  num <- (1/16) * ((M[a,c] + M[b,c]) * q[[d]] + (M[a,d] + M[b,d]) * q[[c]]) *
                  ((M[c,e] + M[d,e]) * q[[f]] + (M[c,f] + M[d,f]) * q[[e]])
  den <- p[[c]] * p[[d]] *
         ((1/4) * (q[[e]] * Mtq[[f]] + Mtq[[e]] * q[[f]]) +
          (1/8) * ((M2[a,e] + M2[b,e]) * q[[f]] + (M2[a,f] + M2[b,f]) * q[[e]]))
  num / den
}

#' [SL ec. 4.4] Tres generaciones, pedigri PODADO
#'
#' Mismo caso que `sl_3gen_full` pero sin los progenitores no tipificados.
#' Sin mutacion ambos coinciden; CON mutacion difieren, y esa diferencia es
#' precisamente lo que distingue a un programa que poda de uno que no.
#' @export
sl_3gen_pruned <- function(a, b, c, d, e, f, M, p) {
  M2 <- M %*% M
  num <- (1/16) * ((M[a,c] + M[b,c]) * p[[d]] + (M[a,d] + M[b,d]) * p[[c]]) *
                  ((M[c,e] + M[d,e]) * p[[f]] + (M[c,f] + M[d,f]) * p[[e]])
  den <- p[[c]] * p[[d]] *
         ((1/2) * p[[e]] * p[[f]] +
          (1/8) * ((M2[a,e] + M2[b,e]) * p[[f]] + (M2[a,f] + M2[b,f]) * p[[e]]))
  num / den
}

#' [SL ec. 4.2] Caso incestuoso: el padre del desaparecido es tambien el
#' padre de la madre.  La madre D es el unico pariente tipificado.
#'
#' P(GUI=(u1,u2) | GM=(m1,m2)) =
#'   (1/16) sum_i sum_j M[m_i, u_jbar] (2 q_uj + Q[m_i,u_j]/q_mi + Q[m_ibar,u_j]/q_mibar)
#'
#' @param u genotipo del individuo no identificado; @param mg genotipo de la madre
#' @export
sl_incesto <- function(u, mg, M, p) {
  q <- setNames(q_vec(M, p), rownames(M))
  Q <- Q_mat(M, p); dimnames(Q) <- dimnames(M)
  s <- 0
  for (i in 1:2) for (j in 1:2) {
    ib <- 3 - i; jb <- 3 - j
    s <- s + M[mg[i], u[jb]] *
      (2 * q[[u[j]]] + Q[mg[i], u[j]] / q[[mg[i]]] + Q[mg[ib], u[j]] / q[[mg[ib]]])
  }
  (s / 16) / (p[[u[1]]] * p[[u[2]]])
}

#' [SL Ejemplo A.1] Tercer hermano faltante, sin mutacion
#'
#' S1=(x,y), S2=(x,z), UI=(y,z)  ->  LR = (px+py+pz) / (8 (1+2px) py pz)
#' @export
sl_3hermanos_A1 <- function(px, py, pz) (px + py + pz) / (8 * (1 + 2 * px) * py * pz)

#' [SL Ejemplo A.2] Tercer hermano con alelos F (dropout), sin mutacion
#'
#' S1=(6,8), S2=(7,F)=UI  ->  LR = (1 + p7) / (4 p7)
#' @export
sl_3hermanos_A2 <- function(p7) (1 + p7) / (4 * p7)

#' [SL Ejemplo A.3] Dos tias del mismo lado, sin mutacion
#'
#' S1=(x,y), S2=(x,y), UI=(x,z)
#' LR = (1 + 6px + py + 6 px py + 2 px^2) / (8 px (1 + px + py + 2 px py))
#' @export
sl_2tias_A3 <- function(px, py) {
  (1 + 6*px + py + 6*px*py + 2*px^2) / (8 * px * (1 + px + py + 2*px*py))
}

#' [SL ec. 4.7-4.9] Pedigri consanguineo complejo (sec. 4.6), sin mutacion
#'
#' Tres configuraciones alelicas de la Tabla 2 del articulo.
#' @export
sl_inbred_LR1 <- function(p3) 2*(1 + 9*p3 + 8*p3^2) / (p3 * (7 + 101*p3 + 160*p3^2))
#' @rdname sl_inbred_LR1
#' @export
sl_inbred_LR2 <- function(p3, p4) (3 + 4*p3) / (32 * p3 * p4)
#' @rdname sl_inbred_LR1
#' @export
sl_inbred_LR3 <- function(p2, p4) (1 + 15*p4 + 16*p4^2) / (12*p2*p4 + 128*p2*p4^2)

## --- duo con mutacion, en las dos convenciones de pedigri -------------------

#' LR de un duo padre-hijo CON modelo de mutacion
#'
#' El hijo recibe un alelo del padre tipificado (que puede mutar) y otro del
#' otro progenitor. La distribucion de ese segundo alelo depende de como se
#' represente el pedigri, y es justamente lo que distingue a los programas:
#'
#'   podado = FALSE  el otro progenitor es un fundador NO tipificado del
#'                   pedigri, de modo que su alelo atraviesa una meiosis y se
#'                   distribuye segun q = M^t p  (convencion de pedtools)
#'   podado = TRUE   el otro progenitor se elimina del pedigri y el alelo es
#'                   directamente un alelo fundador, con frecuencia p
#'                   (convencion de Familias, que poda automaticamente)
#'
#' Sin mutacion q = p y ambas coinciden. Es la version en dos individuos del
#' contraste de Slooten (2011) sec. 4.3 entre las ecuaciones (4.3) y (4.4).
#'
#' @param F genotipo del padre; @param C genotipo del hijo
#' @param M matriz de mutacion paterna (dimnames = alelos)
#' @param p frecuencias alelicas
#' @param podado logico, ver arriba
#' @export
gold_duo_mut <- function(F, C, M, p, podado = FALSE) {
  tr <- function(x) 0.5 * (M[F[1], x] + M[F[2], x])
  otro <- if (podado) p else setNames(q_vec(M, p), rownames(M))
  num <- if (C[1] == C[2]) tr(C[1]) * otro[[C[1]]]
         else tr(C[1]) * otro[[C[2]]] + tr(C[2]) * otro[[C[1]]]
  num / hw(C, p)
}

#' LR de un trio de paternidad CON modelo de mutacion
#'
#' Bajo H2 el padre es un individuo no tipificado. Si se lo representa como
#' fundador del pedigri (convencion de pedtools) su alelo atraviesa una
#' meiosis y se distribuye segun q = M^t p; si el programa lo poda, se usa
#' directamente p. Vease `gold_duo_mut`.
#'
#' @param C,M_,F genotipos de hijo, madre y presunto padre
#' @param Mm,Mf matrices de mutacion paterna y materna
#' @export
gold_trio_mut <- function(C, M_, F, Mm, Mf, p, podado = FALSE) {
  tF <- function(x) 0.5 * (Mm[F[1], x] + Mm[F[2], x])
  tM <- function(x) 0.5 * (Mf[M_[1], x] + Mf[M_[2], x])
  otro <- if (podado) p else setNames(q_vec(Mm, p), rownames(Mm))
  num <- if (C[1] == C[2]) tM(C[1]) * tF(C[1])
         else tM(C[1]) * tF(C[2]) + tM(C[2]) * tF(C[1])
  den <- if (C[1] == C[2]) tM(C[1]) * otro[[C[1]]]
         else tM(C[1]) * otro[[C[2]]] + tM(C[2]) * otro[[C[1]]]
  num / den
}

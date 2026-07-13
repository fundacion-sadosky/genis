package pedigree

import java.util.concurrent.atomic.AtomicReference
import javax.inject.Singleton

// Activar un pedigri (dispara matching por Spark) y calcular el LR de un
// escenario son ambas operaciones pesadas en memoria (ver
// docs/pedigree-matching-mendelian-exclusion-fix.md). Correr mas de una en
// simultaneo satura la heap de la JVM y puede tirar abajo la app entera.
// Este lock global asegura que a lo sumo una de estas operaciones este
// corriendo a la vez, sin importar el pedigri.
trait PedigreeProcessingLock {
  def tryAcquire(pedigreeId: Long): Boolean
  def release(pedigreeId: Long): Unit
  def runningPedigreeId: Option[Long]
}

@Singleton
class PedigreeProcessingLockImpl extends PedigreeProcessingLock {
  private val current = new AtomicReference[Option[Long]](None)

  override def tryAcquire(pedigreeId: Long): Boolean =
    current.compareAndSet(None, Some(pedigreeId))

  override def release(pedigreeId: Long): Unit = {
    var observed = current.get()
    while (observed.contains(pedigreeId) && !current.compareAndSet(observed, None)) {
      observed = current.get()
    }
  }

  override def runningPedigreeId: Option[Long] = current.get()
}
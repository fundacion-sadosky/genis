package util

import scala.collection.generic.CanBuildFrom
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

object FutureUtils {
  def swap[T](o: Option[Future[T]]): Future[Option[T]] =
    o.map(_.map(Some(_))).getOrElse(Future.successful(None))

  def seq[A, M[X] <: TraversableOnce[X]](in: M[() => Future[A]])(implicit cbf: CanBuildFrom[M[()=>Future[A]], A, M[A]], executor: ExecutionContext): Future[M[A]] = {
    in.foldLeft(Future.successful(cbf(in))) {
      (fr, ffa) => for (r <- fr; a <- ffa()) yield (r += a)
    } map (_.result())
  }
  def toFunction(fasd:Future[Unit]):() => Future[Unit] = {
    { () => fasd}
  }
}

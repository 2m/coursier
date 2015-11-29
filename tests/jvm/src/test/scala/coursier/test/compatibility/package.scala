package coursier.test

import coursier.Platform

import scala.concurrent.{ExecutionContext, Future}
import scalaz.concurrent.Task

package object compatibility {

  implicit val executionContext = scala.concurrent.ExecutionContext.global

  implicit class TaskExtensions[T](val underlying: Task[T]) extends AnyVal {
    def runF: Future[T] = Future.successful(underlying.run)
  }

  def textResource(path: String)(implicit ec: ExecutionContext): Future[String] = Future {
    def is = getClass
      .getClassLoader
      .getResource(path)
      .openStream()

    new String(Platform.readFullySync(is), "UTF-8")
  }

}

package scala.meta.jsonrpc

import monix.eval.Task
import scribe.LoggerSupport

object Services {
  def empty(logger: LoggerSupport): Services = new Services(Nil, logger)
}

final class Services private (
    val services: List[Service],
    logger: LoggerSupport
) {

  def request[A, B](endpoint: Endpoint[A, B])(f: A => B): Services =
    requestAsync[A, B](endpoint)(params => Task(Right(f(params))))

  def requestAsync[A, B](
      endpoint: Endpoint[A, B]
  )(f: A => Task[Either[Response.Error, B]]): Services =
    addService(
      Service.request[A, B](endpoint.method)(f)(
        endpoint.readwriterParams,
        endpoint.readwriterResult
      )
    )

  def notification[A](endpoint: Endpoint[A, Unit])(f: A => Unit): Services =
    notificationAsync[A](endpoint)(params => Task(f(params)))

  def notificationAsync[A](
      endpoint: Endpoint[A, Unit]
  )(f: A => Task[Unit]): Services =
    addService(
      Service.notification[A](endpoint.method, logger)(f)(
        endpoint.readwriterParams
      )
    )

  def byMethodName: Map[String, Service] =
    services.iterator.map(s => s.methodName -> s).toMap

  def addService(service: Service): Services = {
    val duplicate = services.find(_.methodName == service.methodName)
    require(
      duplicate.isEmpty,
      s"Duplicate service handler for method '${duplicate.get.methodName}'"
    )
    new Services(service :: services, logger)
  }

}

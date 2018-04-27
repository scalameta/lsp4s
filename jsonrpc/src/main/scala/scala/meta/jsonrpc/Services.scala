package scala.meta.jsonrpc

import monix.eval.Task
import scribe.LoggerSupport

object Services {
  def empty(logger: LoggerSupport): Services = new Services(Nil, logger)
}

final class Services private (
    val services: List[NamedJsonRpcService],
    logger: LoggerSupport
) {

  def request[A, B](endpoint: Endpoint[A, B])(f: A => B): Services =
    requestAsync[A, B](endpoint)(new Service[A, Either[Response.Error, B]] {
      def handle(request: A): Task[Either[Response.Error, B]] =
        Task(Right(f(request)))
    })

  def requestAsync[A, B](
      endpoint: Endpoint[A, B]
  )(f: Service[A, Either[Response.Error, B]]): Services =
    addService(
      Service.request[A, B](endpoint.method)(f)(
        endpoint.readwriterA,
        endpoint.readwriterB
      )
    )

  def notification[A](endpoint: Endpoint[A, Unit])(f: A => Unit): Services =
    notificationAsync[A](endpoint)(new Service[A, Unit] {
      def handle(request: A): Task[Unit] = Task(f(request))
    })

  def notificationAsync[A](
      endpoint: Endpoint[A, Unit]
  )(f: Service[A, Unit]): Services =
    addService(
      Service.notification[A](endpoint.method, logger)(f)(endpoint.readwriterA)
    )

  def byMethodName: Map[String, NamedJsonRpcService] =
    services.iterator.map(s => s.methodName -> s).toMap

  def addService(service: NamedJsonRpcService): Services = {
    val duplicate = services.find(_.methodName == service.methodName)
    require(
      duplicate.isEmpty,
      s"Duplicate service handler for method '${duplicate.get.methodName}'"
    )
    new Services(service :: services, logger)
  }

}

package endpoints.algebra.server

trait Server {

  val port: Int

  def start(): Unit

  def stop(): Unit

}

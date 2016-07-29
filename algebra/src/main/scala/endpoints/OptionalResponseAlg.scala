package endpoints

trait OptionalResponseAlg extends EndpointsAlg {

  /** Turns a `Response[A]` into a `Response[Option[A]]` */
  def option[A](response: Response[A]): Response[Option[A]]

}

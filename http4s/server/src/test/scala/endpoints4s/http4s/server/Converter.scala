//This is from https://github.com/krasserm/streamz/blob/master/streamz-converter/src/main/scala/streamz/converter/Converter.scala sadly the artifact was originally on bintray and has not been rehosted as of 03.08.21

/*
 * Copyright 2014 - 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package endpoints4s.http4s.server

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

import org.apache.pekko.stream._
import org.apache.pekko.stream.scaladsl.{Flow => PekkoFlow, Sink => PekkoSink, Source => PekkoSource, _}
import org.apache.pekko.{Done, NotUsed}
import cats.effect._
import cats.syntax.all._
import fs2._
import scala.annotation.implicitNotFound
import cats.effect.unsafe.implicits.global
import cats.effect.kernel.Resource.ExitCase.Canceled
import cats.effect.kernel.Resource.ExitCase.Errored
import cats.effect.kernel.Resource.ExitCase.Succeeded

trait Converter {

  /** Converts an Pekko Stream [[Graph]] of [[SourceShape]] to an FS2 [[Stream]].
    * If the materialized value needs be obtained, use [[pekkoSourceToFs2StreamMat]].
    */
  def pekkoSourceToFs2Stream[A](
      source: Graph[SourceShape[A], NotUsed]
  )(implicit materializer: Materializer): Stream[IO, A] =
    Stream.force {
      Async[IO].delay {
        val subscriber = PekkoSource.fromGraph(source).toMat(PekkoSink.queue[A]())(Keep.right).run()
        subscriberStream[A](subscriber)
      }
    }

  /** Converts an Pekko Stream [[Graph]] of [[SourceShape]] to an FS2 [[Stream]]. This method returns the FS2 [[Stream]]
    * and the materialized value of the [[Graph]].
    */
  def pekkoSourceToFs2StreamMat[A, M](
      source: Graph[SourceShape[A], M]
  )(implicit materializer: Materializer): IO[(Stream[IO, A], M)] =
    Async[IO].delay {
      val (mat, subscriber) =
        PekkoSource.fromGraph(source).toMat(PekkoSink.queue[A]())(Keep.both).run()
      (subscriberStream[A](subscriber), mat)
    }

  /** Converts an Pekko Stream [[Graph]] of [[SinkShape]] to an FS2 [[Pipe]].
    * If the materialized value needs be obtained, use [[pekkoSinkToFs2PipeMat]].
    */
  def pekkoSinkToFs2Pipe[A](
      sink: Graph[SinkShape[A], NotUsed]
  )(implicit materializer: Materializer): Pipe[IO, A, Unit] =
    (s: Stream[IO, A]) =>
      Stream.force {
        Async[IO].delay {
          val publisher =
            PekkoSource.queue[A](0, OverflowStrategy.backpressure).toMat(sink)(Keep.left).run()
          publisherStream[A](publisher, s)
        }
      }

  /** Converts an Pekko Stream [[Graph]] of [[SinkShape]] to an FS2 [[Pipe]]. This method returns the FS2 [[Pipe]]
    * and the materialized value of the [[Graph]].
    */
  def pekkoSinkToFs2PipeMat[A, M](
      sink: Graph[SinkShape[A], M]
  )(implicit materializer: Materializer): IO[(Pipe[IO, A, Unit], M)] =
    Async[IO].delay {
      val (publisher, mat) =
        PekkoSource.queue[A](0, OverflowStrategy.backpressure).toMat(sink)(Keep.both).run()
      ((s: Stream[IO, A]) => publisherStream[A](publisher, s), mat)
    }

  /** Converts an pekko sink with a success-status-indicating Future[M]
    * materialized result into an fs2 Pipe which will fail if the Future fails.
    * The stream returned by this will emit the Future's value one time at the end,
    * then terminate.
    */
  def pekkoSinkToFs2PipeMat[A, M](
      pekkoSink: Graph[SinkShape[A], Future[M]]
  )(implicit ec: ExecutionContext, m: Materializer): IO[Pipe[IO, A, Either[Throwable, M]]] =
    for {
      promise <- Deferred[IO, Either[Throwable, M]]
      fs2Sink <- pekkoSinkToFs2PipeMat[A, Future[M]](pekkoSink).flatMap { case (stream, mat) =>
        // This callback tells the pekko materialized future to store its result status into the Promise
        val callback = Async[IO].delay(mat.onComplete {
          case Failure(ex)    => promise.complete(ex.asLeft).unsafeRunSync()
          case Success(value) => promise.complete(value.asRight).unsafeRunSync()
        })
        callback.map(_ => stream)
      }
    } yield { (in: Stream[IO, A]) =>
      {
        // Async wait on the promise to be completed
        val materializedResultStream = Stream.eval(promise.get)
        val fs2Stream: Stream[IO, Unit] = fs2Sink.apply(in)

        // Run the pekko sink for its effects and then run stream containing the effect of getting the Promise results
        fs2Stream.drain ++ materializedResultStream
      }
    }

  /** Converts an Pekko Stream [[Graph]] of [[FlowShape]] to an FS2 [[Pipe]].
    * If the materialized value needs be obtained, use [[pekkoSinkToFs2PipeMat]].
    */
  def pekkoFlowToFs2Pipe[A, B](
      flow: Graph[FlowShape[A, B], NotUsed]
  )(implicit materializer: Materializer): Pipe[IO, A, B] =
    (s: Stream[IO, A]) =>
      Stream.force {
        Async[IO].delay {
          val src = PekkoSource.queue[A](0, OverflowStrategy.backpressure)
          val snk = PekkoSink.queue[B]()
          val (publisher, subscriber) = src.viaMat(flow)(Keep.left).toMat(snk)(Keep.both).run()
          transformerStream[A, B](subscriber, publisher, s)
        }
      }

  /** Converts an Pekko Stream [[Graph]] of [[FlowShape]] to an FS2 [[Pipe]]. This method returns the FS2 [[Pipe]]
    * and the materialized value of the [[Graph]].
    */
  def pekkoFlowToFs2PipeMat[A, B, M](
      flow: Graph[FlowShape[A, B], M]
  )(implicit materializer: Materializer): IO[(Pipe[IO, A, B], M)] =
    Async[IO].delay {
      val src = PekkoSource.queue[A](0, OverflowStrategy.backpressure)
      val snk = PekkoSink.queue[B]()
      val ((publisher, mat), subscriber) = src.viaMat(flow)(Keep.both).toMat(snk)(Keep.both).run()
      ((s: Stream[IO, A]) => transformerStream[A, B](subscriber, publisher, s), mat)
    }

  /** Converts an FS2 [[Stream]] to an Pekko Stream [[Graph]] of [[SourceShape]]. The [[Stream]] is run when the
    * [[Graph]] is materialized.
    */
  def fs2StreamToPekkoSource[A](
      stream: Stream[IO, A]
  ): Graph[SourceShape[A], NotUsed] = {
    val source = PekkoSource.queue[A](0, OverflowStrategy.backpressure)
    // A sink that runs an FS2 publisherStream when consuming the publisher actor (= materialized value) of source
    val sink = PekkoSink.foreach[SourceQueueWithComplete[A]] { p =>
      // Fire and forget Future so it runs in the background
      import cats.effect.unsafe.implicits.global

       publisherStream[A](p, stream).compile.drain.unsafeToFuture()
      ()
    }

    PekkoSource
      .fromGraph(GraphDSL.createGraph(source) { implicit builder => source =>
        import GraphDSL.Implicits._
        builder.materializedValue ~> sink
        SourceShape(source.out)
      })
      .mapMaterializedValue(_ => NotUsed)
  }

  /** Converts an FS2 [[Pipe]] to an Pekko Stream [[Graph]] of [[SinkShape]]. The [[Sink]] is run when the
    * [[Graph]] is materialized.
    */
  def fs2PipeToPekkoSink[A](
      sink: Pipe[IO, A, Unit]
  ): Graph[SinkShape[A], Future[Done]] = {
    val sink1: PekkoSink[A, SinkQueueWithCancel[A]] = PekkoSink.queue[A]()
    // A sink that runs an FS2 subscriberStream when consuming the subscriber actor (= materialized value) of sink1.
    // The future returned from unsafeToFuture() completes when the subscriber stream completes and is made
    // available as materialized value of this sink.
    val sink2: PekkoSink[SinkQueueWithCancel[A], Future[Done]] = PekkoFlow[SinkQueueWithCancel[A]]
      .map(s =>
        subscriberStream[A](s).through(sink).compile.drain.as(Done: Done).unsafeToFuture()
      )
      .toMat(PekkoSink.head)(Keep.right)
      .mapMaterializedValue(ffd => ffd.flatten)
    // fromFuture dance above is because scala 2.11 lacks Future#flatten. `pure` instead of `delay`
    // because the future value is already strict by the time we get it.

    PekkoSink
      .fromGraph(GraphDSL.createGraph(sink1, sink2)(Keep.both) { implicit builder => (sink1, sink2) =>
        import GraphDSL.Implicits._
        builder.materializedValue ~> PekkoFlow[(SinkQueueWithCancel[A], _)].map(_._1) ~> sink2
        SinkShape(sink1.in)
      })
      .mapMaterializedValue(_._2)
  }

  /** Converts an FS2 [[Pipe]] to an Pekko Stream [[Graph]] of [[FlowShape]]. The [[Pipe]] is run when the
    * [[Graph]] is materialized.
    */
  def fs2PipeToPekkoFlow[A, B](
      pipe: Pipe[IO, A, B]
  ): Graph[FlowShape[A, B], NotUsed] = {
    val source = PekkoSource.queue[B](0, OverflowStrategy.backpressure)
    val sink1: PekkoSink[A, SinkQueueWithCancel[A]] = PekkoSink.queue[A]()
    // A sink that runs an FS2 transformerStream when consuming the publisher actor (= materialized value) of source
    // and the subscriber actor (= materialized value) of sink1
    val sink2 = PekkoSink.foreach[(SourceQueueWithComplete[B], SinkQueueWithCancel[A])] { ps =>
      // Fire and forget Future so it runs in the background
      transformerStream(ps._2, ps._1, pipe).compile.drain.unsafeToFuture()
      ()
    }

    PekkoFlow
      .fromGraph(GraphDSL.createGraph(source, sink1)(Keep.both) { implicit builder => (source, sink1) =>
        import GraphDSL.Implicits._
        builder.materializedValue ~> sink2
        FlowShape(sink1.in, source.out)
      })
      .mapMaterializedValue(_ => NotUsed)
  }

  private def subscriberStream[A](
      subscriber: SinkQueueWithCancel[A]
  ): Stream[IO, A] = {
    val pull = Async[IO].fromFuture(Async[IO].delay(subscriber.pull()))
    val cancel = Async[IO].delay(subscriber.cancel())
    Stream.repeatEval(pull).unNoneTerminate.onFinalize(cancel)
  }

  private def publisherStream[A](
      publisher: SourceQueueWithComplete[A],
      stream: Stream[IO, A]
  ): Stream[IO, Unit] = {
    def publish(a: A): IO[Option[Unit]] = Async[IO]
      .fromFuture(Async[IO].delay(publisher.offer(a)))
      .flatMap {
        case QueueOfferResult.Enqueued       => ().some.pure[IO]
        case QueueOfferResult.Failure(cause) => Async[IO].raiseError[Option[Unit]](cause)
        case QueueOfferResult.QueueClosed    => none[Unit].pure[IO]
        case QueueOfferResult.Dropped =>
          Concurrent[IO].raiseError[Option[Unit]](
            new IllegalStateException(
              "This should never happen because we use OverflowStrategy.backpressure"
            )
          )
      }
      .recover {
        // This handles a race condition between `interruptWhen` and `publish`.
        // There's no guarantee that, when the pekko sink is terminated, we will observe the
        // `interruptWhen` termination before calling publish one last time.
        // Such a call fails with StreamDetachedException
        case _: StreamDetachedException => none[Unit]
      }

    def watchCompletion: IO[Unit] =
      Async[IO].fromFuture(Concurrent[IO].delay(publisher.watchCompletion())).void
    def fail(e: Throwable): IO[Unit] = Concurrent[IO].delay(publisher.fail(e)) >> watchCompletion
    def complete: IO[Unit] = Concurrent[IO].delay(publisher.complete()) >> watchCompletion

    stream
      .interruptWhen(watchCompletion.attempt)
      .evalMap(publish)
      .unNoneTerminate
      .onFinalizeCase {
        case Canceled => complete
        case Errored(e) => fail(e)
        case Succeeded => complete
      }
  }

  private def transformerStream[A, B](
      subscriber: SinkQueueWithCancel[B],
      publisher: SourceQueueWithComplete[A],
      stream: Stream[IO, A]
  ): Stream[IO, B] =
    subscriberStream[B](subscriber).concurrently(publisherStream[A](publisher, stream))

  private def transformerStream[A, B](
      subscriber: SinkQueueWithCancel[A],
      publisher: SourceQueueWithComplete[B],
      pipe: Pipe[IO, A, B]
  ): Stream[IO, Unit] =
    subscriberStream[A](subscriber).through(pipe).through(s => publisherStream(publisher, s))
}

trait ConverterDsl extends Converter {

  implicit class PekkoSourceDsl[A, M](source: Graph[SourceShape[A], M]) {

    /** @see [[Converter#pekkoSourceToFs2Stream]] */
    def toStream(implicit
        materializer: Materializer,
        @implicitNotFound(
          "Cannot convert `Source[A, M]` to `Stream[F, A]` - `M` value would be discarded.\nIf that is intended, first convert the `Source` to `Source[A, NotUsed]`.\nIf `M` should not be discarded, then use `source.toStreamMat[F]` instead."
        ) ev: M <:< NotUsed
    ): Stream[IO, A] = {
      val _ =
        ev // to suppress 'never used' warning. The warning fires on 2.12 but not on 2.13, so I can't use `nowarn`
      pekkoSourceToFs2Stream(source.asInstanceOf[Graph[SourceShape[A], NotUsed]])
    }

    /** @see [[Converter#pekkoSourceToFs2StreamMat]] */
    def toStreamMat(implicit
        materializer: Materializer
    ): IO[(Stream[IO, A], M)] =
      pekkoSourceToFs2StreamMat(source)

    @deprecated(
      message =
        "Use `.toStream[F]` for M=NotUsed; use `.toStreamMat[F]` for other M. This version relies on side effects.",
      since = "0.11"
    )
    def toStream(onMaterialization: M => Unit = _ => ())(implicit
        materializer: Materializer
    ): Stream[IO, A] =
      Stream.force(pekkoSourceToFs2StreamMat(source).map { case (pekkoStream, mat) =>
        onMaterialization(mat)
        pekkoStream
      })
  }

  implicit class PekkoSinkFutureDsl[A, M](sink: Graph[SinkShape[A], Future[M]]) {

    /** @see [[Converter#pekkoSinkToFs2SinkMat]] */
    def toPipeMatWithResult(implicit
        ec: ExecutionContext,
        m: Materializer
    ): IO[Pipe[IO, A, Either[Throwable, M]]] =
      pekkoSinkToFs2PipeMat[A, M](sink)

  }

  implicit class PekkoSinkDsl[A, M](sink: Graph[SinkShape[A], M]) {

    /** @see [[Converter#pekkoSinkToFs2Sink]] */
    def toPipe(implicit
        materializer: Materializer,
        @implicitNotFound(
          "Cannot convert `Sink[A, M]` to `Pipe[F, A, Unit]` - `M` value would be discarded.\nIf that is intended, first convert the `Sink` to `Sink[A, NotUsed]`.\nIf `M` should not be discarded, then use `sink.toPipeMat[F]` instead."
        ) ev: M <:< NotUsed
    ): Pipe[IO, A, Unit] = {
      val _ =
        ev // to suppress 'never used' warning. The warning fires on 2.12 but not on 2.13, so I can't use `nowarn`
      pekkoSinkToFs2Pipe(sink.asInstanceOf[Graph[SinkShape[A], NotUsed]])
    }

    /** @see [[Converter#pekkoSinkToFs2SinkMat]] */
    def toPipeMat(implicit
        materializer: Materializer
    ): IO[(Pipe[IO, A, Unit], M)] =
      pekkoSinkToFs2PipeMat(sink)

    @deprecated(
      message =
        "Use `.toSink[F]` for M=NotUsed; use `.toSinkMat[F]` for other M. This version relies on side effects.",
      since = "0.11"
    )
    def toSink(
        onMaterialization: M => Unit
    )(implicit materializer: Materializer): Pipe[IO, A, Unit] =
      (s: Stream[IO, A]) =>
        Stream.force {
          pekkoSinkToFs2PipeMat(sink).map { case (fs2Sink, mat) =>
            onMaterialization(mat)
            s.through(fs2Sink)
          }
        }

  }

  implicit class PekkoFlowDsl[A, B, M](flow: Graph[FlowShape[A, B], M]) {

    /** @see [[Converter#pekkoFlowToFs2Pipe]] */
    def toPipe(implicit
        materializer: Materializer,
        @implicitNotFound(
          "Cannot convert `Flow[A, B, M]` to `Pipe[F, A, B]` - `M` value would be discarded.\nIf that is intended, first convert the `Flow` to `Flow[A, B, NotUsed]`.\nIf `M` should not be discarded, then use `flow.toPipeMat[F]` instead."
        ) ev: M <:< NotUsed
    ): Pipe[IO, A, B] = {
      val _ =
        ev // to suppress 'never used' warning. The warning fires on 2.12 but not on 2.13, so I can't use `nowarn`
      pekkoFlowToFs2Pipe(flow.asInstanceOf[Graph[FlowShape[A, B], NotUsed]])
    }

    /** @see [[Converter#pekkoFlowToFs2PipeMat]] */
    def toPipeMat(implicit
        materializer: Materializer
    ): IO[(Pipe[IO, A, B], M)] =
      pekkoFlowToFs2PipeMat(flow)

    @deprecated(
      message =
        "Use `.toPipe[F]` for M=NotUsed; use `.toPipeMat[F]` for other M. This version relies on side effects.",
      since = "0.11"
    )
    def toPipe(
        onMaterialization: M => Unit = _ => ()
    )(implicit materializer: Materializer): Pipe[IO, A, B] =
      (s: Stream[IO, A]) =>
        Stream.force {
          pekkoFlowToFs2PipeMat(flow).map { case (fs2Pipe, mat) =>
            onMaterialization(mat)
            s.through(fs2Pipe)
          }
        }
  }

  implicit class FS2StreamNothingDsl[A](stream: Stream[Nothing, A]) {

    /** @see [[Converter#fs2StreamToPekkoSource]] */
    @deprecated("Use `stream.covary[F].toSource` instead", "0.10")
    def toSource(implicit contextShift: Async[IO]): Graph[SourceShape[A], NotUsed] =
      fs2StreamToPekkoSource(stream: Stream[IO, A])
  }

  implicit class FS2StreamPureDsl[A](stream: Stream[Pure, A]) {

    /** @see [[Converter#fs2StreamToPekkoSource]] */
    @deprecated("Use `stream.covary[F].toSource` instead", "0.10")
    def toSource(implicit contextShift: Async[IO]): Graph[SourceShape[A], NotUsed] =
      fs2StreamToPekkoSource(stream: Stream[IO, A])
  }

  implicit class FS2StreamIODsl[A](stream: Stream[IO, A]) {

    /** @see [[Converter#fs2StreamToPekkoSource]] */
    def toSource: Graph[SourceShape[A], NotUsed] =
      fs2StreamToPekkoSource(stream)
  }

  implicit class FS2SinkIODsl[A](sink: Pipe[IO, A, Unit]) {

    /** @see [[Converter#fs2PipeToPekkoSink]] */
    def toSink: Graph[SinkShape[A], Future[Done]] =
      fs2PipeToPekkoSink(sink)
  }

  implicit class FS2PipeIODsl[A, B](pipe: Pipe[IO, A, B]) {

    /** @see [[Converter#fs2PipeToPekkoFlow]] */
    def toFlow: Graph[FlowShape[A, B], NotUsed] =
      fs2PipeToPekkoFlow(pipe)
  }
}

object ConverterSyntax extends ConverterDsl

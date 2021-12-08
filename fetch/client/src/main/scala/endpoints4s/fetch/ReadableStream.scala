package endpoints4s.fetch

import org.scalajs.dom.ReadableStreamReader
import org.scalajs.dom.WriteableStream

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

//FIXME copypasta from scalajs-dom https://github.com/scala-js/scala-js-dom/pull/628
//FIXME should be removed once scalajs-dom PR is released

/** defined at [[https://streams.spec.whatwg.org/#readable-stream ¶2.1. Readable Streams]] of whatwg Streams spec.
  *
  * @tparam T
  *   Type of the Chunks returned by the Stream. Can't make it coveriant, due to T
  */
@js.native
private[fetch] trait ReadableStream[+T] extends js.Object {

  /** The locked getter returns whether or not the readable stream is locked to a reader.
    *
    * throws scala.scalajs.js.TypeError if the stream is not readable
    */
  def locked: Boolean = js.native

  /** The cancel method cancels the stream, signaling a loss of interest in the stream by a consumer. The supplied
    * reason argument will be given to the underlying source, which may or may not use it.
    *
    * @param reason
    *   the reason
    * @return
    *   a Promise
    */
  def cancel(reason: js.UndefOr[Any] = js.native): js.Promise[Unit] = js.native

  /** See [[https://streams.spec.whatwg.org/#rs-get-reader ¶3.2.4.3. getReader()]] of whatwg streams spec. Also see the
    * example usage there.
    *
    * The getReader method creates a readable stream reader and locks the stream to the new reader. While the stream is
    * locked, no other reader can be acquired until this one is released. The returned reader provides the ability to
    * directly read individual chunks from the stream via the reader’s read method. This functionality is especially
    * useful for creating abstractions that desire the ability to consume a stream in its entirety. By getting a reader
    * for the stream, you can ensure nobodyA else can interleave reads with yours or cancel the stream, which would
    * interfere with your abstraction.
    *
    * Note that if a stream becomes closed or errored, any reader it is locked to is automatically released.
    *
    * throws scala.scalajs.js.TypeError if not a readable stream
    *
    * @return
    *   a new ReadableStreamReader
    */
  def getReader(): ReadableStreamReader[T] = js.native

  /** see [[https://streams.spec.whatwg.org/#rs-pipe-through §3.2.4.4. pipeThrough({ writable, readable }, options)]]
    *
    * The pipeThrough method provides a convenient, chainable way of piping this readable stream through a transform
    * stream (or any other { writable, readable } pair). It simply pipes the stream into the writable side of the
    * supplied pair, and returns the readable side for further use . Piping a stream will generally lock it for the
    * duration of the pipe, preventing any other consumer fromA acquiring a reader.
    *
    * This method is intentionally generic; it does not require that its this value be a ReadableStream object. It also
    * does not require that its writable argument be a WritableStream instance, or that its readable argument be a
    * ReadableStream instance.
    *
    * //todo: determine the type of options
    */
  def pipeThrough[U](
      pair: Any, // TODO js.Tuple2[WriteableStream[T], ReadableStream[U]]
      options: Any = js.native
  ): ReadableStream[U] = js.native

  /** See
    * [[https://streams.spec.whatwg.org/#rs-pipe-to ¶3.2.4.5. pipeTo(dest, { preventClose, preventAbort, preventCancel } = {})]]
    * of whatwg Streams spec.
    *
    * The pipeTo method pipes this readable stream to a given writable stream. The way in which the piping process
    * behaves under various error conditions can be customized with a number of passed options. It returns a promise
    * that fulfills when the piping process completes successfully, or rejects if any errors were encountered.
    *
    * Piping a stream will generally lock it for the duration of the pipe, preventing any other consumer from acquiring
    * a reader. This method is intentionally generic; it does not require that its this value be a ReadableStream
    * object.
    *
    * //todo: determine the type of options
    */
  def pipeTo(dest: WriteableStream[T], options: Any = js.native): Unit = js.native

  /** See [[https://streams.spec.whatwg.org/#rs-tee ¶3.2.4.6. tee()]] of whatwg streams spec.
    *
    * The tee method tees this readable stream, returning a two-element array containing the two resulting branches as
    * new ReadableStream instances.
    *
    * Teeing a stream will lock it, preventing any other consumer from acquiring a reader. To cancel the stream, cancel
    * both of the resulting branches; a composite cancellation reason will then be propagated to the stream’s underlying
    * source.
    *
    * Note that the chunks seen in each branch will be the same object. If the chunks are not immutable, this could
    * allow interference between the two branches. (Let us know if you think we should add an option to tee that creates
    * structured clones of the chunks for each branch.)
    */
  def tee(): js.Array[_ <: ReadableStream[T]] =
    js.native // TODO js.Tuple2[ReadableStream[T], ReadableStream[T]]
}

private[fetch] object ReadableStream {

  def apply[T](
      underlyingSource: js.UndefOr[ReadableStreamUnderlyingSource[T]] = js.undefined,
      queuingStrategy: js.UndefOr[QueuingStrategy[T]] = js.undefined
  ): ReadableStream[T] = {
    js.Dynamic
      .newInstance(js.Dynamic.global.ReadableStream)(
        underlyingSource.asInstanceOf[js.Any],
        queuingStrategy.asInstanceOf[js.Any]
      )
      .asInstanceOf[ReadableStream[T]]
  }
}

/** See [[https://streams.spec.whatwg.org/#underlying-source-api ¶4.2.3. The underlying source API]] of whatwg streams
  * spec.
  *
  * @tparam T
  *   Type of the Chunks returned by the Stream
  */
private[fetch] trait ReadableStreamUnderlyingSource[T] extends js.Object {

  /** A function that is called immediately during creation of the ReadableStream.
    *
    * If this setup process is asynchronous, it can return a promise to signal success or failure; a rejected promise
    * will error the stream. Any thrown exceptions will be re-thrown by the [[ReadableStream]] constructor.
    */
  var start: js.UndefOr[js.Function1[ReadableStreamController[T], js.UndefOr[js.Promise[Unit]]]] =
    js.undefined

  /** A function that is called whenever the stream’s internal queue of chunks becomes not full, i.e. whenever the
    * queue’s desired size becomes positive. Generally, it will be called repeatedly until the queue reaches its high
    * water mark (i.e. until the desired size becomes non-positive).
    *
    * This function will not be called until [[start]] successfully completes. Additionally, it will only be called
    * repeatedly if it enqueues at least one chunk or fulfills a BYOB request; a no-op [[pull]] implementation will not
    * be continually called.
    *
    * If the function returns a promise, then it will not be called again until that promise fulfills. (If the promise
    * rejects, the stream will become errored.) This is mainly used in the case of pull sources, where the promise
    * returned represents the process of acquiring a new chunk. Throwing an exception is treated the same as returning a
    * rejected promise.
    */
  var pull: js.UndefOr[js.Function1[ReadableStreamController[T], js.UndefOr[js.Promise[Unit]]]] =
    js.undefined

  /** A function that is called whenever the consumer cancels the stream, via [[ReadableStream.cancel]] or
    * [[ReadableStreamReader.cancel():scala\.scalajs\.js\.Promise[Unit]*]]. It takes as its argument the same value as
    * was passed to those methods by the consumer. If the shutdown process is asynchronous, it can return a promise to
    * signal success or failure; the result will be communicated via the return value of the [[cancel]] method that was
    * called. Additionally, a rejected promise will error the stream, instead of letting it close. Throwing an exception
    * is treated the same as returning a rejected promise.
    */
  var cancel: js.UndefOr[js.Function1[js.Any, js.UndefOr[js.Promise[Unit]]]] = js.undefined

  /** Can be set to "bytes" to signal that the constructed [[ReadableStream]] is a readable byte stream.
    *
    * Setting any value other than "bytes" or undefined will cause the ReadableStream() constructor to throw an
    * exception.
    */
  var `type`: js.UndefOr[ReadableStreamType] = js.undefined

  /** (byte streams only)
    *
    * Can be set to a positive integer to cause the implementation to automatically allocate buffers for the underlying
    * source code to write into.
    */
  var autoAllocateChunkSize: js.UndefOr[Int] = js.undefined
}

/** See [[https://streams.spec.whatwg.org/#qs-api ¶7.1. The queuing strategy API]]
  *
  * @tparam T
  *   Type of the Chunks returned by the Stream
  */
private[fetch] trait QueuingStrategy[T] extends js.Object {

  /** A non-negative number indicating the high water mark of the stream using this queuing strategy. */
  var highWaterMark: Int

  /** (non-byte streams only)
    *
    * The result is used to determine backpressure, manifesting via the appropriate desiredSize property. For readable
    * streams, it also governs when the underlying source's [[ReadableStreamUnderlyingSource.pull]] method is called.
    *
    * A function that computes and returns the finite non-negative size of the given chunk value.
    */
  var size: js.Function1[T, Int]
}

@js.native
private[fetch] sealed trait ReadableStreamType extends js.Any

private[fetch] object ReadableStreamType {
  val bytes: ReadableStreamType = "bytes".asInstanceOf[ReadableStreamType]
}

/** [[https://streams.spec.whatwg.org/#rs-controller-class ¶3.3 Class ReadableStreamController]] of whatwg spec
  *
  * The ReadableStreamController constructor cannot be used directly; it only works on a ReadableStream that is in the
  * middle of being constructed.
  *
  * @param stream
  *   can be null
  * @tparam T
  *   Type of the Chunks to be enqueued to the Stream
  */
@js.native
@JSGlobal
private[fetch] class ReadableStreamController[-T] private[this] () extends js.Object {

  /** The desiredSize getter returns the desired size to fill the controlled stream’s internal queue. It can be
    * negative, if the queue is over-full. An underlying source should use this information to determine when and how to
    * apply backpressure.
    *
    * @return
    *   the size of the strem - no idea if this actually is an int
    */
  def desiredSize: Int = js.native

  /** The close method will close the controlled readable stream. Consumers will still be able to read any
    * previously-enqueued chunks from the stream, but once those are read, the stream will become closed throws
    * scala.scalajs.js.TypeError if this is not a readable controller
    */
  def close(): Unit = js.native

  /** The enqueue method will enqueue a given chunk in the controlled readable stream.
    *
    * @param chunk
    *   throws scala.scalajs.js.RangeError if size is too big
    * @return
    *   seems like its an undefOr[Int] of the size
    */
  def enqueue(chunk: T): Unit = js.native
  def enqueue(): Unit = js.native

  /** The error method will error the readable stream, making all future interactions with it fail with the given error
    * e.
    *
    * @param e
    *   : an error - can this be any type? throws scala.scalajs.js.TypeError
    */
  def error(e: Any): Unit = js.native
}

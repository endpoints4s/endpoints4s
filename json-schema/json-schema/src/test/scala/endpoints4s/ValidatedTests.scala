package endpoints4s

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.{Failure, Success}

class ValidatedTests extends AnyWordSpec with Matchers {

  sealed trait FooBar

  case class Foo(content: String) extends FooBar {
    def toBar: Bar = Bar(content)
  }

  case class Bar(content: String) extends FooBar

  private val inputFoo = Foo("input")
  private val inputBar = Bar("input")

  private val validFoo = Valid(inputFoo)
  private val validBar = Valid(inputBar)
  private val invalid: Validated[Foo] = Invalid(Seq("Error1", "Error2"))

  "fold" should {

    "apply the valid function when used on a Valid" in {
      validFoo.fold(foo => foo.content, _ => "invalid") shouldBe "input"
    }

    "apply the invalid function when used on an Invalid" in {
      invalid.fold(foo => foo.content, _ => "invalid") shouldBe "invalid"
    }

  }

  "map" should {

    "map the value in a valid to the result of the given function" in {
      validFoo.map(_.toBar) shouldBe Valid(inputBar)
    }

    "leave an Invalid untouched" in {
      invalid.map(_.toBar) shouldBe invalid
    }

  }

  "mapErrors" should {

    "leave a Valid untouched" in {
      validFoo.mapErrors(_.map(_ + "a")) shouldBe validFoo
    }

    "map the errors in an Invalid to the result of the given function" in {
      invalid.mapErrors(_.map(_ + "a")) shouldBe Invalid(Seq("Error1a", "Error2a"))
    }

  }

  "flatMap" should {

    "validate the value in a Valid" in {
      validFoo.flatMap(foo => Valid(foo.toBar)) shouldBe validBar
      validFoo.flatMap(_ => Invalid("incorrect")) shouldBe Invalid("incorrect")

    }

    "leave an Invalid untouched" in {
      invalid.flatMap(foo => Valid(foo.toBar)) shouldBe invalid
      invalid.flatMap(_ => Invalid("incorrect")) shouldBe invalid

    }

  }

  "zip" should {

    "return a Valid with a tuple when given two Valids" in {
      validFoo.zip(validBar) shouldBe Valid((inputFoo, inputBar))
    }

    "flatten the tuple and remove any Units when given two Valids" in {
      // This is retesting the tupler but it doesn't hurt to have it tested in this context.

      validFoo.zip(Valid(())) shouldBe validFoo
      Valid(()).zip(validBar) shouldBe validBar
      Valid((inputFoo, Foo("secondFoo"))).zip(validBar) shouldBe Valid(
        (inputFoo, Foo("secondFoo"), inputBar)
      )
    }

    "return the invalid when one of the inputs is Invalid" in {
      invalid.zip(validBar) shouldBe invalid
      validFoo.zip(invalid) shouldBe invalid
    }

    "return an invalid with concatenated error Seqs when both inputs are Invalid" in {
      invalid.zip(Invalid("newerror")) shouldBe Invalid(Seq("Error1", "Error2", "newerror"))
    }

  }

  "toEither" should {

    "return a Right with the value when called on a Valid" in {
      validFoo.toEither shouldBe Right(inputFoo)
    }

    "return a Left with the errors when called on an Invalid" in {
      invalid.toEither shouldBe Left(Seq("Error1", "Error2"))
    }

  }

  "fromEither" should {

    "transform a Right into a Valid" in {
      Validated.fromEither(Right(inputFoo)) shouldBe validFoo
    }

    "transform a Left into an Invalid" in {
      Validated.fromEither(Left(Seq("Error1", "Error2"))) shouldBe invalid
    }

  }

  "fromOption" should {

    "transform a Some into a Valid" in {
      Validated.fromOption(Some(inputFoo))("input was None") shouldBe validFoo
    }

    "transform a None into an Invalid with the given error" in {
      Validated.fromOption(None)("input was None") shouldBe Invalid("input was None")
    }

  }

  "fromTry" should {

    "transform a Success into a Valid" in {
      Validated.fromTry(Success(inputFoo)) shouldBe validFoo
    }

    "transform a Failure into an Invalid with the exception's message" in {
      Validated.fromTry(Failure(new RuntimeException("failed try"))) shouldBe Invalid("failed try")
    }

  }

  "sequence" should {

    "turn an IterableOnce of all Valids into a Valid of an iterable" in {
      Validated.sequence(Seq[Validated[FooBar]](validFoo, validBar, validFoo)) shouldBe Valid(
        Seq(inputFoo, inputBar, inputFoo)
      )
    }

    "return an Valid empty sequence, given an empty sequence" in {
      Validated.sequence(Seq.empty[Validated[Unit]]) shouldBe Valid(Seq.empty)
    }

    "return the Invalid if the IterableOnce contains one" in {
      Validated.sequence(Seq(validFoo, invalid, validFoo)) shouldBe invalid
      Validated.sequence(Seq(invalid, validBar, validFoo)) shouldBe invalid
    }

    "return an Invalid with concatenated error messages if the IterableOnce contains multiple invalids" in {
      Validated.sequence(Seq(invalid, validBar, Invalid("newerror"))) shouldBe Invalid(
        Seq("Error1", "Error2", "newerror")
      )
    }

  }

  "traverse" should {

    "validate all items in an IterableOnce using the given function and return a Valid of an IterableOnce if all are valid" in {
      Validated.traverse(Seq(inputFoo, inputBar, inputFoo))(Valid(_)) shouldBe Valid(
        Seq(inputFoo, inputBar, inputFoo)
      )
    }

    "return an Valid empty sequence, given an empty sequence" in {
      Validated.traverse(Seq.empty[Unit])(x => Valid(x)) shouldBe Valid(Seq.empty)
    }

    "validate all items in an IterableOnce using the given function and return the Invalid if one is invalid" in {
      Validated.traverse(Seq(inputFoo, inputBar, inputFoo)) {
        case foo: Foo => Valid(foo)
        case _ : Bar => Invalid("bar")
      } shouldBe Invalid("bar")
    }

    "validate all items in an IterableOnce using the given function and return an Invalid with concatenated error messages if multiple are invalid" in {
      Validated.traverse(Seq(inputFoo, inputBar, inputFoo)) {
        case _: Foo => Invalid("foo")
        case bar: Bar => Valid(bar)
      } shouldBe Invalid(Seq("foo", "foo"))
    }

  }

}

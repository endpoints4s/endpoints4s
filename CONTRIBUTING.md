Contributing
============

## Layout

~~~
algebras/                   Algebra interfaces
akka-http/                  Interpreters based on Akka-http
play/                       Interpreters based on Play framework
scalaj/                     Interpreters based on Scalaj
xhr/                        Scala.js interpreters based on XMLHttpRequest
openapi/                    Interpreters generating OpenAPI documentation
testsuite/                  Test kit
documentation/              User manual and examples
sbt-assets/                 Sbt settings to help handling assets
~~~

## Cheat sheet

### Compile the project with the default Scala version

~~~ sh
$ sbt compile
~~~

### Run the tests

~~~ sh
$ sbt test
~~~

Or, for a specific Scala version:

~~~ sh
$ sbt "++ 2.12.11 test"
$ sbt "++ 2.13.2 test"
~~~

### Format source code

We use Scalafmt. You can use the `scalafmt` sbt task like the following:

~~~ sh
> scalafmt
~~~

### Preview the documentation

~~~ sh
$ sbt manual/previewSite
~~~

And then go to http://localhost:4000.

### Publish the documentation

~~~ sh
$ sbt manual/ghpagesPushSite
~~~

### Run the examples 

~~~ sh
++ 2.13.2 example-basic-play-server/run
~~~

## Working with mill

Endpoints comes with experimental mill build which is meant for evaluating mill as a replacecement for sbt

~~~sh
mill genidea #generate idea project config
mill all __.compile #compile everything
mill all __.test #test everything
~~~
After generating intellij project you may need to navigate to Settings -> Languages & Frameworks -> Worksheet and set inteprpretation mode to "Always Ammonite"

## Release process

1. Bump the version of every module that hasn’t been bumped (e.g., change `1.0.0+` into
   `1.0.1`, `1.1.0`, or `2.0.0`, according to the compatibility guarantees of the module)
2. Run the following command:
   ~~~ sh
   $ sbt versionCheck "++ 2.12.12 publishSigned" "++ 2.13.3 publishSigned" sonatypeReleaseAll "++ 2.13.3 manual/makeSite" manual/ghpagesPushSite
   ~~~
3. Reset the compatibility intention to `Compatibility.BinaryAndSourceCompatible`,
   and add a `+` suffix to the version of every module (e.g., change `1.0.0`
   into `1.0.0+`)

## Breakage policy

Our goal is to publish backward binary compatible releases of the algebra modules for as long
as possible. It is OK to break compatibility in interpreter modules, but if possible we
should keep backward compatibility.

So, algebra and interpreters have different version numbers and compatibility guarantees
across releases.

After every release, the level of compatibility is reset to `Compatibility.BinaryAndSourceCompatible`
in every module.

If necessary, we can relax this constraint to `Compatibility.BinaryCompatible` in modules that
need to introduce potential source incompatibilities. In such a case, we can also preset the
version number of the module (e.g., we can change `1.0.0+` into `1.1.0`).

If necessary, we can relax this constraint to `Compatibility.None` in interpreter modules that
need to break binary compatibility. In such a case, we can also preset the version number of
the module (e.g., we can change `1.0.0+` into `2.0.0`).

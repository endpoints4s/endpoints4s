Contributing
============

## Layout

~~~
algebras/                   Algebra interfaces
akka-http/                  Interpreters based on Akka-http
http4s/                     Interpreters based on http4s
play/                       Interpreters based on Play framework
scalaj/                     Interpreters based on Scalaj
sttp/                       Interpreters based on sttp
xhr/                        Scala.js interpreters based on XMLHttpRequest
openapi/                    Interpreters generating OpenAPI documentation
documentation/              User manual and examples
sbt-assets/                 Sbt plugin to help handling assets
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
$ sbt "++ 2.12.13 test"
$ sbt "++ 2.13.7 test"
~~~

### Introduction of source or binary incompatibilities

The project is made of several modules, and each module has its own compatibility
guarantees. When we cut a release, we publish all the modules, each with its own
version number.

The modules providing _algebras_ have strong compatibility guarantees, whereas
the modules providing _interpreters_ are more prone to breaking backward
compatibility. This is because in a system based on microservices, it should be
possible to work on each service independently of the others. However, when the
API of a service is used by another service, that API is shared in the form of
a module dependency, which introduce a dependency on the endpoints4s algebras,
by transitivity. Thus, it is very important that algebra modules evolve in a
backward binary compatible way. That allows service to depend on the API of
several other services, possibly using different versions of endpoints4s.

We use the recommended versioning scheme, Early SemVer. This means that we
signal that we introduce source incompatibilities in a module by bumping its
minor version number, and we signal that we introduce binary incompatibilities
by bumping its major version number.

We use sbt-version-policy to check the binary compatibility between releases,
and to manage the release versions accordingly.

To check that your changes don’t break the versioning policy, run the task
`versionPolicyCheck`:

~~~ sh
$ sbt versionPolicyCheck
~~~

If this task fails, either find a way to implement your contribution in a
non-breaking way, or relax the compatibility guarantees of the module
by changing its setting `versionPolicyIntention`.

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
++ 2.13.7 example-basic-play-server/run
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

1. Make sure the release notes for the next release are ready (https://github.com/endpoints4s/endpoints4s/releases)
   Every PR should be listed (except @scala-steward’s PRs), and there should be a table at the
   beginning of the release notes, summarizing the modules’ versions and their dependencies’
   versions.
2. Bump the version of every module (e.g., change `1.0.0+n` into `1.0.1`, `1.1.0`, or `2.0.0`,
   according to the compatibility guarantees of the module). If a module does not define its
   version (this is the case for all the modules that only provide algebras), it uses the one
   defined in the root file `build.sbt`. All the interpreters have their own version numbers
   and compatibility guarantees, so they all define both settings `versionPolicyIntention` and
   `version`.
3. Commit the new versions
   ~~~ sh
   git commit -a -m "Set release versions"
   ~~~
4. Run `versionPolicyCheck` and `versionCheck`
   ~~~ sh
   sbt versionPolicyCheck versionCheck
   ~~~
5. Upload the bundles to sonatype, release them, and publish the documentation website
   ~~~ sh
   sbt "++ 2.12.13 publishSigned" "++ 2.13.7 publishSigned" sonatypeReleaseAll "++ 2.13.7 manual/makeSite" manual/ghpagesPushSite
   ~~~
6. Create a tag `vx.y.z`
   ~~~ sh
   git tag vx.y.z
   ~~~
7. Reset the compatibility intention to `Compatibility.BinaryAndSourceCompatible`,
   and add a `+n` suffix to the version of every module (e.g., change `1.0.0`
   into `1.0.0+n`). In case a module used its own value for `versionPolicyIntention`
   instead of reusing the one defined at the build level, comment it out.
8. Commit the changes
   ~~~ sh
   git commit -a -m "Reset compatibility intention"
   ~~~
9. Push
   ~~~ sh
   git push --tags origin master
   ~~~
10. Publish the release notes on GitHub

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
version number of the module (e.g., we can change `1.0.0+n` into `1.1.0`).

If necessary, we can relax this constraint to `Compatibility.None` in interpreter modules that
need to break binary compatibility. In such a case, we can also preset the version number of
the module (e.g., we can change `1.0.0+n` into `2.0.0`).

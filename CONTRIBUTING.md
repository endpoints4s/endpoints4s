Contributing
============

## Layout

~~~
algebras/                   Algebra interfaces
pekko-http/                  Interpreters based on Pekko-http
http4s/                     Interpreters based on http4s
play/                       Interpreters based on Play framework
sttp/                       Interpreters based on sttp
xhr/                        Scala.js interpreters based on XMLHttpRequest
fetch/                      Scala.js interpreters based on Fetch
openapi/                    Interpreters generating OpenAPI documentation
documentation/              User manual and examples
stub-server/                HTTP server used for testing client interpreters
sbt-assets/                 Sbt plugin to help handling assets
~~~

## Cheat sheet

Start the `sbt` shell from the project root directory. Then run the following command from
the sbt prompt.

### Compile the project with the default Scala version

~~~
> compile
~~~

### Run the tests

~~~
> sbt test
~~~

Or, for a specific Scala version:

~~~
> ++ 2.12 test
> ++ 2.13 test
> ++ 3 test
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

~~~
> versionPolicyCheck
~~~

If this task fails, either find a way to implement your contribution in a
non-breaking way, or relax the compatibility guarantees of the module
by changing its setting `versionPolicyIntention`.

### Format source code

We use Scalafmt. You can use the `scalafmt` sbt task like the following:

~~~
> scalafmt
~~~

### Preview the documentation

~~~
> manual/previewSite
~~~

And then go to http://localhost:4000.

### Publish the documentation

~~~
> manual/ghpagesPushSite
~~~

### Run the examples 

~~~
> ++ 2.13 example-basic-play-server/run
~~~

## Release process

We use `sbt-release`, and we cut a release by manually triggering the `release` GitHub workflow.

1. Make sure the draft release notes for the next release (https://github.com/endpoints4s/endpoints4s/releases)
   list all the PRs that have been merged (except @scala-steward’s PRs).
2. Run the [release](https://github.com/endpoints4s/endpoints4s/actions/workflows/release.yml) workflow from the GitHub UI.
3. The workflow will compute the next release version for every artifact, run the compatibility checks, publish
   the artifacts, push a Git tag (matching the version of the algebra), and update the documentation website.
4. In the GitHub [releases](https://github.com/endpoints4s/endpoints4s/releases) page, associate the draft
   release with the tag that has just been pushed, and update it to include a table at the
   beginning of the release notes summarizing the modules’ versions and their dependencies’
   versions.
5. Manually reset the compatibility intention of all the modules to `Compatibility.BinaryAndSourceCompatible`.
   In case a module used its own value for `versionPolicyIntention` instead of reusing the one defined at the
   build level, comment it out.
6. Commit the changes
   ~~~ sh
   git commit -a -m "Reset compatibility intention"
   ~~~
7. Push
   ~~~ sh
   git push origin master
   ~~~
8. Publish the release notes on GitHub

## Breakage policy

Our goal is to publish backward binary compatible releases of the algebra modules for as long
as possible. It is OK to break compatibility in interpreter modules, but if possible we
should keep backwards compatibility.

So, algebra and interpreters have different version numbers and compatibility guarantees
across releases.

After every release, the level of compatibility is reset to `Compatibility.BinaryAndSourceCompatible`
in every module.

If necessary, we can relax this constraint to `Compatibility.BinaryCompatible` in modules that
need to introduce potential source incompatibilities.

If necessary, we can relax this constraint to `Compatibility.None` in interpreter modules that
need to break binary compatibility.

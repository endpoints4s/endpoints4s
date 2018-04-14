Contributing
============

## Layout

~~~
algebras/                   Algebra interfaces
akka-http/                  Interpreters based on Akka-http
play/                       Interpreters based on Play framework
scalaj/                     Interpreters based on Scalaj
xhr/                        Scala.js interpreters based on XMLHttpRequest
openapi/                    Alternative algebra interfaces for generating documentation
testsuite/                  Test kit
documentation/              User manual and examples
sbt-assets/                 Sbt settings to help handling assets
~~~

## Cheat sheet

### Compile the project

~~~ sh
$ sbt +compile
~~~

### Run the tests

~~~ sh
$ sbt +test
~~~

or for faster feedback loop:

~~~ sh
$ sbt "+++ 2.10.6 test"
$ sbt "+++ 2.11.11 test"
$ sbt "+++ 2.12.4 test"
~~~


### Preview the documentation

~~~ sh
$ sbt +manual/previewAuto
~~~

And then go to http://localhost:4000.

### Publish the documentation

~~~ sh
$ sbt +manual/ghpagesPushSite
~~~

### Run the examples 

~~~ sh
+++ 2.12.4 example-basic-play-server/run
~~~

## Working with mill

~~~sh
mill genidea #generate idea project config
mill all __.compile #compile everything
mill all __.test #test everything
~~~
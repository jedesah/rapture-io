[![Build Status](https://travis-ci.org/propensive/rapture-io.png?branch=master)](https://travis-ci.org/propensive/rapture-io)
Rapture I/O
-----------

Please note that Rapture I/O is currently in a state of flux as it being split into a number of smaller modules. The instructions below relate to the 0.8.1 release, and will not work with the current master.

Version 0.8.1 for Scala 2.10.2

**Rapture I/O** is a general purpose I/O library for Scala, providing much of the functionality of `java.io` and `java.net`, plus comprehensive support for working with JSON.

See [rapture.io](http://rapture.io/) for more information, examples and documentation.

### Building Rapture I/O

Rapture I/O has no dependencies (apart from Scala), and you can build it from source with just a few simple commands.

Using Scala 2.10.2 (or later), compile Rapture I/O as follows:

        git clone https://github.com/propensive/rapture-io.git
        cd rapture-io
        scalac -d bin src/*.scala
        jar cf io.jar -C bin rapture

Just include `io.jar` on your classpath to use Rapture I/O.

### Building Rapture I/O using Maven

Thanks to Michel Daviot, you can also use Maven to compile the project using:

        git clone https://github.com/propensive/rapture-io.git
        cd rapture-io
        mvn clean scala:compile package

To use Rapture I/O in your own projects, just include io.jar on your classpath and`import rapture.io._`.

For most functionality, you will also need to specify a strategy for handling exceptions. Import one of the following return-type strategies:

* `strategy.throwExceptions`
* `strategy.captureExceptions`
* `strategy.returnTry`
* `strategy.returnFutures`

Now, you're ready to go! Try this:

        $ scala -cp io.jar
        Welcome to Scala version 2.10.2 (Java HotSpot(TM) 64-Bit Server VM, Java 1.6.0_24).
        Type in expressions to have them evaluated.
        Type :help for more information.

        scala> import rapture.io._
        import rapture.io._

        scala> import strategy.throwExceptions
        throwExceptions

        scala> json"""{ "foo": "Hello world!" }"""
        res0: rapture.io.Json = 
        {
         "foo": "Hello world!"
        }



[![Build Status](https://travis-ci.org/propensive/rapture-io.png?branch=master)](https://travis-ci.org/propensive/rapture-io)
Rapture I/O
===========

Version 0.8.1 (beta) for Scala 2.10.2

Rapture is a general purpose I/O library for Scala.

Compilation
-----------

You can now use Maven to compile the project :

mvn clean scala:compile

to get the jar file :

mvn clean scala:compile package

Rapture I/O can be compiled the "low-tech" way using a few familiar commands, or
you can use the "high-tech" rapture-build Makefile, which can simplify the
compilation and development process.

Whilst the first option requires more steps to complete, developers should
already be very familiar with the commands used in each step.  The second option
should be less effort, though the automation comes at the expense of
transparency.


Simple Compilation
--------------------

Using Scala 2.10.2 (or later), compile Rapture I/O as follows:

        mkdir rapture
        cd rapture
        git clone https://github.com/propensive/rapture-io.git io
        cd io
        scalac -d bin src/*.scala
        jar cf io.jar -C bin rapture

To use Rapture I/O in your own projects, just include io.jar on your classpath, import
rapture.io._.

For most functionality, you will also need to specify a strategy for handling exceptions. Import one of the following return type strategies:

        import strategy.throwExceptions
        import strategy.captureExceptions
        import strategy.returnTry
        import strategy.returnFutures

Now, you're ready to go!

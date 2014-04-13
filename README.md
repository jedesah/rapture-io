[![Build Status](https://travis-ci.org/propensive/rapture-io.png?branch=scala-2.11)](https://travis-ci.org/propensive/rapture-io)

# Rapture IO

Rapture IO is a general purpose IO library for Scala, providing much of the functionality of java.io and java.net with an idiomatic Scala API.

### Status

Rapture IO is *managed*. This means that the API is expected to continue to evolve, but all API changes will be documented with instructions on how to upgrade.

### Availability

Rapture IO 0.9.0 is available under the Apache 2.0 License from Maven Central with group ID `com.propensive` and artifact ID `rapture-io_2.11`.

#### SBT

You can include Rapture IO as a dependency in your own project by adding the following library dependency to your build file:

```scala
libraryDependencies ++= Seq("com.propensive" %% "rapture-io" % "0.9.0")
```

#### Maven

If you use Maven, include the following dependency:

```xml
<dependency>
  <groupId>com.propensive</groupId>
  <artifactId>rapture-io_2.11</artifactId>
  <version>0.9.0<version>
</dependency>
```

#### Download

You can download Rapture IO directly from the [Rapture website](http://rapture.io/)
Rapture IO depends on Scala 2.11 and Rapture Core, URI & MIME, but has no third-party dependencies.

#### Building from source

To build Rapture URI from source, follow these steps:

```
git clone git@github.com:propensive/rapture-io.git
cd rapture-io
sbt package
```

If the compilation is successful, the compiled JAR file should be found in target/scala-2.11

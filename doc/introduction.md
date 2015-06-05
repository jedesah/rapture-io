## Rapture IO

Rapture IO is a library for performing file, network and other I/O tasks in Scala.

Unlike many traditional I/O APIs, Rapture separates the concept of a resource (such as a
file or an HTTP URL) from its capabilities (such as the ability to be read of have its size
checked). This means that any resource can have any capability, provided there is an
implementation for that combination of resource and capability. These capabilities are all
presented through simple and consistent method invocations on resources, so in normal usage
it works just as you would hope.


### Resources

Resources are the very general concept of a type which can have some capability.  Note that
any type in Scala can be a resource -- there's no `Resource` interface they must inherit
from. Rapture defines some standard IO resources for representing files, HTTP(S) URLs,
classpath resources, sockets and system processes, but there's nothing to stop other types
like `Map`, `String` or `java.io.File` being considered resources; it only becomes a
resource by virtue of having one or more capabilities defined on it.

Rapture IO defines some of its own resources, though pre-existing and new types can also be
resources. Typically, a resource in Rapture IO is represented by a URI or URL, whose
internal state is nothing more than an immutable human- and computer-readable reference to
the location of the data which makes up the state of the underlying resource.

It's important to be aware that despite many of the operations performed on resources by
Rapture intrinsically result in modification to the underlying resource data, the JVM heap
objects representing those resources typically do not change; you can have an object
representing a resource called `file:///x/y` which you rename to `file:///x/z`, and the
filesystem will modify some blocks on the physical disk so that the data which was
previously at `file:///x/y` is now at `file:///x/z`, but the original object will still
refer to `file:///x/y`, even though there is now no data at that location.

Likewise, while the JVM heap objects representing resources are immutable, there are no
guarantees that the data they refer to exists, or maintains any consistent state over the
lifetime of the resource instance which refers to it.


### Capabilities

Rapture IO provides several standard capabilities for I/O operations on resources. These
provide functionality including reading, writing, checking the size of, navigating,
deleting, copying and moving. If a resource has a capability, you can call a method on it,
like this:

```
resource.slurp[Char]
```

This looks just like a simple method call on the resource, but differs in that the
functionality is actually provided by implicits and type classes, rather than defined as
part of the resource type. From a design point of view, this means that the capabilities of
a resource do not need to be defined, known, or even anticipated at the time the resource is
defined. From a usage point of view, you rarely if ever need to be aware of this.  Rapture
IO provides implicits which will be resolved by Scala at compile time to ensure that you can
only use capabilities on resources which support them.

Here are some of the capabilities which Rapture IO provides for those resources which
support them:


#### Slurping

Some resources can be "slurped" into memory, usually in the form of a string or a byte
array.  Whilst reading an entire resource into memory may be undesirable for large resources
-- we would prefer streaming solutions, if possible -- it remains a convenient way of
working with small amounts of data.

Slurping resources is provided by the `slurp` method, which takes a single type parameter,
representing the units of data being streamed, typically `Byte` or `Char`. Depending on this
choice, you will get a `String` or `Bytes` object.

```
val content: String = file.slurp[Char]
```


#### Navigation

Many resources, such as filesystem files, are organised into a tree hierarchy, where leaves
are called "files" and nodes are "directories". We call resources such as these *navigable*,
and make available the methods `children`, which returns a `Seq` of child resources
immediately beneath it in the hierarchy, and `isDirectory` which returns true only if the
resource is capable of having children (though it may not actually have any).

```
import rapture.uri._
val dir: FileUrl = uri"file:///home/work/dev"
val files: Seq[FileUrl] = dir.children
```

When calling `children`, the types of the child elements will be the same as that of the
parent resource.

Additionally, it can be useful to iterate over not just the immediate children of a
directory, but all its descendants. The method `descendants` returns an `Iterator` which
provides a pre-order traversal of the directory structure. As the descendants may be
very numerous, the collection returned is a lazily-evaluated `Iterator`, rather than a
strictly-evaluated collection like `List`.


#### Sizing

You can find the size of a resource in bytes by calling `size` on the method.  By default,
this will return the size of the resource in bytes, however if you want to calculate the
size in characters (if it uses a multi-byte encoding) or lines, you can specify an optional
type parameter of `Char` or `String` respectively.

```
scala> val bytes = file.size()
bytes: Long = 4927

scala> val chars = file.size[Char]()
chars: Long = 4919

scala> val lines = file.size[String]()
lines: Long = 74
```

To work with characters or strings, an implicit `Encoding` is required. By default, any
resource which is *readable* can have its size checked, though the complexity of this
operation is typically linear in the size of the resource, as the operation involves
streaming the entire resource. Some resources, like files, can know their size in bytes in
constant time, so where possible, this is preferred.


#### Writing and Appending

A resource which can accept a stream of data, and store it (or similar) is considered
writable. In many cases, however, a distinction is made between *writing* and *appending* to
a file, and resources may support either, both or neither operation.


#### Copying

Copying a resource is to make a replica of it at another resource location. One way of doing
this is to read from the source resource, and to write to the destination resource, and by
this method, any resources with read and write capabilities can be a the source and
destination respectively for a copy operation.

This is, however, an inefficient way of performing a copy in many cases, as it involves
streaming the entire source data into memory, before streaming it out again to the
destination resource. For a large, remote file, this could be very slow, and unnecessary if,
for example, the file were on an FTP server, which supports a simple "copy" command.

So, in the cases where the resources are statically known to be directly copyable, this
method will be preferred over streaming.


#### Deleting

Some resources, such as files on the local filesystem or on an FTP server, may be deletable.
Resources which may be deleted may have a `delete()` method called on them.

```
val file = uri"file:///home/work/garbage"
file.delete()
```


#### Moving and renaming

To be completed.


#### Linking

To be completed.


### Working with Character Encodings

Many operations in Rapture IO involve conversions between bytes and characters. A character
encoding determines the mappings between a byte or byte-sequence, and a character. Most
applications will assume the character encoding of the host operating system, though making
this assumption can result in software which isn't portable. For this reason, Rapture IO
requires that the encoding is specified explicitly if any operation requires it.

The easiest way to specify a character encoding is to import one, like this:

```
import rapture.io._
import rapture.codec._
import encodings.`UTF-8`
```

This will import the `UTF-8` encoding. Other standard encodings available in Java can be
similarly imported, using the encoding's canonical name, enclosed in backticks. The
canonical name (which requires the use of backticks) is the preferred naming scheme because
it avoids having to memorize a translation into valid Scala identifier syntax, in addition
to the name of the encoding itself.

If you really want to use the default character encoding of the host OS, you can import
`encodings.system`.


### URIs and URLs

Most resources in Rapture IO come in the form of URIs. There are three main ways of creating
these:

- Construction from a path
- Parsing a string at runtime
- Using a string-context literal

#### Construction from a path

A URL can be built up by combining a scheme object, such as `File`, `Ftp`, `Http` or `Https`
with a number of string path elements, separated by `/`s. For example:

```
scala> File / "home" / "workspace" / "app.scala"
res: FileUrl = file:///home/workspace/app.scala
```

or

```
scala> Https / "www.example.com" / "src" / "app.scala"
res: HttpUrl = http://www.example.com/src/app.scala
```

Any path-style URL can be appended to using the `/` operator, or manipulated in other ways,
so you can think of the scheme objects as "empty" paths, from which new paths are built.

#### Parsing a path

The scheme objects also all provide a `parse` method, which takes a String at runtime, and
attempts to parse it into a valid instance of the corresponding URL type. This is the method
you would typically use when very little is known about the URL until runtime.


#### Using a string-context literal

Another way of creating a URL is to write a URL-literal, using the `url` string-context, for
example:

```
scala> url"file:///home/workspace/app.scala"
res: FileUrl = file:///home/workspace/app.scala

scala> url"http://www.example.com/src/app.scala
res: HttpUrl = http://www.example.com/src/app.scala
```

This approach combines a lightweight syntax with compile-time parsing. When you write a
`url` string-context literal, a macro is run at compile time which extracts the URL scheme
from the string, finds the corresponding Scheme object, and invokes the `parse` method on
the URL. This is how, in the example above, the compiler is able to provide accurate
return types like `FileUrl` and `HttpUrl`, based only on the part of the URL before the `:`.


### Working with `Bytes`

Whilst `String`s provide a convenient way of working immutably with arbitrary-length
sequences of `Char`s, there is no standard equivalent for `Byte`s. The `Bytes` type is
designed to fill this gap, as a wrapper around an `Array[Byte]` providing a few convenient
methods.

The `Bytes` type has `hashCode` and `equals` methods defined which distinguish one `Bytes`
instance from another based on their entire contents. The `toString` method will display the
entire byte-string in hexadecimal. To view the bytes in another encoding, such as BASE-64 or
binary, the `as` method will return a `String` of the bytes in the encoding specified by its
type parameter, for example, `bytes.as[Base64]` or `bytes.as[Binary]`.


### Working with Modes

Most I/O operations in Rapture may fail. A file write operation may fail because the disk is
read-only, or a network operation may fail because the network is down. By default, these
fallible operations will throw an exception, which should be caught and handled.

This may be the appropriate approach to dealing with failures in some environments, such as
script-style coding, or working within the REPL, but for robust production code it may be
more apt to handle failures in the type system using return types like `Try` or `Either`, or
even to dispatch the operations to a thread pool, and have operations return a `Future`.

Rapture IO supports all these possibilities, and others, by means of *modes* from the
Rapture Core project. By importing an alternative mode into scope, for example,

```
import modes.returnTry
```

the static return type of every fallible method will be adapted to be a `Try`.

Please see the Rapture Core documentation for more information about working with modes.


### Unified Streams

Most of the time, Rapture IO obviates the need to work directly with low-level byte or
character streams. But should you need to, the `Input` and `Output` types are provided for
data streaming into and out of the JVM. These types are parameterized on the type of the
units of data they stream, typically `Byte`s, `Char`s, or `String`s. So the equivalent of a
`java.io.InputStream` is an `Input[Byte]`, and a `java.io.Writer` is an `Output[Char]`.

`Input`s and `Output`s have a similar API to the Java classes they replace. Their
definitions look like this:

```
trait Input[Data] {
  def read(): Data
  def readBlock(array: Array[Data], offset: Int, length: Int): Int
  def ready: Boolean
  def close(): Unit
}

trait Output[Data] {
  def write(data: Data): Unit
  def writeBlock(array: Array[Data], offset: Int, length: Int): Int
  def flush(): Unit
  def close(): Unit
}
```

### Processes as Resources

To be completed.


### IP Addresses

Rapture IO provides representations of IPv4 and IPv6 addresses.

To be completed.

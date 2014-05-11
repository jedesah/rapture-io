/**********************************************************************************************\
* Rapture I/O Library                                                                          *
* Version 0.9.0                                                                                *
*                                                                                              *
* The primary distribution site is                                                             *
*                                                                                              *
*   http://rapture.io/                                                                         *
*                                                                                              *
* Copyright 2010-2014 Jon Pretty, Propensive Ltd.                                              *
*                                                                                              *
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file    *
* except in compliance with the License. You may obtain a copy of the License at               *
*                                                                                              *
*   http://www.apache.org/licenses/LICENSE-2.0                                                 *
*                                                                                              *
* Unless required by applicable law or agreed to in writing, software distributed under the    *
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,    *
* either express or implied. See the License for the specific language governing permissions   *
* and limitations under the License.                                                           *
\**********************************************************************************************/
package rapture.io
import rapture.core._
import rapture.uri._
import rapture.mime._

import java.io._
import java.net._
import scala.reflect._

import language.higherKinds

object Utils {

  /** Safely closes a stream after processing */
  def ensuring[Result, Stream](create: Stream)(body: Stream => Result)(close: Stream => Unit) = {
    val stream = create
    val result = try { body(stream) } catch {
      case e: Throwable => try { close(stream) } catch { case e2: Exception => () }
      throw e
    }
    close(stream)

    result
  }
}

case class Stdout[Data](implicit outputBuilder: OutputBuilder[OutputStream, Data]) {
  def output(implicit mode: Mode[IoMethods]): mode.Wrap[Output[Data], Exception] =
    mode.wrap(outputBuilder.output(System.out)(raw))
}

case class Stderr[Data](implicit outputBuilder: OutputBuilder[OutputStream, Data]) {
  def output(implicit mode: Mode[IoMethods]): mode.Wrap[Output[Data], Exception] =
    mode.wrap(outputBuilder.output(System.err)(raw))
}

case class Stdin[Data](implicit inputBuilder: InputBuilder[InputStream, Data]) {
  def input(implicit mode: Mode[IoMethods]): mode.Wrap[Input[Data], Exception] =
    mode.wrap(inputBuilder.input(System.in)(raw))
}

/** Makes a `String` viewable as an `rapture.io.Input[Char]` */
case class StringIsInput(string: String) extends CharInput(new StringReader(string))

/** Makes an `Array[Byte]` viewable as an `Input[Byte]` */
case class ByteArrayInput(array: Array[Byte]) extends ByteInput(new ByteArrayInputStream(array))

/** Type trait for building a new `Input` from particular kind of input stream
  *
  * @tparam InputType The type of input that is to be interpreted as an `Input`,
  *         such as `java.io.InputStream` or `java.io.Reader`
  * @tparam Data The type of data that the `Input` carries */
trait InputBuilder[InputType, Data] {
  def input(s: InputType)(implicit mode: Mode[IoMethods]): mode.Wrap[Input[Data], Exception]
}

/** Type trait for building a new `Output[Data]` from particular kind of output stream
  *
  * @tparam OutputType The type of output that is to be interpreted as an `Output`,
  *         such as [[java.io.OutputStream]] or [[java.io.Writer]]
  * @tparam Data The type of data that the [[Output]] carries */
trait OutputBuilder[OutputType, Data] {
  def output(s: OutputType)(implicit mode: Mode[IoMethods]): mode.Wrap[Output[Data], Exception]
}

object AppenderBuilder extends AppenderBuilder[java.io.Writer, Char] {
  def appendOutput(s: java.io.Writer) = new CharOutput(s)
}

trait AppenderBuilder[OutputType, Data] { def appendOutput(s: OutputType): Output[Data] }

object Appendable {
  class Capability[Res](res: Res) {
    implicit protected val errorHandler = raw

    def appendOutput[Data](implicit sa: Appender[Res, Data], mode: Mode[IoMethods]) =
      sa.appendOutput(res)
    
    def handleAppend[Data, Result](body: Output[Data] => Result)(implicit sw:
        Appender[Res, Data]): Result = {
      ensuring(appendOutput[Data])(body) { out =>
        out.flush()
        if(!sw.doNotClose) out.close()
      }
    }
  }

}

object Readable {
  class Capability[Res](res: Res) {

    implicit private val errorHandler = raw

    /** Gets the input for the resource specified in this resource */
    def input[Data](implicit sr: Reader[Res, Data], mode: Mode[IoMethods]):
      mode.Wrap[Input[Data], Exception] = mode.wrap(sr.input(res))
   
    /** Pumps the input for the specified resource to the destination resource provided */
    def >[Data, DestRes](dest: DestRes)(implicit sr:
        Reader[Res, Data], sw: Writer[DestRes, Data], mode: Mode[IoMethods],
        mf: ClassTag[Data]): mode.Wrap[Int, Exception] =
      mode.wrap(handleInput[Data, Int] { in =>
        writable(dest).handleOutput[Data, Int](in pumpTo _)
      })
    
    def |[Data, DestRes](dest: DestRes)(implicit sr:
        Reader[Res, Data], sw: Writer[DestRes, Data], mf: ClassTag[Data]):
        DestRes = {
          >(dest)
          dest
        }

    def >>[Data, DestRes](dest: DestRes)(implicit sr:
        Reader[Res, Data], sw: Appender[DestRes, Data], mode: Mode[IoMethods],
        mf: ClassTag[Data]): mode.Wrap[Int, Exception] =
      mode.wrap(handleInput[Data, Int] { in =>
        dest.handleAppend[Data, Int](in pumpTo _)
      })

    /** Pumps the input for the specified resource to the destination output provided
      *
      * @tparam Data The type that the data should be pumped as
      * @param out The destination for data to be pumped to */
    def >[Data](out: Output[Data])(implicit sr: Reader[Res, Data],
        mode: Mode[IoMethods], mf: ClassTag[Data]): mode.Wrap[Int, Exception] =
      mode.wrap(handleInput[Data, Int](_ pumpTo out))

    /** Carefully handles writing to the input stream, ensuring that it is closed following
      * data being written to the stream. Handling an input stream which is already being handled
      * will have no effect.
      *
      * @tparam Data The type of data the stream should carry
      * @tparam Result The type of body's result
      * @param body The code to be executed upon the this Input before it is closed */
    def handleInput[Data, Result](body: Input[Data] => Result)(implicit sr:
        Reader[Res, Data]): Result =
      ensuring(input[Data])(body) { in => if(!sr.doNotClose) in.close() }
  }
}

object Writable {
  class Capability[Res](res: Res) {
    
    implicit private val errorHandler = raw
    
    /** Gets the output stream directly
      *
      * @tparam Data The type of data to be carried by the `Output` */
    def output[Data](implicit sw: Writer[Res, Data], mode: Mode[IoMethods]):
        mode.Wrap[Output[Data], Exception] = mode.wrap(sw.output(res))
    
    /** Carefully handles writing to the output stream, ensuring that it is closed following
      * data being written.
      *
      * @param body The code to be executed upon this `Output` before being closed.
      * @return The result from executing the body */
    def handleOutput[Data, Result](body: Output[Data] => Result)(implicit sw:
        Writer[Res, Data]): Result =
      ensuring(output[Data])(body) { out =>
        out.flush()
        if(!sw.doNotClose) out.close()
      }
  }
}

/** Type trait for defining how a resource of type U should 
  *
  * @tparam Url Url for which this corresponds
  * @tparam Data Units of data to be streamed, typically `Byte` or `Char` */
@implicitNotFound(msg = "Cannot write ${Data} data to ${UrlType} resources. Note that if you "+
    "are working with Char data, you will require an implicit character encoding, e.g. "+
    "import encodings.system or import encodings.`UTF-8`.")
trait Writer[-UrlType, @specialized(Byte, Char) Data] {
  def doNotClose = false
  def output(url: UrlType)(implicit mode: Mode[IoMethods]): mode.Wrap[Output[Data], Exception]
}

trait Appender[-UrlType, Data] {
  def doNotClose = false
  def appendOutput(url: UrlType)(implicit mode: Mode[IoMethods]): mode.Wrap[Output[Data], Exception]
}

/*  Extract the encoding from an HTTP stream */
/*private def extractEncoding(huc: HttpURLConnection): mode.Wrap[String, Exception] = mode.wrap {
  
  huc.getContentEncoding match {
    case null =>
      huc.getContentType match {
        case null =>
          encodings.`ISO-8859-1`.name
        case ct =>
          ct.split(";") map (_.trim) find (_ startsWith "charset=") map (_ substring 8) getOrElse
              encodings.`ISO-8859-1`.name
      }
    case ce => ce
  }
}*/

trait TypedInput { thisInput: Input[_] =>
  def mimeType: MimeTypes.MimeType
}

/** An Input provides an incoming stream of data */
trait Input[@specialized(Byte, Char) Data] extends Seq[Data] { thisInput =>

  private var beingHandled = false

  override def toString() = "<input>"
  
  def length: Int = throw new Exception("Cannot calculate length of a stream")

  def apply(n: Int) = {
    for(i <- 0 until n) read()
    read().get
  }

  def typed(mime: MimeTypes.MimeType) = new Input[Data] with TypedInput {
    def mimeType = mime
    def ready() = thisInput.ready()
    def close() = thisInput.close()
    def read() = thisInput.read()
  }

  def iterator: Iterator[Data] = new Iterator[Data] {
    private var nextVal: Option[Data] = read()
    def hasNext = nextVal.isDefined
    def next() = {
      val x = nextVal.get
      nextVal = read()
      x
    }
  }

  /** Returns whether the stream can be read without blocking */
  def ready(): Boolean

  /** Reads a single item of data from the input stream.  Note that each call to this method
    * will result in a new instance of `Some` to be constructed, so for reading larger block, use
    * the `readBlock` method which may be implemented more efficiently. */
  def read(): Option[Data]
 
  /** Default implementation for reading a block of data from the input stream into the specified
    * array.
    *
    * The basic implementation is provided for convenience, though it is not an efficient
    * implementation and results in large numbers of boxing objects being created unnecessarily.
    * Subclasses of Input should provide their own implementations of writeBlock.
    *
    * @param array The array into which content should be read
    * @param offset The offset to the position within the array that data should start to be
    *        written.  Defaults to the start of the array.
    * @param length The length of data to read from the stream into the array.
    * @return The number of items of data transferred */
  def readBlock(array: Array[Data], offset: Int = 0, length: Int = -1): Int = {

    val end = if(length < 0) (array.length - offset) else (offset + length)

    read() match {
      case None => -1
      case Some(c) =>
        array(offset) = c
        var i = offset + 1
        var continue = true
        while(i < end && continue) {
          read() match {
            case None =>
              continue = false
            case Some(c) =>
              array(i) = c
              i += 1
          }
        }

        i - offset
    }
  }

  /** Closes the input stream so that no further data will be provided. */
  def close(): Unit
  
  /** Pumps data from this `Input` to the specified `Output` until the end of the stream is
    * reached.
    *
    * @param out The output stream to receive data pumped from this `Input` */
  def pumpTo(out: Output[Data])(implicit mf: ClassTag[Data]): Int = {
    val buf = new Array[Data](65536)
    var len = readBlock(buf)
    var count = 0
    while(len >= 0) {
      out.writeBlock(buf, length = len)
      count += len
      len = readBlock(buf)
    }
    count
  }

  def map[T](fn: Data => T): Input[T] = new Input[T] {
    def read(): Option[T] = thisInput.read().map(fn)
    def ready(): Boolean = thisInput.ready()
    def close(): Unit = thisInput.close()
  }

  /** Maps elements of the input stream to zero, one or many elements, producing a new input
    * stream. */
  def flatMap[T](fn: Data => Seq[T]): Input[T] = new Input[T] {
    private var buf: Seq[T] = Nil
    private var cur = 0
    private var avail = 0
    
    def read(): Option[T] = if(cur == avail) {
      cur = 0
      avail = 0
      thisInput.read().map(fn) match {
        case None => None
        case Some(xs) =>
          if(xs.isEmpty) read()
          else if(xs.length == 1) xs.headOption
          else {
            avail = xs.length
            cur += 1
            buf = xs
            xs.headOption
          }
      }
    } else {
      cur += 1
      Some(buf(cur - 1))
    }
    
    def ready(): Boolean = cur < avail || thisInput.ready()
    
    def close(): Unit = {
      cur = 0
      avail = 0
      thisInput.close()
    }
  }

  override def foreach[U](fn: Data => U): Unit = {
    var next: Option[Data] = read()
    while(next != None) {
      fn(next.get)
      next = read()
    }
  }
}

/** Defines a generic output stream */
trait Output[@specialized(Byte, Char) Data] {
  private var beingHandled = false
 
  /** Writes one item of data to this stream
    *
    * @param data The data to be written */
  def write(data: Data): Unit

  /** Writes a block of data from an array to the stream
    *
    * The basic implementation is provided for convenience, though it is not an efficient
    * implementation and results in large numbers of boxing objects being created unnecessarily.
    * Subclasses of Output should provide their own implementations of writeBlock.
    *
    * @param array the Array containing the data to be written to the stream
    * @param offset The offset to the position within the array that data should start to be
    *        read from when writing to the stream.  Defaults to the start of the array.
    * @param length The length of data to write from the array into the stream. Defaults to the
    *        remainder of the array.
    * @return The number of data items written. */
  def writeBlock(array: Array[Data], offset: Int = 0, length: Int = -1): Int = {
    
    val end = if(length < 0) (array.length - offset) else (offset + length)
    array.slice(offset, end).foreach(write)

    end - offset
  }
 
  /** Flushes the stream */
  def flush(): Unit

  /** Closes the stream */
  def close(): Unit
}

/** Generic type class for reading a particular kind of data from 
  */
@implicitNotFound(msg = "Cannot find implicit Reader for ${UrlType} resources. "+
    "${UrlType} resources can only be read if a Reader implicit exists within scope. "+
    "Note that if you are working with Char data, you will require an implicit character "+
    "encoding, e.g. import encodings.system or import encodings.`UTF-8`.")
trait Reader[-UrlType, @specialized(Byte, Char) Data] {
  
  implicit private val errorHandler = raw

  def doNotClose = false

  /** Creates the `Input` for streaming data of the specified type from the given URL
    *
    * @param url The URL to get the input stream from
    * @return an `Input[Data]` for the specified URL */
  def input(url: UrlType)(implicit mode: Mode[IoMethods]): mode.Wrap[Input[Data], Exception]
  
  /** Pumps data from the specified URL to the given destination URL */
  def pump[DestUrlType <: Url[DestUrlType]](url: UrlType, dest: DestUrlType)(implicit sw:
      Writer[DestUrlType, Data], mf: ClassTag[Data]): Int =
    input(url) pumpTo sw.output(dest)
}

/** Type class object for reading `Char`s from a `String` */
object StringCharReader extends Reader[String, Char] {
  def input(s: String)(implicit mode: Mode[IoMethods]): mode.Wrap[Input[Char], Exception] =
    mode.wrap(StringIsInput(s))
}

/** Type class object for reading `Byte`s from a `Array[Byte]` */
object ByteArrayReader extends Reader[Array[Byte], Byte] {
  def input(s: Array[Byte])(implicit mode: Mode[IoMethods]): mode.Wrap[Input[Byte], Exception] =
    mode.wrap(ByteArrayInput(s))
}

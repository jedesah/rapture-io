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
import rapture.codec._
import rapture.uri._
import java.util.zip._
import java.io._
import language.higherKinds

import language.experimental.macros

trait IoMethods extends ModeGroup

trait LowPriorityImplicits {
  implicit val byteAccumulator = ByteAccumulator
  
  implicit val stringAccumulator = StringAccumulator

  implicit def stringByteReader(implicit encoding: Encoding): Reader[String, Byte] =
    new Reader[String, Byte] {
      def input(s: String)(implicit mode: Mode[IoMethods]): mode.Wrap[Input[Byte], Exception] =
        mode.wrap(ByteArrayInput(s.getBytes(encoding.name)))
    }
}

object `package` extends LowPriorityImplicits {
  
  private implicit val errorHandler = raw
  
  implicit def inputStreamReader[T, I[T] <: Input[T]]: Reader[I[T], T] =
    new Reader[I[T], T] {
      def input(in: I[T])(implicit mode: Mode[IoMethods]): mode.Wrap[Input[T], Exception] =
        mode.wrap(in) 
    }

  implicit def byteToLineReaders[T](implicit jisr: JavaInputStreamReader[T],
      encoding: Encoding): Reader[T, String] = new Reader[T, String] {
    def input(t: T)(implicit mode: Mode[IoMethods]): mode.Wrap[Input[String], Exception] =
      mode.wrap(new LineInput(new InputStreamReader(jisr.getInputStream(t))))
  }

  implicit def byteToLineWriters[T](implicit jisw: JavaOutputStreamWriter[T],
      encoding: Encoding): Writer[T, String] = new Writer[T, String] {
    def output(t: T)(implicit mode: Mode[IoMethods]): mode.Wrap[Output[String], Exception] =
      mode.wrap(new LineOutput(new OutputStreamWriter(jisw.getOutputStream(t))))
  }

  implicit def byteToCharReaders[T](implicit jisr: JavaInputStreamReader[T],
      encoding: Encoding): Reader[T, Char] = new Reader[T, Char] {
    def input(t: T)(implicit mode: Mode[IoMethods]): mode.Wrap[Input[Char], Exception] =
      mode.wrap(new CharInput(new InputStreamReader(jisr.getInputStream(t))))
  }

  implicit def byteToCharWriters[T](implicit jisw: JavaOutputStreamWriter[T],
      encoding: Encoding): Writer[T, Char] = new Writer[T, Char] {
    def output(t: T)(implicit mode: Mode[IoMethods]): mode.Wrap[Output[Char], Exception] =
      mode.wrap(new CharOutput(new OutputStreamWriter(jisw.getOutputStream(t))))
  }

  implicit def stringInputBuilder(implicit encoding: Encoding): InputBuilder[InputStream,
      String] =
    new InputBuilder[InputStream, String] {
      def input(s: InputStream)(implicit mode: Mode[IoMethods]): mode.Wrap[Input[String], Exception] =
        mode.wrap(new LineInput(new InputStreamReader(s, encoding.name)))
    }

  implicit def stringOutputBuilder(implicit encoding: Encoding):
      OutputBuilder[OutputStream, String] =
    new OutputBuilder[OutputStream, String] {
      def output(s: OutputStream)(implicit mode: Mode[IoMethods]): mode.Wrap[
          Output[String], Exception] =
        mode.wrap(new LineOutput(new OutputStreamWriter(s, encoding.name)))
    }

  implicit def resourceBytes[Res](res: Res)(implicit sr: Reader[Res, Byte]): Bytes =
    slurpable(res).slurp[Byte]

  /** Views an `Input[Byte]` as a `java.io.InputStream` */
  implicit def inputStreamUnwrapper(is: Input[Byte]): InputStream =
    new InputStream { def read() = is.read().map(_.toInt).getOrElse(-1) }

  /** Type class definition for creating an Output[Char] from a Java OutputStream, taking an
    * [[Encoding]] implicitly for converting between `Byte`s and `Char`s */
  implicit def outputStreamCharBuilder(implicit encoding: Encoding):
      OutputBuilder[OutputStream, Char] =
    new OutputBuilder[OutputStream, Char] {
      def output(s: OutputStream)(implicit mode: Mode[IoMethods]):
          mode.Wrap[Output[Char], Exception] =
        mode.wrap(new CharOutput(new OutputStreamWriter(s, encoding.name)))
    }

  /** Type class definition for creating an Input[Char] from a Java InputStream, taking an
    * [[Encoding]] implicitly for converting between `Byte`s and `Char`s */
  implicit def inputStreamCharBuilder(implicit encoding: Encoding):
      InputBuilder[InputStream, Char] =
    new InputBuilder[InputStream, Char] {
      def input(s: InputStream)(implicit mode: Mode[IoMethods]): mode.Wrap[Input[Char], Exception] =
        mode.wrap(new CharInput(new InputStreamReader(s, encoding.name)))
    }

  implicit def stdoutWriter[Data]: Writer[Stdout[Data], Data] =
      new Writer[Stdout[Data], Data] {
    override def doNotClose = true
    def output(stdout: Stdout[Data])(implicit mode: Mode[IoMethods]):
        mode.Wrap[Output[Data], Exception] = mode.wrap(stdout.output)
  }

  implicit def stderrWriter[Data]: Writer[Stderr[Data], Data] =
    new Writer[Stderr[Data], Data] {
      override def doNotClose = true
      def output(stderr: Stderr[Data])(implicit mode: Mode[IoMethods]):
          mode.Wrap[Output[Data], Exception] = mode.wrap[Output[Data], Exception](stderr.output)
  }

  implicit def stdin[Data]: Reader[Stdin[Data], Data] =
    new Reader[Stdin[Data], Data] {
      override def doNotClose = true
      def input(stdin: Stdin[Data])(implicit mode: Mode[IoMethods]):
          mode.Wrap[Input[Data], Exception] = mode.wrap[Input[Data], Exception](stdin.input)
    }

  def DevNull[T]: Output[T] = new Output[T] {
    def close() = ()
    def flush() = ()
    def write(t: T) = ()
  }

  implicit val buildInputStream: InputBuilder[InputStream, Byte] = InputStreamBuilder
  implicit val buildOutputStream: OutputBuilder[OutputStream, Byte] = OutputStreamBuilder
  implicit val buildReader: InputBuilder[java.io.Reader, Char] = ReaderBuilder
  implicit val buildLineReader: InputBuilder[java.io.Reader, String] = LineReaderBuilder
  implicit val buildWriter: OutputBuilder[java.io.Writer, Char] = WriterBuilder

  implicit val charAccumulator = CharAccumulator

  implicit val buildAppender: AppenderBuilder[java.io.Writer, Char] = AppenderBuilder
  implicit val stringCharReader: Reader[String, Char] = StringCharReader
  implicit val byteArrayReader: Reader[Array[Byte], Byte] = ByteArrayReader
  implicit val bytesReader: Reader[Bytes, Byte] = BytesReader

  implicit val classpathStreamByteReader: JavaInputStreamReader[ClasspathUrl] =
    ClasspathStreamByteReader

  def ensuring[Result, Stream](create: Stream)(body: Stream => Result)(close: Stream => Unit):
      Result = Utils.ensuring[Result, Stream](create)(body)(close)

  
  
  implicit def stringMethods(s: String): StringMethods = new StringMethods(s)

  implicit def copyable[Res](res: Res): Copyable.Capability[Res] = new Copyable.Capability[Res](res)
  implicit def appendable[Res](res: Res): Appendable.Capability[Res] = new Appendable.Capability[Res](res)
  implicit def readable[Res](res: Res): Readable.Capability[Res] = new Readable.Capability[Res](res)
  implicit def deletable[Res](res: Res): Deletable.Capability[Res] = new Deletable.Capability[Res](res)
  implicit def slurpable[Res](res: Res): Slurpable.Capability[Res] = new Slurpable.Capability[Res](res)
  implicit def writable[Res](res: Res): Writable.Capability[Res] = new Writable.Capability[Res](res)
  implicit def movable[Res](res: Res): Movable.Capability[Res] = new Movable.Capability[Res](res)
  implicit def sizable[Res](res: Res): Sizable.Capability[Res] = new Sizable.Capability[Res](res)
  
}

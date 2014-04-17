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
import java.util.zip._
import java.io._
import language.higherKinds

import language.experimental.macros

trait LowPriorityImplicits {
  implicit val byteAccumulator = ByteAccumulator
  
  implicit val stringAccumulator = StringAccumulator

  implicit def stringByteReader(implicit encoding: Encoding): StreamReader[String, Byte] =
    new StreamReader[String, Byte] {
      def input(s: String)(implicit eh: ExceptionHandler): eh.![Input[Byte], Exception] =
        eh.wrap(ByteArrayInput(s.getBytes(encoding.name)))
    }
}

object `package` extends LowPriorityImplicits {
  
  private implicit val errorHandler = raw
  
  implicit def inputStreamReader[T, I[T] <: Input[T]]: StreamReader[I[T], T] =
    new StreamReader[I[T], T] {
      def input(in: I[T])(implicit eh: ExceptionHandler): eh.![Input[T], Exception] =
        eh.wrap(in) 
    }

  /** Provides methods for URLs which can be written to as streams, most importantly for getting
    * an `Output` */
  implicit def makeWritable[Res](url: Res): Writable[Res] =
    new Writable[Res](url)

  implicit def byteToLineReaders[T](implicit jisr: JavaInputStreamReader[T],
      encoding: Encoding): StreamReader[T, String] = new StreamReader[T, String] {
    def input(t: T)(implicit eh: ExceptionHandler): eh.![Input[String], Exception] =
      eh.wrap(new LineInput(new InputStreamReader(jisr.getInputStream(t))))
  }

  implicit def byteToLineWriters[T](implicit jisw: JavaOutputStreamWriter[T],
      encoding: Encoding): StreamWriter[T, String] = new StreamWriter[T, String] {
    def output(t: T)(implicit eh: ExceptionHandler): eh.![Output[String], Exception] =
      eh.wrap(new LineOutput(new OutputStreamWriter(jisw.getOutputStream(t))))
  }

  implicit def byteToCharReaders[T](implicit jisr: JavaInputStreamReader[T],
      encoding: Encoding): StreamReader[T, Char] = new StreamReader[T, Char] {
    def input(t: T)(implicit eh: ExceptionHandler): eh.![Input[Char], Exception] =
      eh.wrap(new CharInput(new InputStreamReader(jisr.getInputStream(t))))
  }

  implicit def byteToCharWriters[T](implicit jisw: JavaOutputStreamWriter[T],
      encoding: Encoding): StreamWriter[T, Char] = new StreamWriter[T, Char] {
    def output(t: T)(implicit eh: ExceptionHandler): eh.![Output[Char], Exception] =
      eh.wrap(new CharOutput(new OutputStreamWriter(jisw.getOutputStream(t))))
  }

  implicit def stringInputBuilder(implicit encoding: Encoding): InputBuilder[InputStream,
      String] =
    new InputBuilder[InputStream, String] {
      def input(s: InputStream)(implicit eh: ExceptionHandler): eh.![Input[String], Exception] =
        eh.wrap(new LineInput(new InputStreamReader(s, encoding.name)))
    }

  implicit def stringOutputBuilder(implicit encoding: Encoding):
      OutputBuilder[OutputStream, String] =
    new OutputBuilder[OutputStream, String] {
      def output(s: OutputStream)(implicit eh: ExceptionHandler): eh.![
          Output[String], Exception] =
        eh.wrap(new LineOutput(new OutputStreamWriter(s, encoding.name)))
    }

  /** Views an `Input[Byte]` as a `java.io.InputStream` */
  implicit def inputStreamUnwrapper(is: Input[Byte]): InputStream =
    new InputStream { def read() = is.read().map(_.toInt).getOrElse(-1) }

  /** Type class definition for creating an Output[Char] from a Java OutputStream, taking an
    * [[Encoding]] implicitly for converting between `Byte`s and `Char`s */
  implicit def outputStreamCharBuilder(implicit encoding: Encoding):
      OutputBuilder[OutputStream, Char] =
    new OutputBuilder[OutputStream, Char] {
      def output(s: OutputStream)(implicit eh: ExceptionHandler):
          eh.![Output[Char], Exception] =
        eh.wrap(new CharOutput(new OutputStreamWriter(s, encoding.name)))
    }

  /** Type class definition for creating an Input[Char] from a Java InputStream, taking an
    * [[Encoding]] implicitly for converting between `Byte`s and `Char`s */
  implicit def inputStreamCharBuilder(implicit encoding: Encoding):
      InputBuilder[InputStream, Char] =
    new InputBuilder[InputStream, Char] {
      def input(s: InputStream)(implicit eh: ExceptionHandler): eh.![Input[Char], Exception] =
        eh.wrap(new CharInput(new InputStreamReader(s, encoding.name)))
    }

  implicit def stdoutWriter[Data]: StreamWriter[Stdout[Data], Data] =
      new StreamWriter[Stdout[Data], Data] {
    override def doNotClose = true
    def output(stdout: Stdout[Data])(implicit eh: ExceptionHandler):
        eh.![Output[Data], Exception] = eh.wrap(stdout.output)
  }

  implicit def stderrWriter[Data]: StreamWriter[Stderr[Data], Data] =
    new StreamWriter[Stderr[Data], Data] {
      override def doNotClose = true
      def output(stderr: Stderr[Data])(implicit eh: ExceptionHandler):
          eh.![Output[Data], Exception] = eh.wrap[Output[Data], Exception](stderr.output)
  }

  implicit def stdin[Data]: StreamReader[Stdin[Data], Data] =
    new StreamReader[Stdin[Data], Data] {
      override def doNotClose = true
      def input(stdin: Stdin[Data])(implicit eh: ExceptionHandler):
          eh.![Input[Data], Exception] = eh.wrap[Input[Data], Exception](stdin.input)
    }

  def DevNull[T]: Output[T] = new Output[T] {
    def close() = ()
    def flush() = ()
    def write(t: T) = ()
  }

  type AfterPath = Map[Char, (String, Double)]
  
  implicit val buildInputStream: InputBuilder[InputStream, Byte] = InputStreamBuilder
  implicit val buildOutputStream: OutputBuilder[OutputStream, Byte] = OutputStreamBuilder
  implicit val buildReader: InputBuilder[Reader, Char] = ReaderBuilder
  implicit val buildLineReader: InputBuilder[Reader, String] = LineReaderBuilder
  implicit val buildWriter: OutputBuilder[Writer, Char] = WriterBuilder

  implicit val charAccumulator = CharAccumulator

  implicit val buildAppender: AppenderBuilder[Writer, Char] = AppenderBuilder
  implicit val stringCharReader: StreamReader[String, Char] = StringCharReader
  implicit val byteArrayReader: StreamReader[Array[Byte], Byte] = ByteArrayReader

  implicit val classpathStreamByteReader: JavaInputStreamReader[ClasspathUrl] =
    ClasspathStreamByteReader

  def ensuring[Result, Stream](create: Stream)(body: Stream => Result)(close: Stream => Unit):
      Result = Utils.ensuring[Result, Stream](create)(body)(close)

  implicit def slurpable[Res](url: Res): Slurpable[Res] = new Slurpable[Res](url)
  
  implicit def appendable[Res](url: Res): Appendable[Res] = new Appendable[Res](url)
  
  implicit def readable[Res](url: Res): Readable[Res] = new Readable[Res](url)
  implicit def stringMethods(s: String): StringMethods = new StringMethods(s)
  
}

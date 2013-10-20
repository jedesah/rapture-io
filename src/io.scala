/**********************************************************************************************\
* Rapture I/O Library                                                                          *
* Version 0.8.2                                                                                *
*                                                                                              *
* The primary distribution site is                                                             *
*                                                                                              *
*   http://rapture.io/                                                                         *
*                                                                                              *
* Copyright 2010-2013 Propensive Ltd.                                                          *
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
package rapture

import implementation._

import language.higherKinds

import scala.reflect.ClassTag
import scala.concurrent._
import scala.concurrent.duration._

import java.io._
import java.net._

/** Combines different elements of the I/O framework.  This class provides implementations of
  * type class objects which should be given higher priority than the defaults.  This allows
  * methods which stream from URLs which have alternative means of being read to favour one type
  * of stream over another without explicitly specifying a type parameter.  Specifically,
  * `FileUrl`s should be read and written and  `HttpUrl`s should be read as
  * byte-streams */
class BaseIo extends FileHandling with Extracting with MimeHandling with JsonProcessing with
    Encrypting with Encodings with IpHandling with Logging with MimeTyping with Misc with Linking
    with ClasspathHandling with Finance with FtpHandling with
    Emailing with Generating with Zipping with Browsing {

  /** Type class object for reading `Byte`s from `FileUrl`s */
  implicit object FileStreamByteReader extends JavaInputStreamReader[FileUrl](f =>
      new FileInputStream(f.javaFile))

  /** Type class object for reading `Byte`s from `HttpUrl`s */
  implicit object HttpStreamByteReader extends JavaInputStreamReader[HttpUrl](
      _.javaConnection.getInputStream)

  implicit val procByteStreamReader =
    new JavaInputStreamReader[Proc](_.process.getInputStream)

  implicit val procByteStreamWriter =
    new JavaOutputStreamWriter[Proc](_.process.getOutputStream)

  implicit def stdoutWriter[Data] = new StreamWriter[Stdout[Data], Data] {
    override def doNotClose = true
    def output(stdout: Stdout[Data])(implicit eh: ExceptionHandler): eh.![Exception, Output[Data]] =
      eh.except[Exception, Output[Data]](stdout.output)
  }

  implicit def stderrWriter[Data] = new StreamWriter[Stderr[Data], Data] {
    override def doNotClose = true
    def output(stderr: Stderr[Data])(implicit eh: ExceptionHandler): eh.![Exception, Output[Data]] =
      eh.except[Exception, Output[Data]](stderr.output)
  }

  implicit def stdin[Data] = new StreamReader[Stdin[Data], Data] {
    override def doNotClose = true
    def input(stdin: Stdin[Data])(implicit eh: ExceptionHandler): eh.![Exception, Input[Data]] =
      eh.except[Exception, Input[Data]](stdin.input)
  }

  def randomGuid() = java.util.UUID.randomUUID().toString

  def DevNull[T] = new Output[T] {
    def close() = ()
    def flush() = ()
    def write(t: T) = ()
  }

  implicit object HttpFileUrlLinkable extends Linkable[FileUrl, HttpUrl] {
    type Result = HttpUrl
    def link(src: FileUrl, dest: HttpUrl) = dest
  }

  implicit object HttpFileUrlsLinkable extends Linkable[HttpUrl, FileUrl] {
    type Result = FileUrl
    def link(src: HttpUrl, dest: FileUrl) = dest
  }


  object JavaResources {
    import language.reflectiveCalls
    
    type StructuralReadable = { def getInputStream(): InputStream }
    type StructuralWritable = { def getOutputStream(): OutputStream }
    
    implicit val structuralReader =
      new JavaInputStreamReader[StructuralReadable](_.getInputStream())
    
    implicit val structuralWriter =
      new JavaOutputStreamWriter[StructuralWritable](_.getOutputStream())
  
    implicit val javaFileReader = new JavaInputStreamReader[java.io.File](f =>
        new java.io.FileInputStream(f))
    
    implicit val javaFileWriter = new JavaOutputStreamWriter[java.io.File](f =>
        new java.io.FileOutputStream(f))
    
    implicit val javaFileAppender = new JavaOutputStreamAppender[java.io.File](f =>
        new java.io.FileOutputStream(f, true))
  }

}

object io extends BaseIo

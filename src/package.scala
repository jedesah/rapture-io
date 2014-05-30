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

object `package` {
  
  /** Views an `Input[Byte]` as a `java.io.InputStream` */
  implicit def inputStreamUnwrapper(is: Input[Byte]): InputStream =
    new InputStream { def read() = is.read().map(_.toInt).getOrElse(-1) }

  implicit val classpathStreamByteReader: JavaInputStreamReader[ClasspathUrl] =
    ClasspathStreamByteReader

  def ensuring[Result, Stream](create: Stream)(body: Stream => Result)(close: Stream => Unit):
      Result = Utils.ensuring[Result, Stream](create)(body)(close)

  
  
  implicit def stringMethods(s: String): StringMethods = new StringMethods(s)

  implicit def copyable[Res](res: Res): Copyable.Capability[Res] =
    new Copyable.Capability[Res](res)
  
  implicit def appendable[Res](res: Res): Appendable.Capability[Res] =
    new Appendable.Capability[Res](res)
  
  implicit def readable[Res](res: Res): Readable.Capability[Res] =
    new Readable.Capability[Res](res)
  
  implicit def deletable[Res](res: Res): Deletable.Capability[Res] =
    new Deletable.Capability[Res](res)
  
  implicit def slurpable[Res](res: Res): Slurpable.Capability[Res] =
    new Slurpable.Capability[Res](res)
  
  implicit def writable[Res](res: Res): Writable.Capability[Res] =
    new Writable.Capability[Res](res)
  
  implicit def movable[Res](res: Res): Movable.Capability[Res] =
    new Movable.Capability[Res](res)
  
  implicit def sizable[Res](res: Res): Sizable.Capability[Res] =
    new Sizable.Capability[Res](res)
  
}

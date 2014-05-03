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

import scala.reflect._

import java.io._


/** Interface for an accumulator which is a special kind of output which collects and stores all
  * input in a buffer which can be retrieved afterwards.  No guarantees are made about input
  * supplied after the buffer has been retrieved.
  *
  * @tparam Data The type of data to be accumulated
  * @tparam Acc The type into which the data will be accumulated */
trait Accumulator[Data, Acc] extends Output[Data] { def buffer: Acc }

/** Defines a trait for creating new `Accumulator`s */
trait AccumulatorBuilder[T] {
  type Out
  def make(): Accumulator[T, Out]
}

/** Collects `Byte`s into an `Array[Byte]` */
class ByteArrayOutput extends {
  private val baos = new ByteArrayOutputStream
} with ByteOutput(baos) with Accumulator[Byte, Array[Byte]] {
  def buffer: Array[Byte] = baos.toByteArray
}

/** Collects `String`s into another `String` */
class LinesOutput extends {
  private val sw = new StringWriter
} with LineOutput(sw) with Accumulator[String, String] {
  def buffer: String = sw.toString
}

/** Type class object for creating an accumulator Bytes into an `Array` of `Byte`s */
object ByteAccumulator extends AccumulatorBuilder[Byte] {
  type Out = Array[Byte]
  def make() = new ByteArrayOutput
}

/** Type class object for creating an accumulator of `String`s */
object StringAccumulator extends AccumulatorBuilder[String] {
  type Out = String
  def make() = new LinesOutput
}

/** Type class object for creating an accumulator of `Char`s into a `String` */
object CharAccumulator extends AccumulatorBuilder[Char] {
  type Out = String
  def make() = new StringOutput
}

/** Collects `Char`s into a `String` */
class StringOutput extends {
  private val sw = new StringWriter
} with CharOutput(sw) with Accumulator[Char, String] {
  def buffer: String = sw.toString
}

class Slurpable[UrlType](url: UrlType) {
  /** Reads in the entirety of the stream and accumulates it into an appropriate object
    * depending on the availability of implicit Accumulator type class objects in scope.
    *
    * @usecase def slurp[Char](): String
    * @usecase def slurp[Byte](): Array[Byte]
    * @tparam Data The units of data being slurped
    * @return The accumulated data */
  def slurp[Data]()(implicit accumulatorBuilder: AccumulatorBuilder[Data], rts: Rts[IoMethods],
      sr: StreamReader[UrlType, Data], mf: ClassTag[Data]): rts.Wrap[accumulatorBuilder.Out, Exception] =
    rts.wrap {
      val c = accumulatorBuilder.make()
      url.handleInput[Data, Int](_ pumpTo c)
      c.buffer
    }
}

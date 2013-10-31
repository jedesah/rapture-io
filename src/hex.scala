/**********************************************************************************************\
* Rapture I/O Library                                                                          *
* Version 0.9.0                                                                                *
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
package rapture.implementation
import rapture._
import rapture.core._

trait Hex {

  object Hex {
    def encode(a: Array[Byte]): String =
      new String(a flatMap { n => Array((n & 255) >> 4 & 15, n & 15) } map { _ + 48 } map { i =>
        (if(i > 57) i + 39 else i).toChar })

    def decode(s: String)(implicit eh: ExceptionHandler): eh.![Exception, Array[Byte]] = eh.except {
      (if(s.length%2 == 0) s else "0"+s).to[Array].grouped(2).to[Array] map { case Array(l, r) =>
        (((l - 48)%39 << 4) + (r - 48)%39).toByte
      }
    }
  }

}

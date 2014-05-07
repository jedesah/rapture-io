/**********************************************************************************************\
* Rapture I/O Library                                                                          *
* Version 0.10.0                                                                               *
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

object Copyable {
  class Capability[FromType](from: FromType) {
    def copyTo[ToType](to: ToType)(implicit rts: Rts[IoMethods],
        copyable: Copyable[FromType, ToType]): rts.Wrap[Long, Exception] =
      rts.wrap(?[Copyable[FromType, ToType]].copy(from, to))
  }
}

trait Copyable[FromType, ToType] {
  def copy(from: FromType, to: ToType): Long
}


object Sizable {
  class Capability[UrlType: Sizable](url: UrlType) {
    /** Returns the size in bytes of this URL */
    def size(implicit rts: Rts[IoMethods]): rts.Wrap[Long, Exception] =
      rts wrap ?[Sizable[UrlType]].size(url)
  }
}

trait Sizable[Res] {
  type ExceptionType <: Exception
  /** Returns the size in bytes of the specified URL */
  def size(res: Res): Long
}



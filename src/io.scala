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
package rapture.io

import rapture.core._

import language.higherKinds

import scala.reflect.ClassTag
import scala.concurrent._
import scala.concurrent.duration._

import java.io._
import java.net._

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

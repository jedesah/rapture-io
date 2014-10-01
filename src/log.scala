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

import scala.annotation.elidable
import scala.reflect._
import scala.collection.mutable._

/** Basic logging functionality, introducing the concept of logging zones. Note that this is
  * almost certainly not as efficient as it ought to be, so use something else if efficiency
  * matters to you. */

case class Zone(name: String)

case class Level(level: Int, name: String)
object Trace extends Level(6, "trace")
object Debug extends Level(5, "debug")
object Info extends Level(4, "info")
object Warn extends Level(3, "warn")
object Error extends Level(2, "error")
object Fatal extends Level(1, "fatal")

trait Logger { def log(msg: String, level: Level, zone: Zone) }

case object StdoutLogger extends Logger {
  def log(msg: String, level: Level, zone: Zone) = println(msg)
}

object log {

  implicit val zone = Zone("logger")
  
  val listeners: HashSet[(Logger, Level, Map[Zone, Level])] = new HashSet()

  val df = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
  var dateString = ""
  var dateCreated = 0L

  def listen(logger: Logger, level: Level = Info, spec: Map[Zone, Level] = Map()): Unit = {
    info("Registering listener")
    listeners += ((logger, level, spec))
  }

  def unlisten(logger: Logger) = synchronized {
    listeners.find(_._1 == logger) foreach (listeners -= _)
  }

  @inline
  @elidable(1)
  def trace(msg: => String)(implicit zone: Zone) =
    log(Trace, zone, msg)
  
  @inline
  @elidable(2)
  def debug(msg: => String)(implicit zone: Zone) =
    log(Debug, zone, msg)
  
  @inline
  @elidable(3)
  def info(msg: => String)(implicit zone: Zone) =
    log(Info, zone, msg)
  
  @inline
  @elidable(4)
  def warn(msg: => String)(implicit zone: Zone) =
    log(Warn, zone, msg)
  
  @inline
  @elidable(5)
  def error(msg: => String)(implicit zone: Zone) =
    log(Error, zone, msg)
  
  @inline
  def fatal(msg: => String)(implicit zone: Zone) =
    log(Fatal, zone, msg)
  
  @inline
  def exception(e: => Throwable)(implicit zone: Zone) = {
    log(Error, zone, e.toString)
    log(Debug, zone, "    "+e.getStackTrace.mkString("\n    "))
  }

  private def log(level: Level, zone: Zone, msg: String): Unit = {
    val time: Long = System.currentTimeMillis
    // Ensures the date is only formatted when it changes
    if(time != dateCreated) {
      dateString = df.format(time)
      dateCreated = time
    }
    val m = if(msg == null) "null" else msg
    val z = if(zone == null) "null" else zone.name
    val ln = m.replaceAll("\n", "\n                                       ")
    val formattedMsg = "%1$-23s %2$-5s %3$-8s %4$s".format(dateString, level.name, z, ln)
    
    for((lgr, lvl, spec) <- listeners if spec.getOrElse(zone, lvl).level >= level.level) {
      try lgr.log(formattedMsg, level, zone) catch {
        case e: Exception =>
      }
    }
  }
}

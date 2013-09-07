/**************************************************************************************************
Rapture I/O Library
Version 0.8.0

The primary distribution site is

  http://www.propensive.com/

Copyright 2010-2013 Propensive Ltd.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is
distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing permissions and limitations under the
License.
***************************************************************************************************/

package rapture.implementation

import scala.reflect._
import scala.reflect.api._
import scala.reflect.runtime._
import scala.reflect.macros._

object CaseClassExtraction {
  
  import language.experimental.macros
  
  def materialize[T: c.WeakTypeTag](c: Context): c.Expr[Extractor[T]] = {
    import c.universe._

    //val m = (weakTypeOf[T].declarations collect {
    //  case m: MethodSymbol if m.isPrimaryConstructor => m.asMethod
    //} headOption) getOrElse { throw new RuntimeException("No primary constructor") }

    val extractor = typeOf[Extractor[_]].typeSymbol.asType.toTypeConstructor

    val params = weakTypeOf[T].declarations collect {
      case m: MethodSymbol if m.isCaseAccessor => m.asMethod
    } map { p =>

      val imp = c.Expr[Extractor[_]](c.inferImplicitValue(appliedType(extractor,
          List(p.typeSignature.typeSymbol.asType.toType)), false, false))

      Apply(
        Select(imp.tree, newTermName("construct")),
        List(Apply(
          Select(
            TypeApply(
              Select(Ident(newTermName("json")), newTermName("asInstanceOf")),
              List(AppliedTypeTree(
                Select(Ident(definitions.PredefModule), newTypeName("Map")),
                List(
                  Select(
                    Ident(definitions.PredefModule),
                    newTypeName("String")
                  ),
                  Ident(definitions.AnyClass)
                ))
              )
            ),
            newTermName("apply")
          ),
          List(Literal(Constant(p.name.toString)))
        ))
      )
    }

    val construction = c.Expr(
      New(
        weakTypeOf[T].typeSymbol.asType,
        params.to[List]: _*
      )
    )

    reify(new Extractor[T] { def construct(json: Any): T = construction.splice })
  }
}


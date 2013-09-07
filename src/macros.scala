package rapture.experimental

object caseClassExtractors {
  
  import language.experimental.macros
  import rapture.implementation.Extractor
  import scala.reflect.api._
  import scala.reflect._
  import scala.reflect.runtime._
  import scala.reflect.macros._

  def summonImpl[T: c.WeakTypeTag](c: Context): c.Expr[Extractor[T]] = {
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


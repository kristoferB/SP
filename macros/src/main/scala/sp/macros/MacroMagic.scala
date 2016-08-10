package sp.macros

case class APIDefinition(name: String, parameters: List[APIParameters])
case class APIParameters(param: String,
                         ofType: String,
                         default: Option[String] = None,
                         domain: Option[List[String]] = None)

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context



object MacroMagic {
  def values[A]: List[Class[_]] = macro values_impl[A]

  def info[A]: List[String] = macro jsonMagic[A]

  def values_impl[A: c.WeakTypeTag](c: Context) = {
    import c.universe._

    val tpe = c.weakTypeOf[A].typeSymbol.asClass
    val subClasses = tpe.knownDirectSubclasses

    val names = subClasses.collect{
      case c: ClassSymbol => q"classOf[${c.asType}]"
    }.toList

    q"""
        List[Class[_]](..$names)

      """


  }

  def jsonMagic[A: c.WeakTypeTag](c: Context) = {
    import c.universe._

    val tpe = c.weakTypeOf[A].typeSymbol.asClass
    val subClasses = tpe.knownDirectSubclasses.toList

    // Create APIDefinitions here and return based on case classes
    // get defualt value from case class and domain from predef method domain: Map[String, List[JValue]]



    val classAndParams = subClasses.collect{
      case x: ClassSymbol if x.isCaseClass => {
        val declarations = x.info.decls
        val ctor = declarations.collectFirst {
          case m: MethodSymbol if m.isPrimaryConstructor => m
        }.get
        x -> ctor.paramLists.head
      }
    }.toList


    val classAndDefaultValues = subClasses.collect{
      case classSym: ClassSymbol if classSym.isCaseClass => {
        val moduleSym = classSym.companion
        val apply = moduleSym.typeSignature.decl(TermName("apply")).asMethod
        val kvps = apply.paramLists.head.map(_.asTerm).zipWithIndex.map{ case (p, i) =>
          val pName = p.name.toString
          val default = if (!p.isParamWithDefault) None
                        else Some(q"Some($moduleSym.${TermName("apply$default$" + (i + 1))}.toString)")

          val pType = p.typeSignature

          (q"$pName", q"$pType", default)

        }
        classSym -> kvps

      }
    }



    val json = classAndDefaultValues.flatMap{case (cl, pr) =>
      pr.map{p =>
        val pD = p._3.getOrElse(q"None")
        q""
      }
//      s"""{
//         |'isa' : '${cl.fullName}',
//         |'params': {
//         |${pr.map{p =>
//        s"{'param': ${q"${p._3}"}}"}.mkString(",\n")}
//         |}
//         |}
//       """.stripMargin
    }



    q"""
        List(..$json)

      """
//
//      q"${classAndParams2}.toString"


  }


//  def getDefaults[A: c.WeakTypeTag](c: Context) = {
//    import c.universe._
//
//    val classS = c.weakTypeOf[A].typeSymbol.asClass
//    if (classS.isCaseClass) {
//      val apply = classS.typeSignature.decl(TermName("apply")).asMethod
//    }
//
//  }





}
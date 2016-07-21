package sp.macros

case class APIDefinition(name: String, parameters: List[APIParameters])
case class APIParameters(param: String,
                         ofType: String,
                         default: Option[String],
                         domain: Option[List[String]])

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

object MacroMagic {
  def values[A]: List[Class[_]] = macro values_impl[A]

  def info[A]: String = macro jsonMagic[A]

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

        val classSym = x
        val moduleSym = classSym.companion
        val apply = moduleSym.typeSignature.decl(TermName("apply")).asMethod
        val kvps = apply.paramLists.head.map(_.asTerm).zipWithIndex.flatMap{ case (p, i) =>
          if (!p.isParamWithDefault) None
          else {
            val getterName = TermName("apply$default$" + (i + 1))
            Some(q"${p.name.toString} -> $moduleSym.$getterName")
          }
        }

        val k = q"Map[String, Any](..$kvps).map(_.toString).toList"


        val declarations = x.info.decls
        val ctor = declarations.collectFirst {
          case m: MethodSymbol if m.isPrimaryConstructor => m
        }.get
        x -> ctor.paramLists.head
      }
    }.toList


    val classAndParams2 = subClasses.collect{
      case x: ClassSymbol if x.isCaseClass => {

        val classSym = x
        val moduleSym = classSym.companion
        val apply = moduleSym.typeSignature.decl(TermName("apply")).asMethod
        val kvps = apply.paramLists.head.map(_.asTerm).zipWithIndex.flatMap{ case (p, i) =>
          if (!p.isParamWithDefault) None
          else {
            val getterName = TermName("apply$default$" + (i + 1))
            Some(q"${p.name.toString} -> $moduleSym.$getterName")
          }
        }

        q"Map[String, Any](..$kvps)"

      }
    }



    val json = classAndParams.map{case (cl, pr) =>
      s"""{
         |'isa' : '${cl.fullName}',
         |'params': {
         |${pr.map{p =>
        s"{'param': '${p.name}', 'ofType': '${p.typeSignature}'}"}.mkString(",\n")}
         |}
         |}
       """.stripMargin
    }



    q"""
        List[String](..$json)

      """

      q"${classAndParams2}.toString"


  }




}
package sp.macros

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context



object MacroMagic {
  def values[A]: List[Class[_]] = macro values_impl[A]

  def info[A, B]: List[String] = macro jsonMagic[A, B]

  import scala.reflect.runtime.{universe => ru}
  def getMeClazzes[A: ru.TypeTag] = {
    val tpe = ru.weakTypeOf[A].typeSymbol.asClass
    val mi = ru.runtimeMirror(this.getClass.getClassLoader)
    val subClasses: List[Class[_]] = tpe.knownDirectSubclasses.toList.map(c => mi.runtimeClass(c.asClass))
    subClasses
  }






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



  def jsonMagic[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context) = {
    import c.universe._

    val tpe = c.weakTypeOf[A].typeSymbol.asClass
    val subClasses = tpe.knownDirectSubclasses.toList

    val tpeSupport = c.weakTypeOf[B].typeSymbol.asClass
    val supportClasses = tpeSupport.knownDirectSubclasses.toList


    def packageName(sym: Symbol) = {
      def enclosingPackage(sym: Symbol): Symbol = {
        if (sym == NoSymbol) NoSymbol
        else if (sym.isPackage) sym
        else enclosingPackage(sym.owner)
      }
      val pkg = enclosingPackage(sym)
      if (!pkg.isPackage) ""
      else pkg.fullName
    }

    def typeName(sym: Symbol) = {
      val pkg = packageName(sym)
      sym.fullName.replaceFirst(pkg+".", "")
    }

    val classAndDefaultValues = subClasses.collect{
      case classSym: ClassSymbol if classSym.isCaseClass => {
        val moduleSym = classSym.companion
        val apply = moduleSym.typeSignature.decl(TermName("apply")).asMethod
        val kvps = apply.paramLists.head.map(_.asTerm).zipWithIndex.map{ case (p, i) =>
          val pName = p.name.toString
          val pType = typeName(p.typeSignature.typeSymbol)

          // Need to pick this out by calling the apply default from here
//          val default = if (!p.isParamWithDefault) None else {
//            Some(q"Some($moduleSym.${TermName("apply$default$" + (i + 1))}.toString)")
//          }

          s""" "$pName" : {
              |"key" : "$pName",
              |"ofType" : "$pType"
             |}""".stripMargin
        }

        val cName = typeName(classSym)

        s"""{
           |"isa" : "${cName}",
           |${kvps.mkString(",")}}
           |""".stripMargin

      }
    }


    val sub = supportClasses.collect{
      case classSym: ClassSymbol if classSym.isCaseClass => {
        val moduleSym = classSym.companion
        val apply = moduleSym.typeSignature.decl(TermName("apply")).asMethod
        val kvps = apply.paramLists.head.map(_.asTerm).zipWithIndex.map{ case (p, i) =>
          val pName = p.name.toString
          val pType = typeName(p.typeSignature.typeSymbol)

          s""" "$pName" : {
              |  "key" : "$pName",
              |  "ofType" : "$pType"
              |}""".stripMargin


        }

        val cName = typeName(classSym)

        s""" "${cName}" : {${kvps.mkString(",")}} """.stripMargin

      }
    }

    val sJson = classAndDefaultValues :+ s""" {"subs": {${sub.mkString(",")}}}"""

    q"""
        List(..${sJson})

      """


  }






}
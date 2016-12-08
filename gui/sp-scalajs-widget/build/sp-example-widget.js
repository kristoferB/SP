(function(){
'use strict';
/* Scala.js runtime support
 * Copyright 2013 LAMP/EPFL
 * Author: Sébastien Doeraene
 */

/* ---------------------------------- *
 * The top-level Scala.js environment *
 * ---------------------------------- */





// Get the environment info
var $env = (typeof __ScalaJSEnv === "object" && __ScalaJSEnv) ? __ScalaJSEnv : {};

// Global scope
var $g =
  (typeof $env["global"] === "object" && $env["global"])
    ? $env["global"]
    : ((typeof global === "object" && global && global["Object"] === Object) ? global : this);
$env["global"] = $g;

// Where to send exports



var $e =
  (typeof $env["exportsNamespace"] === "object" && $env["exportsNamespace"])
    ? $env["exportsNamespace"] : $g;

$env["exportsNamespace"] = $e;

// Freeze the environment info
$g["Object"]["freeze"]($env);

// Linking info - must be in sync with scala.scalajs.runtime.LinkingInfo
var $linkingInfo = {
  "envInfo": $env,
  "semantics": {




    "asInstanceOfs": 1,










    "moduleInit": 2,





    "strictFloats": false,




    "productionMode": false

  },



  "assumingES6": false,

  "linkerVersion": "0.6.13"
};
$g["Object"]["freeze"]($linkingInfo);
$g["Object"]["freeze"]($linkingInfo["semantics"]);

// Snapshots of builtins and polyfills






var $imul = $g["Math"]["imul"] || (function(a, b) {
  // See https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Math/imul
  var ah = (a >>> 16) & 0xffff;
  var al = a & 0xffff;
  var bh = (b >>> 16) & 0xffff;
  var bl = b & 0xffff;
  // the shift by 0 fixes the sign on the high part
  // the final |0 converts the unsigned value into a signed value
  return ((al * bl) + (((ah * bl + al * bh) << 16) >>> 0) | 0);
});

var $fround = $g["Math"]["fround"] ||









  (function(v) {
    return +v;
  });


var $clz32 = $g["Math"]["clz32"] || (function(i) {
  // See Hacker's Delight, Section 5-3
  if (i === 0) return 32;
  var r = 1;
  if ((i & 0xffff0000) === 0) { i <<= 16; r += 16; };
  if ((i & 0xff000000) === 0) { i <<= 8; r += 8; };
  if ((i & 0xf0000000) === 0) { i <<= 4; r += 4; };
  if ((i & 0xc0000000) === 0) { i <<= 2; r += 2; };
  return r + (i >> 31);
});


// Other fields

















var $lastIDHash = 0; // last value attributed to an id hash code



var $idHashCodeMap = $g["WeakMap"] ? new $g["WeakMap"]() : null;



// Core mechanism

var $makeIsArrayOfPrimitive = function(primitiveData) {
  return function(obj, depth) {
    return !!(obj && obj.$classData &&
      (obj.$classData.arrayDepth === depth) &&
      (obj.$classData.arrayBase === primitiveData));
  }
};


var $makeAsArrayOfPrimitive = function(isInstanceOfFunction, arrayEncodedName) {
  return function(obj, depth) {
    if (isInstanceOfFunction(obj, depth) || (obj === null))
      return obj;
    else
      $throwArrayCastException(obj, arrayEncodedName, depth);
  }
};


/** Encode a property name for runtime manipulation
  *  Usage:
  *    env.propertyName({someProp:0})
  *  Returns:
  *    "someProp"
  *  Useful when the property is renamed by a global optimizer (like Closure)
  *  but we must still get hold of a string of that name for runtime
  * reflection.
  */
var $propertyName = function(obj) {
  for (var prop in obj)
    return prop;
};

// Runtime functions

var $isScalaJSObject = function(obj) {
  return !!(obj && obj.$classData);
};


var $throwClassCastException = function(instance, classFullName) {




  throw new $c_sjsr_UndefinedBehaviorError().init___jl_Throwable(
    new $c_jl_ClassCastException().init___T(
      instance + " is not an instance of " + classFullName));

};

var $throwArrayCastException = function(instance, classArrayEncodedName, depth) {
  for (; depth; --depth)
    classArrayEncodedName = "[" + classArrayEncodedName;
  $throwClassCastException(instance, classArrayEncodedName);
};


var $noIsInstance = function(instance) {
  throw new $g["TypeError"](
    "Cannot call isInstance() on a Class representing a raw JS trait/object");
};

var $makeNativeArrayWrapper = function(arrayClassData, nativeArray) {
  return new arrayClassData.constr(nativeArray);
};

var $newArrayObject = function(arrayClassData, lengths) {
  return $newArrayObjectInternal(arrayClassData, lengths, 0);
};

var $newArrayObjectInternal = function(arrayClassData, lengths, lengthIndex) {
  var result = new arrayClassData.constr(lengths[lengthIndex]);

  if (lengthIndex < lengths.length-1) {
    var subArrayClassData = arrayClassData.componentData;
    var subLengthIndex = lengthIndex+1;
    var underlying = result.u;
    for (var i = 0; i < underlying.length; i++) {
      underlying[i] = $newArrayObjectInternal(
        subArrayClassData, lengths, subLengthIndex);
    }
  }

  return result;
};

var $objectToString = function(instance) {
  if (instance === void 0)
    return "undefined";
  else
    return instance.toString();
};

var $objectGetClass = function(instance) {
  switch (typeof instance) {
    case "string":
      return $d_T.getClassOf();
    case "number": {
      var v = instance | 0;
      if (v === instance) { // is the value integral?
        if ($isByte(v))
          return $d_jl_Byte.getClassOf();
        else if ($isShort(v))
          return $d_jl_Short.getClassOf();
        else
          return $d_jl_Integer.getClassOf();
      } else {
        if ($isFloat(instance))
          return $d_jl_Float.getClassOf();
        else
          return $d_jl_Double.getClassOf();
      }
    }
    case "boolean":
      return $d_jl_Boolean.getClassOf();
    case "undefined":
      return $d_sr_BoxedUnit.getClassOf();
    default:
      if (instance === null)
        return instance.getClass__jl_Class();
      else if ($is_sjsr_RuntimeLong(instance))
        return $d_jl_Long.getClassOf();
      else if ($isScalaJSObject(instance))
        return instance.$classData.getClassOf();
      else
        return null; // Exception?
  }
};

var $objectClone = function(instance) {
  if ($isScalaJSObject(instance) || (instance === null))
    return instance.clone__O();
  else
    throw new $c_jl_CloneNotSupportedException().init___();
};

var $objectNotify = function(instance) {
  // final and no-op in java.lang.Object
  if (instance === null)
    instance.notify__V();
};

var $objectNotifyAll = function(instance) {
  // final and no-op in java.lang.Object
  if (instance === null)
    instance.notifyAll__V();
};

var $objectFinalize = function(instance) {
  if ($isScalaJSObject(instance) || (instance === null))
    instance.finalize__V();
  // else no-op
};

var $objectEquals = function(instance, rhs) {
  if ($isScalaJSObject(instance) || (instance === null))
    return instance.equals__O__Z(rhs);
  else if (typeof instance === "number")
    return typeof rhs === "number" && $numberEquals(instance, rhs);
  else
    return instance === rhs;
};

var $numberEquals = function(lhs, rhs) {
  return (lhs === rhs) ? (
    // 0.0.equals(-0.0) must be false
    lhs !== 0 || 1/lhs === 1/rhs
  ) : (
    // are they both NaN?
    (lhs !== lhs) && (rhs !== rhs)
  );
};

var $objectHashCode = function(instance) {
  switch (typeof instance) {
    case "string":
      return $m_sjsr_RuntimeString$().hashCode__T__I(instance);
    case "number":
      return $m_sjsr_Bits$().numberHashCode__D__I(instance);
    case "boolean":
      return instance ? 1231 : 1237;
    case "undefined":
      return 0;
    default:
      if ($isScalaJSObject(instance) || instance === null)
        return instance.hashCode__I();

      else if ($idHashCodeMap === null)
        return 42;

      else
        return $systemIdentityHashCode(instance);
  }
};

var $comparableCompareTo = function(instance, rhs) {
  switch (typeof instance) {
    case "string":

      $as_T(rhs);

      return instance === rhs ? 0 : (instance < rhs ? -1 : 1);
    case "number":

      $as_jl_Number(rhs);

      return $m_jl_Double$().compare__D__D__I(instance, rhs);
    case "boolean":

      $asBoolean(rhs);

      return instance - rhs; // yes, this gives the right result
    default:
      return instance.compareTo__O__I(rhs);
  }
};

var $charSequenceLength = function(instance) {
  if (typeof(instance) === "string")

    return $uI(instance["length"]);



  else
    return instance.length__I();
};

var $charSequenceCharAt = function(instance, index) {
  if (typeof(instance) === "string")

    return $uI(instance["charCodeAt"](index)) & 0xffff;



  else
    return instance.charAt__I__C(index);
};

var $charSequenceSubSequence = function(instance, start, end) {
  if (typeof(instance) === "string")

    return $as_T(instance["substring"](start, end));



  else
    return instance.subSequence__I__I__jl_CharSequence(start, end);
};

var $booleanBooleanValue = function(instance) {
  if (typeof instance === "boolean") return instance;
  else                               return instance.booleanValue__Z();
};

var $numberByteValue = function(instance) {
  if (typeof instance === "number") return (instance << 24) >> 24;
  else                              return instance.byteValue__B();
};
var $numberShortValue = function(instance) {
  if (typeof instance === "number") return (instance << 16) >> 16;
  else                              return instance.shortValue__S();
};
var $numberIntValue = function(instance) {
  if (typeof instance === "number") return instance | 0;
  else                              return instance.intValue__I();
};
var $numberLongValue = function(instance) {
  if (typeof instance === "number")
    return $m_sjsr_RuntimeLong$().fromDouble__D__sjsr_RuntimeLong(instance);
  else
    return instance.longValue__J();
};
var $numberFloatValue = function(instance) {
  if (typeof instance === "number") return $fround(instance);
  else                              return instance.floatValue__F();
};
var $numberDoubleValue = function(instance) {
  if (typeof instance === "number") return instance;
  else                              return instance.doubleValue__D();
};

var $isNaN = function(instance) {
  return instance !== instance;
};

var $isInfinite = function(instance) {
  return !$g["isFinite"](instance) && !$isNaN(instance);
};

var $doubleToInt = function(x) {
  return (x > 2147483647) ? (2147483647) : ((x < -2147483648) ? -2147483648 : (x | 0));
};

/** Instantiates a JS object with variadic arguments to the constructor. */
var $newJSObjectWithVarargs = function(ctor, args) {
  // This basically emulates the ECMAScript specification for 'new'.
  var instance = $g["Object"]["create"](ctor.prototype);
  var result = ctor["apply"](instance, args);
  switch (typeof result) {
    case "string": case "number": case "boolean": case "undefined": case "symbol":
      return instance;
    default:
      return result === null ? instance : result;
  }
};

var $resolveSuperRef = function(initialProto, propName) {
  var getPrototypeOf = $g["Object"]["getPrototypeOf"];
  var getOwnPropertyDescriptor = $g["Object"]["getOwnPropertyDescriptor"];

  var superProto = getPrototypeOf(initialProto);
  while (superProto !== null) {
    var desc = getOwnPropertyDescriptor(superProto, propName);
    if (desc !== void 0)
      return desc;
    superProto = getPrototypeOf(superProto);
  }

  return void 0;
};

var $superGet = function(initialProto, self, propName) {
  var desc = $resolveSuperRef(initialProto, propName);
  if (desc !== void 0) {
    var getter = desc["get"];
    if (getter !== void 0)
      return getter["call"](self);
    else
      return desc["value"];
  }
  return void 0;
};

var $superSet = function(initialProto, self, propName, value) {
  var desc = $resolveSuperRef(initialProto, propName);
  if (desc !== void 0) {
    var setter = desc["set"];
    if (setter !== void 0) {
      setter["call"](self, value);
      return void 0;
    }
  }
  throw new $g["TypeError"]("super has no setter '" + propName + "'.");
};







var $propertiesOf = function(obj) {
  var result = [];
  for (var prop in obj)
    result["push"](prop);
  return result;
};

var $systemArraycopy = function(src, srcPos, dest, destPos, length) {
  var srcu = src.u;
  var destu = dest.u;
  if (srcu !== destu || destPos < srcPos || srcPos + length < destPos) {
    for (var i = 0; i < length; i++)
      destu[destPos+i] = srcu[srcPos+i];
  } else {
    for (var i = length-1; i >= 0; i--)
      destu[destPos+i] = srcu[srcPos+i];
  }
};

var $systemIdentityHashCode =

  ($idHashCodeMap !== null) ?

  (function(obj) {
    switch (typeof obj) {
      case "string": case "number": case "boolean": case "undefined":
        return $objectHashCode(obj);
      default:
        if (obj === null) {
          return 0;
        } else {
          var hash = $idHashCodeMap["get"](obj);
          if (hash === void 0) {
            hash = ($lastIDHash + 1) | 0;
            $lastIDHash = hash;
            $idHashCodeMap["set"](obj, hash);
          }
          return hash;
        }
    }

  }) :
  (function(obj) {
    if ($isScalaJSObject(obj)) {
      var hash = obj["$idHashCode$0"];
      if (hash !== void 0) {
        return hash;
      } else if (!$g["Object"]["isSealed"](obj)) {
        hash = ($lastIDHash + 1) | 0;
        $lastIDHash = hash;
        obj["$idHashCode$0"] = hash;
        return hash;
      } else {
        return 42;
      }
    } else if (obj === null) {
      return 0;
    } else {
      return $objectHashCode(obj);
    }

  });

// is/as for hijacked boxed classes (the non-trivial ones)

var $isByte = function(v) {
  return (v << 24 >> 24) === v && 1/v !== 1/-0;
};

var $isShort = function(v) {
  return (v << 16 >> 16) === v && 1/v !== 1/-0;
};

var $isInt = function(v) {
  return (v | 0) === v && 1/v !== 1/-0;
};

var $isFloat = function(v) {



  return typeof v === "number";

};


var $asUnit = function(v) {
  if (v === void 0 || v === null)
    return v;
  else
    $throwClassCastException(v, "scala.runtime.BoxedUnit");
};

var $asBoolean = function(v) {
  if (typeof v === "boolean" || v === null)
    return v;
  else
    $throwClassCastException(v, "java.lang.Boolean");
};

var $asByte = function(v) {
  if ($isByte(v) || v === null)
    return v;
  else
    $throwClassCastException(v, "java.lang.Byte");
};

var $asShort = function(v) {
  if ($isShort(v) || v === null)
    return v;
  else
    $throwClassCastException(v, "java.lang.Short");
};

var $asInt = function(v) {
  if ($isInt(v) || v === null)
    return v;
  else
    $throwClassCastException(v, "java.lang.Integer");
};

var $asFloat = function(v) {
  if ($isFloat(v) || v === null)
    return v;
  else
    $throwClassCastException(v, "java.lang.Float");
};

var $asDouble = function(v) {
  if (typeof v === "number" || v === null)
    return v;
  else
    $throwClassCastException(v, "java.lang.Double");
};


// Unboxes


var $uZ = function(value) {
  return !!$asBoolean(value);
};
var $uB = function(value) {
  return $asByte(value) | 0;
};
var $uS = function(value) {
  return $asShort(value) | 0;
};
var $uI = function(value) {
  return $asInt(value) | 0;
};
var $uJ = function(value) {
  return null === value ? $m_sjsr_RuntimeLong$().Zero$1
                        : $as_sjsr_RuntimeLong(value);
};
var $uF = function(value) {
  /* Here, it is fine to use + instead of fround, because asFloat already
   * ensures that the result is either null or a float.
   */
  return +$asFloat(value);
};
var $uD = function(value) {
  return +$asDouble(value);
};






// TypeArray conversions

var $byteArray2TypedArray = function(value) { return new $g["Int8Array"](value.u); };
var $shortArray2TypedArray = function(value) { return new $g["Int16Array"](value.u); };
var $charArray2TypedArray = function(value) { return new $g["Uint16Array"](value.u); };
var $intArray2TypedArray = function(value) { return new $g["Int32Array"](value.u); };
var $floatArray2TypedArray = function(value) { return new $g["Float32Array"](value.u); };
var $doubleArray2TypedArray = function(value) { return new $g["Float64Array"](value.u); };

var $typedArray2ByteArray = function(value) {
  var arrayClassData = $d_B.getArrayOf();
  return new arrayClassData.constr(new $g["Int8Array"](value));
};
var $typedArray2ShortArray = function(value) {
  var arrayClassData = $d_S.getArrayOf();
  return new arrayClassData.constr(new $g["Int16Array"](value));
};
var $typedArray2CharArray = function(value) {
  var arrayClassData = $d_C.getArrayOf();
  return new arrayClassData.constr(new $g["Uint16Array"](value));
};
var $typedArray2IntArray = function(value) {
  var arrayClassData = $d_I.getArrayOf();
  return new arrayClassData.constr(new $g["Int32Array"](value));
};
var $typedArray2FloatArray = function(value) {
  var arrayClassData = $d_F.getArrayOf();
  return new arrayClassData.constr(new $g["Float32Array"](value));
};
var $typedArray2DoubleArray = function(value) {
  var arrayClassData = $d_D.getArrayOf();
  return new arrayClassData.constr(new $g["Float64Array"](value));
};

/* We have to force a non-elidable *read* of $e, otherwise Closure will
 * eliminate it altogether, along with all the exports, which is ... er ...
 * plain wrong.
 */
this["__ScalaJSExportsNamespace"] = $e;

// TypeData class


/** @constructor */
var $TypeData = function() {




  // Runtime support
  this.constr = void 0;
  this.parentData = void 0;
  this.ancestors = null;
  this.componentData = null;
  this.arrayBase = null;
  this.arrayDepth = 0;
  this.zero = null;
  this.arrayEncodedName = "";
  this._classOf = void 0;
  this._arrayOf = void 0;
  this.isArrayOf = void 0;

  // java.lang.Class support
  this["name"] = "";
  this["isPrimitive"] = false;
  this["isInterface"] = false;
  this["isArrayClass"] = false;
  this["isRawJSType"] = false;
  this["isInstance"] = void 0;
};


$TypeData.prototype.initPrim = function(



    zero, arrayEncodedName, displayName) {
  // Runtime support
  this.ancestors = {};
  this.componentData = null;
  this.zero = zero;
  this.arrayEncodedName = arrayEncodedName;
  this.isArrayOf = function(obj, depth) { return false; };

  // java.lang.Class support
  this["name"] = displayName;
  this["isPrimitive"] = true;
  this["isInstance"] = function(obj) { return false; };

  return this;
};


$TypeData.prototype.initClass = function(



    internalNameObj, isInterface, fullName,
    ancestors, isRawJSType, parentData, isInstance, isArrayOf) {
  var internalName = $propertyName(internalNameObj);

  isInstance = isInstance || function(obj) {
    return !!(obj && obj.$classData && obj.$classData.ancestors[internalName]);
  };

  isArrayOf = isArrayOf || function(obj, depth) {
    return !!(obj && obj.$classData && (obj.$classData.arrayDepth === depth)
      && obj.$classData.arrayBase.ancestors[internalName])
  };

  // Runtime support
  this.parentData = parentData;
  this.ancestors = ancestors;
  this.arrayEncodedName = "L"+fullName+";";
  this.isArrayOf = isArrayOf;

  // java.lang.Class support
  this["name"] = fullName;
  this["isInterface"] = isInterface;
  this["isRawJSType"] = !!isRawJSType;
  this["isInstance"] = isInstance;

  return this;
};


$TypeData.prototype.initArray = function(



    componentData) {
  // The constructor

  var componentZero0 = componentData.zero;

  // The zero for the Long runtime representation
  // is a special case here, since the class has not
  // been defined yet, when this file is read
  var componentZero = (componentZero0 == "longZero")
    ? $m_sjsr_RuntimeLong$().Zero$1
    : componentZero0;


  /** @constructor */
  var ArrayClass = function(arg) {
    if (typeof(arg) === "number") {
      // arg is the length of the array
      this.u = new Array(arg);
      for (var i = 0; i < arg; i++)
        this.u[i] = componentZero;
    } else {
      // arg is a native array that we wrap
      this.u = arg;
    }
  }
  ArrayClass.prototype = new $h_O;
  ArrayClass.prototype.constructor = ArrayClass;

  ArrayClass.prototype.clone__O = function() {
    if (this.u instanceof Array)
      return new ArrayClass(this.u["slice"](0));
    else
      // The underlying Array is a TypedArray
      return new ArrayClass(new this.u.constructor(this.u));
  };

























  ArrayClass.prototype.$classData = this;

  // Don't generate reflective call proxies. The compiler special cases
  // reflective calls to methods on scala.Array

  // The data

  var encodedName = "[" + componentData.arrayEncodedName;
  var componentBase = componentData.arrayBase || componentData;
  var arrayDepth = componentData.arrayDepth + 1;

  var isInstance = function(obj) {
    return componentBase.isArrayOf(obj, arrayDepth);
  }

  // Runtime support
  this.constr = ArrayClass;
  this.parentData = $d_O;
  this.ancestors = {O: 1, jl_Cloneable: 1, Ljava_io_Serializable: 1};
  this.componentData = componentData;
  this.arrayBase = componentBase;
  this.arrayDepth = arrayDepth;
  this.zero = null;
  this.arrayEncodedName = encodedName;
  this._classOf = undefined;
  this._arrayOf = undefined;
  this.isArrayOf = undefined;

  // java.lang.Class support
  this["name"] = encodedName;
  this["isPrimitive"] = false;
  this["isInterface"] = false;
  this["isArrayClass"] = true;
  this["isInstance"] = isInstance;

  return this;
};


$TypeData.prototype.getClassOf = function() {



  if (!this._classOf)
    this._classOf = new $c_jl_Class().init___jl_ScalaJSClassData(this);
  return this._classOf;
};


$TypeData.prototype.getArrayOf = function() {



  if (!this._arrayOf)
    this._arrayOf = new $TypeData().initArray(this);
  return this._arrayOf;
};

// java.lang.Class support


$TypeData.prototype["getFakeInstance"] = function() {



  if (this === $d_T)
    return "some string";
  else if (this === $d_jl_Boolean)
    return false;
  else if (this === $d_jl_Byte ||
           this === $d_jl_Short ||
           this === $d_jl_Integer ||
           this === $d_jl_Float ||
           this === $d_jl_Double)
    return 0;
  else if (this === $d_jl_Long)
    return $m_sjsr_RuntimeLong$().Zero$1;
  else if (this === $d_sr_BoxedUnit)
    return void 0;
  else
    return {$classData: this};
};


$TypeData.prototype["getSuperclass"] = function() {



  return this.parentData ? this.parentData.getClassOf() : null;
};


$TypeData.prototype["getComponentType"] = function() {



  return this.componentData ? this.componentData.getClassOf() : null;
};


$TypeData.prototype["newArrayOfThisClass"] = function(lengths) {



  var arrayClassData = this;
  for (var i = 0; i < lengths.length; i++)
    arrayClassData = arrayClassData.getArrayOf();
  return $newArrayObject(arrayClassData, lengths);
};




// Create primitive types

var $d_V = new $TypeData().initPrim(undefined, "V", "void");
var $d_Z = new $TypeData().initPrim(false, "Z", "boolean");
var $d_C = new $TypeData().initPrim(0, "C", "char");
var $d_B = new $TypeData().initPrim(0, "B", "byte");
var $d_S = new $TypeData().initPrim(0, "S", "short");
var $d_I = new $TypeData().initPrim(0, "I", "int");
var $d_J = new $TypeData().initPrim("longZero", "J", "long");
var $d_F = new $TypeData().initPrim(0.0, "F", "float");
var $d_D = new $TypeData().initPrim(0.0, "D", "double");

// Instance tests for array of primitives

var $isArrayOf_Z = $makeIsArrayOfPrimitive($d_Z);
$d_Z.isArrayOf = $isArrayOf_Z;

var $isArrayOf_C = $makeIsArrayOfPrimitive($d_C);
$d_C.isArrayOf = $isArrayOf_C;

var $isArrayOf_B = $makeIsArrayOfPrimitive($d_B);
$d_B.isArrayOf = $isArrayOf_B;

var $isArrayOf_S = $makeIsArrayOfPrimitive($d_S);
$d_S.isArrayOf = $isArrayOf_S;

var $isArrayOf_I = $makeIsArrayOfPrimitive($d_I);
$d_I.isArrayOf = $isArrayOf_I;

var $isArrayOf_J = $makeIsArrayOfPrimitive($d_J);
$d_J.isArrayOf = $isArrayOf_J;

var $isArrayOf_F = $makeIsArrayOfPrimitive($d_F);
$d_F.isArrayOf = $isArrayOf_F;

var $isArrayOf_D = $makeIsArrayOfPrimitive($d_D);
$d_D.isArrayOf = $isArrayOf_D;


// asInstanceOfs for array of primitives
var $asArrayOf_Z = $makeAsArrayOfPrimitive($isArrayOf_Z, "Z");
var $asArrayOf_C = $makeAsArrayOfPrimitive($isArrayOf_C, "C");
var $asArrayOf_B = $makeAsArrayOfPrimitive($isArrayOf_B, "B");
var $asArrayOf_S = $makeAsArrayOfPrimitive($isArrayOf_S, "S");
var $asArrayOf_I = $makeAsArrayOfPrimitive($isArrayOf_I, "I");
var $asArrayOf_J = $makeAsArrayOfPrimitive($isArrayOf_J, "J");
var $asArrayOf_F = $makeAsArrayOfPrimitive($isArrayOf_F, "F");
var $asArrayOf_D = $makeAsArrayOfPrimitive($isArrayOf_D, "D");

function $is_F1(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.F1)))
}
function $as_F1(obj) {
  return (($is_F1(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "scala.Function1"))
}
function $isArrayOf_F1(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.F1)))
}
function $asArrayOf_F1(obj, depth) {
  return (($isArrayOf_F1(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Lscala.Function1;", depth))
}
function $s_Ljapgolly_scalajs_react_CompState$WriteCallbackOps$class__setState__Ljapgolly_scalajs_react_CompState$WriteCallbackOps__O__F0__F0($$this, s, cb) {
  var f = new $c_sjsr_AnonFunction0().init___sjs_js_Function0((function(arg$outer, s$1, cb$5) {
    return (function() {
      var this$1 = arg$outer.a__Ljapgolly_scalajs_react_CompState$Accessor();
      var $$ = arg$outer.$$__O();
      this$1.setState__Ljapgolly_scalajs_react_CompScope$CanSetState__O__F0__V($$, s$1, cb$5)
    })
  })($$this, s, cb));
  return f
}
function $s_Ljapgolly_scalajs_react_vdom_Extra$Attrs$class__$$init$__Ljapgolly_scalajs_react_vdom_Extra$Attrs__V($$this) {
  $$this.ref$1 = $m_Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$();
  $$this.key$1 = new $c_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic().init___T("key")
}
function $s_Ljapgolly_scalajs_react_vdom_HtmlAttrs$class__$$init$__Ljapgolly_scalajs_react_vdom_HtmlAttrs__V($$this) {
  $$this.onChange$1 = new $c_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic().init___T("onChange");
  $$this.onClick$1 = new $c_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic().init___T("onClick");
  $$this.src$1 = new $c_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic().init___T("src");
  $$this.title$1 = new $c_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic().init___T("title");
  $$this.type$1 = new $c_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic().init___T("type");
  $$this.tpe$1 = $$this.type$1;
  $$this.value$1 = new $c_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic().init___T("value")
}
function $s_Ljapgolly_scalajs_react_vdom_HtmlTags$class__$$init$__Ljapgolly_scalajs_react_vdom_HtmlTags__V($$this) {
  var namespaceConfig = $m_Ljapgolly_scalajs_react_vdom_NamespaceHtml$().implicitNamespace$1;
  $m_Ljapgolly_scalajs_react_vdom_Escaping$().assertValidTag__T__V("p");
  $$this.p$1 = new $c_Ljapgolly_scalajs_react_vdom_ReactTagOf().init___T__sci_List__T("p", $m_sci_Nil$(), namespaceConfig);
  var namespaceConfig$1 = $m_Ljapgolly_scalajs_react_vdom_NamespaceHtml$().implicitNamespace$1;
  $m_Ljapgolly_scalajs_react_vdom_Escaping$().assertValidTag__T__V("div");
  $$this.div$1 = new $c_Ljapgolly_scalajs_react_vdom_ReactTagOf().init___T__sci_List__T("div", $m_sci_Nil$(), namespaceConfig$1);
  var namespaceConfig$2 = $m_Ljapgolly_scalajs_react_vdom_NamespaceHtml$().implicitNamespace$1;
  $m_Ljapgolly_scalajs_react_vdom_Escaping$().assertValidTag__T__V("a");
  $$this.a$1 = new $c_Ljapgolly_scalajs_react_vdom_ReactTagOf().init___T__sci_List__T("a", $m_sci_Nil$(), namespaceConfig$2);
  var namespaceConfig$3 = $m_Ljapgolly_scalajs_react_vdom_NamespaceHtml$().implicitNamespace$1;
  $m_Ljapgolly_scalajs_react_vdom_Escaping$().assertValidTag__T__V("span");
  $$this.span$1 = new $c_Ljapgolly_scalajs_react_vdom_ReactTagOf().init___T__sci_List__T("span", $m_sci_Nil$(), namespaceConfig$3);
  var namespaceConfig$4 = $m_Ljapgolly_scalajs_react_vdom_NamespaceHtml$().implicitNamespace$1;
  $m_Ljapgolly_scalajs_react_vdom_Escaping$().assertValidTag__T__V("img");
  $$this.img$1 = new $c_Ljapgolly_scalajs_react_vdom_ReactTagOf().init___T__sci_List__T("img", $m_sci_Nil$(), namespaceConfig$4);
  var namespaceConfig$5 = $m_Ljapgolly_scalajs_react_vdom_NamespaceHtml$().implicitNamespace$1;
  $m_Ljapgolly_scalajs_react_vdom_Escaping$().assertValidTag__T__V("button");
  $$this.button$1 = new $c_Ljapgolly_scalajs_react_vdom_ReactTagOf().init___T__sci_List__T("button", $m_sci_Nil$(), namespaceConfig$5)
}
function $is_Ljapgolly_scalajs_react_vdom_TagMod(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.Ljapgolly_scalajs_react_vdom_TagMod)))
}
function $as_Ljapgolly_scalajs_react_vdom_TagMod(obj) {
  return (($is_Ljapgolly_scalajs_react_vdom_TagMod(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "japgolly.scalajs.react.vdom.TagMod"))
}
function $isArrayOf_Ljapgolly_scalajs_react_vdom_TagMod(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.Ljapgolly_scalajs_react_vdom_TagMod)))
}
function $asArrayOf_Ljapgolly_scalajs_react_vdom_TagMod(obj, depth) {
  return (($isArrayOf_Ljapgolly_scalajs_react_vdom_TagMod(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Ljapgolly.scalajs.react.vdom.TagMod;", depth))
}
/** @constructor */
function $c_O() {
  /*<skip>*/
}
/** @constructor */
function $h_O() {
  /*<skip>*/
}
$h_O.prototype = $c_O.prototype;
$c_O.prototype.init___ = (function() {
  return this
});
$c_O.prototype.equals__O__Z = (function(that) {
  return (this === that)
});
$c_O.prototype.toString__T = (function() {
  var jsx$2 = $objectGetClass(this).getName__T();
  var i = this.hashCode__I();
  var x = $uD((i >>> 0));
  var jsx$1 = x.toString(16);
  return ((jsx$2 + "@") + $as_T(jsx$1))
});
$c_O.prototype.hashCode__I = (function() {
  return $systemIdentityHashCode(this)
});
$c_O.prototype.toString = (function() {
  return this.toString__T()
});
function $is_O(obj) {
  return (obj !== null)
}
function $as_O(obj) {
  return obj
}
function $isArrayOf_O(obj, depth) {
  var data = (obj && obj.$classData);
  if ((!data)) {
    return false
  } else {
    var arrayDepth = (data.arrayDepth || 0);
    return ((!(arrayDepth < depth)) && ((arrayDepth > depth) || (!data.arrayBase.isPrimitive)))
  }
}
function $asArrayOf_O(obj, depth) {
  return (($isArrayOf_O(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Ljava.lang.Object;", depth))
}
var $d_O = new $TypeData().initClass({
  O: 0
}, false, "java.lang.Object", {
  O: 1
}, (void 0), (void 0), $is_O, $isArrayOf_O);
$c_O.prototype.$classData = $d_O;
function $s_s_Product2$class__productElement__s_Product2__I__O($$this, n) {
  switch (n) {
    case 0: {
      return $$this.$$und1$f;
      break
    }
    case 1: {
      return $$this.$$und2$f;
      break
    }
    default: {
      throw new $c_jl_IndexOutOfBoundsException().init___T(("" + n))
    }
  }
}
function $s_s_util_control_NoStackTrace$class__fillInStackTrace__s_util_control_NoStackTrace__jl_Throwable($$this) {
  var this$1 = $m_s_util_control_NoStackTrace$();
  if (this$1.$$undnoSuppression$1) {
    return $c_jl_Throwable.prototype.fillInStackTrace__jl_Throwable.call($$this)
  } else {
    return $as_jl_Throwable($$this)
  }
}
function $s_sc_GenSeqLike$class__equals__sc_GenSeqLike__O__Z($$this, that) {
  if ($is_sc_GenSeq(that)) {
    var x2 = $as_sc_GenSeq(that);
    return $$this.sameElements__sc_GenIterable__Z(x2)
  } else {
    return false
  }
}
function $s_sc_IndexedSeqOptimized$class__lengthCompare__sc_IndexedSeqOptimized__I__I($$this, len) {
  return (($$this.length__I() - len) | 0)
}
function $s_sc_IndexedSeqOptimized$class__sameElements__sc_IndexedSeqOptimized__sc_GenIterable__Z($$this, that) {
  if ($is_sc_IndexedSeq(that)) {
    var x2 = $as_sc_IndexedSeq(that);
    var len = $$this.length__I();
    if ((len === x2.length__I())) {
      var i = 0;
      while (((i < len) && $m_sr_BoxesRunTime$().equals__O__O__Z($$this.apply__I__O(i), x2.apply__I__O(i)))) {
        i = ((1 + i) | 0)
      };
      return (i === len)
    } else {
      return false
    }
  } else {
    return $s_sc_IterableLike$class__sameElements__sc_IterableLike__sc_GenIterable__Z($$this, that)
  }
}
function $s_sc_IndexedSeqOptimized$class__foreach__sc_IndexedSeqOptimized__F1__V($$this, f) {
  var i = 0;
  var len = $$this.length__I();
  while ((i < len)) {
    f.apply__O__O($$this.apply__I__O(i));
    i = ((1 + i) | 0)
  }
}
function $s_sc_IndexedSeqOptimized$class__isEmpty__sc_IndexedSeqOptimized__Z($$this) {
  return ($$this.length__I() === 0)
}
function $s_sc_IterableLike$class__sameElements__sc_IterableLike__sc_GenIterable__Z($$this, that) {
  var these = $$this.iterator__sc_Iterator();
  var those = that.iterator__sc_Iterator();
  while ((these.hasNext__Z() && those.hasNext__Z())) {
    if ((!$m_sr_BoxesRunTime$().equals__O__O__Z(these.next__O(), those.next__O()))) {
      return false
    }
  };
  return ((!these.hasNext__Z()) && (!those.hasNext__Z()))
}
function $s_sc_Iterator$class__isEmpty__sc_Iterator__Z($$this) {
  return (!$$this.hasNext__Z())
}
function $s_sc_Iterator$class__toString__sc_Iterator__T($$this) {
  return (($$this.hasNext__Z() ? "non-empty" : "empty") + " iterator")
}
function $s_sc_Iterator$class__foreach__sc_Iterator__F1__V($$this, f) {
  while ($$this.hasNext__Z()) {
    f.apply__O__O($$this.next__O())
  }
}
function $s_sc_LinearSeqOptimized$class__lengthCompare__sc_LinearSeqOptimized__I__I($$this, len) {
  return ((len < 0) ? 1 : $s_sc_LinearSeqOptimized$class__loop$1__p0__sc_LinearSeqOptimized__I__sc_LinearSeqOptimized__I__I($$this, 0, $$this, len))
}
function $s_sc_LinearSeqOptimized$class__apply__sc_LinearSeqOptimized__I__O($$this, n) {
  var rest = $$this.drop__I__sci_List(n);
  if (((n < 0) || rest.isEmpty__Z())) {
    throw new $c_jl_IndexOutOfBoundsException().init___T(("" + n))
  };
  return rest.head__O()
}
function $s_sc_LinearSeqOptimized$class__loop$1__p0__sc_LinearSeqOptimized__I__sc_LinearSeqOptimized__I__I($$this, i, xs, len$1) {
  _loop: while (true) {
    if ((i === len$1)) {
      return (xs.isEmpty__Z() ? 0 : 1)
    } else if (xs.isEmpty__Z()) {
      return (-1)
    } else {
      var temp$i = ((1 + i) | 0);
      var this$1 = xs;
      var temp$xs = this$1.tail__sci_List();
      i = temp$i;
      xs = temp$xs;
      continue _loop
    }
  }
}
function $s_sc_LinearSeqOptimized$class__length__sc_LinearSeqOptimized__I($$this) {
  var these = $$this;
  var len = 0;
  while ((!these.isEmpty__Z())) {
    len = ((1 + len) | 0);
    var this$1 = these;
    these = this$1.tail__sci_List()
  };
  return len
}
function $s_sc_LinearSeqOptimized$class__sameElements__sc_LinearSeqOptimized__sc_GenIterable__Z($$this, that) {
  if ($is_sc_LinearSeq(that)) {
    var x2 = $as_sc_LinearSeq(that);
    if (($$this === x2)) {
      return true
    } else {
      var these = $$this;
      var those = x2;
      while ((((!these.isEmpty__Z()) && (!those.isEmpty__Z())) && $m_sr_BoxesRunTime$().equals__O__O__Z(these.head__O(), those.head__O()))) {
        var this$1 = these;
        these = this$1.tail__sci_List();
        var this$2 = those;
        those = this$2.tail__sci_List()
      };
      return (these.isEmpty__Z() && those.isEmpty__Z())
    }
  } else {
    return $s_sc_IterableLike$class__sameElements__sc_IterableLike__sc_GenIterable__Z($$this, that)
  }
}
function $s_sc_SeqLike$class__isEmpty__sc_SeqLike__Z($$this) {
  return ($$this.lengthCompare__I__I(0) === 0)
}
function $s_sc_TraversableLike$class__toString__sc_TraversableLike__T($$this) {
  var start = ($$this.stringPrefix__T() + "(");
  return $s_sc_TraversableOnce$class__mkString__sc_TraversableOnce__T__T__T__T($$this, start, ", ", ")")
}
function $s_sc_TraversableLike$class__stringPrefix__sc_TraversableLike__T($$this) {
  var this$1 = $$this.repr__O();
  var string = $objectGetClass(this$1).getName__T();
  var idx1 = $m_sjsr_RuntimeString$().lastIndexOf__T__I__I(string, 46);
  if ((idx1 !== (-1))) {
    var thiz = string;
    var beginIndex = ((1 + idx1) | 0);
    string = $as_T(thiz.substring(beginIndex))
  };
  var idx2 = $m_sjsr_RuntimeString$().indexOf__T__I__I(string, 36);
  if ((idx2 !== (-1))) {
    var thiz$1 = string;
    string = $as_T(thiz$1.substring(0, idx2))
  };
  return string
}
function $s_sc_TraversableOnce$class__addString__sc_TraversableOnce__scm_StringBuilder__T__T__T__scm_StringBuilder($$this, b, start, sep, end) {
  var first = new $c_sr_BooleanRef().init___Z(true);
  b.append__T__scm_StringBuilder(start);
  $$this.foreach__F1__V(new $c_sjsr_AnonFunction1().init___sjs_js_Function1((function($$this$1, first$1, b$1, sep$1) {
    return (function(x$2) {
      if (first$1.elem$1) {
        b$1.append__O__scm_StringBuilder(x$2);
        first$1.elem$1 = false;
        return (void 0)
      } else {
        b$1.append__T__scm_StringBuilder(sep$1);
        return b$1.append__O__scm_StringBuilder(x$2)
      }
    })
  })($$this, first, b, sep)));
  b.append__T__scm_StringBuilder(end);
  return b
}
function $s_sc_TraversableOnce$class__mkString__sc_TraversableOnce__T__T__T__T($$this, start, sep, end) {
  var b = new $c_scm_StringBuilder().init___();
  var this$1 = $s_sc_TraversableOnce$class__addString__sc_TraversableOnce__scm_StringBuilder__T__T__T__scm_StringBuilder($$this, b, start, sep, end);
  var this$2 = this$1.underlying$5;
  return this$2.content$1
}
function $s_sc_TraversableOnce$class__nonEmpty__sc_TraversableOnce__Z($$this) {
  return (!$$this.isEmpty__Z())
}
function $s_sci_VectorPointer$class__getElem__sci_VectorPointer__I__I__O($$this, index, xor) {
  if ((xor < 32)) {
    return $$this.display0__AO().u[(31 & index)]
  } else if ((xor < 1024)) {
    return $asArrayOf_O($$this.display1__AO().u[(31 & (index >> 5))], 1).u[(31 & index)]
  } else if ((xor < 32768)) {
    return $asArrayOf_O($asArrayOf_O($$this.display2__AO().u[(31 & (index >> 10))], 1).u[(31 & (index >> 5))], 1).u[(31 & index)]
  } else if ((xor < 1048576)) {
    return $asArrayOf_O($asArrayOf_O($asArrayOf_O($$this.display3__AO().u[(31 & (index >> 15))], 1).u[(31 & (index >> 10))], 1).u[(31 & (index >> 5))], 1).u[(31 & index)]
  } else if ((xor < 33554432)) {
    return $asArrayOf_O($asArrayOf_O($asArrayOf_O($asArrayOf_O($$this.display4__AO().u[(31 & (index >> 20))], 1).u[(31 & (index >> 15))], 1).u[(31 & (index >> 10))], 1).u[(31 & (index >> 5))], 1).u[(31 & index)]
  } else if ((xor < 1073741824)) {
    return $asArrayOf_O($asArrayOf_O($asArrayOf_O($asArrayOf_O($asArrayOf_O($$this.display5__AO().u[(31 & (index >> 25))], 1).u[(31 & (index >> 20))], 1).u[(31 & (index >> 15))], 1).u[(31 & (index >> 10))], 1).u[(31 & (index >> 5))], 1).u[(31 & index)]
  } else {
    throw new $c_jl_IllegalArgumentException().init___()
  }
}
function $s_sci_VectorPointer$class__stabilize__sci_VectorPointer__I__V($$this, index) {
  var x1 = (((-1) + $$this.depth__I()) | 0);
  switch (x1) {
    case 5: {
      var a = $$this.display5__AO();
      $$this.display5$und$eq__AO__V($s_sci_VectorPointer$class__copyOf__sci_VectorPointer__AO__AO($$this, a));
      var a$1 = $$this.display4__AO();
      $$this.display4$und$eq__AO__V($s_sci_VectorPointer$class__copyOf__sci_VectorPointer__AO__AO($$this, a$1));
      var a$2 = $$this.display3__AO();
      $$this.display3$und$eq__AO__V($s_sci_VectorPointer$class__copyOf__sci_VectorPointer__AO__AO($$this, a$2));
      var a$3 = $$this.display2__AO();
      $$this.display2$und$eq__AO__V($s_sci_VectorPointer$class__copyOf__sci_VectorPointer__AO__AO($$this, a$3));
      var a$4 = $$this.display1__AO();
      $$this.display1$und$eq__AO__V($s_sci_VectorPointer$class__copyOf__sci_VectorPointer__AO__AO($$this, a$4));
      $$this.display5__AO().u[(31 & (index >> 25))] = $$this.display4__AO();
      $$this.display4__AO().u[(31 & (index >> 20))] = $$this.display3__AO();
      $$this.display3__AO().u[(31 & (index >> 15))] = $$this.display2__AO();
      $$this.display2__AO().u[(31 & (index >> 10))] = $$this.display1__AO();
      $$this.display1__AO().u[(31 & (index >> 5))] = $$this.display0__AO();
      break
    }
    case 4: {
      var a$5 = $$this.display4__AO();
      $$this.display4$und$eq__AO__V($s_sci_VectorPointer$class__copyOf__sci_VectorPointer__AO__AO($$this, a$5));
      var a$6 = $$this.display3__AO();
      $$this.display3$und$eq__AO__V($s_sci_VectorPointer$class__copyOf__sci_VectorPointer__AO__AO($$this, a$6));
      var a$7 = $$this.display2__AO();
      $$this.display2$und$eq__AO__V($s_sci_VectorPointer$class__copyOf__sci_VectorPointer__AO__AO($$this, a$7));
      var a$8 = $$this.display1__AO();
      $$this.display1$und$eq__AO__V($s_sci_VectorPointer$class__copyOf__sci_VectorPointer__AO__AO($$this, a$8));
      $$this.display4__AO().u[(31 & (index >> 20))] = $$this.display3__AO();
      $$this.display3__AO().u[(31 & (index >> 15))] = $$this.display2__AO();
      $$this.display2__AO().u[(31 & (index >> 10))] = $$this.display1__AO();
      $$this.display1__AO().u[(31 & (index >> 5))] = $$this.display0__AO();
      break
    }
    case 3: {
      var a$9 = $$this.display3__AO();
      $$this.display3$und$eq__AO__V($s_sci_VectorPointer$class__copyOf__sci_VectorPointer__AO__AO($$this, a$9));
      var a$10 = $$this.display2__AO();
      $$this.display2$und$eq__AO__V($s_sci_VectorPointer$class__copyOf__sci_VectorPointer__AO__AO($$this, a$10));
      var a$11 = $$this.display1__AO();
      $$this.display1$und$eq__AO__V($s_sci_VectorPointer$class__copyOf__sci_VectorPointer__AO__AO($$this, a$11));
      $$this.display3__AO().u[(31 & (index >> 15))] = $$this.display2__AO();
      $$this.display2__AO().u[(31 & (index >> 10))] = $$this.display1__AO();
      $$this.display1__AO().u[(31 & (index >> 5))] = $$this.display0__AO();
      break
    }
    case 2: {
      var a$12 = $$this.display2__AO();
      $$this.display2$und$eq__AO__V($s_sci_VectorPointer$class__copyOf__sci_VectorPointer__AO__AO($$this, a$12));
      var a$13 = $$this.display1__AO();
      $$this.display1$und$eq__AO__V($s_sci_VectorPointer$class__copyOf__sci_VectorPointer__AO__AO($$this, a$13));
      $$this.display2__AO().u[(31 & (index >> 10))] = $$this.display1__AO();
      $$this.display1__AO().u[(31 & (index >> 5))] = $$this.display0__AO();
      break
    }
    case 1: {
      var a$14 = $$this.display1__AO();
      $$this.display1$und$eq__AO__V($s_sci_VectorPointer$class__copyOf__sci_VectorPointer__AO__AO($$this, a$14));
      $$this.display1__AO().u[(31 & (index >> 5))] = $$this.display0__AO();
      break
    }
    case 0: {
      break
    }
    default: {
      throw new $c_s_MatchError().init___O(x1)
    }
  }
}
function $s_sci_VectorPointer$class__initFrom__sci_VectorPointer__sci_VectorPointer__I__V($$this, that, depth) {
  $$this.depth$und$eq__I__V(depth);
  var x1 = (((-1) + depth) | 0);
  switch (x1) {
    case (-1): {
      break
    }
    case 0: {
      $$this.display0$und$eq__AO__V(that.display0__AO());
      break
    }
    case 1: {
      $$this.display1$und$eq__AO__V(that.display1__AO());
      $$this.display0$und$eq__AO__V(that.display0__AO());
      break
    }
    case 2: {
      $$this.display2$und$eq__AO__V(that.display2__AO());
      $$this.display1$und$eq__AO__V(that.display1__AO());
      $$this.display0$und$eq__AO__V(that.display0__AO());
      break
    }
    case 3: {
      $$this.display3$und$eq__AO__V(that.display3__AO());
      $$this.display2$und$eq__AO__V(that.display2__AO());
      $$this.display1$und$eq__AO__V(that.display1__AO());
      $$this.display0$und$eq__AO__V(that.display0__AO());
      break
    }
    case 4: {
      $$this.display4$und$eq__AO__V(that.display4__AO());
      $$this.display3$und$eq__AO__V(that.display3__AO());
      $$this.display2$und$eq__AO__V(that.display2__AO());
      $$this.display1$und$eq__AO__V(that.display1__AO());
      $$this.display0$und$eq__AO__V(that.display0__AO());
      break
    }
    case 5: {
      $$this.display5$und$eq__AO__V(that.display5__AO());
      $$this.display4$und$eq__AO__V(that.display4__AO());
      $$this.display3$und$eq__AO__V(that.display3__AO());
      $$this.display2$und$eq__AO__V(that.display2__AO());
      $$this.display1$und$eq__AO__V(that.display1__AO());
      $$this.display0$und$eq__AO__V(that.display0__AO());
      break
    }
    default: {
      throw new $c_s_MatchError().init___O(x1)
    }
  }
}
function $s_sci_VectorPointer$class__gotoNextBlockStart__sci_VectorPointer__I__I__V($$this, index, xor) {
  if ((xor < 1024)) {
    $$this.display0$und$eq__AO__V($asArrayOf_O($$this.display1__AO().u[(31 & (index >> 5))], 1))
  } else if ((xor < 32768)) {
    $$this.display1$und$eq__AO__V($asArrayOf_O($$this.display2__AO().u[(31 & (index >> 10))], 1));
    $$this.display0$und$eq__AO__V($asArrayOf_O($$this.display1__AO().u[0], 1))
  } else if ((xor < 1048576)) {
    $$this.display2$und$eq__AO__V($asArrayOf_O($$this.display3__AO().u[(31 & (index >> 15))], 1));
    $$this.display1$und$eq__AO__V($asArrayOf_O($$this.display2__AO().u[0], 1));
    $$this.display0$und$eq__AO__V($asArrayOf_O($$this.display1__AO().u[0], 1))
  } else if ((xor < 33554432)) {
    $$this.display3$und$eq__AO__V($asArrayOf_O($$this.display4__AO().u[(31 & (index >> 20))], 1));
    $$this.display2$und$eq__AO__V($asArrayOf_O($$this.display3__AO().u[0], 1));
    $$this.display1$und$eq__AO__V($asArrayOf_O($$this.display2__AO().u[0], 1));
    $$this.display0$und$eq__AO__V($asArrayOf_O($$this.display1__AO().u[0], 1))
  } else if ((xor < 1073741824)) {
    $$this.display4$und$eq__AO__V($asArrayOf_O($$this.display5__AO().u[(31 & (index >> 25))], 1));
    $$this.display3$und$eq__AO__V($asArrayOf_O($$this.display4__AO().u[0], 1));
    $$this.display2$und$eq__AO__V($asArrayOf_O($$this.display3__AO().u[0], 1));
    $$this.display1$und$eq__AO__V($asArrayOf_O($$this.display2__AO().u[0], 1));
    $$this.display0$und$eq__AO__V($asArrayOf_O($$this.display1__AO().u[0], 1))
  } else {
    throw new $c_jl_IllegalArgumentException().init___()
  }
}
function $s_sci_VectorPointer$class__gotoPos__sci_VectorPointer__I__I__V($$this, index, xor) {
  if ((xor >= 32)) {
    if ((xor < 1024)) {
      $$this.display0$und$eq__AO__V($asArrayOf_O($$this.display1__AO().u[(31 & (index >> 5))], 1))
    } else if ((xor < 32768)) {
      $$this.display1$und$eq__AO__V($asArrayOf_O($$this.display2__AO().u[(31 & (index >> 10))], 1));
      $$this.display0$und$eq__AO__V($asArrayOf_O($$this.display1__AO().u[(31 & (index >> 5))], 1))
    } else if ((xor < 1048576)) {
      $$this.display2$und$eq__AO__V($asArrayOf_O($$this.display3__AO().u[(31 & (index >> 15))], 1));
      $$this.display1$und$eq__AO__V($asArrayOf_O($$this.display2__AO().u[(31 & (index >> 10))], 1));
      $$this.display0$und$eq__AO__V($asArrayOf_O($$this.display1__AO().u[(31 & (index >> 5))], 1))
    } else if ((xor < 33554432)) {
      $$this.display3$und$eq__AO__V($asArrayOf_O($$this.display4__AO().u[(31 & (index >> 20))], 1));
      $$this.display2$und$eq__AO__V($asArrayOf_O($$this.display3__AO().u[(31 & (index >> 15))], 1));
      $$this.display1$und$eq__AO__V($asArrayOf_O($$this.display2__AO().u[(31 & (index >> 10))], 1));
      $$this.display0$und$eq__AO__V($asArrayOf_O($$this.display1__AO().u[(31 & (index >> 5))], 1))
    } else if ((xor < 1073741824)) {
      $$this.display4$und$eq__AO__V($asArrayOf_O($$this.display5__AO().u[(31 & (index >> 25))], 1));
      $$this.display3$und$eq__AO__V($asArrayOf_O($$this.display4__AO().u[(31 & (index >> 20))], 1));
      $$this.display2$und$eq__AO__V($asArrayOf_O($$this.display3__AO().u[(31 & (index >> 15))], 1));
      $$this.display1$und$eq__AO__V($asArrayOf_O($$this.display2__AO().u[(31 & (index >> 10))], 1));
      $$this.display0$und$eq__AO__V($asArrayOf_O($$this.display1__AO().u[(31 & (index >> 5))], 1))
    } else {
      throw new $c_jl_IllegalArgumentException().init___()
    }
  }
}
function $s_sci_VectorPointer$class__copyOf__sci_VectorPointer__AO__AO($$this, a) {
  var b = $newArrayObject($d_O.getArrayOf(), [a.u.length]);
  var length = a.u.length;
  $systemArraycopy(a, 0, b, 0, length);
  return b
}
/** @constructor */
function $c_Ljapgolly_scalajs_react_Callback$undTempHack$() {
  $c_O.call(this);
  this.empty$1 = null
}
$c_Ljapgolly_scalajs_react_Callback$undTempHack$.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_Callback$undTempHack$.prototype.constructor = $c_Ljapgolly_scalajs_react_Callback$undTempHack$;
/** @constructor */
function $h_Ljapgolly_scalajs_react_Callback$undTempHack$() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_Callback$undTempHack$.prototype = $c_Ljapgolly_scalajs_react_Callback$undTempHack$.prototype;
$c_Ljapgolly_scalajs_react_Callback$undTempHack$.prototype.init___ = (function() {
  $n_Ljapgolly_scalajs_react_Callback$undTempHack$ = this;
  this.empty$1 = new $c_sjsr_AnonFunction0().init___sjs_js_Function0((function(a$1) {
    return (function() {
      return a$1
    })
  })((void 0)));
  return this
});
var $d_Ljapgolly_scalajs_react_Callback$undTempHack$ = new $TypeData().initClass({
  Ljapgolly_scalajs_react_Callback$undTempHack$: 0
}, false, "japgolly.scalajs.react.Callback_TempHack$", {
  Ljapgolly_scalajs_react_Callback$undTempHack$: 1,
  O: 1
});
$c_Ljapgolly_scalajs_react_Callback$undTempHack$.prototype.$classData = $d_Ljapgolly_scalajs_react_Callback$undTempHack$;
var $n_Ljapgolly_scalajs_react_Callback$undTempHack$ = (void 0);
function $m_Ljapgolly_scalajs_react_Callback$undTempHack$() {
  if ((!$n_Ljapgolly_scalajs_react_Callback$undTempHack$)) {
    $n_Ljapgolly_scalajs_react_Callback$undTempHack$ = new $c_Ljapgolly_scalajs_react_Callback$undTempHack$().init___()
  };
  return $n_Ljapgolly_scalajs_react_Callback$undTempHack$
}
/** @constructor */
function $c_Ljapgolly_scalajs_react_CallbackTo() {
  $c_O.call(this);
  this.japgolly$scalajs$react$CallbackTo$$f$1 = null
}
$c_Ljapgolly_scalajs_react_CallbackTo.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_CallbackTo.prototype.constructor = $c_Ljapgolly_scalajs_react_CallbackTo;
/** @constructor */
function $h_Ljapgolly_scalajs_react_CallbackTo() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_CallbackTo.prototype = $c_Ljapgolly_scalajs_react_CallbackTo.prototype;
$c_Ljapgolly_scalajs_react_CallbackTo.prototype.init___F0 = (function(f) {
  this.japgolly$scalajs$react$CallbackTo$$f$1 = f;
  return this
});
$c_Ljapgolly_scalajs_react_CallbackTo.prototype.equals__O__Z = (function(x$1) {
  return $m_Ljapgolly_scalajs_react_CallbackTo$().equals$extension__F0__O__Z(this.japgolly$scalajs$react$CallbackTo$$f$1, x$1)
});
$c_Ljapgolly_scalajs_react_CallbackTo.prototype.hashCode__I = (function() {
  var $$this = this.japgolly$scalajs$react$CallbackTo$$f$1;
  return $systemIdentityHashCode($$this)
});
function $is_Ljapgolly_scalajs_react_CallbackTo(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.Ljapgolly_scalajs_react_CallbackTo)))
}
function $as_Ljapgolly_scalajs_react_CallbackTo(obj) {
  return (($is_Ljapgolly_scalajs_react_CallbackTo(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "japgolly.scalajs.react.CallbackTo"))
}
function $isArrayOf_Ljapgolly_scalajs_react_CallbackTo(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.Ljapgolly_scalajs_react_CallbackTo)))
}
function $asArrayOf_Ljapgolly_scalajs_react_CallbackTo(obj, depth) {
  return (($isArrayOf_Ljapgolly_scalajs_react_CallbackTo(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Ljapgolly.scalajs.react.CallbackTo;", depth))
}
var $d_Ljapgolly_scalajs_react_CallbackTo = new $TypeData().initClass({
  Ljapgolly_scalajs_react_CallbackTo: 0
}, false, "japgolly.scalajs.react.CallbackTo", {
  Ljapgolly_scalajs_react_CallbackTo: 1,
  O: 1
});
$c_Ljapgolly_scalajs_react_CallbackTo.prototype.$classData = $d_Ljapgolly_scalajs_react_CallbackTo;
/** @constructor */
function $c_Ljapgolly_scalajs_react_CallbackTo$() {
  $c_O.call(this)
}
$c_Ljapgolly_scalajs_react_CallbackTo$.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_CallbackTo$.prototype.constructor = $c_Ljapgolly_scalajs_react_CallbackTo$;
/** @constructor */
function $h_Ljapgolly_scalajs_react_CallbackTo$() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_CallbackTo$.prototype = $c_Ljapgolly_scalajs_react_CallbackTo$.prototype;
$c_Ljapgolly_scalajs_react_CallbackTo$.prototype.init___ = (function() {
  return this
});
$c_Ljapgolly_scalajs_react_CallbackTo$.prototype.equals$extension__F0__O__Z = (function($$this, x$1) {
  if ($is_Ljapgolly_scalajs_react_CallbackTo(x$1)) {
    var CallbackTo$1 = ((x$1 === null) ? null : $as_Ljapgolly_scalajs_react_CallbackTo(x$1).japgolly$scalajs$react$CallbackTo$$f$1);
    return ($$this === CallbackTo$1)
  } else {
    return false
  }
});
$c_Ljapgolly_scalajs_react_CallbackTo$.prototype.toJsCallback$extension__F0__sjs_js_UndefOr = (function($$this) {
  if (this.isEmpty$und$qmark$extension__F0__Z($$this)) {
    return (void 0)
  } else {
    var value = this.toJsFn$extension__F0__sjs_js_Function0($$this);
    return value
  }
});
$c_Ljapgolly_scalajs_react_CallbackTo$.prototype.isEmpty$und$qmark$extension__F0__Z = (function($$this) {
  return ($$this === $m_Ljapgolly_scalajs_react_package$().Callback$1.empty$1)
});
$c_Ljapgolly_scalajs_react_CallbackTo$.prototype.toJsFn$extension__F0__sjs_js_Function0 = (function($$this) {
  return (function(f) {
    return (function() {
      return f.apply__O()
    })
  })($$this)
});
var $d_Ljapgolly_scalajs_react_CallbackTo$ = new $TypeData().initClass({
  Ljapgolly_scalajs_react_CallbackTo$: 0
}, false, "japgolly.scalajs.react.CallbackTo$", {
  Ljapgolly_scalajs_react_CallbackTo$: 1,
  O: 1
});
$c_Ljapgolly_scalajs_react_CallbackTo$.prototype.$classData = $d_Ljapgolly_scalajs_react_CallbackTo$;
var $n_Ljapgolly_scalajs_react_CallbackTo$ = (void 0);
function $m_Ljapgolly_scalajs_react_CallbackTo$() {
  if ((!$n_Ljapgolly_scalajs_react_CallbackTo$)) {
    $n_Ljapgolly_scalajs_react_CallbackTo$ = new $c_Ljapgolly_scalajs_react_CallbackTo$().init___()
  };
  return $n_Ljapgolly_scalajs_react_CallbackTo$
}
/** @constructor */
function $c_Ljapgolly_scalajs_react_CompState$Accessor() {
  $c_O.call(this)
}
$c_Ljapgolly_scalajs_react_CompState$Accessor.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_CompState$Accessor.prototype.constructor = $c_Ljapgolly_scalajs_react_CompState$Accessor;
/** @constructor */
function $h_Ljapgolly_scalajs_react_CompState$Accessor() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_CompState$Accessor.prototype = $c_Ljapgolly_scalajs_react_CompState$Accessor.prototype;
/** @constructor */
function $c_Ljapgolly_scalajs_react_CompState$RootAccessor$() {
  $c_O.call(this);
  this.instance$1 = null
}
$c_Ljapgolly_scalajs_react_CompState$RootAccessor$.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_CompState$RootAccessor$.prototype.constructor = $c_Ljapgolly_scalajs_react_CompState$RootAccessor$;
/** @constructor */
function $h_Ljapgolly_scalajs_react_CompState$RootAccessor$() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_CompState$RootAccessor$.prototype = $c_Ljapgolly_scalajs_react_CompState$RootAccessor$.prototype;
$c_Ljapgolly_scalajs_react_CompState$RootAccessor$.prototype.init___ = (function() {
  $n_Ljapgolly_scalajs_react_CompState$RootAccessor$ = this;
  this.instance$1 = new $c_Ljapgolly_scalajs_react_CompState$RootAccessor().init___();
  return this
});
var $d_Ljapgolly_scalajs_react_CompState$RootAccessor$ = new $TypeData().initClass({
  Ljapgolly_scalajs_react_CompState$RootAccessor$: 0
}, false, "japgolly.scalajs.react.CompState$RootAccessor$", {
  Ljapgolly_scalajs_react_CompState$RootAccessor$: 1,
  O: 1
});
$c_Ljapgolly_scalajs_react_CompState$RootAccessor$.prototype.$classData = $d_Ljapgolly_scalajs_react_CompState$RootAccessor$;
var $n_Ljapgolly_scalajs_react_CompState$RootAccessor$ = (void 0);
function $m_Ljapgolly_scalajs_react_CompState$RootAccessor$() {
  if ((!$n_Ljapgolly_scalajs_react_CompState$RootAccessor$)) {
    $n_Ljapgolly_scalajs_react_CompState$RootAccessor$ = new $c_Ljapgolly_scalajs_react_CompState$RootAccessor$().init___()
  };
  return $n_Ljapgolly_scalajs_react_CompState$RootAccessor$
}
/** @constructor */
function $c_Ljapgolly_scalajs_react_LifecycleInput() {
  $c_O.call(this)
}
$c_Ljapgolly_scalajs_react_LifecycleInput.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_LifecycleInput.prototype.constructor = $c_Ljapgolly_scalajs_react_LifecycleInput;
/** @constructor */
function $h_Ljapgolly_scalajs_react_LifecycleInput() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_LifecycleInput.prototype = $c_Ljapgolly_scalajs_react_LifecycleInput.prototype;
/** @constructor */
function $c_Ljapgolly_scalajs_react_ReactComponentB() {
  $c_O.call(this);
  this.name$1 = null;
  this.japgolly$scalajs$react$ReactComponentB$$isf$f = null;
  this.japgolly$scalajs$react$ReactComponentB$$ibf$f = null;
  this.japgolly$scalajs$react$ReactComponentB$$rf$f = null;
  this.japgolly$scalajs$react$ReactComponentB$$lc$f = null;
  this.japgolly$scalajs$react$ReactComponentB$$jsMixins$f = null
}
$c_Ljapgolly_scalajs_react_ReactComponentB.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_ReactComponentB.prototype.constructor = $c_Ljapgolly_scalajs_react_ReactComponentB;
/** @constructor */
function $h_Ljapgolly_scalajs_react_ReactComponentB() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_ReactComponentB.prototype = $c_Ljapgolly_scalajs_react_ReactComponentB.prototype;
$c_Ljapgolly_scalajs_react_ReactComponentB.prototype.init___T__F1__s_Option__F1__Ljapgolly_scalajs_react_ReactComponentB$LifeCycle__sci_Vector = (function(name, isf, ibf, rf, lc, jsMixins) {
  this.name$1 = name;
  this.japgolly$scalajs$react$ReactComponentB$$isf$f = isf;
  this.japgolly$scalajs$react$ReactComponentB$$ibf$f = ibf;
  this.japgolly$scalajs$react$ReactComponentB$$rf$f = rf;
  this.japgolly$scalajs$react$ReactComponentB$$lc$f = lc;
  this.japgolly$scalajs$react$ReactComponentB$$jsMixins$f = jsMixins;
  return this
});
var $d_Ljapgolly_scalajs_react_ReactComponentB = new $TypeData().initClass({
  Ljapgolly_scalajs_react_ReactComponentB: 0
}, false, "japgolly.scalajs.react.ReactComponentB", {
  Ljapgolly_scalajs_react_ReactComponentB: 1,
  O: 1
});
$c_Ljapgolly_scalajs_react_ReactComponentB.prototype.$classData = $d_Ljapgolly_scalajs_react_ReactComponentB;
/** @constructor */
function $c_Ljapgolly_scalajs_react_ReactComponentB$() {
  $c_O.call(this);
  this.BackendKey$1 = null;
  this.japgolly$scalajs$react$ReactComponentB$$alwaysFalse$1 = null
}
$c_Ljapgolly_scalajs_react_ReactComponentB$.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_ReactComponentB$.prototype.constructor = $c_Ljapgolly_scalajs_react_ReactComponentB$;
/** @constructor */
function $h_Ljapgolly_scalajs_react_ReactComponentB$() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_ReactComponentB$.prototype = $c_Ljapgolly_scalajs_react_ReactComponentB$.prototype;
$c_Ljapgolly_scalajs_react_ReactComponentB$.prototype.init___ = (function() {
  $n_Ljapgolly_scalajs_react_ReactComponentB$ = this;
  this.japgolly$scalajs$react$ReactComponentB$$alwaysFalse$1 = new $c_sjsr_AnonFunction0().init___sjs_js_Function0((function(a$1) {
    return (function() {
      return a$1
    })
  })(false));
  return this
});
var $d_Ljapgolly_scalajs_react_ReactComponentB$ = new $TypeData().initClass({
  Ljapgolly_scalajs_react_ReactComponentB$: 0
}, false, "japgolly.scalajs.react.ReactComponentB$", {
  Ljapgolly_scalajs_react_ReactComponentB$: 1,
  O: 1
});
$c_Ljapgolly_scalajs_react_ReactComponentB$.prototype.$classData = $d_Ljapgolly_scalajs_react_ReactComponentB$;
var $n_Ljapgolly_scalajs_react_ReactComponentB$ = (void 0);
function $m_Ljapgolly_scalajs_react_ReactComponentB$() {
  if ((!$n_Ljapgolly_scalajs_react_ReactComponentB$)) {
    $n_Ljapgolly_scalajs_react_ReactComponentB$ = new $c_Ljapgolly_scalajs_react_ReactComponentB$().init___()
  };
  return $n_Ljapgolly_scalajs_react_ReactComponentB$
}
/** @constructor */
function $c_Ljapgolly_scalajs_react_ReactComponentB$BuildResult() {
  $c_O.call(this)
}
$c_Ljapgolly_scalajs_react_ReactComponentB$BuildResult.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_ReactComponentB$BuildResult.prototype.constructor = $c_Ljapgolly_scalajs_react_ReactComponentB$BuildResult;
/** @constructor */
function $h_Ljapgolly_scalajs_react_ReactComponentB$BuildResult() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_ReactComponentB$BuildResult.prototype = $c_Ljapgolly_scalajs_react_ReactComponentB$BuildResult.prototype;
/** @constructor */
function $c_Ljapgolly_scalajs_react_ReactComponentB$BuildResultLowPri() {
  $c_O.call(this)
}
$c_Ljapgolly_scalajs_react_ReactComponentB$BuildResultLowPri.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_ReactComponentB$BuildResultLowPri.prototype.constructor = $c_Ljapgolly_scalajs_react_ReactComponentB$BuildResultLowPri;
/** @constructor */
function $h_Ljapgolly_scalajs_react_ReactComponentB$BuildResultLowPri() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_ReactComponentB$BuildResultLowPri.prototype = $c_Ljapgolly_scalajs_react_ReactComponentB$BuildResultLowPri.prototype;
/** @constructor */
function $c_Ljapgolly_scalajs_react_ReactComponentB$Builder() {
  $c_O.call(this);
  this.buildFn$1 = null;
  this.$$outer$1 = null
}
$c_Ljapgolly_scalajs_react_ReactComponentB$Builder.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_ReactComponentB$Builder.prototype.constructor = $c_Ljapgolly_scalajs_react_ReactComponentB$Builder;
/** @constructor */
function $h_Ljapgolly_scalajs_react_ReactComponentB$Builder() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_ReactComponentB$Builder.prototype = $c_Ljapgolly_scalajs_react_ReactComponentB$Builder.prototype;
$c_Ljapgolly_scalajs_react_ReactComponentB$Builder.prototype.init___Ljapgolly_scalajs_react_ReactComponentB__F1 = (function($$outer, buildFn) {
  this.buildFn$1 = buildFn;
  if (($$outer === null)) {
    throw $m_sjsr_package$().unwrapJavaScriptException__jl_Throwable__O(null)
  } else {
    this.$$outer$1 = $$outer
  };
  return this
});
$c_Ljapgolly_scalajs_react_ReactComponentB$Builder.prototype.buildSpec__Ljapgolly_scalajs_react_ReactComponentSpec = (function() {
  var spec = $m_sjs_js_Dictionary$().empty__sjs_js_Dictionary();
  var this$1 = $m_s_Option$().apply__O__s_Option(this.$$outer$1.name$1);
  if ((!this$1.isEmpty__Z())) {
    var arg1 = this$1.get__O();
    var n = $as_T(arg1);
    spec.displayName = ($m_Ljapgolly_scalajs_react_package$(), n)
  };
  if (this.$$outer$1.japgolly$scalajs$react$ReactComponentB$$ibf$f.isDefined__Z()) {
    spec.backend = null
  };
  spec.render = (function(f) {
    return (function() {
      return f.apply__O__O(this)
    })
  })(this.$$outer$1.japgolly$scalajs$react$ReactComponentB$$rf$f);
  var elem = $m_s_None$();
  var elem$1 = null;
  elem$1 = elem;
  var this$4 = this.$$outer$1.japgolly$scalajs$react$ReactComponentB$$ibf$f;
  if ((!this$4.isEmpty__Z())) {
    var v1 = this$4.get__O();
    var initBackend = $as_F1(v1);
    var f$1 = new $c_sjsr_AnonFunction1().init___sjs_js_Function1((function(initBackend$1) {
      return (function($$$) {
        var backend = initBackend$1.apply__O__O($$$);
        $$$.backend = backend
      })
    })(initBackend));
    var this$5 = $as_s_Option(elem$1);
    var f$2 = new $c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$japgolly$scalajs$react$ReactComponentB$Builder$$onWillMountFn$1$2().init___Ljapgolly_scalajs_react_ReactComponentB$Builder__F1(this, f$1);
    if (this$5.isEmpty__Z()) {
      var jsx$1 = f$1
    } else {
      var v1$1 = this$5.get__O();
      var jsx$1 = f$2.apply__F1__F1($as_F1(v1$1))
    };
    elem$1 = new $c_s_Some().init___O(jsx$1)
  };
  var value = this.$$outer$1.japgolly$scalajs$react$ReactComponentB$$lc$f.componentWillMount$1;
  if ((value !== (void 0))) {
    var f$3 = $as_F1(value);
    var f$4 = new $c_sjsr_AnonFunction1().init___sjs_js_Function1((function(f$19) {
      return (function(x$14$2) {
        var $$this = $as_Ljapgolly_scalajs_react_CallbackTo(f$19.apply__O__O(x$14$2)).japgolly$scalajs$react$CallbackTo$$f$1;
        $$this.apply__O()
      })
    })(f$3));
    var this$10 = $as_s_Option(elem$1);
    var f$5 = new $c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$japgolly$scalajs$react$ReactComponentB$Builder$$onWillMountFn$1$2().init___Ljapgolly_scalajs_react_ReactComponentB$Builder__F1(this, f$4);
    if (this$10.isEmpty__Z()) {
      var jsx$2 = f$4
    } else {
      var v1$2 = this$10.get__O();
      var jsx$2 = f$5.apply__F1__F1($as_F1(v1$2))
    };
    elem$1 = new $c_s_Some().init___O(jsx$2)
  };
  var this$11 = $as_s_Option(elem$1);
  if ((!this$11.isEmpty__Z())) {
    var arg1$1 = this$11.get__O();
    var f$6 = $as_F1(arg1$1);
    spec.componentWillMount = (function(f$7) {
      return (function() {
        return f$7.apply__O__O(this)
      })
    })(f$6)
  };
  var initStateFn = new $c_sjsr_AnonFunction1().init___sjs_js_Function1((function(arg$outer) {
    return (function($$$$1) {
      var jsx$3 = $m_Ljapgolly_scalajs_react_package$();
      var $$this$1 = $as_Ljapgolly_scalajs_react_CallbackTo(arg$outer.$$outer$1.japgolly$scalajs$react$ReactComponentB$$isf$f.apply__O__O($$$$1)).japgolly$scalajs$react$CallbackTo$$f$1;
      return jsx$3.WrapObj__O__Ljapgolly_scalajs_react_package$WrapObj($$this$1.apply__O())
    })
  })(this));
  spec.getInitialState = (function(f$8) {
    return (function() {
      return f$8.apply__O__O(this)
    })
  })(initStateFn);
  var value$1 = this.$$outer$1.japgolly$scalajs$react$ReactComponentB$$lc$f.getDefaultProps$1;
  if ((value$1 === (void 0))) {
    var value$2 = (void 0)
  } else {
    var x$15 = $as_Ljapgolly_scalajs_react_CallbackTo(value$1).japgolly$scalajs$react$CallbackTo$$f$1;
    var value$2 = $m_Ljapgolly_scalajs_react_CallbackTo$().toJsCallback$extension__F0__sjs_js_UndefOr(x$15)
  };
  if ((value$2 !== (void 0))) {
    spec.getDefaultProps = value$2
  };
  var fn = this.$$outer$1.japgolly$scalajs$react$ReactComponentB$$lc$f.componentWillUnmount$1;
  if ((fn !== (void 0))) {
    var f$9 = $as_F1(fn);
    var g = new $c_sjsr_AnonFunction1().init___sjs_js_Function1((function(f$18) {
      return (function(a$2) {
        var $$this$2 = $as_Ljapgolly_scalajs_react_CallbackTo(f$18.apply__O__O(a$2)).japgolly$scalajs$react$CallbackTo$$f$1;
        $$this$2.apply__O()
      })
    })(f$9));
    spec.componentWillUnmount = (function(f$10) {
      return (function() {
        return f$10.apply__O__O(this)
      })
    })(g)
  };
  var fn$1 = this.$$outer$1.japgolly$scalajs$react$ReactComponentB$$lc$f.componentDidMount$1;
  if ((fn$1 !== (void 0))) {
    var f$11 = $as_F1(fn$1);
    var g$1 = new $c_sjsr_AnonFunction1().init___sjs_js_Function1((function(f$18$1) {
      return (function(a$2$1) {
        var $$this$3 = $as_Ljapgolly_scalajs_react_CallbackTo(f$18$1.apply__O__O(a$2$1)).japgolly$scalajs$react$CallbackTo$$f$1;
        $$this$3.apply__O()
      })
    })(f$11));
    spec.componentDidMount = (function(f$12) {
      return (function() {
        return f$12.apply__O__O(this)
      })
    })(g$1)
  };
  var a = new $c_sjsr_AnonFunction3().init___sjs_js_Function3((function($$$$2, nextProps$2, nextState$2) {
    return new $c_Ljapgolly_scalajs_react_ComponentWillUpdate().init___Ljapgolly_scalajs_react_CompScope$WillUpdate__O__O($$$$2, nextProps$2, nextState$2)
  }));
  var fn$2 = this.$$outer$1.japgolly$scalajs$react$ReactComponentB$$lc$f.componentWillUpdate$1;
  var f$13 = new $c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnPS$1$1().init___Ljapgolly_scalajs_react_ReactComponentB$Builder__sjs_js_Dictionary__F3__T(this, spec, a, "componentWillUpdate");
  if ((fn$2 !== (void 0))) {
    f$13.apply__F1__V($as_F1(fn$2))
  };
  var a$1 = new $c_sjsr_AnonFunction3().init___sjs_js_Function3((function($$$$3, prevProps$2, prevState$2) {
    return new $c_Ljapgolly_scalajs_react_ComponentDidUpdate().init___Ljapgolly_scalajs_react_CompScope$DuringCallbackM__O__O($$$$3, prevProps$2, prevState$2)
  }));
  var fn$3 = this.$$outer$1.japgolly$scalajs$react$ReactComponentB$$lc$f.componentDidUpdate$1;
  var f$14 = new $c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnPS$1$1().init___Ljapgolly_scalajs_react_ReactComponentB$Builder__sjs_js_Dictionary__F3__T(this, spec, a$1, "componentDidUpdate");
  if ((fn$3 !== (void 0))) {
    f$14.apply__F1__V($as_F1(fn$3))
  };
  var a$3 = new $c_sjsr_AnonFunction3().init___sjs_js_Function3((function($$$$4, nextProps$2$1, nextState$2$1) {
    return new $c_Ljapgolly_scalajs_react_ShouldComponentUpdate().init___Ljapgolly_scalajs_react_CompScope$DuringCallbackM__O__O($$$$4, nextProps$2$1, nextState$2$1)
  }));
  var fn$4 = this.$$outer$1.japgolly$scalajs$react$ReactComponentB$$lc$f.shouldComponentUpdate$1;
  var f$15 = new $c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnPS$1$1().init___Ljapgolly_scalajs_react_ReactComponentB$Builder__sjs_js_Dictionary__F3__T(this, spec, a$3, "shouldComponentUpdate");
  if ((fn$4 !== (void 0))) {
    f$15.apply__F1__V($as_F1(fn$4))
  };
  var a$4 = new $c_sjsr_AnonFunction2().init___sjs_js_Function2((function($$$$5, nextProps$2$2) {
    return new $c_Ljapgolly_scalajs_react_ComponentWillReceiveProps().init___Ljapgolly_scalajs_react_CompScope$DuringCallbackM__O($$$$5, nextProps$2$2)
  }));
  var fn$5 = this.$$outer$1.japgolly$scalajs$react$ReactComponentB$$lc$f.componentWillReceiveProps$1;
  var f$16 = new $c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnP$1$1().init___Ljapgolly_scalajs_react_ReactComponentB$Builder__sjs_js_Dictionary__F2__T(this, spec, a$4, "componentWillReceiveProps");
  if ((fn$5 !== (void 0))) {
    f$16.apply__F1__V($as_F1(fn$5))
  };
  var this$39 = this.$$outer$1.japgolly$scalajs$react$ReactComponentB$$jsMixins$f;
  if ($s_sc_TraversableOnce$class__nonEmpty__sc_TraversableOnce__Z(this$39)) {
    var col = this.$$outer$1.japgolly$scalajs$react$ReactComponentB$$jsMixins$f;
    if ($is_sjs_js_ArrayOps(col)) {
      var x2 = $as_sjs_js_ArrayOps(col);
      var jsx$4 = x2.scala$scalajs$js$ArrayOps$$array$f
    } else if ($is_sjs_js_WrappedArray(col)) {
      var x3 = $as_sjs_js_WrappedArray(col);
      var jsx$4 = x3.array$6
    } else {
      var result = [];
      var this$41 = col.iterator__sci_VectorIterator();
      while (this$41.$$undhasNext$2) {
        var arg1$2 = this$41.next__O();
        $uI(result.push(arg1$2))
      };
      var jsx$4 = result
    };
    spec.mixins = jsx$4
  };
  var value$3 = this.$$outer$1.japgolly$scalajs$react$ReactComponentB$$lc$f.configureSpec$1;
  if ((value$3 !== (void 0))) {
    var x$17 = $as_F1(value$3);
    var $$this$4 = $as_Ljapgolly_scalajs_react_CallbackTo(x$17.apply__O__O(spec)).japgolly$scalajs$react$CallbackTo$$f$1;
    $$this$4.apply__O()
  };
  return spec
});
$c_Ljapgolly_scalajs_react_ReactComponentB$Builder.prototype.build__O = (function() {
  var c = $g.React.createClass(this.buildSpec__Ljapgolly_scalajs_react_ReactComponentSpec());
  var f = $g.React.createFactory(c);
  var r = new $c_Ljapgolly_scalajs_react_ReactComponentC$ReqProps().init___Ljapgolly_scalajs_react_ReactComponentCU__Ljapgolly_scalajs_react_ReactClass__sjs_js_UndefOr__sjs_js_UndefOr(f, c, (void 0), (void 0));
  return this.buildFn$1.apply__O__O(r)
});
var $d_Ljapgolly_scalajs_react_ReactComponentB$Builder = new $TypeData().initClass({
  Ljapgolly_scalajs_react_ReactComponentB$Builder: 0
}, false, "japgolly.scalajs.react.ReactComponentB$Builder", {
  Ljapgolly_scalajs_react_ReactComponentB$Builder: 1,
  O: 1
});
$c_Ljapgolly_scalajs_react_ReactComponentB$Builder.prototype.$classData = $d_Ljapgolly_scalajs_react_ReactComponentB$Builder;
/** @constructor */
function $c_Ljapgolly_scalajs_react_ReactComponentB$P() {
  $c_O.call(this);
  this.name$1 = null
}
$c_Ljapgolly_scalajs_react_ReactComponentB$P.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_ReactComponentB$P.prototype.constructor = $c_Ljapgolly_scalajs_react_ReactComponentB$P;
/** @constructor */
function $h_Ljapgolly_scalajs_react_ReactComponentB$P() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_ReactComponentB$P.prototype = $c_Ljapgolly_scalajs_react_ReactComponentB$P.prototype;
$c_Ljapgolly_scalajs_react_ReactComponentB$P.prototype.initialState__F0__Ljapgolly_scalajs_react_ReactComponentB$PS = (function(s) {
  return this.initialStateCB__F0__Ljapgolly_scalajs_react_ReactComponentB$PS(s)
});
$c_Ljapgolly_scalajs_react_ReactComponentB$P.prototype.initialStateCB__F0__Ljapgolly_scalajs_react_ReactComponentB$PS = (function(s) {
  return this.getInitialStateCB__F1__Ljapgolly_scalajs_react_ReactComponentB$PS(new $c_sjsr_AnonFunction1().init___sjs_js_Function1((function(s$1) {
    return (function(x$4$2) {
      return new $c_Ljapgolly_scalajs_react_CallbackTo().init___F0(s$1)
    })
  })(s)))
});
$c_Ljapgolly_scalajs_react_ReactComponentB$P.prototype.getInitialStateCB__F1__Ljapgolly_scalajs_react_ReactComponentB$PS = (function(f) {
  return new $c_Ljapgolly_scalajs_react_ReactComponentB$PS().init___T__F1(this.name$1, f)
});
$c_Ljapgolly_scalajs_react_ReactComponentB$P.prototype.init___T = (function(name) {
  this.name$1 = name;
  return this
});
var $d_Ljapgolly_scalajs_react_ReactComponentB$P = new $TypeData().initClass({
  Ljapgolly_scalajs_react_ReactComponentB$P: 0
}, false, "japgolly.scalajs.react.ReactComponentB$P", {
  Ljapgolly_scalajs_react_ReactComponentB$P: 1,
  O: 1
});
$c_Ljapgolly_scalajs_react_ReactComponentB$P.prototype.$classData = $d_Ljapgolly_scalajs_react_ReactComponentB$P;
/** @constructor */
function $c_Ljapgolly_scalajs_react_ReactComponentB$PS() {
  $c_O.call(this);
  this.name$1 = null;
  this.isf$1 = null
}
$c_Ljapgolly_scalajs_react_ReactComponentB$PS.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_ReactComponentB$PS.prototype.constructor = $c_Ljapgolly_scalajs_react_ReactComponentB$PS;
/** @constructor */
function $h_Ljapgolly_scalajs_react_ReactComponentB$PS() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_ReactComponentB$PS.prototype = $c_Ljapgolly_scalajs_react_ReactComponentB$PS.prototype;
$c_Ljapgolly_scalajs_react_ReactComponentB$PS.prototype.backend__F1__Ljapgolly_scalajs_react_ReactComponentB$PSB = (function(initBackend) {
  return new $c_Ljapgolly_scalajs_react_ReactComponentB$PSB().init___T__F1__s_Option(this.name$1, this.isf$1, new $c_s_Some().init___O(initBackend))
});
$c_Ljapgolly_scalajs_react_ReactComponentB$PS.prototype.init___T__F1 = (function(name, isf) {
  this.name$1 = name;
  this.isf$1 = isf;
  return this
});
var $d_Ljapgolly_scalajs_react_ReactComponentB$PS = new $TypeData().initClass({
  Ljapgolly_scalajs_react_ReactComponentB$PS: 0
}, false, "japgolly.scalajs.react.ReactComponentB$PS", {
  Ljapgolly_scalajs_react_ReactComponentB$PS: 1,
  O: 1
});
$c_Ljapgolly_scalajs_react_ReactComponentB$PS.prototype.$classData = $d_Ljapgolly_scalajs_react_ReactComponentB$PS;
/** @constructor */
function $c_Ljapgolly_scalajs_react_ReactComponentB$PSB() {
  $c_O.call(this);
  this.name$1 = null;
  this.isf$1 = null;
  this.ibf$1 = null
}
$c_Ljapgolly_scalajs_react_ReactComponentB$PSB.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_ReactComponentB$PSB.prototype.constructor = $c_Ljapgolly_scalajs_react_ReactComponentB$PSB;
/** @constructor */
function $h_Ljapgolly_scalajs_react_ReactComponentB$PSB() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_ReactComponentB$PSB.prototype = $c_Ljapgolly_scalajs_react_ReactComponentB$PSB.prototype;
$c_Ljapgolly_scalajs_react_ReactComponentB$PSB.prototype.init___T__F1__s_Option = (function(name, isf, ibf) {
  this.name$1 = name;
  this.isf$1 = isf;
  this.ibf$1 = ibf;
  return this
});
$c_Ljapgolly_scalajs_react_ReactComponentB$PSB.prototype.render__F1__Ljapgolly_scalajs_react_ReactComponentB$PSBR = (function(f) {
  return new $c_Ljapgolly_scalajs_react_ReactComponentB$PSBR().init___T__F1__s_Option__F1(this.name$1, this.isf$1, this.ibf$1, f)
});
var $d_Ljapgolly_scalajs_react_ReactComponentB$PSB = new $TypeData().initClass({
  Ljapgolly_scalajs_react_ReactComponentB$PSB: 0
}, false, "japgolly.scalajs.react.ReactComponentB$PSB", {
  Ljapgolly_scalajs_react_ReactComponentB$PSB: 1,
  O: 1
});
$c_Ljapgolly_scalajs_react_ReactComponentB$PSB.prototype.$classData = $d_Ljapgolly_scalajs_react_ReactComponentB$PSB;
/** @constructor */
function $c_Ljapgolly_scalajs_react_ReactComponentB$PSBR() {
  $c_O.call(this);
  this.name$1 = null;
  this.isf$1 = null;
  this.ibf$1 = null;
  this.rf$1 = null
}
$c_Ljapgolly_scalajs_react_ReactComponentB$PSBR.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_ReactComponentB$PSBR.prototype.constructor = $c_Ljapgolly_scalajs_react_ReactComponentB$PSBR;
/** @constructor */
function $h_Ljapgolly_scalajs_react_ReactComponentB$PSBR() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_ReactComponentB$PSBR.prototype = $c_Ljapgolly_scalajs_react_ReactComponentB$PSBR.prototype;
$c_Ljapgolly_scalajs_react_ReactComponentB$PSBR.prototype.init___T__F1__s_Option__F1 = (function(name, isf, ibf, rf) {
  this.name$1 = name;
  this.isf$1 = isf;
  this.ibf$1 = ibf;
  this.rf$1 = rf;
  return this
});
$c_Ljapgolly_scalajs_react_ReactComponentB$PSBR.prototype.domType__Ljapgolly_scalajs_react_ReactComponentB = (function() {
  var jsx$5 = this.name$1;
  var jsx$4 = this.isf$1;
  var jsx$3 = this.ibf$1;
  var jsx$2 = this.rf$1;
  $m_Ljapgolly_scalajs_react_ReactComponentB$();
  var jsx$1 = new $c_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle().init___sjs_js_UndefOr__sjs_js_UndefOr__sjs_js_UndefOr__sjs_js_UndefOr__sjs_js_UndefOr__sjs_js_UndefOr__sjs_js_UndefOr__sjs_js_UndefOr__sjs_js_UndefOr((void 0), (void 0), (void 0), (void 0), (void 0), (void 0), (void 0), (void 0), (void 0));
  var this$2 = $m_s_package$().Vector$1;
  return new $c_Ljapgolly_scalajs_react_ReactComponentB().init___T__F1__s_Option__F1__Ljapgolly_scalajs_react_ReactComponentB$LifeCycle__sci_Vector(jsx$5, jsx$4, jsx$3, jsx$2, jsx$1, this$2.NIL$6)
});
var $d_Ljapgolly_scalajs_react_ReactComponentB$PSBR = new $TypeData().initClass({
  Ljapgolly_scalajs_react_ReactComponentB$PSBR: 0
}, false, "japgolly.scalajs.react.ReactComponentB$PSBR", {
  Ljapgolly_scalajs_react_ReactComponentB$PSBR: 1,
  O: 1
});
$c_Ljapgolly_scalajs_react_ReactComponentB$PSBR.prototype.$classData = $d_Ljapgolly_scalajs_react_ReactComponentB$PSBR;
/** @constructor */
function $c_Ljapgolly_scalajs_react_ReactComponentC$() {
  $c_O.call(this);
  this.japgolly$scalajs$react$ReactComponentC$$fnUnit0$f = null
}
$c_Ljapgolly_scalajs_react_ReactComponentC$.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_ReactComponentC$.prototype.constructor = $c_Ljapgolly_scalajs_react_ReactComponentC$;
/** @constructor */
function $h_Ljapgolly_scalajs_react_ReactComponentC$() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_ReactComponentC$.prototype = $c_Ljapgolly_scalajs_react_ReactComponentC$.prototype;
$c_Ljapgolly_scalajs_react_ReactComponentC$.prototype.init___ = (function() {
  $n_Ljapgolly_scalajs_react_ReactComponentC$ = this;
  this.japgolly$scalajs$react$ReactComponentC$$fnUnit0$f = new $c_sjsr_AnonFunction0().init___sjs_js_Function0((function() {
    return (void 0)
  }));
  return this
});
var $d_Ljapgolly_scalajs_react_ReactComponentC$ = new $TypeData().initClass({
  Ljapgolly_scalajs_react_ReactComponentC$: 0
}, false, "japgolly.scalajs.react.ReactComponentC$", {
  Ljapgolly_scalajs_react_ReactComponentC$: 1,
  O: 1
});
$c_Ljapgolly_scalajs_react_ReactComponentC$.prototype.$classData = $d_Ljapgolly_scalajs_react_ReactComponentC$;
var $n_Ljapgolly_scalajs_react_ReactComponentC$ = (void 0);
function $m_Ljapgolly_scalajs_react_ReactComponentC$() {
  if ((!$n_Ljapgolly_scalajs_react_ReactComponentC$)) {
    $n_Ljapgolly_scalajs_react_ReactComponentC$ = new $c_Ljapgolly_scalajs_react_ReactComponentC$().init___()
  };
  return $n_Ljapgolly_scalajs_react_ReactComponentC$
}
/** @constructor */
function $c_Ljapgolly_scalajs_react_vdom_Builder() {
  $c_O.call(this);
  this.className$1 = null;
  this.japgolly$scalajs$react$vdom$Builder$$props$f = null;
  this.japgolly$scalajs$react$vdom$Builder$$style$f = null;
  this.children$1 = null
}
$c_Ljapgolly_scalajs_react_vdom_Builder.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_vdom_Builder.prototype.constructor = $c_Ljapgolly_scalajs_react_vdom_Builder;
/** @constructor */
function $h_Ljapgolly_scalajs_react_vdom_Builder() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_vdom_Builder.prototype = $c_Ljapgolly_scalajs_react_vdom_Builder.prototype;
$c_Ljapgolly_scalajs_react_vdom_Builder.prototype.init___ = (function() {
  this.className$1 = (void 0);
  this.japgolly$scalajs$react$vdom$Builder$$props$f = {};
  this.japgolly$scalajs$react$vdom$Builder$$style$f = {};
  this.children$1 = [];
  return this
});
$c_Ljapgolly_scalajs_react_vdom_Builder.prototype.render__T__Ljapgolly_scalajs_react_ReactElement = (function(tag) {
  var value = this.className$1;
  if ((value !== (void 0))) {
    $m_Ljapgolly_scalajs_react_vdom_Builder$();
    var o = this.japgolly$scalajs$react$vdom$Builder$$props$f;
    o.className = value
  };
  if (($uI($g.Object.keys(this.japgolly$scalajs$react$vdom$Builder$$style$f).length) !== 0)) {
    $m_Ljapgolly_scalajs_react_vdom_Builder$();
    var o$1 = this.japgolly$scalajs$react$vdom$Builder$$props$f;
    var v = this.japgolly$scalajs$react$vdom$Builder$$style$f;
    o$1.style = v
  };
  return $m_Ljapgolly_scalajs_react_vdom_Builder$().buildFn$1.apply__O__O__O__O(tag, this.japgolly$scalajs$react$vdom$Builder$$props$f, this.children$1)
});
$c_Ljapgolly_scalajs_react_vdom_Builder.prototype.addAttr__T__sjs_js_Any__V = (function(k, v) {
  $m_Ljapgolly_scalajs_react_vdom_Builder$();
  var o = this.japgolly$scalajs$react$vdom$Builder$$props$f;
  o[k] = v
});
$c_Ljapgolly_scalajs_react_vdom_Builder.prototype.appendChild__Ljapgolly_scalajs_react_ReactNode__V = (function(c) {
  this.children$1.push(c)
});
var $d_Ljapgolly_scalajs_react_vdom_Builder = new $TypeData().initClass({
  Ljapgolly_scalajs_react_vdom_Builder: 0
}, false, "japgolly.scalajs.react.vdom.Builder", {
  Ljapgolly_scalajs_react_vdom_Builder: 1,
  O: 1
});
$c_Ljapgolly_scalajs_react_vdom_Builder.prototype.$classData = $d_Ljapgolly_scalajs_react_vdom_Builder;
/** @constructor */
function $c_Ljapgolly_scalajs_react_vdom_Builder$() {
  $c_O.call(this);
  this.buildFn$1 = null
}
$c_Ljapgolly_scalajs_react_vdom_Builder$.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_vdom_Builder$.prototype.constructor = $c_Ljapgolly_scalajs_react_vdom_Builder$;
/** @constructor */
function $h_Ljapgolly_scalajs_react_vdom_Builder$() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_vdom_Builder$.prototype = $c_Ljapgolly_scalajs_react_vdom_Builder$.prototype;
$c_Ljapgolly_scalajs_react_vdom_Builder$.prototype.init___ = (function() {
  $n_Ljapgolly_scalajs_react_vdom_Builder$ = this;
  this.buildFn$1 = new $c_sjsr_AnonFunction3().init___sjs_js_Function3((function(tag$2, props$2, children$2) {
    var tag = $as_T(tag$2);
    var jsx$1 = $g.React;
    return jsx$1.createElement.apply(jsx$1, [tag, props$2].concat(children$2))
  }));
  return this
});
var $d_Ljapgolly_scalajs_react_vdom_Builder$ = new $TypeData().initClass({
  Ljapgolly_scalajs_react_vdom_Builder$: 0
}, false, "japgolly.scalajs.react.vdom.Builder$", {
  Ljapgolly_scalajs_react_vdom_Builder$: 1,
  O: 1
});
$c_Ljapgolly_scalajs_react_vdom_Builder$.prototype.$classData = $d_Ljapgolly_scalajs_react_vdom_Builder$;
var $n_Ljapgolly_scalajs_react_vdom_Builder$ = (void 0);
function $m_Ljapgolly_scalajs_react_vdom_Builder$() {
  if ((!$n_Ljapgolly_scalajs_react_vdom_Builder$)) {
    $n_Ljapgolly_scalajs_react_vdom_Builder$ = new $c_Ljapgolly_scalajs_react_vdom_Builder$().init___()
  };
  return $n_Ljapgolly_scalajs_react_vdom_Builder$
}
/** @constructor */
function $c_Ljapgolly_scalajs_react_vdom_Escaping$() {
  $c_O.call(this);
  this.tagRegex$1 = null;
  this.attrNameRegex$1 = null;
  this.bitmap$0$1 = 0
}
$c_Ljapgolly_scalajs_react_vdom_Escaping$.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_vdom_Escaping$.prototype.constructor = $c_Ljapgolly_scalajs_react_vdom_Escaping$;
/** @constructor */
function $h_Ljapgolly_scalajs_react_vdom_Escaping$() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_vdom_Escaping$.prototype = $c_Ljapgolly_scalajs_react_vdom_Escaping$.prototype;
$c_Ljapgolly_scalajs_react_vdom_Escaping$.prototype.init___ = (function() {
  return this
});
$c_Ljapgolly_scalajs_react_vdom_Escaping$.prototype.attrNameRegex__p1__ju_regex_Pattern = (function() {
  return (((2 & this.bitmap$0$1) === 0) ? this.attrNameRegex$lzycompute__p1__ju_regex_Pattern() : this.attrNameRegex$1)
});
$c_Ljapgolly_scalajs_react_vdom_Escaping$.prototype.validAttrName__p1__T__Z = (function(s) {
  var this$1 = this.attrNameRegex__p1__ju_regex_Pattern();
  return new $c_ju_regex_Matcher().init___ju_regex_Pattern__jl_CharSequence__I__I(this$1, s, 0, $uI(s.length)).matches__Z()
});
$c_Ljapgolly_scalajs_react_vdom_Escaping$.prototype.tagRegex$lzycompute__p1__ju_regex_Pattern = (function() {
  if (((1 & this.bitmap$0$1) === 0)) {
    var this$2 = new $c_sci_StringOps().init___T("^[a-z][\\w0-9-]*$");
    var groupNames = $m_sci_Nil$();
    var $$this = this$2.repr$1;
    this.tagRegex$1 = new $c_s_util_matching_Regex().init___T__sc_Seq($$this, groupNames).pattern$1;
    this.bitmap$0$1 = (1 | this.bitmap$0$1)
  };
  return this.tagRegex$1
});
$c_Ljapgolly_scalajs_react_vdom_Escaping$.prototype.assertValidTag__T__V = (function(s) {
  if ((!this.validTag__p1__T__Z(s))) {
    throw new $c_jl_IllegalArgumentException().init___T(new $c_s_StringContext().init___sc_Seq(new $c_sjs_js_WrappedArray().init___sjs_js_Array(["Illegal tag name: ", " is not a valid XML tag name"])).s__sc_Seq__T(new $c_sjs_js_WrappedArray().init___sjs_js_Array([s])))
  }
});
$c_Ljapgolly_scalajs_react_vdom_Escaping$.prototype.attrNameRegex$lzycompute__p1__ju_regex_Pattern = (function() {
  if (((2 & this.bitmap$0$1) === 0)) {
    var this$2 = new $c_sci_StringOps().init___T("^[a-zA-Z_:][-a-zA-Z0-9_:.]*$");
    var groupNames = $m_sci_Nil$();
    var $$this = this$2.repr$1;
    this.attrNameRegex$1 = new $c_s_util_matching_Regex().init___T__sc_Seq($$this, groupNames).pattern$1;
    this.bitmap$0$1 = (2 | this.bitmap$0$1)
  };
  return this.attrNameRegex$1
});
$c_Ljapgolly_scalajs_react_vdom_Escaping$.prototype.validTag__p1__T__Z = (function(s) {
  var this$1 = this.tagRegex__p1__ju_regex_Pattern();
  return new $c_ju_regex_Matcher().init___ju_regex_Pattern__jl_CharSequence__I__I(this$1, s, 0, $uI(s.length)).matches__Z()
});
$c_Ljapgolly_scalajs_react_vdom_Escaping$.prototype.assertValidAttrName__T__V = (function(s) {
  if ((!this.validAttrName__p1__T__Z(s))) {
    throw new $c_jl_IllegalArgumentException().init___T(new $c_s_StringContext().init___sc_Seq(new $c_sjs_js_WrappedArray().init___sjs_js_Array(["Illegal attribute name: ", " is not a valid XML attribute name"])).s__sc_Seq__T(new $c_sjs_js_WrappedArray().init___sjs_js_Array([s])))
  }
});
$c_Ljapgolly_scalajs_react_vdom_Escaping$.prototype.tagRegex__p1__ju_regex_Pattern = (function() {
  return (((1 & this.bitmap$0$1) === 0) ? this.tagRegex$lzycompute__p1__ju_regex_Pattern() : this.tagRegex$1)
});
var $d_Ljapgolly_scalajs_react_vdom_Escaping$ = new $TypeData().initClass({
  Ljapgolly_scalajs_react_vdom_Escaping$: 0
}, false, "japgolly.scalajs.react.vdom.Escaping$", {
  Ljapgolly_scalajs_react_vdom_Escaping$: 1,
  O: 1
});
$c_Ljapgolly_scalajs_react_vdom_Escaping$.prototype.$classData = $d_Ljapgolly_scalajs_react_vdom_Escaping$;
var $n_Ljapgolly_scalajs_react_vdom_Escaping$ = (void 0);
function $m_Ljapgolly_scalajs_react_vdom_Escaping$() {
  if ((!$n_Ljapgolly_scalajs_react_vdom_Escaping$)) {
    $n_Ljapgolly_scalajs_react_vdom_Escaping$ = new $c_Ljapgolly_scalajs_react_vdom_Escaping$().init___()
  };
  return $n_Ljapgolly_scalajs_react_vdom_Escaping$
}
/** @constructor */
function $c_Ljapgolly_scalajs_react_vdom_Extra$AttrExt$() {
  $c_O.call(this)
}
$c_Ljapgolly_scalajs_react_vdom_Extra$AttrExt$.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_vdom_Extra$AttrExt$.prototype.constructor = $c_Ljapgolly_scalajs_react_vdom_Extra$AttrExt$;
/** @constructor */
function $h_Ljapgolly_scalajs_react_vdom_Extra$AttrExt$() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_vdom_Extra$AttrExt$.prototype = $c_Ljapgolly_scalajs_react_vdom_Extra$AttrExt$.prototype;
$c_Ljapgolly_scalajs_react_vdom_Extra$AttrExt$.prototype.init___ = (function() {
  return this
});
$c_Ljapgolly_scalajs_react_vdom_Extra$AttrExt$.prototype.$$minus$minus$greater$extension__Ljapgolly_scalajs_react_vdom_ReactAttr__F0__Ljapgolly_scalajs_react_vdom_DomCallbackResult__Ljapgolly_scalajs_react_vdom_TagMod = (function($$this, callback, evidence$1) {
  return $$this.$$colon$eq__O__F2__Ljapgolly_scalajs_react_vdom_TagMod((function(callback$1) {
    return (function() {
      var $$this$1 = $as_Ljapgolly_scalajs_react_CallbackTo(callback$1.apply__O()).japgolly$scalajs$react$CallbackTo$$f$1;
      return $$this$1.apply__O()
    })
  })(callback), $m_Ljapgolly_scalajs_react_vdom_Implicits$().$$undreact$undattrJsFn$2)
});
$c_Ljapgolly_scalajs_react_vdom_Extra$AttrExt$.prototype.$$eq$eq$greater$extension__Ljapgolly_scalajs_react_vdom_ReactAttr__F1__Ljapgolly_scalajs_react_vdom_DomCallbackResult__Ljapgolly_scalajs_react_vdom_TagMod = (function($$this, eventHandler, evidence$2) {
  return $$this.$$colon$eq__O__F2__Ljapgolly_scalajs_react_vdom_TagMod((function(eventHandler$1) {
    return (function(e$2) {
      var $$this$1 = $as_Ljapgolly_scalajs_react_CallbackTo(eventHandler$1.apply__O__O(e$2)).japgolly$scalajs$react$CallbackTo$$f$1;
      return $$this$1.apply__O()
    })
  })(eventHandler), $m_Ljapgolly_scalajs_react_vdom_Implicits$().$$undreact$undattrJsFn$2)
});
var $d_Ljapgolly_scalajs_react_vdom_Extra$AttrExt$ = new $TypeData().initClass({
  Ljapgolly_scalajs_react_vdom_Extra$AttrExt$: 0
}, false, "japgolly.scalajs.react.vdom.Extra$AttrExt$", {
  Ljapgolly_scalajs_react_vdom_Extra$AttrExt$: 1,
  O: 1
});
$c_Ljapgolly_scalajs_react_vdom_Extra$AttrExt$.prototype.$classData = $d_Ljapgolly_scalajs_react_vdom_Extra$AttrExt$;
var $n_Ljapgolly_scalajs_react_vdom_Extra$AttrExt$ = (void 0);
function $m_Ljapgolly_scalajs_react_vdom_Extra$AttrExt$() {
  if ((!$n_Ljapgolly_scalajs_react_vdom_Extra$AttrExt$)) {
    $n_Ljapgolly_scalajs_react_vdom_Extra$AttrExt$ = new $c_Ljapgolly_scalajs_react_vdom_Extra$AttrExt$().init___()
  };
  return $n_Ljapgolly_scalajs_react_vdom_Extra$AttrExt$
}
/** @constructor */
function $c_Ljapgolly_scalajs_react_vdom_LowPri() {
  $c_O.call(this)
}
$c_Ljapgolly_scalajs_react_vdom_LowPri.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_vdom_LowPri.prototype.constructor = $c_Ljapgolly_scalajs_react_vdom_LowPri;
/** @constructor */
function $h_Ljapgolly_scalajs_react_vdom_LowPri() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_vdom_LowPri.prototype = $c_Ljapgolly_scalajs_react_vdom_LowPri.prototype;
/** @constructor */
function $c_Ljapgolly_scalajs_react_vdom_NamespaceHtml$() {
  $c_O.call(this);
  this.implicitNamespace$1 = null
}
$c_Ljapgolly_scalajs_react_vdom_NamespaceHtml$.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_vdom_NamespaceHtml$.prototype.constructor = $c_Ljapgolly_scalajs_react_vdom_NamespaceHtml$;
/** @constructor */
function $h_Ljapgolly_scalajs_react_vdom_NamespaceHtml$() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_vdom_NamespaceHtml$.prototype = $c_Ljapgolly_scalajs_react_vdom_NamespaceHtml$.prototype;
$c_Ljapgolly_scalajs_react_vdom_NamespaceHtml$.prototype.init___ = (function() {
  this.implicitNamespace$1 = "http://www.w3.org/1999/xhtml";
  return this
});
var $d_Ljapgolly_scalajs_react_vdom_NamespaceHtml$ = new $TypeData().initClass({
  Ljapgolly_scalajs_react_vdom_NamespaceHtml$: 0
}, false, "japgolly.scalajs.react.vdom.NamespaceHtml$", {
  Ljapgolly_scalajs_react_vdom_NamespaceHtml$: 1,
  O: 1
});
$c_Ljapgolly_scalajs_react_vdom_NamespaceHtml$.prototype.$classData = $d_Ljapgolly_scalajs_react_vdom_NamespaceHtml$;
var $n_Ljapgolly_scalajs_react_vdom_NamespaceHtml$ = (void 0);
function $m_Ljapgolly_scalajs_react_vdom_NamespaceHtml$() {
  if ((!$n_Ljapgolly_scalajs_react_vdom_NamespaceHtml$)) {
    $n_Ljapgolly_scalajs_react_vdom_NamespaceHtml$ = new $c_Ljapgolly_scalajs_react_vdom_NamespaceHtml$().init___()
  };
  return $n_Ljapgolly_scalajs_react_vdom_NamespaceHtml$
}
/** @constructor */
function $c_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$() {
  $c_O.call(this);
  this.string$1 = null
}
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$.prototype.constructor = $c_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$;
/** @constructor */
function $h_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$.prototype = $c_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$.prototype;
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$.prototype.init___ = (function() {
  $n_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$ = this;
  var fn = new $c_sjsr_AnonFunction2().init___sjs_js_Function2((function(x$3$2, x$4$2) {
    var x$3 = $as_F1(x$3$2);
    var x$4 = $as_T(x$4$2);
    x$3.apply__O__O(x$4)
  }));
  this.string$1 = fn;
  return this
});
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$.prototype.map__F1__F2 = (function(f) {
  var fn = new $c_sjsr_AnonFunction2().init___sjs_js_Function2((function(f$1) {
    return (function(b$2, a$2) {
      var b = $as_F1(b$2);
      b.apply__O__O(f$1.apply__O__O(a$2))
    })
  })(f));
  return fn
});
var $d_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$ = new $TypeData().initClass({
  Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$: 0
}, false, "japgolly.scalajs.react.vdom.ReactAttr$ValueType$", {
  Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$: 1,
  O: 1
});
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$.prototype.$classData = $d_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$;
var $n_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$ = (void 0);
function $m_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$() {
  if ((!$n_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$)) {
    $n_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$ = new $c_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$().init___()
  };
  return $n_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$
}
/** @constructor */
function $c_Ljapgolly_scalajs_react_vdom_ReactStyle$ValueType$() {
  $c_O.call(this);
  this.string$1 = null
}
$c_Ljapgolly_scalajs_react_vdom_ReactStyle$ValueType$.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_vdom_ReactStyle$ValueType$.prototype.constructor = $c_Ljapgolly_scalajs_react_vdom_ReactStyle$ValueType$;
/** @constructor */
function $h_Ljapgolly_scalajs_react_vdom_ReactStyle$ValueType$() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_vdom_ReactStyle$ValueType$.prototype = $c_Ljapgolly_scalajs_react_vdom_ReactStyle$ValueType$.prototype;
$c_Ljapgolly_scalajs_react_vdom_ReactStyle$ValueType$.prototype.init___ = (function() {
  $n_Ljapgolly_scalajs_react_vdom_ReactStyle$ValueType$ = this;
  var fn = new $c_sjsr_AnonFunction2().init___sjs_js_Function2((function(x$3$2, x$4$2) {
    var x$3 = $as_F1(x$3$2);
    var x$4 = $as_T(x$4$2);
    x$3.apply__O__O(x$4)
  }));
  this.string$1 = fn;
  return this
});
$c_Ljapgolly_scalajs_react_vdom_ReactStyle$ValueType$.prototype.stringValue__F2 = (function() {
  var fn = new $c_sjsr_AnonFunction2().init___sjs_js_Function2((function(b$2, a$2) {
    var b = $as_F1(b$2);
    b.apply__O__O($objectToString(a$2))
  }));
  return fn
});
var $d_Ljapgolly_scalajs_react_vdom_ReactStyle$ValueType$ = new $TypeData().initClass({
  Ljapgolly_scalajs_react_vdom_ReactStyle$ValueType$: 0
}, false, "japgolly.scalajs.react.vdom.ReactStyle$ValueType$", {
  Ljapgolly_scalajs_react_vdom_ReactStyle$ValueType$: 1,
  O: 1
});
$c_Ljapgolly_scalajs_react_vdom_ReactStyle$ValueType$.prototype.$classData = $d_Ljapgolly_scalajs_react_vdom_ReactStyle$ValueType$;
var $n_Ljapgolly_scalajs_react_vdom_ReactStyle$ValueType$ = (void 0);
function $m_Ljapgolly_scalajs_react_vdom_ReactStyle$ValueType$() {
  if ((!$n_Ljapgolly_scalajs_react_vdom_ReactStyle$ValueType$)) {
    $n_Ljapgolly_scalajs_react_vdom_ReactStyle$ValueType$ = new $c_Ljapgolly_scalajs_react_vdom_ReactStyle$ValueType$().init___()
  };
  return $n_Ljapgolly_scalajs_react_vdom_ReactStyle$ValueType$
}
/** @constructor */
function $c_Lsp_widgets_TestWidget() {
  $c_O.call(this);
  this.RootComponent$1 = null;
  this.SubComponent$1 = null
}
$c_Lsp_widgets_TestWidget.prototype = new $h_O();
$c_Lsp_widgets_TestWidget.prototype.constructor = $c_Lsp_widgets_TestWidget;
/** @constructor */
function $h_Lsp_widgets_TestWidget() {
  /*<skip>*/
}
$h_Lsp_widgets_TestWidget.prototype = $c_Lsp_widgets_TestWidget.prototype;
$c_Lsp_widgets_TestWidget.prototype.init___ = (function() {
  $m_Ljapgolly_scalajs_react_ReactComponentB$();
  var x = ($m_Ljapgolly_scalajs_react_ReactComponentB$(), new $c_Ljapgolly_scalajs_react_ReactComponentB$P().init___T("RootComponent")).initialState__F0__Ljapgolly_scalajs_react_ReactComponentB$PS(new $c_sjsr_AnonFunction0().init___sjs_js_Function0((function() {
    return "RootComponent's input"
  }))).backend__F1__Ljapgolly_scalajs_react_ReactComponentB$PSB(new $c_sjsr_AnonFunction1().init___sjs_js_Function1((function(arg$outer) {
    return (function(x$2) {
      return new $c_Lsp_widgets_TestWidget$RootBackend().init___Lsp_widgets_TestWidget__Ljapgolly_scalajs_react_BackendScope(arg$outer, x$2)
    })
  })(this))).render__F1__Ljapgolly_scalajs_react_ReactComponentB$PSBR(new $c_sjsr_AnonFunction1().init___sjs_js_Function1((function(x$2$1) {
    $m_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$();
    var jsx$1 = $as_Lsp_widgets_TestWidget$RootBackend(x$2$1.backend);
    $m_Ljapgolly_scalajs_react_package$();
    var this$3 = new $c_Ljapgolly_scalajs_react_CompState$ReadDirectWriteCallback().init___O__Ljapgolly_scalajs_react_CompState$Accessor(x$2$1, $m_Ljapgolly_scalajs_react_CompState$RootAccessor$().instance$1);
    var this$4 = this$3.a$1;
    var $$ = this$3.$$$1;
    var t = jsx$1.render__T__Ljapgolly_scalajs_react_vdom_ReactTagOf($as_T(this$4.state__Ljapgolly_scalajs_react_CompScope$CanSetState__O($$)));
    return t.render__Ljapgolly_scalajs_react_ReactElement()
  })));
  var w = $m_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$().buildResultUnit__Ljapgolly_scalajs_react_ReactComponentB$BuildResult();
  $m_Ljapgolly_scalajs_react_ReactComponentB$();
  var this$8 = x.domType__Ljapgolly_scalajs_react_ReactComponentB();
  var buildFn = w.apply$2;
  this.RootComponent$1 = $as_Ljapgolly_scalajs_react_ReactComponentC$ConstProps(new $c_Ljapgolly_scalajs_react_ReactComponentB$Builder().init___Ljapgolly_scalajs_react_ReactComponentB__F1(this$8, buildFn).build__O()).reactClass$2;
  $m_Ljapgolly_scalajs_react_ReactComponentB$();
  var x$1 = ($m_Ljapgolly_scalajs_react_ReactComponentB$(), new $c_Ljapgolly_scalajs_react_ReactComponentB$P().init___T("SubComponent")).initialState__F0__Ljapgolly_scalajs_react_ReactComponentB$PS(new $c_sjsr_AnonFunction0().init___sjs_js_Function0((function() {
    return "SubComponents's input = its title"
  }))).backend__F1__Ljapgolly_scalajs_react_ReactComponentB$PSB(new $c_sjsr_AnonFunction1().init___sjs_js_Function1((function(arg$outer$1) {
    return (function(x$2$2) {
      return new $c_Lsp_widgets_TestWidget$SubBackend().init___Lsp_widgets_TestWidget__Ljapgolly_scalajs_react_BackendScope(arg$outer$1, x$2$2)
    })
  })(this))).render__F1__Ljapgolly_scalajs_react_ReactComponentB$PSBR(new $c_sjsr_AnonFunction1().init___sjs_js_Function1((function(x$2$3) {
    $m_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$();
    var jsx$2 = $as_Lsp_widgets_TestWidget$SubBackend(x$2$3.backend);
    $m_Ljapgolly_scalajs_react_package$();
    var this$11 = new $c_Ljapgolly_scalajs_react_CompState$ReadDirectWriteCallback().init___O__Ljapgolly_scalajs_react_CompState$Accessor(x$2$3, $m_Ljapgolly_scalajs_react_CompState$RootAccessor$().instance$1);
    var this$12 = this$11.a$1;
    var $$$1 = this$11.$$$1;
    var t$1 = jsx$2.render__T__Ljapgolly_scalajs_react_vdom_ReactTagOf($as_T(this$12.state__Ljapgolly_scalajs_react_CompScope$CanSetState__O($$$1)));
    return t$1.render__Ljapgolly_scalajs_react_ReactElement()
  })));
  var w$1 = $m_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$().buildResultUnit__Ljapgolly_scalajs_react_ReactComponentB$BuildResult();
  $m_Ljapgolly_scalajs_react_ReactComponentB$();
  var this$16 = x$1.domType__Ljapgolly_scalajs_react_ReactComponentB();
  var buildFn$1 = w$1.apply$2;
  this.SubComponent$1 = $as_Ljapgolly_scalajs_react_ReactComponentC$ConstProps(new $c_Ljapgolly_scalajs_react_ReactComponentB$Builder().init___Ljapgolly_scalajs_react_ReactComponentB__F1(this$16, buildFn$1).build__O());
  return this
});
$c_Lsp_widgets_TestWidget.prototype.$$js$exported$prop$getComponent__O = (function() {
  return this.RootComponent$1
});
Object.defineProperty($c_Lsp_widgets_TestWidget.prototype, "getComponent", {
  "get": (function() {
    return this.$$js$exported$prop$getComponent__O()
  }),
  "enumerable": true
});
var $d_Lsp_widgets_TestWidget = new $TypeData().initClass({
  Lsp_widgets_TestWidget: 0
}, false, "sp.widgets.TestWidget", {
  Lsp_widgets_TestWidget: 1,
  O: 1
});
$c_Lsp_widgets_TestWidget.prototype.$classData = $d_Lsp_widgets_TestWidget;
$e.sp = ($e.sp || {});
$e.sp.widgets = ($e.sp.widgets || {});
/** @constructor */
$e.sp.widgets.TestWidget = (function() {
  var $thiz = new $c_Lsp_widgets_TestWidget();
  $c_Lsp_widgets_TestWidget.prototype.init___.call($thiz);
  return $thiz
});
$e.sp.widgets.TestWidget.prototype = $c_Lsp_widgets_TestWidget.prototype;
/** @constructor */
function $c_Lsp_widgets_TestWidget$RootBackend() {
  $c_O.call(this);
  this.$$$1 = null;
  this.$$outer$f = null
}
$c_Lsp_widgets_TestWidget$RootBackend.prototype = new $h_O();
$c_Lsp_widgets_TestWidget$RootBackend.prototype.constructor = $c_Lsp_widgets_TestWidget$RootBackend;
/** @constructor */
function $h_Lsp_widgets_TestWidget$RootBackend() {
  /*<skip>*/
}
$h_Lsp_widgets_TestWidget$RootBackend.prototype = $c_Lsp_widgets_TestWidget$RootBackend.prototype;
$c_Lsp_widgets_TestWidget$RootBackend.prototype.render__T__Ljapgolly_scalajs_react_vdom_ReactTagOf = (function(state) {
  var jsx$8 = ($m_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$(), $m_Ljapgolly_scalajs_react_vdom_package$Tags$()).div$1;
  var jsx$7 = ($m_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$(), $m_Ljapgolly_scalajs_react_vdom_package$Tags$()).div$1;
  var jsx$6 = $m_Ljapgolly_scalajs_react_vdom_Extra$AttrExt$();
  $m_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$();
  var a = ($m_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$(), $m_Ljapgolly_scalajs_react_vdom_package$Attrs$()).onClick$1;
  var jsx$5 = jsx$7.apply__sc_Seq__Ljapgolly_scalajs_react_vdom_ReactTagOf(new $c_sjs_js_WrappedArray().init___sjs_js_Array([jsx$6.$$minus$minus$greater$extension__Ljapgolly_scalajs_react_vdom_ReactAttr__F0__Ljapgolly_scalajs_react_vdom_DomCallbackResult__Ljapgolly_scalajs_react_vdom_TagMod(a, new $c_sjsr_AnonFunction0().init___sjs_js_Function0((function(arg$outer) {
    return (function() {
      return new $c_Ljapgolly_scalajs_react_CallbackTo().init___F0(arg$outer.clearAndFocusInput__F0())
    })
  })(this)), null), ($m_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$(), new $c_Ljapgolly_scalajs_react_vdom_ReactNodeFrag().init___Ljapgolly_scalajs_react_ReactNode(($m_Ljapgolly_scalajs_react_package$(), "This text is static content in TestWidget")))]));
  var jsx$4 = ($m_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$(), $m_Ljapgolly_scalajs_react_vdom_package$Tags$()).input__Ljapgolly_scalajs_react_vdom_HtmlTags$input$();
  var this$11 = ($m_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$(), $m_Ljapgolly_scalajs_react_vdom_package$Attrs$()).value$1;
  $m_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$();
  var t = $m_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$().string$1;
  var jsx$3 = new $c_Ljapgolly_scalajs_react_vdom_ReactAttr$NameAndValue().init___T__O__F2(this$11.name$1, state, t);
  var jsx$2 = $m_Ljapgolly_scalajs_react_vdom_Extra$AttrExt$();
  $m_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$();
  var a$1 = ($m_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$(), $m_Ljapgolly_scalajs_react_vdom_package$Attrs$()).onChange$1;
  var jsx$1 = jsx$4.apply__sc_Seq__Ljapgolly_scalajs_react_vdom_ReactTagOf(new $c_sjs_js_WrappedArray().init___sjs_js_Array([jsx$3, jsx$2.$$eq$eq$greater$extension__Ljapgolly_scalajs_react_vdom_ReactAttr__F1__Ljapgolly_scalajs_react_vdom_DomCallbackResult__Ljapgolly_scalajs_react_vdom_TagMod(a$1, new $c_sjsr_AnonFunction1().init___sjs_js_Function1((function(arg$outer$1) {
    return (function(e$2) {
      return new $c_Ljapgolly_scalajs_react_CallbackTo().init___F0(arg$outer$1.handleChange__Ljapgolly_scalajs_react_SyntheticEvent__F0(e$2))
    })
  })(this)), null)]));
  $m_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$();
  var v = this.$$outer$f.SubComponent$1.apply__sc_Seq__Ljapgolly_scalajs_react_ReactComponentU($m_sci_Nil$());
  return jsx$8.apply__sc_Seq__Ljapgolly_scalajs_react_vdom_ReactTagOf(new $c_sjs_js_WrappedArray().init___sjs_js_Array([jsx$5, jsx$1, new $c_Ljapgolly_scalajs_react_vdom_ReactNodeFrag().init___Ljapgolly_scalajs_react_ReactNode(v)]))
});
$c_Lsp_widgets_TestWidget$RootBackend.prototype.init___Lsp_widgets_TestWidget__Ljapgolly_scalajs_react_BackendScope = (function($$outer, $$) {
  this.$$$1 = $$;
  if (($$outer === null)) {
    throw $m_sjsr_package$().unwrapJavaScriptException__jl_Throwable__O(null)
  } else {
    this.$$outer$f = $$outer
  };
  return this
});
$c_Lsp_widgets_TestWidget$RootBackend.prototype.clearAndFocusInput__F0 = (function() {
  $m_Ljapgolly_scalajs_react_package$();
  var $$ = this.$$$1;
  var qual$1 = new $c_Ljapgolly_scalajs_react_CompState$ReadCallbackWriteCallback().init___O__Ljapgolly_scalajs_react_CompState$Accessor($$, $m_Ljapgolly_scalajs_react_CompState$RootAccessor$().instance$1);
  var x$2 = $m_Ljapgolly_scalajs_react_package$().Callback$1.empty$1;
  return $s_Ljapgolly_scalajs_react_CompState$WriteCallbackOps$class__setState__Ljapgolly_scalajs_react_CompState$WriteCallbackOps__O__F0__F0(qual$1, "", x$2)
});
$c_Lsp_widgets_TestWidget$RootBackend.prototype.handleChange__Ljapgolly_scalajs_react_SyntheticEvent__F0 = (function(e) {
  $m_Ljapgolly_scalajs_react_package$();
  var $$ = this.$$$1;
  var qual$2 = new $c_Ljapgolly_scalajs_react_CompState$ReadCallbackWriteCallback().init___O__Ljapgolly_scalajs_react_CompState$Accessor($$, $m_Ljapgolly_scalajs_react_CompState$RootAccessor$().instance$1);
  var x$3 = $as_T(e.target.value);
  var x$4 = $m_Ljapgolly_scalajs_react_package$().Callback$1.empty$1;
  return $s_Ljapgolly_scalajs_react_CompState$WriteCallbackOps$class__setState__Ljapgolly_scalajs_react_CompState$WriteCallbackOps__O__F0__F0(qual$2, x$3, x$4)
});
function $is_Lsp_widgets_TestWidget$RootBackend(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.Lsp_widgets_TestWidget$RootBackend)))
}
function $as_Lsp_widgets_TestWidget$RootBackend(obj) {
  return (($is_Lsp_widgets_TestWidget$RootBackend(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "sp.widgets.TestWidget$RootBackend"))
}
function $isArrayOf_Lsp_widgets_TestWidget$RootBackend(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.Lsp_widgets_TestWidget$RootBackend)))
}
function $asArrayOf_Lsp_widgets_TestWidget$RootBackend(obj, depth) {
  return (($isArrayOf_Lsp_widgets_TestWidget$RootBackend(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Lsp.widgets.TestWidget$RootBackend;", depth))
}
var $d_Lsp_widgets_TestWidget$RootBackend = new $TypeData().initClass({
  Lsp_widgets_TestWidget$RootBackend: 0
}, false, "sp.widgets.TestWidget$RootBackend", {
  Lsp_widgets_TestWidget$RootBackend: 1,
  O: 1
});
$c_Lsp_widgets_TestWidget$RootBackend.prototype.$classData = $d_Lsp_widgets_TestWidget$RootBackend;
/** @constructor */
function $c_Lsp_widgets_TestWidget$SubBackend() {
  $c_O.call(this);
  this.$$$1 = null;
  this.$$outer$f = null
}
$c_Lsp_widgets_TestWidget$SubBackend.prototype = new $h_O();
$c_Lsp_widgets_TestWidget$SubBackend.prototype.constructor = $c_Lsp_widgets_TestWidget$SubBackend;
/** @constructor */
function $h_Lsp_widgets_TestWidget$SubBackend() {
  /*<skip>*/
}
$h_Lsp_widgets_TestWidget$SubBackend.prototype = $c_Lsp_widgets_TestWidget$SubBackend.prototype;
$c_Lsp_widgets_TestWidget$SubBackend.prototype.render__T__Ljapgolly_scalajs_react_vdom_ReactTagOf = (function(state) {
  var jsx$7 = ($m_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$(), $m_Ljapgolly_scalajs_react_vdom_package$Tags$()).div$1;
  var jsx$6 = ($m_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$(), $m_Ljapgolly_scalajs_react_vdom_package$Tags$()).div$1;
  var jsx$5 = $m_Ljapgolly_scalajs_react_vdom_Extra$AttrExt$();
  $m_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$();
  var a = ($m_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$(), $m_Ljapgolly_scalajs_react_vdom_package$Attrs$()).onClick$1;
  var jsx$4 = jsx$6.apply__sc_Seq__Ljapgolly_scalajs_react_vdom_ReactTagOf(new $c_sjs_js_WrappedArray().init___sjs_js_Array([jsx$5.$$minus$minus$greater$extension__Ljapgolly_scalajs_react_vdom_ReactAttr__F0__Ljapgolly_scalajs_react_vdom_DomCallbackResult__Ljapgolly_scalajs_react_vdom_TagMod(a, new $c_sjsr_AnonFunction0().init___sjs_js_Function0((function(arg$outer) {
    return (function() {
      return new $c_Ljapgolly_scalajs_react_CallbackTo().init___F0(arg$outer.clearAndFocusInput__F0())
    })
  })(this)), null), ($m_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$(), new $c_Ljapgolly_scalajs_react_vdom_ReactNodeFrag().init___Ljapgolly_scalajs_react_ReactNode(($m_Ljapgolly_scalajs_react_package$(), state)))]));
  var jsx$3 = ($m_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$(), $m_Ljapgolly_scalajs_react_vdom_package$Tags$()).input__Ljapgolly_scalajs_react_vdom_HtmlTags$input$();
  var this$11 = ($m_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$(), $m_Ljapgolly_scalajs_react_vdom_package$Attrs$()).value$1;
  $m_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$();
  var t = $m_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$().string$1;
  var jsx$2 = new $c_Ljapgolly_scalajs_react_vdom_ReactAttr$NameAndValue().init___T__O__F2(this$11.name$1, state, t);
  var jsx$1 = $m_Ljapgolly_scalajs_react_vdom_Extra$AttrExt$();
  $m_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$();
  var a$1 = ($m_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$(), $m_Ljapgolly_scalajs_react_vdom_package$Attrs$()).onChange$1;
  return jsx$7.apply__sc_Seq__Ljapgolly_scalajs_react_vdom_ReactTagOf(new $c_sjs_js_WrappedArray().init___sjs_js_Array([jsx$4, jsx$3.apply__sc_Seq__Ljapgolly_scalajs_react_vdom_ReactTagOf(new $c_sjs_js_WrappedArray().init___sjs_js_Array([jsx$2, jsx$1.$$eq$eq$greater$extension__Ljapgolly_scalajs_react_vdom_ReactAttr__F1__Ljapgolly_scalajs_react_vdom_DomCallbackResult__Ljapgolly_scalajs_react_vdom_TagMod(a$1, new $c_sjsr_AnonFunction1().init___sjs_js_Function1((function(arg$outer$1) {
    return (function(e$2) {
      return new $c_Ljapgolly_scalajs_react_CallbackTo().init___F0(arg$outer$1.handleChange__Ljapgolly_scalajs_react_SyntheticEvent__F0(e$2))
    })
  })(this)), null)]))]))
});
$c_Lsp_widgets_TestWidget$SubBackend.prototype.init___Lsp_widgets_TestWidget__Ljapgolly_scalajs_react_BackendScope = (function($$outer, $$) {
  this.$$$1 = $$;
  if (($$outer === null)) {
    throw $m_sjsr_package$().unwrapJavaScriptException__jl_Throwable__O(null)
  } else {
    this.$$outer$f = $$outer
  };
  return this
});
$c_Lsp_widgets_TestWidget$SubBackend.prototype.clearAndFocusInput__F0 = (function() {
  $m_Ljapgolly_scalajs_react_package$();
  var $$ = this.$$$1;
  var qual$3 = new $c_Ljapgolly_scalajs_react_CompState$ReadCallbackWriteCallback().init___O__Ljapgolly_scalajs_react_CompState$Accessor($$, $m_Ljapgolly_scalajs_react_CompState$RootAccessor$().instance$1);
  var x$6 = $m_Ljapgolly_scalajs_react_package$().Callback$1.empty$1;
  return $s_Ljapgolly_scalajs_react_CompState$WriteCallbackOps$class__setState__Ljapgolly_scalajs_react_CompState$WriteCallbackOps__O__F0__F0(qual$3, "", x$6)
});
$c_Lsp_widgets_TestWidget$SubBackend.prototype.handleChange__Ljapgolly_scalajs_react_SyntheticEvent__F0 = (function(e) {
  $m_Ljapgolly_scalajs_react_package$();
  var $$ = this.$$$1;
  var qual$4 = new $c_Ljapgolly_scalajs_react_CompState$ReadCallbackWriteCallback().init___O__Ljapgolly_scalajs_react_CompState$Accessor($$, $m_Ljapgolly_scalajs_react_CompState$RootAccessor$().instance$1);
  var x$7 = $as_T(e.target.value);
  var x$8 = $m_Ljapgolly_scalajs_react_package$().Callback$1.empty$1;
  return $s_Ljapgolly_scalajs_react_CompState$WriteCallbackOps$class__setState__Ljapgolly_scalajs_react_CompState$WriteCallbackOps__O__F0__F0(qual$4, x$7, x$8)
});
function $is_Lsp_widgets_TestWidget$SubBackend(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.Lsp_widgets_TestWidget$SubBackend)))
}
function $as_Lsp_widgets_TestWidget$SubBackend(obj) {
  return (($is_Lsp_widgets_TestWidget$SubBackend(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "sp.widgets.TestWidget$SubBackend"))
}
function $isArrayOf_Lsp_widgets_TestWidget$SubBackend(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.Lsp_widgets_TestWidget$SubBackend)))
}
function $asArrayOf_Lsp_widgets_TestWidget$SubBackend(obj, depth) {
  return (($isArrayOf_Lsp_widgets_TestWidget$SubBackend(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Lsp.widgets.TestWidget$SubBackend;", depth))
}
var $d_Lsp_widgets_TestWidget$SubBackend = new $TypeData().initClass({
  Lsp_widgets_TestWidget$SubBackend: 0
}, false, "sp.widgets.TestWidget$SubBackend", {
  Lsp_widgets_TestWidget$SubBackend: 1,
  O: 1
});
$c_Lsp_widgets_TestWidget$SubBackend.prototype.$classData = $d_Lsp_widgets_TestWidget$SubBackend;
/** @constructor */
function $c_jl_Class() {
  $c_O.call(this);
  this.data$1 = null
}
$c_jl_Class.prototype = new $h_O();
$c_jl_Class.prototype.constructor = $c_jl_Class;
/** @constructor */
function $h_jl_Class() {
  /*<skip>*/
}
$h_jl_Class.prototype = $c_jl_Class.prototype;
$c_jl_Class.prototype.getName__T = (function() {
  return $as_T(this.data$1.name)
});
$c_jl_Class.prototype.isPrimitive__Z = (function() {
  return $uZ(this.data$1.isPrimitive)
});
$c_jl_Class.prototype.toString__T = (function() {
  return ((this.isInterface__Z() ? "interface " : (this.isPrimitive__Z() ? "" : "class ")) + this.getName__T())
});
$c_jl_Class.prototype.init___jl_ScalaJSClassData = (function(data) {
  this.data$1 = data;
  return this
});
$c_jl_Class.prototype.isInterface__Z = (function() {
  return $uZ(this.data$1.isInterface)
});
var $d_jl_Class = new $TypeData().initClass({
  jl_Class: 0
}, false, "java.lang.Class", {
  jl_Class: 1,
  O: 1
});
$c_jl_Class.prototype.$classData = $d_jl_Class;
/** @constructor */
function $c_s_LowPriorityImplicits() {
  $c_O.call(this)
}
$c_s_LowPriorityImplicits.prototype = new $h_O();
$c_s_LowPriorityImplicits.prototype.constructor = $c_s_LowPriorityImplicits;
/** @constructor */
function $h_s_LowPriorityImplicits() {
  /*<skip>*/
}
$h_s_LowPriorityImplicits.prototype = $c_s_LowPriorityImplicits.prototype;
/** @constructor */
function $c_s_math_Ordered$() {
  $c_O.call(this)
}
$c_s_math_Ordered$.prototype = new $h_O();
$c_s_math_Ordered$.prototype.constructor = $c_s_math_Ordered$;
/** @constructor */
function $h_s_math_Ordered$() {
  /*<skip>*/
}
$h_s_math_Ordered$.prototype = $c_s_math_Ordered$.prototype;
$c_s_math_Ordered$.prototype.init___ = (function() {
  return this
});
var $d_s_math_Ordered$ = new $TypeData().initClass({
  s_math_Ordered$: 0
}, false, "scala.math.Ordered$", {
  s_math_Ordered$: 1,
  O: 1
});
$c_s_math_Ordered$.prototype.$classData = $d_s_math_Ordered$;
var $n_s_math_Ordered$ = (void 0);
function $m_s_math_Ordered$() {
  if ((!$n_s_math_Ordered$)) {
    $n_s_math_Ordered$ = new $c_s_math_Ordered$().init___()
  };
  return $n_s_math_Ordered$
}
/** @constructor */
function $c_s_package$() {
  $c_O.call(this);
  this.AnyRef$1 = null;
  this.Traversable$1 = null;
  this.Iterable$1 = null;
  this.Seq$1 = null;
  this.IndexedSeq$1 = null;
  this.Iterator$1 = null;
  this.List$1 = null;
  this.Nil$1 = null;
  this.$$colon$colon$1 = null;
  this.$$plus$colon$1 = null;
  this.$$colon$plus$1 = null;
  this.Stream$1 = null;
  this.$$hash$colon$colon$1 = null;
  this.Vector$1 = null;
  this.StringBuilder$1 = null;
  this.Range$1 = null;
  this.BigDecimal$1 = null;
  this.BigInt$1 = null;
  this.Equiv$1 = null;
  this.Fractional$1 = null;
  this.Integral$1 = null;
  this.Numeric$1 = null;
  this.Ordered$1 = null;
  this.Ordering$1 = null;
  this.Either$1 = null;
  this.Left$1 = null;
  this.Right$1 = null;
  this.bitmap$0$1 = 0
}
$c_s_package$.prototype = new $h_O();
$c_s_package$.prototype.constructor = $c_s_package$;
/** @constructor */
function $h_s_package$() {
  /*<skip>*/
}
$h_s_package$.prototype = $c_s_package$.prototype;
$c_s_package$.prototype.init___ = (function() {
  $n_s_package$ = this;
  this.AnyRef$1 = new $c_s_package$$anon$1().init___();
  this.Traversable$1 = $m_sc_Traversable$();
  this.Iterable$1 = $m_sc_Iterable$();
  this.Seq$1 = $m_sc_Seq$();
  this.IndexedSeq$1 = $m_sc_IndexedSeq$();
  this.Iterator$1 = $m_sc_Iterator$();
  this.List$1 = $m_sci_List$();
  this.Nil$1 = $m_sci_Nil$();
  this.$$colon$colon$1 = $m_sci_$colon$colon$();
  this.$$plus$colon$1 = $m_sc_$plus$colon$();
  this.$$colon$plus$1 = $m_sc_$colon$plus$();
  this.Stream$1 = $m_sci_Stream$();
  this.$$hash$colon$colon$1 = $m_sci_Stream$$hash$colon$colon$();
  this.Vector$1 = $m_sci_Vector$();
  this.StringBuilder$1 = $m_scm_StringBuilder$();
  this.Range$1 = $m_sci_Range$();
  this.Equiv$1 = $m_s_math_Equiv$();
  this.Fractional$1 = $m_s_math_Fractional$();
  this.Integral$1 = $m_s_math_Integral$();
  this.Numeric$1 = $m_s_math_Numeric$();
  this.Ordered$1 = $m_s_math_Ordered$();
  this.Ordering$1 = $m_s_math_Ordering$();
  this.Either$1 = $m_s_util_Either$();
  this.Left$1 = $m_s_util_Left$();
  this.Right$1 = $m_s_util_Right$();
  return this
});
var $d_s_package$ = new $TypeData().initClass({
  s_package$: 0
}, false, "scala.package$", {
  s_package$: 1,
  O: 1
});
$c_s_package$.prototype.$classData = $d_s_package$;
var $n_s_package$ = (void 0);
function $m_s_package$() {
  if ((!$n_s_package$)) {
    $n_s_package$ = new $c_s_package$().init___()
  };
  return $n_s_package$
}
/** @constructor */
function $c_s_reflect_ClassManifestFactory$() {
  $c_O.call(this);
  this.Byte$1 = null;
  this.Short$1 = null;
  this.Char$1 = null;
  this.Int$1 = null;
  this.Long$1 = null;
  this.Float$1 = null;
  this.Double$1 = null;
  this.Boolean$1 = null;
  this.Unit$1 = null;
  this.Any$1 = null;
  this.Object$1 = null;
  this.AnyVal$1 = null;
  this.Nothing$1 = null;
  this.Null$1 = null
}
$c_s_reflect_ClassManifestFactory$.prototype = new $h_O();
$c_s_reflect_ClassManifestFactory$.prototype.constructor = $c_s_reflect_ClassManifestFactory$;
/** @constructor */
function $h_s_reflect_ClassManifestFactory$() {
  /*<skip>*/
}
$h_s_reflect_ClassManifestFactory$.prototype = $c_s_reflect_ClassManifestFactory$.prototype;
$c_s_reflect_ClassManifestFactory$.prototype.init___ = (function() {
  $n_s_reflect_ClassManifestFactory$ = this;
  this.Byte$1 = $m_s_reflect_ManifestFactory$ByteManifest$();
  this.Short$1 = $m_s_reflect_ManifestFactory$ShortManifest$();
  this.Char$1 = $m_s_reflect_ManifestFactory$CharManifest$();
  this.Int$1 = $m_s_reflect_ManifestFactory$IntManifest$();
  this.Long$1 = $m_s_reflect_ManifestFactory$LongManifest$();
  this.Float$1 = $m_s_reflect_ManifestFactory$FloatManifest$();
  this.Double$1 = $m_s_reflect_ManifestFactory$DoubleManifest$();
  this.Boolean$1 = $m_s_reflect_ManifestFactory$BooleanManifest$();
  this.Unit$1 = $m_s_reflect_ManifestFactory$UnitManifest$();
  this.Any$1 = $m_s_reflect_ManifestFactory$AnyManifest$();
  this.Object$1 = $m_s_reflect_ManifestFactory$ObjectManifest$();
  this.AnyVal$1 = $m_s_reflect_ManifestFactory$AnyValManifest$();
  this.Nothing$1 = $m_s_reflect_ManifestFactory$NothingManifest$();
  this.Null$1 = $m_s_reflect_ManifestFactory$NullManifest$();
  return this
});
var $d_s_reflect_ClassManifestFactory$ = new $TypeData().initClass({
  s_reflect_ClassManifestFactory$: 0
}, false, "scala.reflect.ClassManifestFactory$", {
  s_reflect_ClassManifestFactory$: 1,
  O: 1
});
$c_s_reflect_ClassManifestFactory$.prototype.$classData = $d_s_reflect_ClassManifestFactory$;
var $n_s_reflect_ClassManifestFactory$ = (void 0);
function $m_s_reflect_ClassManifestFactory$() {
  if ((!$n_s_reflect_ClassManifestFactory$)) {
    $n_s_reflect_ClassManifestFactory$ = new $c_s_reflect_ClassManifestFactory$().init___()
  };
  return $n_s_reflect_ClassManifestFactory$
}
/** @constructor */
function $c_s_reflect_ManifestFactory$() {
  $c_O.call(this)
}
$c_s_reflect_ManifestFactory$.prototype = new $h_O();
$c_s_reflect_ManifestFactory$.prototype.constructor = $c_s_reflect_ManifestFactory$;
/** @constructor */
function $h_s_reflect_ManifestFactory$() {
  /*<skip>*/
}
$h_s_reflect_ManifestFactory$.prototype = $c_s_reflect_ManifestFactory$.prototype;
$c_s_reflect_ManifestFactory$.prototype.init___ = (function() {
  return this
});
var $d_s_reflect_ManifestFactory$ = new $TypeData().initClass({
  s_reflect_ManifestFactory$: 0
}, false, "scala.reflect.ManifestFactory$", {
  s_reflect_ManifestFactory$: 1,
  O: 1
});
$c_s_reflect_ManifestFactory$.prototype.$classData = $d_s_reflect_ManifestFactory$;
var $n_s_reflect_ManifestFactory$ = (void 0);
function $m_s_reflect_ManifestFactory$() {
  if ((!$n_s_reflect_ManifestFactory$)) {
    $n_s_reflect_ManifestFactory$ = new $c_s_reflect_ManifestFactory$().init___()
  };
  return $n_s_reflect_ManifestFactory$
}
/** @constructor */
function $c_s_reflect_package$() {
  $c_O.call(this);
  this.ClassManifest$1 = null;
  this.Manifest$1 = null
}
$c_s_reflect_package$.prototype = new $h_O();
$c_s_reflect_package$.prototype.constructor = $c_s_reflect_package$;
/** @constructor */
function $h_s_reflect_package$() {
  /*<skip>*/
}
$h_s_reflect_package$.prototype = $c_s_reflect_package$.prototype;
$c_s_reflect_package$.prototype.init___ = (function() {
  $n_s_reflect_package$ = this;
  this.ClassManifest$1 = $m_s_reflect_ClassManifestFactory$();
  this.Manifest$1 = $m_s_reflect_ManifestFactory$();
  return this
});
var $d_s_reflect_package$ = new $TypeData().initClass({
  s_reflect_package$: 0
}, false, "scala.reflect.package$", {
  s_reflect_package$: 1,
  O: 1
});
$c_s_reflect_package$.prototype.$classData = $d_s_reflect_package$;
var $n_s_reflect_package$ = (void 0);
function $m_s_reflect_package$() {
  if ((!$n_s_reflect_package$)) {
    $n_s_reflect_package$ = new $c_s_reflect_package$().init___()
  };
  return $n_s_reflect_package$
}
/** @constructor */
function $c_s_sys_package$() {
  $c_O.call(this)
}
$c_s_sys_package$.prototype = new $h_O();
$c_s_sys_package$.prototype.constructor = $c_s_sys_package$;
/** @constructor */
function $h_s_sys_package$() {
  /*<skip>*/
}
$h_s_sys_package$.prototype = $c_s_sys_package$.prototype;
$c_s_sys_package$.prototype.init___ = (function() {
  return this
});
$c_s_sys_package$.prototype.error__T__sr_Nothing$ = (function(message) {
  throw $m_sjsr_package$().unwrapJavaScriptException__jl_Throwable__O(new $c_jl_RuntimeException().init___T(message))
});
var $d_s_sys_package$ = new $TypeData().initClass({
  s_sys_package$: 0
}, false, "scala.sys.package$", {
  s_sys_package$: 1,
  O: 1
});
$c_s_sys_package$.prototype.$classData = $d_s_sys_package$;
var $n_s_sys_package$ = (void 0);
function $m_s_sys_package$() {
  if ((!$n_s_sys_package$)) {
    $n_s_sys_package$ = new $c_s_sys_package$().init___()
  };
  return $n_s_sys_package$
}
/** @constructor */
function $c_s_util_Either$() {
  $c_O.call(this)
}
$c_s_util_Either$.prototype = new $h_O();
$c_s_util_Either$.prototype.constructor = $c_s_util_Either$;
/** @constructor */
function $h_s_util_Either$() {
  /*<skip>*/
}
$h_s_util_Either$.prototype = $c_s_util_Either$.prototype;
$c_s_util_Either$.prototype.init___ = (function() {
  return this
});
var $d_s_util_Either$ = new $TypeData().initClass({
  s_util_Either$: 0
}, false, "scala.util.Either$", {
  s_util_Either$: 1,
  O: 1
});
$c_s_util_Either$.prototype.$classData = $d_s_util_Either$;
var $n_s_util_Either$ = (void 0);
function $m_s_util_Either$() {
  if ((!$n_s_util_Either$)) {
    $n_s_util_Either$ = new $c_s_util_Either$().init___()
  };
  return $n_s_util_Either$
}
/** @constructor */
function $c_s_util_control_Breaks() {
  $c_O.call(this);
  this.scala$util$control$Breaks$$breakException$1 = null
}
$c_s_util_control_Breaks.prototype = new $h_O();
$c_s_util_control_Breaks.prototype.constructor = $c_s_util_control_Breaks;
/** @constructor */
function $h_s_util_control_Breaks() {
  /*<skip>*/
}
$h_s_util_control_Breaks.prototype = $c_s_util_control_Breaks.prototype;
$c_s_util_control_Breaks.prototype.init___ = (function() {
  this.scala$util$control$Breaks$$breakException$1 = new $c_s_util_control_BreakControl().init___();
  return this
});
var $d_s_util_control_Breaks = new $TypeData().initClass({
  s_util_control_Breaks: 0
}, false, "scala.util.control.Breaks", {
  s_util_control_Breaks: 1,
  O: 1
});
$c_s_util_control_Breaks.prototype.$classData = $d_s_util_control_Breaks;
/** @constructor */
function $c_s_util_hashing_MurmurHash3() {
  $c_O.call(this)
}
$c_s_util_hashing_MurmurHash3.prototype = new $h_O();
$c_s_util_hashing_MurmurHash3.prototype.constructor = $c_s_util_hashing_MurmurHash3;
/** @constructor */
function $h_s_util_hashing_MurmurHash3() {
  /*<skip>*/
}
$h_s_util_hashing_MurmurHash3.prototype = $c_s_util_hashing_MurmurHash3.prototype;
$c_s_util_hashing_MurmurHash3.prototype.mixLast__I__I__I = (function(hash, data) {
  var k = data;
  k = $imul((-862048943), k);
  var i = k;
  k = ((i << 15) | ((i >>> 17) | 0));
  k = $imul(461845907, k);
  return (hash ^ k)
});
$c_s_util_hashing_MurmurHash3.prototype.mix__I__I__I = (function(hash, data) {
  var h = this.mixLast__I__I__I(hash, data);
  var i = h;
  h = ((i << 13) | ((i >>> 19) | 0));
  return (((-430675100) + $imul(5, h)) | 0)
});
$c_s_util_hashing_MurmurHash3.prototype.avalanche__p1__I__I = (function(hash) {
  var h = hash;
  h = (h ^ ((h >>> 16) | 0));
  h = $imul((-2048144789), h);
  h = (h ^ ((h >>> 13) | 0));
  h = $imul((-1028477387), h);
  h = (h ^ ((h >>> 16) | 0));
  return h
});
$c_s_util_hashing_MurmurHash3.prototype.productHash__s_Product__I__I = (function(x, seed) {
  var arr = x.productArity__I();
  if ((arr === 0)) {
    var this$1 = x.productPrefix__T();
    return $m_sjsr_RuntimeString$().hashCode__T__I(this$1)
  } else {
    var h = seed;
    var i = 0;
    while ((i < arr)) {
      h = this.mix__I__I__I(h, $m_sr_ScalaRunTime$().hash__O__I(x.productElement__I__O(i)));
      i = ((1 + i) | 0)
    };
    return this.finalizeHash__I__I__I(h, arr)
  }
});
$c_s_util_hashing_MurmurHash3.prototype.finalizeHash__I__I__I = (function(hash, length) {
  return this.avalanche__p1__I__I((hash ^ length))
});
$c_s_util_hashing_MurmurHash3.prototype.orderedHash__sc_TraversableOnce__I__I = (function(xs, seed) {
  var n = new $c_sr_IntRef().init___I(0);
  var h = new $c_sr_IntRef().init___I(seed);
  xs.foreach__F1__V(new $c_sjsr_AnonFunction1().init___sjs_js_Function1((function($this, n$1, h$1) {
    return (function(x$2) {
      h$1.elem$1 = $this.mix__I__I__I(h$1.elem$1, $m_sr_ScalaRunTime$().hash__O__I(x$2));
      n$1.elem$1 = ((1 + n$1.elem$1) | 0)
    })
  })(this, n, h)));
  return this.finalizeHash__I__I__I(h.elem$1, n.elem$1)
});
$c_s_util_hashing_MurmurHash3.prototype.listHash__sci_List__I__I = (function(xs, seed) {
  var n = 0;
  var h = seed;
  var elems = xs;
  while ((!elems.isEmpty__Z())) {
    var head = elems.head__O();
    var this$1 = elems;
    var tail = this$1.tail__sci_List();
    h = this.mix__I__I__I(h, $m_sr_ScalaRunTime$().hash__O__I(head));
    n = ((1 + n) | 0);
    elems = tail
  };
  return this.finalizeHash__I__I__I(h, n)
});
/** @constructor */
function $c_sc_$colon$plus$() {
  $c_O.call(this)
}
$c_sc_$colon$plus$.prototype = new $h_O();
$c_sc_$colon$plus$.prototype.constructor = $c_sc_$colon$plus$;
/** @constructor */
function $h_sc_$colon$plus$() {
  /*<skip>*/
}
$h_sc_$colon$plus$.prototype = $c_sc_$colon$plus$.prototype;
$c_sc_$colon$plus$.prototype.init___ = (function() {
  return this
});
var $d_sc_$colon$plus$ = new $TypeData().initClass({
  sc_$colon$plus$: 0
}, false, "scala.collection.$colon$plus$", {
  sc_$colon$plus$: 1,
  O: 1
});
$c_sc_$colon$plus$.prototype.$classData = $d_sc_$colon$plus$;
var $n_sc_$colon$plus$ = (void 0);
function $m_sc_$colon$plus$() {
  if ((!$n_sc_$colon$plus$)) {
    $n_sc_$colon$plus$ = new $c_sc_$colon$plus$().init___()
  };
  return $n_sc_$colon$plus$
}
/** @constructor */
function $c_sc_$plus$colon$() {
  $c_O.call(this)
}
$c_sc_$plus$colon$.prototype = new $h_O();
$c_sc_$plus$colon$.prototype.constructor = $c_sc_$plus$colon$;
/** @constructor */
function $h_sc_$plus$colon$() {
  /*<skip>*/
}
$h_sc_$plus$colon$.prototype = $c_sc_$plus$colon$.prototype;
$c_sc_$plus$colon$.prototype.init___ = (function() {
  return this
});
var $d_sc_$plus$colon$ = new $TypeData().initClass({
  sc_$plus$colon$: 0
}, false, "scala.collection.$plus$colon$", {
  sc_$plus$colon$: 1,
  O: 1
});
$c_sc_$plus$colon$.prototype.$classData = $d_sc_$plus$colon$;
var $n_sc_$plus$colon$ = (void 0);
function $m_sc_$plus$colon$() {
  if ((!$n_sc_$plus$colon$)) {
    $n_sc_$plus$colon$ = new $c_sc_$plus$colon$().init___()
  };
  return $n_sc_$plus$colon$
}
/** @constructor */
function $c_sc_Iterator$() {
  $c_O.call(this);
  this.empty$1 = null
}
$c_sc_Iterator$.prototype = new $h_O();
$c_sc_Iterator$.prototype.constructor = $c_sc_Iterator$;
/** @constructor */
function $h_sc_Iterator$() {
  /*<skip>*/
}
$h_sc_Iterator$.prototype = $c_sc_Iterator$.prototype;
$c_sc_Iterator$.prototype.init___ = (function() {
  $n_sc_Iterator$ = this;
  this.empty$1 = new $c_sc_Iterator$$anon$2().init___();
  return this
});
var $d_sc_Iterator$ = new $TypeData().initClass({
  sc_Iterator$: 0
}, false, "scala.collection.Iterator$", {
  sc_Iterator$: 1,
  O: 1
});
$c_sc_Iterator$.prototype.$classData = $d_sc_Iterator$;
var $n_sc_Iterator$ = (void 0);
function $m_sc_Iterator$() {
  if ((!$n_sc_Iterator$)) {
    $n_sc_Iterator$ = new $c_sc_Iterator$().init___()
  };
  return $n_sc_Iterator$
}
/** @constructor */
function $c_scg_GenMapFactory() {
  $c_O.call(this)
}
$c_scg_GenMapFactory.prototype = new $h_O();
$c_scg_GenMapFactory.prototype.constructor = $c_scg_GenMapFactory;
/** @constructor */
function $h_scg_GenMapFactory() {
  /*<skip>*/
}
$h_scg_GenMapFactory.prototype = $c_scg_GenMapFactory.prototype;
/** @constructor */
function $c_scg_GenericCompanion() {
  $c_O.call(this)
}
$c_scg_GenericCompanion.prototype = new $h_O();
$c_scg_GenericCompanion.prototype.constructor = $c_scg_GenericCompanion;
/** @constructor */
function $h_scg_GenericCompanion() {
  /*<skip>*/
}
$h_scg_GenericCompanion.prototype = $c_scg_GenericCompanion.prototype;
/** @constructor */
function $c_sci_Stream$$hash$colon$colon$() {
  $c_O.call(this)
}
$c_sci_Stream$$hash$colon$colon$.prototype = new $h_O();
$c_sci_Stream$$hash$colon$colon$.prototype.constructor = $c_sci_Stream$$hash$colon$colon$;
/** @constructor */
function $h_sci_Stream$$hash$colon$colon$() {
  /*<skip>*/
}
$h_sci_Stream$$hash$colon$colon$.prototype = $c_sci_Stream$$hash$colon$colon$.prototype;
$c_sci_Stream$$hash$colon$colon$.prototype.init___ = (function() {
  return this
});
var $d_sci_Stream$$hash$colon$colon$ = new $TypeData().initClass({
  sci_Stream$$hash$colon$colon$: 0
}, false, "scala.collection.immutable.Stream$$hash$colon$colon$", {
  sci_Stream$$hash$colon$colon$: 1,
  O: 1
});
$c_sci_Stream$$hash$colon$colon$.prototype.$classData = $d_sci_Stream$$hash$colon$colon$;
var $n_sci_Stream$$hash$colon$colon$ = (void 0);
function $m_sci_Stream$$hash$colon$colon$() {
  if ((!$n_sci_Stream$$hash$colon$colon$)) {
    $n_sci_Stream$$hash$colon$colon$ = new $c_sci_Stream$$hash$colon$colon$().init___()
  };
  return $n_sci_Stream$$hash$colon$colon$
}
/** @constructor */
function $c_sci_StringOps$() {
  $c_O.call(this)
}
$c_sci_StringOps$.prototype = new $h_O();
$c_sci_StringOps$.prototype.constructor = $c_sci_StringOps$;
/** @constructor */
function $h_sci_StringOps$() {
  /*<skip>*/
}
$h_sci_StringOps$.prototype = $c_sci_StringOps$.prototype;
$c_sci_StringOps$.prototype.init___ = (function() {
  return this
});
$c_sci_StringOps$.prototype.equals$extension__T__O__Z = (function($$this, x$1) {
  if ($is_sci_StringOps(x$1)) {
    var StringOps$1 = ((x$1 === null) ? null : $as_sci_StringOps(x$1).repr$1);
    return ($$this === StringOps$1)
  } else {
    return false
  }
});
var $d_sci_StringOps$ = new $TypeData().initClass({
  sci_StringOps$: 0
}, false, "scala.collection.immutable.StringOps$", {
  sci_StringOps$: 1,
  O: 1
});
$c_sci_StringOps$.prototype.$classData = $d_sci_StringOps$;
var $n_sci_StringOps$ = (void 0);
function $m_sci_StringOps$() {
  if ((!$n_sci_StringOps$)) {
    $n_sci_StringOps$ = new $c_sci_StringOps$().init___()
  };
  return $n_sci_StringOps$
}
/** @constructor */
function $c_sjs_js_Dictionary$() {
  $c_O.call(this)
}
$c_sjs_js_Dictionary$.prototype = new $h_O();
$c_sjs_js_Dictionary$.prototype.constructor = $c_sjs_js_Dictionary$;
/** @constructor */
function $h_sjs_js_Dictionary$() {
  /*<skip>*/
}
$h_sjs_js_Dictionary$.prototype = $c_sjs_js_Dictionary$.prototype;
$c_sjs_js_Dictionary$.prototype.init___ = (function() {
  return this
});
$c_sjs_js_Dictionary$.prototype.empty__sjs_js_Dictionary = (function() {
  return {}
});
var $d_sjs_js_Dictionary$ = new $TypeData().initClass({
  sjs_js_Dictionary$: 0
}, false, "scala.scalajs.js.Dictionary$", {
  sjs_js_Dictionary$: 1,
  O: 1
});
$c_sjs_js_Dictionary$.prototype.$classData = $d_sjs_js_Dictionary$;
var $n_sjs_js_Dictionary$ = (void 0);
function $m_sjs_js_Dictionary$() {
  if ((!$n_sjs_js_Dictionary$)) {
    $n_sjs_js_Dictionary$ = new $c_sjs_js_Dictionary$().init___()
  };
  return $n_sjs_js_Dictionary$
}
/** @constructor */
function $c_sjsr_Bits$() {
  $c_O.call(this);
  this.scala$scalajs$runtime$Bits$$$undareTypedArraysSupported$f = false;
  this.arrayBuffer$1 = null;
  this.int32Array$1 = null;
  this.float32Array$1 = null;
  this.float64Array$1 = null;
  this.areTypedArraysBigEndian$1 = false;
  this.highOffset$1 = 0;
  this.lowOffset$1 = 0
}
$c_sjsr_Bits$.prototype = new $h_O();
$c_sjsr_Bits$.prototype.constructor = $c_sjsr_Bits$;
/** @constructor */
function $h_sjsr_Bits$() {
  /*<skip>*/
}
$h_sjsr_Bits$.prototype = $c_sjsr_Bits$.prototype;
$c_sjsr_Bits$.prototype.init___ = (function() {
  $n_sjsr_Bits$ = this;
  var x = ((($g.ArrayBuffer && $g.Int32Array) && $g.Float32Array) && $g.Float64Array);
  this.scala$scalajs$runtime$Bits$$$undareTypedArraysSupported$f = $uZ((!(!x)));
  this.arrayBuffer$1 = (this.scala$scalajs$runtime$Bits$$$undareTypedArraysSupported$f ? new $g.ArrayBuffer(8) : null);
  this.int32Array$1 = (this.scala$scalajs$runtime$Bits$$$undareTypedArraysSupported$f ? new $g.Int32Array(this.arrayBuffer$1, 0, 2) : null);
  this.float32Array$1 = (this.scala$scalajs$runtime$Bits$$$undareTypedArraysSupported$f ? new $g.Float32Array(this.arrayBuffer$1, 0, 2) : null);
  this.float64Array$1 = (this.scala$scalajs$runtime$Bits$$$undareTypedArraysSupported$f ? new $g.Float64Array(this.arrayBuffer$1, 0, 1) : null);
  if ((!this.scala$scalajs$runtime$Bits$$$undareTypedArraysSupported$f)) {
    var jsx$1 = true
  } else {
    this.int32Array$1[0] = 16909060;
    var jsx$1 = ($uB(new $g.Int8Array(this.arrayBuffer$1, 0, 8)[0]) === 1)
  };
  this.areTypedArraysBigEndian$1 = jsx$1;
  this.highOffset$1 = (this.areTypedArraysBigEndian$1 ? 0 : 1);
  this.lowOffset$1 = (this.areTypedArraysBigEndian$1 ? 1 : 0);
  return this
});
$c_sjsr_Bits$.prototype.numberHashCode__D__I = (function(value) {
  var iv = $uI((value | 0));
  if (((iv === value) && ((1.0 / value) !== (-Infinity)))) {
    return iv
  } else {
    var t = this.doubleToLongBits__D__J(value);
    var lo = t.lo$2;
    var hi = t.hi$2;
    return (lo ^ hi)
  }
});
$c_sjsr_Bits$.prototype.doubleToLongBitsPolyfill__p1__D__J = (function(value) {
  if ((value !== value)) {
    var _3 = $uD($g.Math.pow(2.0, 51));
    var x1_$_$$und1$1 = false;
    var x1_$_$$und2$1 = 2047;
    var x1_$_$$und3$1 = _3
  } else if (((value === Infinity) || (value === (-Infinity)))) {
    var _1 = (value < 0);
    var x1_$_$$und1$1 = _1;
    var x1_$_$$und2$1 = 2047;
    var x1_$_$$und3$1 = 0.0
  } else if ((value === 0.0)) {
    var _1$1 = ((1 / value) === (-Infinity));
    var x1_$_$$und1$1 = _1$1;
    var x1_$_$$und2$1 = 0;
    var x1_$_$$und3$1 = 0.0
  } else {
    var s = (value < 0);
    var av = (s ? (-value) : value);
    if ((av >= $uD($g.Math.pow(2.0, (-1022))))) {
      var twoPowFbits = $uD($g.Math.pow(2.0, 52));
      var a = ($uD($g.Math.log(av)) / 0.6931471805599453);
      var x = $uD($g.Math.floor(a));
      var a$1 = $uI((x | 0));
      var e = ((a$1 < 1023) ? a$1 : 1023);
      var b = e;
      var n = ((av / $uD($g.Math.pow(2.0, b))) * twoPowFbits);
      var w = $uD($g.Math.floor(n));
      var f = (n - w);
      var f$1 = ((f < 0.5) ? w : ((f > 0.5) ? (1 + w) : (((w % 2) !== 0) ? (1 + w) : w)));
      if (((f$1 / twoPowFbits) >= 2)) {
        e = ((1 + e) | 0);
        f$1 = 1.0
      };
      if ((e > 1023)) {
        e = 2047;
        f$1 = 0.0
      } else {
        e = ((1023 + e) | 0);
        f$1 = (f$1 - twoPowFbits)
      };
      var _2 = e;
      var _3$1 = f$1;
      var x1_$_$$und1$1 = s;
      var x1_$_$$und2$1 = _2;
      var x1_$_$$und3$1 = _3$1
    } else {
      var n$1 = (av / $uD($g.Math.pow(2.0, (-1074))));
      var w$1 = $uD($g.Math.floor(n$1));
      var f$2 = (n$1 - w$1);
      var _3$2 = ((f$2 < 0.5) ? w$1 : ((f$2 > 0.5) ? (1 + w$1) : (((w$1 % 2) !== 0) ? (1 + w$1) : w$1)));
      var x1_$_$$und1$1 = s;
      var x1_$_$$und2$1 = 0;
      var x1_$_$$und3$1 = _3$2
    }
  };
  var s$1 = $uZ(x1_$_$$und1$1);
  var e$1 = $uI(x1_$_$$und2$1);
  var f$3 = $uD(x1_$_$$und3$1);
  var x$1 = (f$3 / 4.294967296E9);
  var hif = $uI((x$1 | 0));
  var hi = (((s$1 ? (-2147483648) : 0) | (e$1 << 20)) | hif);
  var lo = $uI((f$3 | 0));
  return new $c_sjsr_RuntimeLong().init___I__I(lo, hi)
});
$c_sjsr_Bits$.prototype.doubleToLongBits__D__J = (function(value) {
  if (this.scala$scalajs$runtime$Bits$$$undareTypedArraysSupported$f) {
    this.float64Array$1[0] = value;
    var value$1 = $uI(this.int32Array$1[this.highOffset$1]);
    var value$2 = $uI(this.int32Array$1[this.lowOffset$1]);
    return new $c_sjsr_RuntimeLong().init___I__I(value$2, value$1)
  } else {
    return this.doubleToLongBitsPolyfill__p1__D__J(value)
  }
});
var $d_sjsr_Bits$ = new $TypeData().initClass({
  sjsr_Bits$: 0
}, false, "scala.scalajs.runtime.Bits$", {
  sjsr_Bits$: 1,
  O: 1
});
$c_sjsr_Bits$.prototype.$classData = $d_sjsr_Bits$;
var $n_sjsr_Bits$ = (void 0);
function $m_sjsr_Bits$() {
  if ((!$n_sjsr_Bits$)) {
    $n_sjsr_Bits$ = new $c_sjsr_Bits$().init___()
  };
  return $n_sjsr_Bits$
}
/** @constructor */
function $c_sjsr_RuntimeString$() {
  $c_O.call(this);
  this.CASE$undINSENSITIVE$undORDER$1 = null;
  this.bitmap$0$1 = false
}
$c_sjsr_RuntimeString$.prototype = new $h_O();
$c_sjsr_RuntimeString$.prototype.constructor = $c_sjsr_RuntimeString$;
/** @constructor */
function $h_sjsr_RuntimeString$() {
  /*<skip>*/
}
$h_sjsr_RuntimeString$.prototype = $c_sjsr_RuntimeString$.prototype;
$c_sjsr_RuntimeString$.prototype.init___ = (function() {
  return this
});
$c_sjsr_RuntimeString$.prototype.indexOf__T__I__I__I = (function(thiz, ch, fromIndex) {
  var str = this.fromCodePoint__p1__I__T(ch);
  return $uI(thiz.indexOf(str, fromIndex))
});
$c_sjsr_RuntimeString$.prototype.valueOf__O__T = (function(value) {
  return ((value === null) ? "null" : $objectToString(value))
});
$c_sjsr_RuntimeString$.prototype.lastIndexOf__T__I__I = (function(thiz, ch) {
  var str = this.fromCodePoint__p1__I__T(ch);
  return $uI(thiz.lastIndexOf(str))
});
$c_sjsr_RuntimeString$.prototype.indexOf__T__I__I = (function(thiz, ch) {
  var str = this.fromCodePoint__p1__I__T(ch);
  return $uI(thiz.indexOf(str))
});
$c_sjsr_RuntimeString$.prototype.fromCodePoint__p1__I__T = (function(codePoint) {
  if ((((-65536) & codePoint) === 0)) {
    return $as_T($g.String.fromCharCode(codePoint))
  } else if (((codePoint < 0) || (codePoint > 1114111))) {
    throw new $c_jl_IllegalArgumentException().init___()
  } else {
    var offsetCp = (((-65536) + codePoint) | 0);
    return $as_T($g.String.fromCharCode((55296 | (offsetCp >> 10)), (56320 | (1023 & offsetCp))))
  }
});
$c_sjsr_RuntimeString$.prototype.hashCode__T__I = (function(thiz) {
  var res = 0;
  var mul = 1;
  var i = (((-1) + $uI(thiz.length)) | 0);
  while ((i >= 0)) {
    var jsx$1 = res;
    var index = i;
    res = ((jsx$1 + $imul((65535 & $uI(thiz.charCodeAt(index))), mul)) | 0);
    mul = $imul(31, mul);
    i = (((-1) + i) | 0)
  };
  return res
});
var $d_sjsr_RuntimeString$ = new $TypeData().initClass({
  sjsr_RuntimeString$: 0
}, false, "scala.scalajs.runtime.RuntimeString$", {
  sjsr_RuntimeString$: 1,
  O: 1
});
$c_sjsr_RuntimeString$.prototype.$classData = $d_sjsr_RuntimeString$;
var $n_sjsr_RuntimeString$ = (void 0);
function $m_sjsr_RuntimeString$() {
  if ((!$n_sjsr_RuntimeString$)) {
    $n_sjsr_RuntimeString$ = new $c_sjsr_RuntimeString$().init___()
  };
  return $n_sjsr_RuntimeString$
}
/** @constructor */
function $c_sjsr_package$() {
  $c_O.call(this)
}
$c_sjsr_package$.prototype = new $h_O();
$c_sjsr_package$.prototype.constructor = $c_sjsr_package$;
/** @constructor */
function $h_sjsr_package$() {
  /*<skip>*/
}
$h_sjsr_package$.prototype = $c_sjsr_package$.prototype;
$c_sjsr_package$.prototype.init___ = (function() {
  return this
});
$c_sjsr_package$.prototype.unwrapJavaScriptException__jl_Throwable__O = (function(th) {
  if ($is_sjs_js_JavaScriptException(th)) {
    var x2 = $as_sjs_js_JavaScriptException(th);
    var e = x2.exception$4;
    return e
  } else {
    return th
  }
});
$c_sjsr_package$.prototype.wrapJavaScriptException__O__jl_Throwable = (function(e) {
  if ($is_jl_Throwable(e)) {
    var x2 = $as_jl_Throwable(e);
    return x2
  } else {
    return new $c_sjs_js_JavaScriptException().init___O(e)
  }
});
var $d_sjsr_package$ = new $TypeData().initClass({
  sjsr_package$: 0
}, false, "scala.scalajs.runtime.package$", {
  sjsr_package$: 1,
  O: 1
});
$c_sjsr_package$.prototype.$classData = $d_sjsr_package$;
var $n_sjsr_package$ = (void 0);
function $m_sjsr_package$() {
  if ((!$n_sjsr_package$)) {
    $n_sjsr_package$ = new $c_sjsr_package$().init___()
  };
  return $n_sjsr_package$
}
/** @constructor */
function $c_sr_BoxesRunTime$() {
  $c_O.call(this)
}
$c_sr_BoxesRunTime$.prototype = new $h_O();
$c_sr_BoxesRunTime$.prototype.constructor = $c_sr_BoxesRunTime$;
/** @constructor */
function $h_sr_BoxesRunTime$() {
  /*<skip>*/
}
$h_sr_BoxesRunTime$.prototype = $c_sr_BoxesRunTime$.prototype;
$c_sr_BoxesRunTime$.prototype.init___ = (function() {
  return this
});
$c_sr_BoxesRunTime$.prototype.equalsCharObject__jl_Character__O__Z = (function(xc, y) {
  if ($is_jl_Character(y)) {
    var x2 = $as_jl_Character(y);
    return (xc.value$1 === x2.value$1)
  } else if ($is_jl_Number(y)) {
    var x3 = $as_jl_Number(y);
    if (((typeof x3) === "number")) {
      var x2$1 = $uD(x3);
      return (x2$1 === xc.value$1)
    } else if ($is_sjsr_RuntimeLong(x3)) {
      var t = $uJ(x3);
      var lo = t.lo$2;
      var hi = t.hi$2;
      var value = xc.value$1;
      var hi$1 = (value >> 31);
      return ((lo === value) && (hi === hi$1))
    } else {
      return ((x3 === null) ? (xc === null) : $objectEquals(x3, xc))
    }
  } else {
    return ((xc === null) && (y === null))
  }
});
$c_sr_BoxesRunTime$.prototype.equalsNumObject__jl_Number__O__Z = (function(xn, y) {
  if ($is_jl_Number(y)) {
    var x2 = $as_jl_Number(y);
    return this.equalsNumNum__jl_Number__jl_Number__Z(xn, x2)
  } else if ($is_jl_Character(y)) {
    var x3 = $as_jl_Character(y);
    if (((typeof xn) === "number")) {
      var x2$1 = $uD(xn);
      return (x2$1 === x3.value$1)
    } else if ($is_sjsr_RuntimeLong(xn)) {
      var t = $uJ(xn);
      var lo = t.lo$2;
      var hi = t.hi$2;
      var value = x3.value$1;
      var hi$1 = (value >> 31);
      return ((lo === value) && (hi === hi$1))
    } else {
      return ((xn === null) ? (x3 === null) : $objectEquals(xn, x3))
    }
  } else {
    return ((xn === null) ? (y === null) : $objectEquals(xn, y))
  }
});
$c_sr_BoxesRunTime$.prototype.equals__O__O__Z = (function(x, y) {
  if ((x === y)) {
    return true
  } else if ($is_jl_Number(x)) {
    var x2 = $as_jl_Number(x);
    return this.equalsNumObject__jl_Number__O__Z(x2, y)
  } else if ($is_jl_Character(x)) {
    var x3 = $as_jl_Character(x);
    return this.equalsCharObject__jl_Character__O__Z(x3, y)
  } else {
    return ((x === null) ? (y === null) : $objectEquals(x, y))
  }
});
$c_sr_BoxesRunTime$.prototype.equalsNumNum__jl_Number__jl_Number__Z = (function(xn, yn) {
  if (((typeof xn) === "number")) {
    var x2 = $uD(xn);
    if (((typeof yn) === "number")) {
      var x2$2 = $uD(yn);
      return (x2 === x2$2)
    } else if ($is_sjsr_RuntimeLong(yn)) {
      var t = $uJ(yn);
      var lo = t.lo$2;
      var hi = t.hi$2;
      return (x2 === $m_sjsr_RuntimeLong$().scala$scalajs$runtime$RuntimeLong$$toDouble__I__I__D(lo, hi))
    } else if ($is_s_math_ScalaNumber(yn)) {
      var x4 = $as_s_math_ScalaNumber(yn);
      return x4.equals__O__Z(x2)
    } else {
      return false
    }
  } else if ($is_sjsr_RuntimeLong(xn)) {
    var t$1 = $uJ(xn);
    var lo$1 = t$1.lo$2;
    var hi$1 = t$1.hi$2;
    if ($is_sjsr_RuntimeLong(yn)) {
      var t$2 = $uJ(yn);
      var lo$2 = t$2.lo$2;
      var hi$2 = t$2.hi$2;
      return ((lo$1 === lo$2) && (hi$1 === hi$2))
    } else if (((typeof yn) === "number")) {
      var x3$3 = $uD(yn);
      return ($m_sjsr_RuntimeLong$().scala$scalajs$runtime$RuntimeLong$$toDouble__I__I__D(lo$1, hi$1) === x3$3)
    } else if ($is_s_math_ScalaNumber(yn)) {
      var x4$2 = $as_s_math_ScalaNumber(yn);
      return x4$2.equals__O__Z(new $c_sjsr_RuntimeLong().init___I__I(lo$1, hi$1))
    } else {
      return false
    }
  } else {
    return ((xn === null) ? (yn === null) : $objectEquals(xn, yn))
  }
});
var $d_sr_BoxesRunTime$ = new $TypeData().initClass({
  sr_BoxesRunTime$: 0
}, false, "scala.runtime.BoxesRunTime$", {
  sr_BoxesRunTime$: 1,
  O: 1
});
$c_sr_BoxesRunTime$.prototype.$classData = $d_sr_BoxesRunTime$;
var $n_sr_BoxesRunTime$ = (void 0);
function $m_sr_BoxesRunTime$() {
  if ((!$n_sr_BoxesRunTime$)) {
    $n_sr_BoxesRunTime$ = new $c_sr_BoxesRunTime$().init___()
  };
  return $n_sr_BoxesRunTime$
}
var $d_sr_Null$ = new $TypeData().initClass({
  sr_Null$: 0
}, false, "scala.runtime.Null$", {
  sr_Null$: 1,
  O: 1
});
/** @constructor */
function $c_sr_ScalaRunTime$() {
  $c_O.call(this)
}
$c_sr_ScalaRunTime$.prototype = new $h_O();
$c_sr_ScalaRunTime$.prototype.constructor = $c_sr_ScalaRunTime$;
/** @constructor */
function $h_sr_ScalaRunTime$() {
  /*<skip>*/
}
$h_sr_ScalaRunTime$.prototype = $c_sr_ScalaRunTime$.prototype;
$c_sr_ScalaRunTime$.prototype.init___ = (function() {
  return this
});
$c_sr_ScalaRunTime$.prototype.hash__O__I = (function(x) {
  if ((x === null)) {
    return 0
  } else if ($is_jl_Number(x)) {
    var n = $as_jl_Number(x);
    if (((typeof n) === "number")) {
      var x2 = $uD(n);
      return $m_sr_Statics$().doubleHash__D__I(x2)
    } else if ($is_sjsr_RuntimeLong(n)) {
      var t = $uJ(n);
      var lo = t.lo$2;
      var hi = t.hi$2;
      return $m_sr_Statics$().longHash__J__I(new $c_sjsr_RuntimeLong().init___I__I(lo, hi))
    } else {
      return $objectHashCode(n)
    }
  } else {
    return $objectHashCode(x)
  }
});
$c_sr_ScalaRunTime$.prototype.$$undtoString__s_Product__T = (function(x) {
  var this$1 = x.productIterator__sc_Iterator();
  var start = (x.productPrefix__T() + "(");
  return $s_sc_TraversableOnce$class__mkString__sc_TraversableOnce__T__T__T__T(this$1, start, ",", ")")
});
var $d_sr_ScalaRunTime$ = new $TypeData().initClass({
  sr_ScalaRunTime$: 0
}, false, "scala.runtime.ScalaRunTime$", {
  sr_ScalaRunTime$: 1,
  O: 1
});
$c_sr_ScalaRunTime$.prototype.$classData = $d_sr_ScalaRunTime$;
var $n_sr_ScalaRunTime$ = (void 0);
function $m_sr_ScalaRunTime$() {
  if ((!$n_sr_ScalaRunTime$)) {
    $n_sr_ScalaRunTime$ = new $c_sr_ScalaRunTime$().init___()
  };
  return $n_sr_ScalaRunTime$
}
/** @constructor */
function $c_sr_Statics$() {
  $c_O.call(this)
}
$c_sr_Statics$.prototype = new $h_O();
$c_sr_Statics$.prototype.constructor = $c_sr_Statics$;
/** @constructor */
function $h_sr_Statics$() {
  /*<skip>*/
}
$h_sr_Statics$.prototype = $c_sr_Statics$.prototype;
$c_sr_Statics$.prototype.init___ = (function() {
  return this
});
$c_sr_Statics$.prototype.doubleHash__D__I = (function(dv) {
  var iv = $doubleToInt(dv);
  if ((iv === dv)) {
    return iv
  } else {
    var this$1 = $m_sjsr_RuntimeLong$();
    var lo = this$1.scala$scalajs$runtime$RuntimeLong$$fromDoubleImpl__D__I(dv);
    var hi = this$1.scala$scalajs$runtime$RuntimeLong$$hiReturn$f;
    return (($m_sjsr_RuntimeLong$().scala$scalajs$runtime$RuntimeLong$$toDouble__I__I__D(lo, hi) === dv) ? (lo ^ hi) : $m_sjsr_Bits$().numberHashCode__D__I(dv))
  }
});
$c_sr_Statics$.prototype.longHash__J__I = (function(lv) {
  var lo = lv.lo$2;
  var lo$1 = lv.hi$2;
  return ((lo$1 === (lo >> 31)) ? lo : (lo ^ lo$1))
});
var $d_sr_Statics$ = new $TypeData().initClass({
  sr_Statics$: 0
}, false, "scala.runtime.Statics$", {
  sr_Statics$: 1,
  O: 1
});
$c_sr_Statics$.prototype.$classData = $d_sr_Statics$;
var $n_sr_Statics$ = (void 0);
function $m_sr_Statics$() {
  if ((!$n_sr_Statics$)) {
    $n_sr_Statics$ = new $c_sr_Statics$().init___()
  };
  return $n_sr_Statics$
}
/** @constructor */
function $c_Ljapgolly_scalajs_react_CompState$RootAccessor() {
  $c_Ljapgolly_scalajs_react_CompState$Accessor.call(this)
}
$c_Ljapgolly_scalajs_react_CompState$RootAccessor.prototype = new $h_Ljapgolly_scalajs_react_CompState$Accessor();
$c_Ljapgolly_scalajs_react_CompState$RootAccessor.prototype.constructor = $c_Ljapgolly_scalajs_react_CompState$RootAccessor;
/** @constructor */
function $h_Ljapgolly_scalajs_react_CompState$RootAccessor() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_CompState$RootAccessor.prototype = $c_Ljapgolly_scalajs_react_CompState$RootAccessor.prototype;
$c_Ljapgolly_scalajs_react_CompState$RootAccessor.prototype.init___ = (function() {
  return this
});
$c_Ljapgolly_scalajs_react_CompState$RootAccessor.prototype.state__Ljapgolly_scalajs_react_CompScope$CanSetState__O = (function($$) {
  return $$.state.v
});
$c_Ljapgolly_scalajs_react_CompState$RootAccessor.prototype.setState__Ljapgolly_scalajs_react_CompScope$CanSetState__O__F0__V = (function($$, s, cb) {
  $$.setState($m_Ljapgolly_scalajs_react_package$().WrapObj__O__Ljapgolly_scalajs_react_package$WrapObj(s), $m_Ljapgolly_scalajs_react_CallbackTo$().toJsCallback$extension__F0__sjs_js_UndefOr(cb))
});
var $d_Ljapgolly_scalajs_react_CompState$RootAccessor = new $TypeData().initClass({
  Ljapgolly_scalajs_react_CompState$RootAccessor: 0
}, false, "japgolly.scalajs.react.CompState$RootAccessor", {
  Ljapgolly_scalajs_react_CompState$RootAccessor: 1,
  Ljapgolly_scalajs_react_CompState$Accessor: 1,
  O: 1
});
$c_Ljapgolly_scalajs_react_CompState$RootAccessor.prototype.$classData = $d_Ljapgolly_scalajs_react_CompState$RootAccessor;
/** @constructor */
function $c_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$() {
  $c_Ljapgolly_scalajs_react_ReactComponentB$BuildResultLowPri.call(this)
}
$c_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$.prototype = new $h_Ljapgolly_scalajs_react_ReactComponentB$BuildResultLowPri();
$c_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$.prototype.constructor = $c_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$;
/** @constructor */
function $h_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$.prototype = $c_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$.prototype;
$c_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$.prototype.init___ = (function() {
  return this
});
$c_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$.prototype.buildResultUnit__Ljapgolly_scalajs_react_ReactComponentB$BuildResult = (function() {
  var f = new $c_sjsr_AnonFunction1().init___sjs_js_Function1((function(x$1$2) {
    var x$1 = $as_Ljapgolly_scalajs_react_ReactComponentC$ReqProps(x$1$2);
    var jsx$4 = x$1.factory$2;
    var jsx$3 = x$1.reactClass$2;
    var jsx$2 = x$1.key$2;
    var jsx$1 = x$1.ref$2;
    var x = $m_Ljapgolly_scalajs_react_ReactComponentC$().japgolly$scalajs$react$ReactComponentC$$fnUnit0$f;
    return new $c_Ljapgolly_scalajs_react_ReactComponentC$ConstProps().init___Ljapgolly_scalajs_react_ReactComponentCU__Ljapgolly_scalajs_react_ReactClass__sjs_js_UndefOr__sjs_js_UndefOr__F0(jsx$4, jsx$3, jsx$2, jsx$1, x)
  }));
  return new $c_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$$anon$1().init___F1(f)
});
var $d_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$ = new $TypeData().initClass({
  Ljapgolly_scalajs_react_ReactComponentB$BuildResult$: 0
}, false, "japgolly.scalajs.react.ReactComponentB$BuildResult$", {
  Ljapgolly_scalajs_react_ReactComponentB$BuildResult$: 1,
  Ljapgolly_scalajs_react_ReactComponentB$BuildResultLowPri: 1,
  O: 1
});
$c_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$.prototype.$classData = $d_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$;
var $n_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$ = (void 0);
function $m_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$() {
  if ((!$n_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$)) {
    $n_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$ = new $c_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$().init___()
  };
  return $n_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$
}
/** @constructor */
function $c_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$$anon$1() {
  $c_Ljapgolly_scalajs_react_ReactComponentB$BuildResult.call(this);
  this.apply$2 = null
}
$c_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$$anon$1.prototype = new $h_Ljapgolly_scalajs_react_ReactComponentB$BuildResult();
$c_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$$anon$1.prototype.constructor = $c_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$$anon$1;
/** @constructor */
function $h_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$$anon$1() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$$anon$1.prototype = $c_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$$anon$1.prototype;
$c_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$$anon$1.prototype.init___F1 = (function(f$1) {
  this.apply$2 = f$1;
  return this
});
var $d_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$$anon$1 = new $TypeData().initClass({
  Ljapgolly_scalajs_react_ReactComponentB$BuildResult$$anon$1: 0
}, false, "japgolly.scalajs.react.ReactComponentB$BuildResult$$anon$1", {
  Ljapgolly_scalajs_react_ReactComponentB$BuildResult$$anon$1: 1,
  Ljapgolly_scalajs_react_ReactComponentB$BuildResult: 1,
  O: 1
});
$c_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$$anon$1.prototype.$classData = $d_Ljapgolly_scalajs_react_ReactComponentB$BuildResult$$anon$1;
/** @constructor */
function $c_Ljapgolly_scalajs_react_package$() {
  $c_O.call(this);
  this.Callback$1 = null
}
$c_Ljapgolly_scalajs_react_package$.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_package$.prototype.constructor = $c_Ljapgolly_scalajs_react_package$;
/** @constructor */
function $h_Ljapgolly_scalajs_react_package$() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_package$.prototype = $c_Ljapgolly_scalajs_react_package$.prototype;
$c_Ljapgolly_scalajs_react_package$.prototype.init___ = (function() {
  $n_Ljapgolly_scalajs_react_package$ = this;
  this.Callback$1 = $m_Ljapgolly_scalajs_react_Callback$undTempHack$();
  return this
});
$c_Ljapgolly_scalajs_react_package$.prototype.WrapObj__O__Ljapgolly_scalajs_react_package$WrapObj = (function(v) {
  return {
    "v": v
  }
});
var $d_Ljapgolly_scalajs_react_package$ = new $TypeData().initClass({
  Ljapgolly_scalajs_react_package$: 0
}, false, "japgolly.scalajs.react.package$", {
  Ljapgolly_scalajs_react_package$: 1,
  O: 1,
  Ljapgolly_scalajs_react_ReactEventAliases: 1
});
$c_Ljapgolly_scalajs_react_package$.prototype.$classData = $d_Ljapgolly_scalajs_react_package$;
var $n_Ljapgolly_scalajs_react_package$ = (void 0);
function $m_Ljapgolly_scalajs_react_package$() {
  if ((!$n_Ljapgolly_scalajs_react_package$)) {
    $n_Ljapgolly_scalajs_react_package$ = new $c_Ljapgolly_scalajs_react_package$().init___()
  };
  return $n_Ljapgolly_scalajs_react_package$
}
/** @constructor */
function $c_Ljapgolly_scalajs_react_vdom_Implicits() {
  $c_Ljapgolly_scalajs_react_vdom_LowPri.call(this);
  this.$$undreact$undattrBoolean$2 = null;
  this.$$undreact$undattrInt$2 = null;
  this.$$undreact$undattrLong$2 = null;
  this.$$undreact$undattrDouble$2 = null;
  this.$$undreact$undattrJsThisFn$2 = null;
  this.$$undreact$undattrJsFn$2 = null;
  this.$$undreact$undattrJsObj$2 = null;
  this.$$undreact$undstyleBoolean$2 = null;
  this.$$undreact$undstyleInt$2 = null;
  this.$$undreact$undstyleLong$2 = null;
  this.$$undreact$undstyleDouble$2 = null
}
$c_Ljapgolly_scalajs_react_vdom_Implicits.prototype = new $h_Ljapgolly_scalajs_react_vdom_LowPri();
$c_Ljapgolly_scalajs_react_vdom_Implicits.prototype.constructor = $c_Ljapgolly_scalajs_react_vdom_Implicits;
/** @constructor */
function $h_Ljapgolly_scalajs_react_vdom_Implicits() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_vdom_Implicits.prototype = $c_Ljapgolly_scalajs_react_vdom_Implicits.prototype;
$c_Ljapgolly_scalajs_react_vdom_Implicits.prototype.init___ = (function() {
  this.$$undreact$undattrBoolean$2 = $m_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$().map__F1__F2(new $c_sjsr_AnonFunction1().init___sjs_js_Function1((function(value$2) {
    var value = $uZ(value$2);
    return value
  })));
  this.$$undreact$undattrInt$2 = $m_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$().map__F1__F2(new $c_sjsr_AnonFunction1().init___sjs_js_Function1((function(v$2) {
    var v = $uI(v$2);
    $m_Ljapgolly_scalajs_react_package$();
    return v
  })));
  this.$$undreact$undattrLong$2 = $m_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$().map__F1__F2(new $c_sjsr_AnonFunction1().init___sjs_js_Function1((function(v$2$1) {
    var t = $uJ(v$2$1);
    var lo = t.lo$2;
    var hi = t.hi$2;
    $m_Ljapgolly_scalajs_react_package$();
    return $m_sjsr_RuntimeLong$().scala$scalajs$runtime$RuntimeLong$$toString__I__I__T(lo, hi)
  })));
  this.$$undreact$undattrDouble$2 = $m_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$().map__F1__F2(new $c_sjsr_AnonFunction1().init___sjs_js_Function1((function(v$2$2) {
    var v$1 = $uD(v$2$2);
    $m_Ljapgolly_scalajs_react_package$();
    return v$1
  })));
  $m_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$();
  var f = $m_s_Predef$().singleton$und$less$colon$less$2;
  var fn = new $c_sjsr_AnonFunction2().init___sjs_js_Function2((function(f$1) {
    return (function(b$2, a$2) {
      var b = $as_F1(b$2);
      b.apply__O__O(f$1.apply__O__O(a$2))
    })
  })(f));
  this.$$undreact$undattrJsThisFn$2 = fn;
  $m_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$();
  var f$2 = $m_s_Predef$().singleton$und$less$colon$less$2;
  var fn$1 = new $c_sjsr_AnonFunction2().init___sjs_js_Function2((function(f$1$1) {
    return (function(b$2$1, a$2$1) {
      var b$1 = $as_F1(b$2$1);
      b$1.apply__O__O(f$1$1.apply__O__O(a$2$1))
    })
  })(f$2));
  this.$$undreact$undattrJsFn$2 = fn$1;
  $m_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$();
  var f$3 = $m_s_Predef$().singleton$und$less$colon$less$2;
  var fn$2 = new $c_sjsr_AnonFunction2().init___sjs_js_Function2((function(f$1$2) {
    return (function(b$2$2, a$2$2) {
      var b$3 = $as_F1(b$2$2);
      b$3.apply__O__O(f$1$2.apply__O__O(a$2$2))
    })
  })(f$3));
  this.$$undreact$undattrJsObj$2 = fn$2;
  this.$$undreact$undstyleBoolean$2 = $m_Ljapgolly_scalajs_react_vdom_ReactStyle$ValueType$().stringValue__F2();
  this.$$undreact$undstyleInt$2 = $m_Ljapgolly_scalajs_react_vdom_ReactStyle$ValueType$().stringValue__F2();
  this.$$undreact$undstyleLong$2 = $m_Ljapgolly_scalajs_react_vdom_ReactStyle$ValueType$().stringValue__F2();
  this.$$undreact$undstyleDouble$2 = $m_Ljapgolly_scalajs_react_vdom_ReactStyle$ValueType$().stringValue__F2();
  return this
});
/** @constructor */
function $c_Ljapgolly_scalajs_react_vdom_ReactAttr$NameAndValue() {
  $c_O.call(this);
  this.name$1 = null;
  this.value$1 = null;
  this.valueType$1 = null
}
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$NameAndValue.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$NameAndValue.prototype.constructor = $c_Ljapgolly_scalajs_react_vdom_ReactAttr$NameAndValue;
/** @constructor */
function $h_Ljapgolly_scalajs_react_vdom_ReactAttr$NameAndValue() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_vdom_ReactAttr$NameAndValue.prototype = $c_Ljapgolly_scalajs_react_vdom_ReactAttr$NameAndValue.prototype;
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$NameAndValue.prototype.init___T__O__F2 = (function(name, value, valueType) {
  this.name$1 = name;
  this.value$1 = value;
  this.valueType$1 = valueType;
  return this
});
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$NameAndValue.prototype.applyTo__Ljapgolly_scalajs_react_vdom_Builder__V = (function(b) {
  this.valueType$1.apply__O__O__O(new $c_sjsr_AnonFunction1().init___sjs_js_Function1((function(arg$outer, b$1) {
    return (function(x$2$2) {
      b$1.addAttr__T__sjs_js_Any__V(arg$outer.name$1, x$2$2)
    })
  })(this, b)), this.value$1)
});
var $d_Ljapgolly_scalajs_react_vdom_ReactAttr$NameAndValue = new $TypeData().initClass({
  Ljapgolly_scalajs_react_vdom_ReactAttr$NameAndValue: 0
}, false, "japgolly.scalajs.react.vdom.ReactAttr$NameAndValue", {
  Ljapgolly_scalajs_react_vdom_ReactAttr$NameAndValue: 1,
  O: 1,
  Ljapgolly_scalajs_react_vdom_TagMod: 1
});
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$NameAndValue.prototype.$classData = $d_Ljapgolly_scalajs_react_vdom_ReactAttr$NameAndValue;
/** @constructor */
function $c_jl_Number() {
  $c_O.call(this)
}
$c_jl_Number.prototype = new $h_O();
$c_jl_Number.prototype.constructor = $c_jl_Number;
/** @constructor */
function $h_jl_Number() {
  /*<skip>*/
}
$h_jl_Number.prototype = $c_jl_Number.prototype;
function $is_jl_Number(obj) {
  return (!(!(((obj && obj.$classData) && obj.$classData.ancestors.jl_Number) || ((typeof obj) === "number"))))
}
function $as_jl_Number(obj) {
  return (($is_jl_Number(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "java.lang.Number"))
}
function $isArrayOf_jl_Number(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.jl_Number)))
}
function $asArrayOf_jl_Number(obj, depth) {
  return (($isArrayOf_jl_Number(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Ljava.lang.Number;", depth))
}
/** @constructor */
function $c_jl_Throwable() {
  $c_O.call(this);
  this.s$1 = null;
  this.e$1 = null;
  this.stackTrace$1 = null
}
$c_jl_Throwable.prototype = new $h_O();
$c_jl_Throwable.prototype.constructor = $c_jl_Throwable;
/** @constructor */
function $h_jl_Throwable() {
  /*<skip>*/
}
$h_jl_Throwable.prototype = $c_jl_Throwable.prototype;
$c_jl_Throwable.prototype.fillInStackTrace__jl_Throwable = (function() {
  var v = $g.Error.captureStackTrace;
  if ((v === (void 0))) {
    try {
      var e$1 = {}.undef()
    } catch (e) {
      var e$2 = $m_sjsr_package$().wrapJavaScriptException__O__jl_Throwable(e);
      if ((e$2 !== null)) {
        if ($is_sjs_js_JavaScriptException(e$2)) {
          var x5 = $as_sjs_js_JavaScriptException(e$2);
          var e$3 = x5.exception$4;
          var e$1 = e$3
        } else {
          var e$1;
          throw $m_sjsr_package$().unwrapJavaScriptException__jl_Throwable__O(e$2)
        }
      } else {
        var e$1;
        throw e
      }
    };
    this.stackdata = e$1
  } else {
    $g.Error.captureStackTrace(this);
    this.stackdata = this
  };
  return this
});
$c_jl_Throwable.prototype.getMessage__T = (function() {
  return this.s$1
});
$c_jl_Throwable.prototype.toString__T = (function() {
  var className = $objectGetClass(this).getName__T();
  var message = this.getMessage__T();
  return ((message === null) ? className : ((className + ": ") + message))
});
$c_jl_Throwable.prototype.init___T__jl_Throwable = (function(s, e) {
  this.s$1 = s;
  this.e$1 = e;
  this.fillInStackTrace__jl_Throwable();
  return this
});
function $is_jl_Throwable(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.jl_Throwable)))
}
function $as_jl_Throwable(obj) {
  return (($is_jl_Throwable(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "java.lang.Throwable"))
}
function $isArrayOf_jl_Throwable(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.jl_Throwable)))
}
function $asArrayOf_jl_Throwable(obj, depth) {
  return (($isArrayOf_jl_Throwable(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Ljava.lang.Throwable;", depth))
}
/** @constructor */
function $c_ju_regex_Matcher() {
  $c_O.call(this);
  this.pattern0$1 = null;
  this.input0$1 = null;
  this.regionStart0$1 = 0;
  this.regionEnd0$1 = 0;
  this.regexp$1 = null;
  this.inputstr$1 = null;
  this.lastMatch$1 = null;
  this.lastMatchIsValid$1 = false;
  this.canStillFind$1 = false;
  this.appendPos$1 = 0
}
$c_ju_regex_Matcher.prototype = new $h_O();
$c_ju_regex_Matcher.prototype.constructor = $c_ju_regex_Matcher;
/** @constructor */
function $h_ju_regex_Matcher() {
  /*<skip>*/
}
$h_ju_regex_Matcher.prototype = $c_ju_regex_Matcher.prototype;
$c_ju_regex_Matcher.prototype.find__Z = (function() {
  if (this.canStillFind$1) {
    this.lastMatchIsValid$1 = true;
    this.lastMatch$1 = this.regexp$1.exec(this.inputstr$1);
    if ((this.lastMatch$1 !== null)) {
      var value = this.lastMatch$1[0];
      if ((value === (void 0))) {
        throw new $c_ju_NoSuchElementException().init___T("undefined.get")
      };
      var thiz = $as_T(value);
      if ((thiz === null)) {
        throw new $c_jl_NullPointerException().init___()
      };
      if ((thiz === "")) {
        var ev$1 = this.regexp$1;
        ev$1.lastIndex = ((1 + $uI(ev$1.lastIndex)) | 0)
      }
    } else {
      this.canStillFind$1 = false
    };
    return (this.lastMatch$1 !== null)
  } else {
    return false
  }
});
$c_ju_regex_Matcher.prototype.ensureLastMatch__p1__sjs_js_RegExp$ExecResult = (function() {
  if ((this.lastMatch$1 === null)) {
    throw new $c_jl_IllegalStateException().init___T("No match available")
  };
  return this.lastMatch$1
});
$c_ju_regex_Matcher.prototype.matches__Z = (function() {
  this.reset__ju_regex_Matcher();
  this.find__Z();
  if ((this.lastMatch$1 !== null)) {
    if ((this.start__I() !== 0)) {
      var jsx$1 = true
    } else {
      var jsx$2 = this.end__I();
      var thiz = this.inputstr$1;
      var jsx$1 = (jsx$2 !== $uI(thiz.length))
    }
  } else {
    var jsx$1 = false
  };
  if (jsx$1) {
    this.reset__ju_regex_Matcher()
  };
  return (this.lastMatch$1 !== null)
});
$c_ju_regex_Matcher.prototype.end__I = (function() {
  var jsx$1 = this.start__I();
  var thiz = this.group__T();
  return ((jsx$1 + $uI(thiz.length)) | 0)
});
$c_ju_regex_Matcher.prototype.init___ju_regex_Pattern__jl_CharSequence__I__I = (function(pattern0, input0, regionStart0, regionEnd0) {
  this.pattern0$1 = pattern0;
  this.input0$1 = input0;
  this.regionStart0$1 = regionStart0;
  this.regionEnd0$1 = regionEnd0;
  this.regexp$1 = this.pattern0$1.newJSRegExp__sjs_js_RegExp();
  this.inputstr$1 = $objectToString($charSequenceSubSequence(this.input0$1, this.regionStart0$1, this.regionEnd0$1));
  this.lastMatch$1 = null;
  this.lastMatchIsValid$1 = false;
  this.canStillFind$1 = true;
  this.appendPos$1 = 0;
  return this
});
$c_ju_regex_Matcher.prototype.group__T = (function() {
  var value = this.ensureLastMatch__p1__sjs_js_RegExp$ExecResult()[0];
  if ((value === (void 0))) {
    throw new $c_ju_NoSuchElementException().init___T("undefined.get")
  };
  return $as_T(value)
});
$c_ju_regex_Matcher.prototype.start__I = (function() {
  return $uI(this.ensureLastMatch__p1__sjs_js_RegExp$ExecResult().index)
});
$c_ju_regex_Matcher.prototype.reset__ju_regex_Matcher = (function() {
  this.regexp$1.lastIndex = 0;
  this.lastMatch$1 = null;
  this.lastMatchIsValid$1 = false;
  this.canStillFind$1 = true;
  this.appendPos$1 = 0;
  return this
});
var $d_ju_regex_Matcher = new $TypeData().initClass({
  ju_regex_Matcher: 0
}, false, "java.util.regex.Matcher", {
  ju_regex_Matcher: 1,
  O: 1,
  ju_regex_MatchResult: 1
});
$c_ju_regex_Matcher.prototype.$classData = $d_ju_regex_Matcher;
/** @constructor */
function $c_s_Predef$$anon$3() {
  $c_O.call(this)
}
$c_s_Predef$$anon$3.prototype = new $h_O();
$c_s_Predef$$anon$3.prototype.constructor = $c_s_Predef$$anon$3;
/** @constructor */
function $h_s_Predef$$anon$3() {
  /*<skip>*/
}
$h_s_Predef$$anon$3.prototype = $c_s_Predef$$anon$3.prototype;
$c_s_Predef$$anon$3.prototype.init___ = (function() {
  return this
});
var $d_s_Predef$$anon$3 = new $TypeData().initClass({
  s_Predef$$anon$3: 0
}, false, "scala.Predef$$anon$3", {
  s_Predef$$anon$3: 1,
  O: 1,
  scg_CanBuildFrom: 1
});
$c_s_Predef$$anon$3.prototype.$classData = $d_s_Predef$$anon$3;
/** @constructor */
function $c_s_package$$anon$1() {
  $c_O.call(this)
}
$c_s_package$$anon$1.prototype = new $h_O();
$c_s_package$$anon$1.prototype.constructor = $c_s_package$$anon$1;
/** @constructor */
function $h_s_package$$anon$1() {
  /*<skip>*/
}
$h_s_package$$anon$1.prototype = $c_s_package$$anon$1.prototype;
$c_s_package$$anon$1.prototype.init___ = (function() {
  return this
});
$c_s_package$$anon$1.prototype.toString__T = (function() {
  return "object AnyRef"
});
var $d_s_package$$anon$1 = new $TypeData().initClass({
  s_package$$anon$1: 0
}, false, "scala.package$$anon$1", {
  s_package$$anon$1: 1,
  O: 1,
  s_Specializable: 1
});
$c_s_package$$anon$1.prototype.$classData = $d_s_package$$anon$1;
/** @constructor */
function $c_s_util_hashing_MurmurHash3$() {
  $c_s_util_hashing_MurmurHash3.call(this);
  this.arraySeed$2 = 0;
  this.stringSeed$2 = 0;
  this.productSeed$2 = 0;
  this.symmetricSeed$2 = 0;
  this.traversableSeed$2 = 0;
  this.seqSeed$2 = 0;
  this.mapSeed$2 = 0;
  this.setSeed$2 = 0
}
$c_s_util_hashing_MurmurHash3$.prototype = new $h_s_util_hashing_MurmurHash3();
$c_s_util_hashing_MurmurHash3$.prototype.constructor = $c_s_util_hashing_MurmurHash3$;
/** @constructor */
function $h_s_util_hashing_MurmurHash3$() {
  /*<skip>*/
}
$h_s_util_hashing_MurmurHash3$.prototype = $c_s_util_hashing_MurmurHash3$.prototype;
$c_s_util_hashing_MurmurHash3$.prototype.init___ = (function() {
  $n_s_util_hashing_MurmurHash3$ = this;
  this.seqSeed$2 = $m_sjsr_RuntimeString$().hashCode__T__I("Seq");
  this.mapSeed$2 = $m_sjsr_RuntimeString$().hashCode__T__I("Map");
  this.setSeed$2 = $m_sjsr_RuntimeString$().hashCode__T__I("Set");
  return this
});
$c_s_util_hashing_MurmurHash3$.prototype.seqHash__sc_Seq__I = (function(xs) {
  if ($is_sci_List(xs)) {
    var x2 = $as_sci_List(xs);
    return this.listHash__sci_List__I__I(x2, this.seqSeed$2)
  } else {
    return this.orderedHash__sc_TraversableOnce__I__I(xs, this.seqSeed$2)
  }
});
var $d_s_util_hashing_MurmurHash3$ = new $TypeData().initClass({
  s_util_hashing_MurmurHash3$: 0
}, false, "scala.util.hashing.MurmurHash3$", {
  s_util_hashing_MurmurHash3$: 1,
  s_util_hashing_MurmurHash3: 1,
  O: 1
});
$c_s_util_hashing_MurmurHash3$.prototype.$classData = $d_s_util_hashing_MurmurHash3$;
var $n_s_util_hashing_MurmurHash3$ = (void 0);
function $m_s_util_hashing_MurmurHash3$() {
  if ((!$n_s_util_hashing_MurmurHash3$)) {
    $n_s_util_hashing_MurmurHash3$ = new $c_s_util_hashing_MurmurHash3$().init___()
  };
  return $n_s_util_hashing_MurmurHash3$
}
/** @constructor */
function $c_scg_GenSetFactory() {
  $c_scg_GenericCompanion.call(this)
}
$c_scg_GenSetFactory.prototype = new $h_scg_GenericCompanion();
$c_scg_GenSetFactory.prototype.constructor = $c_scg_GenSetFactory;
/** @constructor */
function $h_scg_GenSetFactory() {
  /*<skip>*/
}
$h_scg_GenSetFactory.prototype = $c_scg_GenSetFactory.prototype;
/** @constructor */
function $c_scg_GenTraversableFactory() {
  $c_scg_GenericCompanion.call(this);
  this.ReusableCBFInstance$2 = null
}
$c_scg_GenTraversableFactory.prototype = new $h_scg_GenericCompanion();
$c_scg_GenTraversableFactory.prototype.constructor = $c_scg_GenTraversableFactory;
/** @constructor */
function $h_scg_GenTraversableFactory() {
  /*<skip>*/
}
$h_scg_GenTraversableFactory.prototype = $c_scg_GenTraversableFactory.prototype;
$c_scg_GenTraversableFactory.prototype.init___ = (function() {
  this.ReusableCBFInstance$2 = new $c_scg_GenTraversableFactory$$anon$1().init___scg_GenTraversableFactory(this);
  return this
});
/** @constructor */
function $c_scg_GenTraversableFactory$GenericCanBuildFrom() {
  $c_O.call(this);
  this.$$outer$f = null
}
$c_scg_GenTraversableFactory$GenericCanBuildFrom.prototype = new $h_O();
$c_scg_GenTraversableFactory$GenericCanBuildFrom.prototype.constructor = $c_scg_GenTraversableFactory$GenericCanBuildFrom;
/** @constructor */
function $h_scg_GenTraversableFactory$GenericCanBuildFrom() {
  /*<skip>*/
}
$h_scg_GenTraversableFactory$GenericCanBuildFrom.prototype = $c_scg_GenTraversableFactory$GenericCanBuildFrom.prototype;
$c_scg_GenTraversableFactory$GenericCanBuildFrom.prototype.init___scg_GenTraversableFactory = (function($$outer) {
  if (($$outer === null)) {
    throw $m_sjsr_package$().unwrapJavaScriptException__jl_Throwable__O(null)
  } else {
    this.$$outer$f = $$outer
  };
  return this
});
/** @constructor */
function $c_scg_MapFactory() {
  $c_scg_GenMapFactory.call(this)
}
$c_scg_MapFactory.prototype = new $h_scg_GenMapFactory();
$c_scg_MapFactory.prototype.constructor = $c_scg_MapFactory;
/** @constructor */
function $h_scg_MapFactory() {
  /*<skip>*/
}
$h_scg_MapFactory.prototype = $c_scg_MapFactory.prototype;
/** @constructor */
function $c_sci_List$$anon$1() {
  $c_O.call(this)
}
$c_sci_List$$anon$1.prototype = new $h_O();
$c_sci_List$$anon$1.prototype.constructor = $c_sci_List$$anon$1;
/** @constructor */
function $h_sci_List$$anon$1() {
  /*<skip>*/
}
$h_sci_List$$anon$1.prototype = $c_sci_List$$anon$1.prototype;
$c_sci_List$$anon$1.prototype.init___ = (function() {
  return this
});
$c_sci_List$$anon$1.prototype.apply__O__O = (function(x) {
  return this
});
$c_sci_List$$anon$1.prototype.toString__T = (function() {
  return "<function1>"
});
var $d_sci_List$$anon$1 = new $TypeData().initClass({
  sci_List$$anon$1: 0
}, false, "scala.collection.immutable.List$$anon$1", {
  sci_List$$anon$1: 1,
  O: 1,
  F1: 1
});
$c_sci_List$$anon$1.prototype.$classData = $d_sci_List$$anon$1;
/** @constructor */
function $c_sr_AbstractFunction0() {
  $c_O.call(this)
}
$c_sr_AbstractFunction0.prototype = new $h_O();
$c_sr_AbstractFunction0.prototype.constructor = $c_sr_AbstractFunction0;
/** @constructor */
function $h_sr_AbstractFunction0() {
  /*<skip>*/
}
$h_sr_AbstractFunction0.prototype = $c_sr_AbstractFunction0.prototype;
$c_sr_AbstractFunction0.prototype.toString__T = (function() {
  return "<function0>"
});
/** @constructor */
function $c_sr_AbstractFunction1() {
  $c_O.call(this)
}
$c_sr_AbstractFunction1.prototype = new $h_O();
$c_sr_AbstractFunction1.prototype.constructor = $c_sr_AbstractFunction1;
/** @constructor */
function $h_sr_AbstractFunction1() {
  /*<skip>*/
}
$h_sr_AbstractFunction1.prototype = $c_sr_AbstractFunction1.prototype;
$c_sr_AbstractFunction1.prototype.toString__T = (function() {
  return "<function1>"
});
/** @constructor */
function $c_sr_AbstractFunction2() {
  $c_O.call(this)
}
$c_sr_AbstractFunction2.prototype = new $h_O();
$c_sr_AbstractFunction2.prototype.constructor = $c_sr_AbstractFunction2;
/** @constructor */
function $h_sr_AbstractFunction2() {
  /*<skip>*/
}
$h_sr_AbstractFunction2.prototype = $c_sr_AbstractFunction2.prototype;
$c_sr_AbstractFunction2.prototype.toString__T = (function() {
  return "<function2>"
});
/** @constructor */
function $c_sr_AbstractFunction3() {
  $c_O.call(this)
}
$c_sr_AbstractFunction3.prototype = new $h_O();
$c_sr_AbstractFunction3.prototype.constructor = $c_sr_AbstractFunction3;
/** @constructor */
function $h_sr_AbstractFunction3() {
  /*<skip>*/
}
$h_sr_AbstractFunction3.prototype = $c_sr_AbstractFunction3.prototype;
$c_sr_AbstractFunction3.prototype.toString__T = (function() {
  return "<function3>"
});
/** @constructor */
function $c_sr_BooleanRef() {
  $c_O.call(this);
  this.elem$1 = false
}
$c_sr_BooleanRef.prototype = new $h_O();
$c_sr_BooleanRef.prototype.constructor = $c_sr_BooleanRef;
/** @constructor */
function $h_sr_BooleanRef() {
  /*<skip>*/
}
$h_sr_BooleanRef.prototype = $c_sr_BooleanRef.prototype;
$c_sr_BooleanRef.prototype.toString__T = (function() {
  var value = this.elem$1;
  return ("" + value)
});
$c_sr_BooleanRef.prototype.init___Z = (function(elem) {
  this.elem$1 = elem;
  return this
});
var $d_sr_BooleanRef = new $TypeData().initClass({
  sr_BooleanRef: 0
}, false, "scala.runtime.BooleanRef", {
  sr_BooleanRef: 1,
  O: 1,
  Ljava_io_Serializable: 1
});
$c_sr_BooleanRef.prototype.$classData = $d_sr_BooleanRef;
var $d_sr_BoxedUnit = new $TypeData().initClass({
  sr_BoxedUnit: 0
}, false, "scala.runtime.BoxedUnit", {
  sr_BoxedUnit: 1,
  O: 1,
  Ljava_io_Serializable: 1
}, (void 0), (void 0), (function(x) {
  return (x === (void 0))
}));
/** @constructor */
function $c_sr_IntRef() {
  $c_O.call(this);
  this.elem$1 = 0
}
$c_sr_IntRef.prototype = new $h_O();
$c_sr_IntRef.prototype.constructor = $c_sr_IntRef;
/** @constructor */
function $h_sr_IntRef() {
  /*<skip>*/
}
$h_sr_IntRef.prototype = $c_sr_IntRef.prototype;
$c_sr_IntRef.prototype.toString__T = (function() {
  var value = this.elem$1;
  return ("" + value)
});
$c_sr_IntRef.prototype.init___I = (function(elem) {
  this.elem$1 = elem;
  return this
});
var $d_sr_IntRef = new $TypeData().initClass({
  sr_IntRef: 0
}, false, "scala.runtime.IntRef", {
  sr_IntRef: 1,
  O: 1,
  Ljava_io_Serializable: 1
});
$c_sr_IntRef.prototype.$classData = $d_sr_IntRef;
/** @constructor */
function $c_Ljapgolly_scalajs_react_ReactComponentC$BaseCtor() {
  $c_O.call(this)
}
$c_Ljapgolly_scalajs_react_ReactComponentC$BaseCtor.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_ReactComponentC$BaseCtor.prototype.constructor = $c_Ljapgolly_scalajs_react_ReactComponentC$BaseCtor;
/** @constructor */
function $h_Ljapgolly_scalajs_react_ReactComponentC$BaseCtor() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_ReactComponentC$BaseCtor.prototype = $c_Ljapgolly_scalajs_react_ReactComponentC$BaseCtor.prototype;
$c_Ljapgolly_scalajs_react_ReactComponentC$BaseCtor.prototype.mkProps__O__Ljapgolly_scalajs_react_package$WrapObj = (function(props) {
  var j = $m_Ljapgolly_scalajs_react_package$().WrapObj__O__Ljapgolly_scalajs_react_package$WrapObj(props);
  var value = this.key__sjs_js_UndefOr();
  if ((value !== (void 0))) {
    j.key = value
  };
  var value$1 = this.ref__sjs_js_UndefOr();
  if ((value$1 !== (void 0))) {
    var r = $as_T(value$1);
    j.ref = ($m_Ljapgolly_scalajs_react_package$(), r)
  };
  return j
});
/** @constructor */
function $c_Ljapgolly_scalajs_react_vdom_Implicits$() {
  $c_Ljapgolly_scalajs_react_vdom_Implicits.call(this)
}
$c_Ljapgolly_scalajs_react_vdom_Implicits$.prototype = new $h_Ljapgolly_scalajs_react_vdom_Implicits();
$c_Ljapgolly_scalajs_react_vdom_Implicits$.prototype.constructor = $c_Ljapgolly_scalajs_react_vdom_Implicits$;
/** @constructor */
function $h_Ljapgolly_scalajs_react_vdom_Implicits$() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_vdom_Implicits$.prototype = $c_Ljapgolly_scalajs_react_vdom_Implicits$.prototype;
$c_Ljapgolly_scalajs_react_vdom_Implicits$.prototype.init___ = (function() {
  $c_Ljapgolly_scalajs_react_vdom_Implicits.prototype.init___.call(this);
  return this
});
var $d_Ljapgolly_scalajs_react_vdom_Implicits$ = new $TypeData().initClass({
  Ljapgolly_scalajs_react_vdom_Implicits$: 0
}, false, "japgolly.scalajs.react.vdom.Implicits$", {
  Ljapgolly_scalajs_react_vdom_Implicits$: 1,
  Ljapgolly_scalajs_react_vdom_Implicits: 1,
  Ljapgolly_scalajs_react_vdom_LowPri: 1,
  O: 1
});
$c_Ljapgolly_scalajs_react_vdom_Implicits$.prototype.$classData = $d_Ljapgolly_scalajs_react_vdom_Implicits$;
var $n_Ljapgolly_scalajs_react_vdom_Implicits$ = (void 0);
function $m_Ljapgolly_scalajs_react_vdom_Implicits$() {
  if ((!$n_Ljapgolly_scalajs_react_vdom_Implicits$)) {
    $n_Ljapgolly_scalajs_react_vdom_Implicits$ = new $c_Ljapgolly_scalajs_react_vdom_Implicits$().init___()
  };
  return $n_Ljapgolly_scalajs_react_vdom_Implicits$
}
/** @constructor */
function $c_Ljapgolly_scalajs_react_vdom_package$Base() {
  $c_Ljapgolly_scalajs_react_vdom_Implicits.call(this)
}
$c_Ljapgolly_scalajs_react_vdom_package$Base.prototype = new $h_Ljapgolly_scalajs_react_vdom_Implicits();
$c_Ljapgolly_scalajs_react_vdom_package$Base.prototype.constructor = $c_Ljapgolly_scalajs_react_vdom_package$Base;
/** @constructor */
function $h_Ljapgolly_scalajs_react_vdom_package$Base() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_vdom_package$Base.prototype = $c_Ljapgolly_scalajs_react_vdom_package$Base.prototype;
var $d_jl_Boolean = new $TypeData().initClass({
  jl_Boolean: 0
}, false, "java.lang.Boolean", {
  jl_Boolean: 1,
  O: 1,
  Ljava_io_Serializable: 1,
  jl_Comparable: 1
}, (void 0), (void 0), (function(x) {
  return ((typeof x) === "boolean")
}));
/** @constructor */
function $c_jl_Character() {
  $c_O.call(this);
  this.value$1 = 0
}
$c_jl_Character.prototype = new $h_O();
$c_jl_Character.prototype.constructor = $c_jl_Character;
/** @constructor */
function $h_jl_Character() {
  /*<skip>*/
}
$h_jl_Character.prototype = $c_jl_Character.prototype;
$c_jl_Character.prototype.equals__O__Z = (function(that) {
  if ($is_jl_Character(that)) {
    var jsx$1 = this.value$1;
    var this$1 = $as_jl_Character(that);
    return (jsx$1 === this$1.value$1)
  } else {
    return false
  }
});
$c_jl_Character.prototype.toString__T = (function() {
  var c = this.value$1;
  return $as_T($g.String.fromCharCode(c))
});
$c_jl_Character.prototype.init___C = (function(value) {
  this.value$1 = value;
  return this
});
$c_jl_Character.prototype.hashCode__I = (function() {
  return this.value$1
});
function $is_jl_Character(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.jl_Character)))
}
function $as_jl_Character(obj) {
  return (($is_jl_Character(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "java.lang.Character"))
}
function $isArrayOf_jl_Character(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.jl_Character)))
}
function $asArrayOf_jl_Character(obj, depth) {
  return (($isArrayOf_jl_Character(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Ljava.lang.Character;", depth))
}
var $d_jl_Character = new $TypeData().initClass({
  jl_Character: 0
}, false, "java.lang.Character", {
  jl_Character: 1,
  O: 1,
  Ljava_io_Serializable: 1,
  jl_Comparable: 1
});
$c_jl_Character.prototype.$classData = $d_jl_Character;
/** @constructor */
function $c_jl_Double$() {
  $c_O.call(this);
  this.TYPE$1 = null;
  this.POSITIVE$undINFINITY$1 = 0.0;
  this.NEGATIVE$undINFINITY$1 = 0.0;
  this.NaN$1 = 0.0;
  this.MAX$undVALUE$1 = 0.0;
  this.MIN$undVALUE$1 = 0.0;
  this.MAX$undEXPONENT$1 = 0;
  this.MIN$undEXPONENT$1 = 0;
  this.SIZE$1 = 0;
  this.doubleStrPat$1 = null;
  this.bitmap$0$1 = false
}
$c_jl_Double$.prototype = new $h_O();
$c_jl_Double$.prototype.constructor = $c_jl_Double$;
/** @constructor */
function $h_jl_Double$() {
  /*<skip>*/
}
$h_jl_Double$.prototype = $c_jl_Double$.prototype;
$c_jl_Double$.prototype.init___ = (function() {
  return this
});
$c_jl_Double$.prototype.compare__D__D__I = (function(a, b) {
  if ((a !== a)) {
    return ((b !== b) ? 0 : 1)
  } else if ((b !== b)) {
    return (-1)
  } else if ((a === b)) {
    if ((a === 0.0)) {
      var ainf = (1.0 / a);
      return ((ainf === (1.0 / b)) ? 0 : ((ainf < 0) ? (-1) : 1))
    } else {
      return 0
    }
  } else {
    return ((a < b) ? (-1) : 1)
  }
});
var $d_jl_Double$ = new $TypeData().initClass({
  jl_Double$: 0
}, false, "java.lang.Double$", {
  jl_Double$: 1,
  O: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_jl_Double$.prototype.$classData = $d_jl_Double$;
var $n_jl_Double$ = (void 0);
function $m_jl_Double$() {
  if ((!$n_jl_Double$)) {
    $n_jl_Double$ = new $c_jl_Double$().init___()
  };
  return $n_jl_Double$
}
/** @constructor */
function $c_jl_Error() {
  $c_jl_Throwable.call(this)
}
$c_jl_Error.prototype = new $h_jl_Throwable();
$c_jl_Error.prototype.constructor = $c_jl_Error;
/** @constructor */
function $h_jl_Error() {
  /*<skip>*/
}
$h_jl_Error.prototype = $c_jl_Error.prototype;
/** @constructor */
function $c_jl_Exception() {
  $c_jl_Throwable.call(this)
}
$c_jl_Exception.prototype = new $h_jl_Throwable();
$c_jl_Exception.prototype.constructor = $c_jl_Exception;
/** @constructor */
function $h_jl_Exception() {
  /*<skip>*/
}
$h_jl_Exception.prototype = $c_jl_Exception.prototype;
/** @constructor */
function $c_ju_regex_Pattern() {
  $c_O.call(this);
  this.jsRegExp$1 = null;
  this.$$undpattern$1 = null;
  this.$$undflags$1 = 0
}
$c_ju_regex_Pattern.prototype = new $h_O();
$c_ju_regex_Pattern.prototype.constructor = $c_ju_regex_Pattern;
/** @constructor */
function $h_ju_regex_Pattern() {
  /*<skip>*/
}
$h_ju_regex_Pattern.prototype = $c_ju_regex_Pattern.prototype;
$c_ju_regex_Pattern.prototype.init___sjs_js_RegExp__T__I = (function(jsRegExp, _pattern, _flags) {
  this.jsRegExp$1 = jsRegExp;
  this.$$undpattern$1 = _pattern;
  this.$$undflags$1 = _flags;
  return this
});
$c_ju_regex_Pattern.prototype.toString__T = (function() {
  return this.$$undpattern$1
});
$c_ju_regex_Pattern.prototype.newJSRegExp__sjs_js_RegExp = (function() {
  var r = new $g.RegExp(this.jsRegExp$1);
  if ((r !== this.jsRegExp$1)) {
    return r
  } else {
    var jsFlags = ((($uZ(this.jsRegExp$1.global) ? "g" : "") + ($uZ(this.jsRegExp$1.ignoreCase) ? "i" : "")) + ($uZ(this.jsRegExp$1.multiline) ? "m" : ""));
    return new $g.RegExp($as_T(this.jsRegExp$1.source), jsFlags)
  }
});
var $d_ju_regex_Pattern = new $TypeData().initClass({
  ju_regex_Pattern: 0
}, false, "java.util.regex.Pattern", {
  ju_regex_Pattern: 1,
  O: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_ju_regex_Pattern.prototype.$classData = $d_ju_regex_Pattern;
/** @constructor */
function $c_ju_regex_Pattern$() {
  $c_O.call(this);
  this.UNIX$undLINES$1 = 0;
  this.CASE$undINSENSITIVE$1 = 0;
  this.COMMENTS$1 = 0;
  this.MULTILINE$1 = 0;
  this.LITERAL$1 = 0;
  this.DOTALL$1 = 0;
  this.UNICODE$undCASE$1 = 0;
  this.CANON$undEQ$1 = 0;
  this.UNICODE$undCHARACTER$undCLASS$1 = 0;
  this.java$util$regex$Pattern$$splitHackPat$1 = null;
  this.java$util$regex$Pattern$$flagHackPat$1 = null
}
$c_ju_regex_Pattern$.prototype = new $h_O();
$c_ju_regex_Pattern$.prototype.constructor = $c_ju_regex_Pattern$;
/** @constructor */
function $h_ju_regex_Pattern$() {
  /*<skip>*/
}
$h_ju_regex_Pattern$.prototype = $c_ju_regex_Pattern$.prototype;
$c_ju_regex_Pattern$.prototype.init___ = (function() {
  $n_ju_regex_Pattern$ = this;
  this.java$util$regex$Pattern$$splitHackPat$1 = new $g.RegExp("^\\\\Q(.|\\n|\\r)\\\\E$");
  this.java$util$regex$Pattern$$flagHackPat$1 = new $g.RegExp("^\\(\\?([idmsuxU]*)(?:-([idmsuxU]*))?\\)");
  return this
});
$c_ju_regex_Pattern$.prototype.compile__T__I__ju_regex_Pattern = (function(regex, flags) {
  if (((16 & flags) !== 0)) {
    var x1 = new $c_T2().init___O__O(this.quote__T__T(regex), flags)
  } else {
    var m = this.java$util$regex$Pattern$$splitHackPat$1.exec(regex);
    if ((m !== null)) {
      var value = m[1];
      if ((value === (void 0))) {
        throw new $c_ju_NoSuchElementException().init___T("undefined.get")
      };
      var this$4 = new $c_s_Some().init___O(new $c_T2().init___O__O(this.quote__T__T($as_T(value)), flags))
    } else {
      var this$4 = $m_s_None$()
    };
    if (this$4.isEmpty__Z()) {
      var m$1 = this.java$util$regex$Pattern$$flagHackPat$1.exec(regex);
      if ((m$1 !== null)) {
        var value$1 = m$1[0];
        if ((value$1 === (void 0))) {
          throw new $c_ju_NoSuchElementException().init___T("undefined.get")
        };
        var thiz = $as_T(value$1);
        var beginIndex = $uI(thiz.length);
        var newPat = $as_T(regex.substring(beginIndex));
        var value$2 = m$1[1];
        if ((value$2 === (void 0))) {
          var flags1 = flags
        } else {
          var chars = $as_T(value$2);
          var this$15 = new $c_sci_StringOps().init___T(chars);
          var start = 0;
          var $$this = this$15.repr$1;
          var end = $uI($$this.length);
          var z = flags;
          var jsx$1;
          _foldl: while (true) {
            if ((start !== end)) {
              var temp$start = ((1 + start) | 0);
              var arg1 = z;
              var arg2 = this$15.apply__I__O(start);
              var f = $uI(arg1);
              if ((arg2 === null)) {
                var c = 0
              } else {
                var this$19 = $as_jl_Character(arg2);
                var c = this$19.value$1
              };
              var temp$z = (f | this.java$util$regex$Pattern$$charToFlag__C__I(c));
              start = temp$start;
              z = temp$z;
              continue _foldl
            };
            var jsx$1 = z;
            break
          };
          var flags1 = $uI(jsx$1)
        };
        var value$3 = m$1[2];
        if ((value$3 === (void 0))) {
          var flags2 = flags1
        } else {
          var chars$3 = $as_T(value$3);
          var this$24 = new $c_sci_StringOps().init___T(chars$3);
          var start$1 = 0;
          var $$this$1 = this$24.repr$1;
          var end$1 = $uI($$this$1.length);
          var z$1 = flags1;
          var jsx$2;
          _foldl$1: while (true) {
            if ((start$1 !== end$1)) {
              var temp$start$1 = ((1 + start$1) | 0);
              var arg1$1 = z$1;
              var arg2$1 = this$24.apply__I__O(start$1);
              var f$1 = $uI(arg1$1);
              if ((arg2$1 === null)) {
                var c$1 = 0
              } else {
                var this$28 = $as_jl_Character(arg2$1);
                var c$1 = this$28.value$1
              };
              var temp$z$1 = (f$1 & (~this.java$util$regex$Pattern$$charToFlag__C__I(c$1)));
              start$1 = temp$start$1;
              z$1 = temp$z$1;
              continue _foldl$1
            };
            var jsx$2 = z$1;
            break
          };
          var flags2 = $uI(jsx$2)
        };
        var this$29 = new $c_s_Some().init___O(new $c_T2().init___O__O(newPat, flags2))
      } else {
        var this$29 = $m_s_None$()
      }
    } else {
      var this$29 = this$4
    };
    var x1 = $as_T2((this$29.isEmpty__Z() ? new $c_T2().init___O__O(regex, flags) : this$29.get__O()))
  };
  if ((x1 === null)) {
    throw new $c_s_MatchError().init___O(x1)
  };
  var jsPattern = $as_T(x1.$$und1$f);
  var flags1$1 = $uI(x1.$$und2$f);
  var jsFlags = (("g" + (((2 & flags1$1) !== 0) ? "i" : "")) + (((8 & flags1$1) !== 0) ? "m" : ""));
  var jsRegExp = new $g.RegExp(jsPattern, jsFlags);
  return new $c_ju_regex_Pattern().init___sjs_js_RegExp__T__I(jsRegExp, regex, flags1$1)
});
$c_ju_regex_Pattern$.prototype.quote__T__T = (function(s) {
  var result = "";
  var i = 0;
  while ((i < $uI(s.length))) {
    var index = i;
    var c = (65535 & $uI(s.charCodeAt(index)));
    var jsx$2 = result;
    switch (c) {
      case 92:
      case 46:
      case 40:
      case 41:
      case 91:
      case 93:
      case 123:
      case 125:
      case 124:
      case 63:
      case 42:
      case 43:
      case 94:
      case 36: {
        var jsx$1 = ("\\" + new $c_jl_Character().init___C(c));
        break
      }
      default: {
        var jsx$1 = new $c_jl_Character().init___C(c)
      }
    };
    result = (("" + jsx$2) + jsx$1);
    i = ((1 + i) | 0)
  };
  return result
});
$c_ju_regex_Pattern$.prototype.java$util$regex$Pattern$$charToFlag__C__I = (function(c) {
  switch (c) {
    case 105: {
      return 2;
      break
    }
    case 100: {
      return 1;
      break
    }
    case 109: {
      return 8;
      break
    }
    case 115: {
      return 32;
      break
    }
    case 117: {
      return 64;
      break
    }
    case 120: {
      return 4;
      break
    }
    case 85: {
      return 256;
      break
    }
    default: {
      $m_s_sys_package$().error__T__sr_Nothing$("bad in-pattern flag")
    }
  }
});
var $d_ju_regex_Pattern$ = new $TypeData().initClass({
  ju_regex_Pattern$: 0
}, false, "java.util.regex.Pattern$", {
  ju_regex_Pattern$: 1,
  O: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_ju_regex_Pattern$.prototype.$classData = $d_ju_regex_Pattern$;
var $n_ju_regex_Pattern$ = (void 0);
function $m_ju_regex_Pattern$() {
  if ((!$n_ju_regex_Pattern$)) {
    $n_ju_regex_Pattern$ = new $c_ju_regex_Pattern$().init___()
  };
  return $n_ju_regex_Pattern$
}
/** @constructor */
function $c_s_Option$() {
  $c_O.call(this)
}
$c_s_Option$.prototype = new $h_O();
$c_s_Option$.prototype.constructor = $c_s_Option$;
/** @constructor */
function $h_s_Option$() {
  /*<skip>*/
}
$h_s_Option$.prototype = $c_s_Option$.prototype;
$c_s_Option$.prototype.init___ = (function() {
  return this
});
$c_s_Option$.prototype.apply__O__s_Option = (function(x) {
  return ((x === null) ? $m_s_None$() : new $c_s_Some().init___O(x))
});
var $d_s_Option$ = new $TypeData().initClass({
  s_Option$: 0
}, false, "scala.Option$", {
  s_Option$: 1,
  O: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_s_Option$.prototype.$classData = $d_s_Option$;
var $n_s_Option$ = (void 0);
function $m_s_Option$() {
  if ((!$n_s_Option$)) {
    $n_s_Option$ = new $c_s_Option$().init___()
  };
  return $n_s_Option$
}
/** @constructor */
function $c_s_Predef$() {
  $c_s_LowPriorityImplicits.call(this);
  this.Map$2 = null;
  this.Set$2 = null;
  this.ClassManifest$2 = null;
  this.Manifest$2 = null;
  this.NoManifest$2 = null;
  this.StringCanBuildFrom$2 = null;
  this.singleton$und$less$colon$less$2 = null;
  this.scala$Predef$$singleton$und$eq$colon$eq$f = null
}
$c_s_Predef$.prototype = new $h_s_LowPriorityImplicits();
$c_s_Predef$.prototype.constructor = $c_s_Predef$;
/** @constructor */
function $h_s_Predef$() {
  /*<skip>*/
}
$h_s_Predef$.prototype = $c_s_Predef$.prototype;
$c_s_Predef$.prototype.init___ = (function() {
  $n_s_Predef$ = this;
  $m_s_package$();
  $m_sci_List$();
  this.Map$2 = $m_sci_Map$();
  this.Set$2 = $m_sci_Set$();
  this.ClassManifest$2 = $m_s_reflect_package$().ClassManifest$1;
  this.Manifest$2 = $m_s_reflect_package$().Manifest$1;
  this.NoManifest$2 = $m_s_reflect_NoManifest$();
  this.StringCanBuildFrom$2 = new $c_s_Predef$$anon$3().init___();
  this.singleton$und$less$colon$less$2 = new $c_s_Predef$$anon$1().init___();
  this.scala$Predef$$singleton$und$eq$colon$eq$f = new $c_s_Predef$$anon$2().init___();
  return this
});
$c_s_Predef$.prototype.require__Z__V = (function(requirement) {
  if ((!requirement)) {
    throw new $c_jl_IllegalArgumentException().init___T("requirement failed")
  }
});
var $d_s_Predef$ = new $TypeData().initClass({
  s_Predef$: 0
}, false, "scala.Predef$", {
  s_Predef$: 1,
  s_LowPriorityImplicits: 1,
  O: 1,
  s_DeprecatedPredef: 1
});
$c_s_Predef$.prototype.$classData = $d_s_Predef$;
var $n_s_Predef$ = (void 0);
function $m_s_Predef$() {
  if ((!$n_s_Predef$)) {
    $n_s_Predef$ = new $c_s_Predef$().init___()
  };
  return $n_s_Predef$
}
/** @constructor */
function $c_s_StringContext$() {
  $c_O.call(this)
}
$c_s_StringContext$.prototype = new $h_O();
$c_s_StringContext$.prototype.constructor = $c_s_StringContext$;
/** @constructor */
function $h_s_StringContext$() {
  /*<skip>*/
}
$h_s_StringContext$.prototype = $c_s_StringContext$.prototype;
$c_s_StringContext$.prototype.init___ = (function() {
  return this
});
$c_s_StringContext$.prototype.treatEscapes0__p1__T__Z__T = (function(str, strict) {
  var len = $uI(str.length);
  var x1 = $m_sjsr_RuntimeString$().indexOf__T__I__I(str, 92);
  switch (x1) {
    case (-1): {
      return str;
      break
    }
    default: {
      return this.replace$1__p1__I__T__Z__I__T(x1, str, strict, len)
    }
  }
});
$c_s_StringContext$.prototype.loop$1__p1__I__I__T__Z__I__jl_StringBuilder__T = (function(i, next, str$1, strict$1, len$1, b$1) {
  _loop: while (true) {
    if ((next >= 0)) {
      if ((next > i)) {
        b$1.append__jl_CharSequence__I__I__jl_StringBuilder(str$1, i, next)
      };
      var idx = ((1 + next) | 0);
      if ((idx >= len$1)) {
        throw new $c_s_StringContext$InvalidEscapeException().init___T__I(str$1, next)
      };
      var index = idx;
      var x1 = (65535 & $uI(str$1.charCodeAt(index)));
      switch (x1) {
        case 98: {
          var c = 8;
          break
        }
        case 116: {
          var c = 9;
          break
        }
        case 110: {
          var c = 10;
          break
        }
        case 102: {
          var c = 12;
          break
        }
        case 114: {
          var c = 13;
          break
        }
        case 34: {
          var c = 34;
          break
        }
        case 39: {
          var c = 39;
          break
        }
        case 92: {
          var c = 92;
          break
        }
        default: {
          if (((x1 >= 48) && (x1 <= 55))) {
            if (strict$1) {
              throw new $c_s_StringContext$InvalidEscapeException().init___T__I(str$1, next)
            };
            var index$1 = idx;
            var leadch = (65535 & $uI(str$1.charCodeAt(index$1)));
            var oct = (((-48) + leadch) | 0);
            idx = ((1 + idx) | 0);
            if ((idx < len$1)) {
              var index$2 = idx;
              var jsx$2 = ((65535 & $uI(str$1.charCodeAt(index$2))) >= 48)
            } else {
              var jsx$2 = false
            };
            if (jsx$2) {
              var index$3 = idx;
              var jsx$1 = ((65535 & $uI(str$1.charCodeAt(index$3))) <= 55)
            } else {
              var jsx$1 = false
            };
            if (jsx$1) {
              var jsx$3 = oct;
              var index$4 = idx;
              oct = (((-48) + (((jsx$3 << 3) + (65535 & $uI(str$1.charCodeAt(index$4)))) | 0)) | 0);
              idx = ((1 + idx) | 0);
              if (((idx < len$1) && (leadch <= 51))) {
                var index$5 = idx;
                var jsx$5 = ((65535 & $uI(str$1.charCodeAt(index$5))) >= 48)
              } else {
                var jsx$5 = false
              };
              if (jsx$5) {
                var index$6 = idx;
                var jsx$4 = ((65535 & $uI(str$1.charCodeAt(index$6))) <= 55)
              } else {
                var jsx$4 = false
              };
              if (jsx$4) {
                var jsx$6 = oct;
                var index$7 = idx;
                oct = (((-48) + (((jsx$6 << 3) + (65535 & $uI(str$1.charCodeAt(index$7)))) | 0)) | 0);
                idx = ((1 + idx) | 0)
              }
            };
            idx = (((-1) + idx) | 0);
            var c = (65535 & oct)
          } else {
            var c;
            throw new $c_s_StringContext$InvalidEscapeException().init___T__I(str$1, next)
          }
        }
      };
      idx = ((1 + idx) | 0);
      b$1.append__C__jl_StringBuilder(c);
      var temp$i = idx;
      var temp$next = $m_sjsr_RuntimeString$().indexOf__T__I__I__I(str$1, 92, idx);
      i = temp$i;
      next = temp$next;
      continue _loop
    } else {
      if ((i < len$1)) {
        b$1.append__jl_CharSequence__I__I__jl_StringBuilder(str$1, i, len$1)
      };
      return b$1.content$1
    }
  }
});
$c_s_StringContext$.prototype.replace$1__p1__I__T__Z__I__T = (function(first, str$1, strict$1, len$1) {
  var b = new $c_jl_StringBuilder().init___();
  return this.loop$1__p1__I__I__T__Z__I__jl_StringBuilder__T(0, first, str$1, strict$1, len$1, b)
});
var $d_s_StringContext$ = new $TypeData().initClass({
  s_StringContext$: 0
}, false, "scala.StringContext$", {
  s_StringContext$: 1,
  O: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_s_StringContext$.prototype.$classData = $d_s_StringContext$;
var $n_s_StringContext$ = (void 0);
function $m_s_StringContext$() {
  if ((!$n_s_StringContext$)) {
    $n_s_StringContext$ = new $c_s_StringContext$().init___()
  };
  return $n_s_StringContext$
}
/** @constructor */
function $c_s_math_Fractional$() {
  $c_O.call(this)
}
$c_s_math_Fractional$.prototype = new $h_O();
$c_s_math_Fractional$.prototype.constructor = $c_s_math_Fractional$;
/** @constructor */
function $h_s_math_Fractional$() {
  /*<skip>*/
}
$h_s_math_Fractional$.prototype = $c_s_math_Fractional$.prototype;
$c_s_math_Fractional$.prototype.init___ = (function() {
  return this
});
var $d_s_math_Fractional$ = new $TypeData().initClass({
  s_math_Fractional$: 0
}, false, "scala.math.Fractional$", {
  s_math_Fractional$: 1,
  O: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_s_math_Fractional$.prototype.$classData = $d_s_math_Fractional$;
var $n_s_math_Fractional$ = (void 0);
function $m_s_math_Fractional$() {
  if ((!$n_s_math_Fractional$)) {
    $n_s_math_Fractional$ = new $c_s_math_Fractional$().init___()
  };
  return $n_s_math_Fractional$
}
/** @constructor */
function $c_s_math_Integral$() {
  $c_O.call(this)
}
$c_s_math_Integral$.prototype = new $h_O();
$c_s_math_Integral$.prototype.constructor = $c_s_math_Integral$;
/** @constructor */
function $h_s_math_Integral$() {
  /*<skip>*/
}
$h_s_math_Integral$.prototype = $c_s_math_Integral$.prototype;
$c_s_math_Integral$.prototype.init___ = (function() {
  return this
});
var $d_s_math_Integral$ = new $TypeData().initClass({
  s_math_Integral$: 0
}, false, "scala.math.Integral$", {
  s_math_Integral$: 1,
  O: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_s_math_Integral$.prototype.$classData = $d_s_math_Integral$;
var $n_s_math_Integral$ = (void 0);
function $m_s_math_Integral$() {
  if ((!$n_s_math_Integral$)) {
    $n_s_math_Integral$ = new $c_s_math_Integral$().init___()
  };
  return $n_s_math_Integral$
}
/** @constructor */
function $c_s_math_Numeric$() {
  $c_O.call(this)
}
$c_s_math_Numeric$.prototype = new $h_O();
$c_s_math_Numeric$.prototype.constructor = $c_s_math_Numeric$;
/** @constructor */
function $h_s_math_Numeric$() {
  /*<skip>*/
}
$h_s_math_Numeric$.prototype = $c_s_math_Numeric$.prototype;
$c_s_math_Numeric$.prototype.init___ = (function() {
  return this
});
var $d_s_math_Numeric$ = new $TypeData().initClass({
  s_math_Numeric$: 0
}, false, "scala.math.Numeric$", {
  s_math_Numeric$: 1,
  O: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_s_math_Numeric$.prototype.$classData = $d_s_math_Numeric$;
var $n_s_math_Numeric$ = (void 0);
function $m_s_math_Numeric$() {
  if ((!$n_s_math_Numeric$)) {
    $n_s_math_Numeric$ = new $c_s_math_Numeric$().init___()
  };
  return $n_s_math_Numeric$
}
function $is_s_math_ScalaNumber(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.s_math_ScalaNumber)))
}
function $as_s_math_ScalaNumber(obj) {
  return (($is_s_math_ScalaNumber(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "scala.math.ScalaNumber"))
}
function $isArrayOf_s_math_ScalaNumber(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.s_math_ScalaNumber)))
}
function $asArrayOf_s_math_ScalaNumber(obj, depth) {
  return (($isArrayOf_s_math_ScalaNumber(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Lscala.math.ScalaNumber;", depth))
}
/** @constructor */
function $c_s_util_Left$() {
  $c_O.call(this)
}
$c_s_util_Left$.prototype = new $h_O();
$c_s_util_Left$.prototype.constructor = $c_s_util_Left$;
/** @constructor */
function $h_s_util_Left$() {
  /*<skip>*/
}
$h_s_util_Left$.prototype = $c_s_util_Left$.prototype;
$c_s_util_Left$.prototype.init___ = (function() {
  return this
});
$c_s_util_Left$.prototype.toString__T = (function() {
  return "Left"
});
var $d_s_util_Left$ = new $TypeData().initClass({
  s_util_Left$: 0
}, false, "scala.util.Left$", {
  s_util_Left$: 1,
  O: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_s_util_Left$.prototype.$classData = $d_s_util_Left$;
var $n_s_util_Left$ = (void 0);
function $m_s_util_Left$() {
  if ((!$n_s_util_Left$)) {
    $n_s_util_Left$ = new $c_s_util_Left$().init___()
  };
  return $n_s_util_Left$
}
/** @constructor */
function $c_s_util_Right$() {
  $c_O.call(this)
}
$c_s_util_Right$.prototype = new $h_O();
$c_s_util_Right$.prototype.constructor = $c_s_util_Right$;
/** @constructor */
function $h_s_util_Right$() {
  /*<skip>*/
}
$h_s_util_Right$.prototype = $c_s_util_Right$.prototype;
$c_s_util_Right$.prototype.init___ = (function() {
  return this
});
$c_s_util_Right$.prototype.toString__T = (function() {
  return "Right"
});
var $d_s_util_Right$ = new $TypeData().initClass({
  s_util_Right$: 0
}, false, "scala.util.Right$", {
  s_util_Right$: 1,
  O: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_s_util_Right$.prototype.$classData = $d_s_util_Right$;
var $n_s_util_Right$ = (void 0);
function $m_s_util_Right$() {
  if ((!$n_s_util_Right$)) {
    $n_s_util_Right$ = new $c_s_util_Right$().init___()
  };
  return $n_s_util_Right$
}
/** @constructor */
function $c_s_util_control_NoStackTrace$() {
  $c_O.call(this);
  this.$$undnoSuppression$1 = false
}
$c_s_util_control_NoStackTrace$.prototype = new $h_O();
$c_s_util_control_NoStackTrace$.prototype.constructor = $c_s_util_control_NoStackTrace$;
/** @constructor */
function $h_s_util_control_NoStackTrace$() {
  /*<skip>*/
}
$h_s_util_control_NoStackTrace$.prototype = $c_s_util_control_NoStackTrace$.prototype;
$c_s_util_control_NoStackTrace$.prototype.init___ = (function() {
  this.$$undnoSuppression$1 = false;
  return this
});
var $d_s_util_control_NoStackTrace$ = new $TypeData().initClass({
  s_util_control_NoStackTrace$: 0
}, false, "scala.util.control.NoStackTrace$", {
  s_util_control_NoStackTrace$: 1,
  O: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_s_util_control_NoStackTrace$.prototype.$classData = $d_s_util_control_NoStackTrace$;
var $n_s_util_control_NoStackTrace$ = (void 0);
function $m_s_util_control_NoStackTrace$() {
  if ((!$n_s_util_control_NoStackTrace$)) {
    $n_s_util_control_NoStackTrace$ = new $c_s_util_control_NoStackTrace$().init___()
  };
  return $n_s_util_control_NoStackTrace$
}
/** @constructor */
function $c_s_util_matching_Regex() {
  $c_O.call(this);
  this.pattern$1 = null;
  this.scala$util$matching$Regex$$groupNames$f = null
}
$c_s_util_matching_Regex.prototype = new $h_O();
$c_s_util_matching_Regex.prototype.constructor = $c_s_util_matching_Regex;
/** @constructor */
function $h_s_util_matching_Regex() {
  /*<skip>*/
}
$h_s_util_matching_Regex.prototype = $c_s_util_matching_Regex.prototype;
$c_s_util_matching_Regex.prototype.init___T__sc_Seq = (function(regex, groupNames) {
  var this$1 = $m_ju_regex_Pattern$();
  $c_s_util_matching_Regex.prototype.init___ju_regex_Pattern__sc_Seq.call(this, this$1.compile__T__I__ju_regex_Pattern(regex, 0), groupNames);
  return this
});
$c_s_util_matching_Regex.prototype.init___ju_regex_Pattern__sc_Seq = (function(pattern, groupNames) {
  this.pattern$1 = pattern;
  this.scala$util$matching$Regex$$groupNames$f = groupNames;
  return this
});
$c_s_util_matching_Regex.prototype.toString__T = (function() {
  return this.pattern$1.$$undpattern$1
});
var $d_s_util_matching_Regex = new $TypeData().initClass({
  s_util_matching_Regex: 0
}, false, "scala.util.matching.Regex", {
  s_util_matching_Regex: 1,
  O: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_s_util_matching_Regex.prototype.$classData = $d_s_util_matching_Regex;
/** @constructor */
function $c_sc_IndexedSeq$$anon$1() {
  $c_scg_GenTraversableFactory$GenericCanBuildFrom.call(this)
}
$c_sc_IndexedSeq$$anon$1.prototype = new $h_scg_GenTraversableFactory$GenericCanBuildFrom();
$c_sc_IndexedSeq$$anon$1.prototype.constructor = $c_sc_IndexedSeq$$anon$1;
/** @constructor */
function $h_sc_IndexedSeq$$anon$1() {
  /*<skip>*/
}
$h_sc_IndexedSeq$$anon$1.prototype = $c_sc_IndexedSeq$$anon$1.prototype;
$c_sc_IndexedSeq$$anon$1.prototype.init___ = (function() {
  $c_scg_GenTraversableFactory$GenericCanBuildFrom.prototype.init___scg_GenTraversableFactory.call(this, $m_sc_IndexedSeq$());
  return this
});
var $d_sc_IndexedSeq$$anon$1 = new $TypeData().initClass({
  sc_IndexedSeq$$anon$1: 0
}, false, "scala.collection.IndexedSeq$$anon$1", {
  sc_IndexedSeq$$anon$1: 1,
  scg_GenTraversableFactory$GenericCanBuildFrom: 1,
  O: 1,
  scg_CanBuildFrom: 1
});
$c_sc_IndexedSeq$$anon$1.prototype.$classData = $d_sc_IndexedSeq$$anon$1;
/** @constructor */
function $c_scg_GenSeqFactory() {
  $c_scg_GenTraversableFactory.call(this)
}
$c_scg_GenSeqFactory.prototype = new $h_scg_GenTraversableFactory();
$c_scg_GenSeqFactory.prototype.constructor = $c_scg_GenSeqFactory;
/** @constructor */
function $h_scg_GenSeqFactory() {
  /*<skip>*/
}
$h_scg_GenSeqFactory.prototype = $c_scg_GenSeqFactory.prototype;
/** @constructor */
function $c_scg_GenTraversableFactory$$anon$1() {
  $c_scg_GenTraversableFactory$GenericCanBuildFrom.call(this);
  this.$$outer$2 = null
}
$c_scg_GenTraversableFactory$$anon$1.prototype = new $h_scg_GenTraversableFactory$GenericCanBuildFrom();
$c_scg_GenTraversableFactory$$anon$1.prototype.constructor = $c_scg_GenTraversableFactory$$anon$1;
/** @constructor */
function $h_scg_GenTraversableFactory$$anon$1() {
  /*<skip>*/
}
$h_scg_GenTraversableFactory$$anon$1.prototype = $c_scg_GenTraversableFactory$$anon$1.prototype;
$c_scg_GenTraversableFactory$$anon$1.prototype.init___scg_GenTraversableFactory = (function($$outer) {
  if (($$outer === null)) {
    throw $m_sjsr_package$().unwrapJavaScriptException__jl_Throwable__O(null)
  } else {
    this.$$outer$2 = $$outer
  };
  $c_scg_GenTraversableFactory$GenericCanBuildFrom.prototype.init___scg_GenTraversableFactory.call(this, $$outer);
  return this
});
var $d_scg_GenTraversableFactory$$anon$1 = new $TypeData().initClass({
  scg_GenTraversableFactory$$anon$1: 0
}, false, "scala.collection.generic.GenTraversableFactory$$anon$1", {
  scg_GenTraversableFactory$$anon$1: 1,
  scg_GenTraversableFactory$GenericCanBuildFrom: 1,
  O: 1,
  scg_CanBuildFrom: 1
});
$c_scg_GenTraversableFactory$$anon$1.prototype.$classData = $d_scg_GenTraversableFactory$$anon$1;
/** @constructor */
function $c_scg_ImmutableMapFactory() {
  $c_scg_MapFactory.call(this)
}
$c_scg_ImmutableMapFactory.prototype = new $h_scg_MapFactory();
$c_scg_ImmutableMapFactory.prototype.constructor = $c_scg_ImmutableMapFactory;
/** @constructor */
function $h_scg_ImmutableMapFactory() {
  /*<skip>*/
}
$h_scg_ImmutableMapFactory.prototype = $c_scg_ImmutableMapFactory.prototype;
/** @constructor */
function $c_sci_$colon$colon$() {
  $c_O.call(this)
}
$c_sci_$colon$colon$.prototype = new $h_O();
$c_sci_$colon$colon$.prototype.constructor = $c_sci_$colon$colon$;
/** @constructor */
function $h_sci_$colon$colon$() {
  /*<skip>*/
}
$h_sci_$colon$colon$.prototype = $c_sci_$colon$colon$.prototype;
$c_sci_$colon$colon$.prototype.init___ = (function() {
  return this
});
$c_sci_$colon$colon$.prototype.toString__T = (function() {
  return "::"
});
var $d_sci_$colon$colon$ = new $TypeData().initClass({
  sci_$colon$colon$: 0
}, false, "scala.collection.immutable.$colon$colon$", {
  sci_$colon$colon$: 1,
  O: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_sci_$colon$colon$.prototype.$classData = $d_sci_$colon$colon$;
var $n_sci_$colon$colon$ = (void 0);
function $m_sci_$colon$colon$() {
  if ((!$n_sci_$colon$colon$)) {
    $n_sci_$colon$colon$ = new $c_sci_$colon$colon$().init___()
  };
  return $n_sci_$colon$colon$
}
/** @constructor */
function $c_sci_Range$() {
  $c_O.call(this);
  this.MAX$undPRINT$1 = 0
}
$c_sci_Range$.prototype = new $h_O();
$c_sci_Range$.prototype.constructor = $c_sci_Range$;
/** @constructor */
function $h_sci_Range$() {
  /*<skip>*/
}
$h_sci_Range$.prototype = $c_sci_Range$.prototype;
$c_sci_Range$.prototype.init___ = (function() {
  this.MAX$undPRINT$1 = 512;
  return this
});
var $d_sci_Range$ = new $TypeData().initClass({
  sci_Range$: 0
}, false, "scala.collection.immutable.Range$", {
  sci_Range$: 1,
  O: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_sci_Range$.prototype.$classData = $d_sci_Range$;
var $n_sci_Range$ = (void 0);
function $m_sci_Range$() {
  if ((!$n_sci_Range$)) {
    $n_sci_Range$ = new $c_sci_Range$().init___()
  };
  return $n_sci_Range$
}
/** @constructor */
function $c_scm_StringBuilder$() {
  $c_O.call(this)
}
$c_scm_StringBuilder$.prototype = new $h_O();
$c_scm_StringBuilder$.prototype.constructor = $c_scm_StringBuilder$;
/** @constructor */
function $h_scm_StringBuilder$() {
  /*<skip>*/
}
$h_scm_StringBuilder$.prototype = $c_scm_StringBuilder$.prototype;
$c_scm_StringBuilder$.prototype.init___ = (function() {
  return this
});
var $d_scm_StringBuilder$ = new $TypeData().initClass({
  scm_StringBuilder$: 0
}, false, "scala.collection.mutable.StringBuilder$", {
  scm_StringBuilder$: 1,
  O: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_scm_StringBuilder$.prototype.$classData = $d_scm_StringBuilder$;
var $n_scm_StringBuilder$ = (void 0);
function $m_scm_StringBuilder$() {
  if ((!$n_scm_StringBuilder$)) {
    $n_scm_StringBuilder$ = new $c_scm_StringBuilder$().init___()
  };
  return $n_scm_StringBuilder$
}
/** @constructor */
function $c_sjsr_AnonFunction0() {
  $c_sr_AbstractFunction0.call(this);
  this.f$2 = null
}
$c_sjsr_AnonFunction0.prototype = new $h_sr_AbstractFunction0();
$c_sjsr_AnonFunction0.prototype.constructor = $c_sjsr_AnonFunction0;
/** @constructor */
function $h_sjsr_AnonFunction0() {
  /*<skip>*/
}
$h_sjsr_AnonFunction0.prototype = $c_sjsr_AnonFunction0.prototype;
$c_sjsr_AnonFunction0.prototype.apply__O = (function() {
  return (0, this.f$2)()
});
$c_sjsr_AnonFunction0.prototype.init___sjs_js_Function0 = (function(f) {
  this.f$2 = f;
  return this
});
var $d_sjsr_AnonFunction0 = new $TypeData().initClass({
  sjsr_AnonFunction0: 0
}, false, "scala.scalajs.runtime.AnonFunction0", {
  sjsr_AnonFunction0: 1,
  sr_AbstractFunction0: 1,
  O: 1,
  F0: 1
});
$c_sjsr_AnonFunction0.prototype.$classData = $d_sjsr_AnonFunction0;
/** @constructor */
function $c_sjsr_AnonFunction1() {
  $c_sr_AbstractFunction1.call(this);
  this.f$2 = null
}
$c_sjsr_AnonFunction1.prototype = new $h_sr_AbstractFunction1();
$c_sjsr_AnonFunction1.prototype.constructor = $c_sjsr_AnonFunction1;
/** @constructor */
function $h_sjsr_AnonFunction1() {
  /*<skip>*/
}
$h_sjsr_AnonFunction1.prototype = $c_sjsr_AnonFunction1.prototype;
$c_sjsr_AnonFunction1.prototype.apply__O__O = (function(arg1) {
  return (0, this.f$2)(arg1)
});
$c_sjsr_AnonFunction1.prototype.init___sjs_js_Function1 = (function(f) {
  this.f$2 = f;
  return this
});
var $d_sjsr_AnonFunction1 = new $TypeData().initClass({
  sjsr_AnonFunction1: 0
}, false, "scala.scalajs.runtime.AnonFunction1", {
  sjsr_AnonFunction1: 1,
  sr_AbstractFunction1: 1,
  O: 1,
  F1: 1
});
$c_sjsr_AnonFunction1.prototype.$classData = $d_sjsr_AnonFunction1;
/** @constructor */
function $c_sjsr_AnonFunction2() {
  $c_sr_AbstractFunction2.call(this);
  this.f$2 = null
}
$c_sjsr_AnonFunction2.prototype = new $h_sr_AbstractFunction2();
$c_sjsr_AnonFunction2.prototype.constructor = $c_sjsr_AnonFunction2;
/** @constructor */
function $h_sjsr_AnonFunction2() {
  /*<skip>*/
}
$h_sjsr_AnonFunction2.prototype = $c_sjsr_AnonFunction2.prototype;
$c_sjsr_AnonFunction2.prototype.init___sjs_js_Function2 = (function(f) {
  this.f$2 = f;
  return this
});
$c_sjsr_AnonFunction2.prototype.apply__O__O__O = (function(arg1, arg2) {
  return (0, this.f$2)(arg1, arg2)
});
var $d_sjsr_AnonFunction2 = new $TypeData().initClass({
  sjsr_AnonFunction2: 0
}, false, "scala.scalajs.runtime.AnonFunction2", {
  sjsr_AnonFunction2: 1,
  sr_AbstractFunction2: 1,
  O: 1,
  F2: 1
});
$c_sjsr_AnonFunction2.prototype.$classData = $d_sjsr_AnonFunction2;
/** @constructor */
function $c_sjsr_AnonFunction3() {
  $c_sr_AbstractFunction3.call(this);
  this.f$2 = null
}
$c_sjsr_AnonFunction3.prototype = new $h_sr_AbstractFunction3();
$c_sjsr_AnonFunction3.prototype.constructor = $c_sjsr_AnonFunction3;
/** @constructor */
function $h_sjsr_AnonFunction3() {
  /*<skip>*/
}
$h_sjsr_AnonFunction3.prototype = $c_sjsr_AnonFunction3.prototype;
$c_sjsr_AnonFunction3.prototype.init___sjs_js_Function3 = (function(f) {
  this.f$2 = f;
  return this
});
$c_sjsr_AnonFunction3.prototype.apply__O__O__O__O = (function(arg1, arg2, arg3) {
  return (0, this.f$2)(arg1, arg2, arg3)
});
var $d_sjsr_AnonFunction3 = new $TypeData().initClass({
  sjsr_AnonFunction3: 0
}, false, "scala.scalajs.runtime.AnonFunction3", {
  sjsr_AnonFunction3: 1,
  sr_AbstractFunction3: 1,
  O: 1,
  F3: 1
});
$c_sjsr_AnonFunction3.prototype.$classData = $d_sjsr_AnonFunction3;
/** @constructor */
function $c_sjsr_RuntimeLong$() {
  $c_O.call(this);
  this.TwoPow32$1 = 0.0;
  this.TwoPow63$1 = 0.0;
  this.UnsignedSafeDoubleHiMask$1 = 0;
  this.AskQuotient$1 = 0;
  this.AskRemainder$1 = 0;
  this.AskBoth$1 = 0;
  this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f = 0;
  this.Zero$1 = null
}
$c_sjsr_RuntimeLong$.prototype = new $h_O();
$c_sjsr_RuntimeLong$.prototype.constructor = $c_sjsr_RuntimeLong$;
/** @constructor */
function $h_sjsr_RuntimeLong$() {
  /*<skip>*/
}
$h_sjsr_RuntimeLong$.prototype = $c_sjsr_RuntimeLong$.prototype;
$c_sjsr_RuntimeLong$.prototype.init___ = (function() {
  $n_sjsr_RuntimeLong$ = this;
  this.Zero$1 = new $c_sjsr_RuntimeLong().init___I__I(0, 0);
  return this
});
$c_sjsr_RuntimeLong$.prototype.Zero__sjsr_RuntimeLong = (function() {
  return this.Zero$1
});
$c_sjsr_RuntimeLong$.prototype.toUnsignedString__p1__I__I__T = (function(lo, hi) {
  if ((((-2097152) & hi) === 0)) {
    var this$5 = ((4.294967296E9 * hi) + $uD((lo >>> 0)));
    return ("" + this$5)
  } else {
    var quotRem = this.unsignedDivModHelper__p1__I__I__I__I__I__sjs_js_$bar(lo, hi, 1000000000, 0, 2);
    var quotLo = $uI(quotRem["0"]);
    var quotHi = $uI(quotRem["1"]);
    var rem = $uI(quotRem["2"]);
    var quot = ((4.294967296E9 * quotHi) + $uD((quotLo >>> 0)));
    var remStr = ("" + rem);
    return ((("" + quot) + $as_T("000000000".substring($uI(remStr.length)))) + remStr)
  }
});
$c_sjsr_RuntimeLong$.prototype.divideImpl__I__I__I__I__I = (function(alo, ahi, blo, bhi) {
  if (((blo | bhi) === 0)) {
    throw new $c_jl_ArithmeticException().init___T("/ by zero")
  };
  if ((ahi === (alo >> 31))) {
    if ((bhi === (blo >> 31))) {
      if (((alo === (-2147483648)) && (blo === (-1)))) {
        this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f = 0;
        return (-2147483648)
      } else {
        var lo = ((alo / blo) | 0);
        this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f = (lo >> 31);
        return lo
      }
    } else if (((alo === (-2147483648)) && ((blo === (-2147483648)) && (bhi === 0)))) {
      this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f = (-1);
      return (-1)
    } else {
      this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f = 0;
      return 0
    }
  } else {
    var neg = (ahi < 0);
    if (neg) {
      var lo$1 = ((-alo) | 0);
      var hi = ((alo !== 0) ? (~ahi) : ((-ahi) | 0));
      var abs_$_lo$2 = lo$1;
      var abs_$_hi$2 = hi
    } else {
      var abs_$_lo$2 = alo;
      var abs_$_hi$2 = ahi
    };
    var neg$1 = (bhi < 0);
    if (neg$1) {
      var lo$2 = ((-blo) | 0);
      var hi$1 = ((blo !== 0) ? (~bhi) : ((-bhi) | 0));
      var abs$1_$_lo$2 = lo$2;
      var abs$1_$_hi$2 = hi$1
    } else {
      var abs$1_$_lo$2 = blo;
      var abs$1_$_hi$2 = bhi
    };
    var absRLo = this.unsigned$und$div__p1__I__I__I__I__I(abs_$_lo$2, abs_$_hi$2, abs$1_$_lo$2, abs$1_$_hi$2);
    if ((neg === neg$1)) {
      return absRLo
    } else {
      var hi$2 = this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f;
      this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f = ((absRLo !== 0) ? (~hi$2) : ((-hi$2) | 0));
      return ((-absRLo) | 0)
    }
  }
});
$c_sjsr_RuntimeLong$.prototype.scala$scalajs$runtime$RuntimeLong$$toDouble__I__I__D = (function(lo, hi) {
  if ((hi < 0)) {
    var x = ((lo !== 0) ? (~hi) : ((-hi) | 0));
    var jsx$1 = $uD((x >>> 0));
    var x$1 = ((-lo) | 0);
    return (-((4.294967296E9 * jsx$1) + $uD((x$1 >>> 0))))
  } else {
    return ((4.294967296E9 * hi) + $uD((lo >>> 0)))
  }
});
$c_sjsr_RuntimeLong$.prototype.fromDouble__D__sjsr_RuntimeLong = (function(value) {
  var lo = this.scala$scalajs$runtime$RuntimeLong$$fromDoubleImpl__D__I(value);
  return new $c_sjsr_RuntimeLong().init___I__I(lo, this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f)
});
$c_sjsr_RuntimeLong$.prototype.scala$scalajs$runtime$RuntimeLong$$fromDoubleImpl__D__I = (function(value) {
  if ((value < (-9.223372036854776E18))) {
    this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f = (-2147483648);
    return 0
  } else if ((value >= 9.223372036854776E18)) {
    this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f = 2147483647;
    return (-1)
  } else {
    var rawLo = $uI((value | 0));
    var x = (value / 4.294967296E9);
    var rawHi = $uI((x | 0));
    this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f = (((value < 0) && (rawLo !== 0)) ? (((-1) + rawHi) | 0) : rawHi);
    return rawLo
  }
});
$c_sjsr_RuntimeLong$.prototype.unsigned$und$div__p1__I__I__I__I__I = (function(alo, ahi, blo, bhi) {
  if ((((-2097152) & ahi) === 0)) {
    if ((((-2097152) & bhi) === 0)) {
      var aDouble = ((4.294967296E9 * ahi) + $uD((alo >>> 0)));
      var bDouble = ((4.294967296E9 * bhi) + $uD((blo >>> 0)));
      var rDouble = (aDouble / bDouble);
      var x = (rDouble / 4.294967296E9);
      this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f = $uI((x | 0));
      return $uI((rDouble | 0))
    } else {
      this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f = 0;
      return 0
    }
  } else if (((bhi === 0) && ((blo & (((-1) + blo) | 0)) === 0))) {
    var pow = ((31 - $clz32(blo)) | 0);
    this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f = ((ahi >>> pow) | 0);
    return (((alo >>> pow) | 0) | ((ahi << 1) << ((31 - pow) | 0)))
  } else if (((blo === 0) && ((bhi & (((-1) + bhi) | 0)) === 0))) {
    var pow$2 = ((31 - $clz32(bhi)) | 0);
    this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f = 0;
    return ((ahi >>> pow$2) | 0)
  } else {
    return $uI(this.unsignedDivModHelper__p1__I__I__I__I__I__sjs_js_$bar(alo, ahi, blo, bhi, 0))
  }
});
$c_sjsr_RuntimeLong$.prototype.scala$scalajs$runtime$RuntimeLong$$toString__I__I__T = (function(lo, hi) {
  return ((hi === (lo >> 31)) ? ("" + lo) : ((hi < 0) ? ("-" + this.toUnsignedString__p1__I__I__T(((-lo) | 0), ((lo !== 0) ? (~hi) : ((-hi) | 0)))) : this.toUnsignedString__p1__I__I__T(lo, hi)))
});
$c_sjsr_RuntimeLong$.prototype.scala$scalajs$runtime$RuntimeLong$$compare__I__I__I__I__I = (function(alo, ahi, blo, bhi) {
  return ((ahi === bhi) ? ((alo === blo) ? 0 : ((((-2147483648) ^ alo) < ((-2147483648) ^ blo)) ? (-1) : 1)) : ((ahi < bhi) ? (-1) : 1))
});
$c_sjsr_RuntimeLong$.prototype.unsignedDivModHelper__p1__I__I__I__I__I__sjs_js_$bar = (function(alo, ahi, blo, bhi, ask) {
  var shift = ((((bhi !== 0) ? $clz32(bhi) : ((32 + $clz32(blo)) | 0)) - ((ahi !== 0) ? $clz32(ahi) : ((32 + $clz32(alo)) | 0))) | 0);
  var n = shift;
  var lo = (((32 & n) === 0) ? (blo << n) : 0);
  var hi = (((32 & n) === 0) ? (((((blo >>> 1) | 0) >>> ((31 - n) | 0)) | 0) | (bhi << n)) : (blo << n));
  var bShiftLo = lo;
  var bShiftHi = hi;
  var remLo = alo;
  var remHi = ahi;
  var quotLo = 0;
  var quotHi = 0;
  while (((shift >= 0) && (((-2097152) & remHi) !== 0))) {
    var alo$1 = remLo;
    var ahi$1 = remHi;
    var blo$1 = bShiftLo;
    var bhi$1 = bShiftHi;
    if (((ahi$1 === bhi$1) ? (((-2147483648) ^ alo$1) >= ((-2147483648) ^ blo$1)) : (((-2147483648) ^ ahi$1) >= ((-2147483648) ^ bhi$1)))) {
      var lo$1 = remLo;
      var hi$1 = remHi;
      var lo$2 = bShiftLo;
      var hi$2 = bShiftHi;
      var lo$3 = ((lo$1 - lo$2) | 0);
      var hi$3 = ((((-2147483648) ^ lo$3) > ((-2147483648) ^ lo$1)) ? (((-1) + ((hi$1 - hi$2) | 0)) | 0) : ((hi$1 - hi$2) | 0));
      remLo = lo$3;
      remHi = hi$3;
      if ((shift < 32)) {
        quotLo = (quotLo | (1 << shift))
      } else {
        quotHi = (quotHi | (1 << shift))
      }
    };
    shift = (((-1) + shift) | 0);
    var lo$4 = bShiftLo;
    var hi$4 = bShiftHi;
    var lo$5 = (((lo$4 >>> 1) | 0) | (hi$4 << 31));
    var hi$5 = ((hi$4 >>> 1) | 0);
    bShiftLo = lo$5;
    bShiftHi = hi$5
  };
  var alo$2 = remLo;
  var ahi$2 = remHi;
  if (((ahi$2 === bhi) ? (((-2147483648) ^ alo$2) >= ((-2147483648) ^ blo)) : (((-2147483648) ^ ahi$2) >= ((-2147483648) ^ bhi)))) {
    var lo$6 = remLo;
    var hi$6 = remHi;
    var remDouble = ((4.294967296E9 * hi$6) + $uD((lo$6 >>> 0)));
    var bDouble = ((4.294967296E9 * bhi) + $uD((blo >>> 0)));
    if ((ask !== 1)) {
      var x = (remDouble / bDouble);
      var lo$7 = $uI((x | 0));
      var x$1 = (x / 4.294967296E9);
      var hi$7 = $uI((x$1 | 0));
      var lo$8 = quotLo;
      var hi$8 = quotHi;
      var lo$9 = ((lo$8 + lo$7) | 0);
      var hi$9 = ((((-2147483648) ^ lo$9) < ((-2147483648) ^ lo$8)) ? ((1 + ((hi$8 + hi$7) | 0)) | 0) : ((hi$8 + hi$7) | 0));
      quotLo = lo$9;
      quotHi = hi$9
    };
    if ((ask !== 0)) {
      var rem_mod_bDouble = (remDouble % bDouble);
      remLo = $uI((rem_mod_bDouble | 0));
      var x$2 = (rem_mod_bDouble / 4.294967296E9);
      remHi = $uI((x$2 | 0))
    }
  };
  if ((ask === 0)) {
    this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f = quotHi;
    var a = quotLo;
    return a
  } else if ((ask === 1)) {
    this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f = remHi;
    var a$1 = remLo;
    return a$1
  } else {
    var _1 = quotLo;
    var _2 = quotHi;
    var _3 = remLo;
    var _4 = remHi;
    var a$2 = [_1, _2, _3, _4];
    return a$2
  }
});
$c_sjsr_RuntimeLong$.prototype.remainderImpl__I__I__I__I__I = (function(alo, ahi, blo, bhi) {
  if (((blo | bhi) === 0)) {
    throw new $c_jl_ArithmeticException().init___T("/ by zero")
  };
  if ((ahi === (alo >> 31))) {
    if ((bhi === (blo >> 31))) {
      if ((blo !== (-1))) {
        var lo = ((alo % blo) | 0);
        this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f = (lo >> 31);
        return lo
      } else {
        this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f = 0;
        return 0
      }
    } else if (((alo === (-2147483648)) && ((blo === (-2147483648)) && (bhi === 0)))) {
      this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f = 0;
      return 0
    } else {
      this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f = ahi;
      return alo
    }
  } else {
    var neg = (ahi < 0);
    if (neg) {
      var lo$1 = ((-alo) | 0);
      var hi = ((alo !== 0) ? (~ahi) : ((-ahi) | 0));
      var abs_$_lo$2 = lo$1;
      var abs_$_hi$2 = hi
    } else {
      var abs_$_lo$2 = alo;
      var abs_$_hi$2 = ahi
    };
    var neg$1 = (bhi < 0);
    if (neg$1) {
      var lo$2 = ((-blo) | 0);
      var hi$1 = ((blo !== 0) ? (~bhi) : ((-bhi) | 0));
      var abs$1_$_lo$2 = lo$2;
      var abs$1_$_hi$2 = hi$1
    } else {
      var abs$1_$_lo$2 = blo;
      var abs$1_$_hi$2 = bhi
    };
    var absRLo = this.unsigned$und$percent__p1__I__I__I__I__I(abs_$_lo$2, abs_$_hi$2, abs$1_$_lo$2, abs$1_$_hi$2);
    if (neg) {
      var hi$2 = this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f;
      this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f = ((absRLo !== 0) ? (~hi$2) : ((-hi$2) | 0));
      return ((-absRLo) | 0)
    } else {
      return absRLo
    }
  }
});
$c_sjsr_RuntimeLong$.prototype.unsigned$und$percent__p1__I__I__I__I__I = (function(alo, ahi, blo, bhi) {
  if ((((-2097152) & ahi) === 0)) {
    if ((((-2097152) & bhi) === 0)) {
      var aDouble = ((4.294967296E9 * ahi) + $uD((alo >>> 0)));
      var bDouble = ((4.294967296E9 * bhi) + $uD((blo >>> 0)));
      var rDouble = (aDouble % bDouble);
      var x = (rDouble / 4.294967296E9);
      this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f = $uI((x | 0));
      return $uI((rDouble | 0))
    } else {
      this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f = ahi;
      return alo
    }
  } else if (((bhi === 0) && ((blo & (((-1) + blo) | 0)) === 0))) {
    this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f = 0;
    return (alo & (((-1) + blo) | 0))
  } else if (((blo === 0) && ((bhi & (((-1) + bhi) | 0)) === 0))) {
    this.scala$scalajs$runtime$RuntimeLong$$hiReturn$f = (ahi & (((-1) + bhi) | 0));
    return alo
  } else {
    return $uI(this.unsignedDivModHelper__p1__I__I__I__I__I__sjs_js_$bar(alo, ahi, blo, bhi, 1))
  }
});
$c_sjsr_RuntimeLong$.prototype.scala$scalajs$runtime$RuntimeLong$$timesHi__I__I__I__I__I = (function(alo, ahi, blo, bhi) {
  var a0 = (65535 & alo);
  var a1 = ((alo >>> 16) | 0);
  var a2 = (65535 & ahi);
  var a3 = ((ahi >>> 16) | 0);
  var b0 = (65535 & blo);
  var b1 = ((blo >>> 16) | 0);
  var b2 = (65535 & bhi);
  var b3 = ((bhi >>> 16) | 0);
  var c1part = (((($imul(a0, b0) >>> 16) | 0) + $imul(a1, b0)) | 0);
  var c2 = ((((c1part >>> 16) | 0) + (((((65535 & c1part) + $imul(a0, b1)) | 0) >>> 16) | 0)) | 0);
  var c3 = ((c2 >>> 16) | 0);
  c2 = (((65535 & c2) + $imul(a2, b0)) | 0);
  c3 = ((c3 + ((c2 >>> 16) | 0)) | 0);
  c2 = (((65535 & c2) + $imul(a1, b1)) | 0);
  c3 = ((c3 + ((c2 >>> 16) | 0)) | 0);
  c2 = (((65535 & c2) + $imul(a0, b2)) | 0);
  c3 = ((c3 + ((c2 >>> 16) | 0)) | 0);
  c3 = ((((((((c3 + $imul(a3, b0)) | 0) + $imul(a2, b1)) | 0) + $imul(a1, b2)) | 0) + $imul(a0, b3)) | 0);
  return ((65535 & c2) | (c3 << 16))
});
var $d_sjsr_RuntimeLong$ = new $TypeData().initClass({
  sjsr_RuntimeLong$: 0
}, false, "scala.scalajs.runtime.RuntimeLong$", {
  sjsr_RuntimeLong$: 1,
  O: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_sjsr_RuntimeLong$.prototype.$classData = $d_sjsr_RuntimeLong$;
var $n_sjsr_RuntimeLong$ = (void 0);
function $m_sjsr_RuntimeLong$() {
  if ((!$n_sjsr_RuntimeLong$)) {
    $n_sjsr_RuntimeLong$ = new $c_sjsr_RuntimeLong$().init___()
  };
  return $n_sjsr_RuntimeLong$
}
var $d_sr_Nothing$ = new $TypeData().initClass({
  sr_Nothing$: 0
}, false, "scala.runtime.Nothing$", {
  sr_Nothing$: 1,
  jl_Throwable: 1,
  O: 1,
  Ljava_io_Serializable: 1
});
/** @constructor */
function $c_Ljapgolly_scalajs_react_ReactComponentC$ConstProps() {
  $c_Ljapgolly_scalajs_react_ReactComponentC$BaseCtor.call(this);
  this.factory$2 = null;
  this.reactClass$2 = null;
  this.key$2 = null;
  this.ref$2 = null;
  this.props$2 = null
}
$c_Ljapgolly_scalajs_react_ReactComponentC$ConstProps.prototype = new $h_Ljapgolly_scalajs_react_ReactComponentC$BaseCtor();
$c_Ljapgolly_scalajs_react_ReactComponentC$ConstProps.prototype.constructor = $c_Ljapgolly_scalajs_react_ReactComponentC$ConstProps;
/** @constructor */
function $h_Ljapgolly_scalajs_react_ReactComponentC$ConstProps() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_ReactComponentC$ConstProps.prototype = $c_Ljapgolly_scalajs_react_ReactComponentC$ConstProps.prototype;
$c_Ljapgolly_scalajs_react_ReactComponentC$ConstProps.prototype.apply__sc_Seq__Ljapgolly_scalajs_react_ReactComponentU = (function(children) {
  var jsx$4 = this.factory$2;
  var jsx$3 = this.mkProps__O__Ljapgolly_scalajs_react_package$WrapObj(this.props$2.apply__O());
  var this$1 = $m_sjsr_package$();
  if ($is_sjs_js_ArrayOps(children)) {
    var x2 = $as_sjs_js_ArrayOps(children);
    var jsx$2 = x2.scala$scalajs$js$ArrayOps$$array$f
  } else if ($is_sjs_js_WrappedArray(children)) {
    var x3 = $as_sjs_js_WrappedArray(children);
    var jsx$2 = x3.array$6
  } else {
    var result = [];
    children.foreach__F1__V(new $c_sjsr_AnonFunction1().init___sjs_js_Function1((function($this, result$1) {
      return (function(x$2) {
        return $uI(result$1.push(x$2))
      })
    })(this$1, result)));
    var jsx$2 = result
  };
  var jsx$1 = [jsx$3].concat(jsx$2);
  return jsx$4.apply((void 0), jsx$1)
});
$c_Ljapgolly_scalajs_react_ReactComponentC$ConstProps.prototype.init___Ljapgolly_scalajs_react_ReactComponentCU__Ljapgolly_scalajs_react_ReactClass__sjs_js_UndefOr__sjs_js_UndefOr__F0 = (function(factory, reactClass, key, ref, props) {
  this.factory$2 = factory;
  this.reactClass$2 = reactClass;
  this.key$2 = key;
  this.ref$2 = ref;
  this.props$2 = props;
  return this
});
$c_Ljapgolly_scalajs_react_ReactComponentC$ConstProps.prototype.ref__sjs_js_UndefOr = (function() {
  return this.ref$2
});
$c_Ljapgolly_scalajs_react_ReactComponentC$ConstProps.prototype.key__sjs_js_UndefOr = (function() {
  return this.key$2
});
function $is_Ljapgolly_scalajs_react_ReactComponentC$ConstProps(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.Ljapgolly_scalajs_react_ReactComponentC$ConstProps)))
}
function $as_Ljapgolly_scalajs_react_ReactComponentC$ConstProps(obj) {
  return (($is_Ljapgolly_scalajs_react_ReactComponentC$ConstProps(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "japgolly.scalajs.react.ReactComponentC$ConstProps"))
}
function $isArrayOf_Ljapgolly_scalajs_react_ReactComponentC$ConstProps(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.Ljapgolly_scalajs_react_ReactComponentC$ConstProps)))
}
function $asArrayOf_Ljapgolly_scalajs_react_ReactComponentC$ConstProps(obj, depth) {
  return (($isArrayOf_Ljapgolly_scalajs_react_ReactComponentC$ConstProps(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Ljapgolly.scalajs.react.ReactComponentC$ConstProps;", depth))
}
var $d_Ljapgolly_scalajs_react_ReactComponentC$ConstProps = new $TypeData().initClass({
  Ljapgolly_scalajs_react_ReactComponentC$ConstProps: 0
}, false, "japgolly.scalajs.react.ReactComponentC$ConstProps", {
  Ljapgolly_scalajs_react_ReactComponentC$ConstProps: 1,
  Ljapgolly_scalajs_react_ReactComponentC$BaseCtor: 1,
  O: 1,
  Ljapgolly_scalajs_react_ReactComponentC: 1,
  Ljapgolly_scalajs_react_package$ReactComponentTypeAux: 1
});
$c_Ljapgolly_scalajs_react_ReactComponentC$ConstProps.prototype.$classData = $d_Ljapgolly_scalajs_react_ReactComponentC$ConstProps;
/** @constructor */
function $c_Ljapgolly_scalajs_react_ReactComponentC$ReqProps() {
  $c_Ljapgolly_scalajs_react_ReactComponentC$BaseCtor.call(this);
  this.factory$2 = null;
  this.reactClass$2 = null;
  this.key$2 = null;
  this.ref$2 = null
}
$c_Ljapgolly_scalajs_react_ReactComponentC$ReqProps.prototype = new $h_Ljapgolly_scalajs_react_ReactComponentC$BaseCtor();
$c_Ljapgolly_scalajs_react_ReactComponentC$ReqProps.prototype.constructor = $c_Ljapgolly_scalajs_react_ReactComponentC$ReqProps;
/** @constructor */
function $h_Ljapgolly_scalajs_react_ReactComponentC$ReqProps() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_ReactComponentC$ReqProps.prototype = $c_Ljapgolly_scalajs_react_ReactComponentC$ReqProps.prototype;
$c_Ljapgolly_scalajs_react_ReactComponentC$ReqProps.prototype.ref__sjs_js_UndefOr = (function() {
  return this.ref$2
});
$c_Ljapgolly_scalajs_react_ReactComponentC$ReqProps.prototype.key__sjs_js_UndefOr = (function() {
  return this.key$2
});
$c_Ljapgolly_scalajs_react_ReactComponentC$ReqProps.prototype.init___Ljapgolly_scalajs_react_ReactComponentCU__Ljapgolly_scalajs_react_ReactClass__sjs_js_UndefOr__sjs_js_UndefOr = (function(factory, reactClass, key, ref) {
  this.factory$2 = factory;
  this.reactClass$2 = reactClass;
  this.key$2 = key;
  this.ref$2 = ref;
  return this
});
function $is_Ljapgolly_scalajs_react_ReactComponentC$ReqProps(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.Ljapgolly_scalajs_react_ReactComponentC$ReqProps)))
}
function $as_Ljapgolly_scalajs_react_ReactComponentC$ReqProps(obj) {
  return (($is_Ljapgolly_scalajs_react_ReactComponentC$ReqProps(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "japgolly.scalajs.react.ReactComponentC$ReqProps"))
}
function $isArrayOf_Ljapgolly_scalajs_react_ReactComponentC$ReqProps(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.Ljapgolly_scalajs_react_ReactComponentC$ReqProps)))
}
function $asArrayOf_Ljapgolly_scalajs_react_ReactComponentC$ReqProps(obj, depth) {
  return (($isArrayOf_Ljapgolly_scalajs_react_ReactComponentC$ReqProps(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Ljapgolly.scalajs.react.ReactComponentC$ReqProps;", depth))
}
var $d_Ljapgolly_scalajs_react_ReactComponentC$ReqProps = new $TypeData().initClass({
  Ljapgolly_scalajs_react_ReactComponentC$ReqProps: 0
}, false, "japgolly.scalajs.react.ReactComponentC$ReqProps", {
  Ljapgolly_scalajs_react_ReactComponentC$ReqProps: 1,
  Ljapgolly_scalajs_react_ReactComponentC$BaseCtor: 1,
  O: 1,
  Ljapgolly_scalajs_react_ReactComponentC: 1,
  Ljapgolly_scalajs_react_package$ReactComponentTypeAux: 1
});
$c_Ljapgolly_scalajs_react_ReactComponentC$ReqProps.prototype.$classData = $d_Ljapgolly_scalajs_react_ReactComponentC$ReqProps;
/** @constructor */
function $c_Ljapgolly_scalajs_react_vdom_ReactTagOf() {
  $c_O.call(this);
  this.tag$1 = null;
  this.modifiers$1 = null;
  this.namespace$1 = null
}
$c_Ljapgolly_scalajs_react_vdom_ReactTagOf.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_vdom_ReactTagOf.prototype.constructor = $c_Ljapgolly_scalajs_react_vdom_ReactTagOf;
/** @constructor */
function $h_Ljapgolly_scalajs_react_vdom_ReactTagOf() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_vdom_ReactTagOf.prototype = $c_Ljapgolly_scalajs_react_vdom_ReactTagOf.prototype;
$c_Ljapgolly_scalajs_react_vdom_ReactTagOf.prototype.apply__sc_Seq__Ljapgolly_scalajs_react_vdom_ReactTagOf = (function(xs) {
  var this$1 = this.modifiers$1;
  var x$3 = new $c_sci_$colon$colon().init___O__sci_List(xs, this$1);
  var x$4 = this.tag$1;
  var x$5 = this.namespace$1;
  return new $c_Ljapgolly_scalajs_react_vdom_ReactTagOf().init___T__sci_List__T(x$4, x$3, x$5)
});
$c_Ljapgolly_scalajs_react_vdom_ReactTagOf.prototype.toString__T = (function() {
  return $objectToString(this.render__Ljapgolly_scalajs_react_ReactElement())
});
$c_Ljapgolly_scalajs_react_vdom_ReactTagOf.prototype.init___T__sci_List__T = (function(tag, modifiers, namespace) {
  this.tag$1 = tag;
  this.modifiers$1 = modifiers;
  this.namespace$1 = namespace;
  return this
});
$c_Ljapgolly_scalajs_react_vdom_ReactTagOf.prototype.render__Ljapgolly_scalajs_react_ReactElement = (function() {
  var b = new $c_Ljapgolly_scalajs_react_vdom_Builder().init___();
  this.build__p1__Ljapgolly_scalajs_react_vdom_Builder__V(b);
  return b.render__T__Ljapgolly_scalajs_react_ReactElement(this.tag$1)
});
$c_Ljapgolly_scalajs_react_vdom_ReactTagOf.prototype.build__p1__Ljapgolly_scalajs_react_vdom_Builder__V = (function(b) {
  var current = this.modifiers$1;
  var this$1 = this.modifiers$1;
  var arr = $newArrayObject($d_sc_Seq.getArrayOf(), [$s_sc_LinearSeqOptimized$class__length__sc_LinearSeqOptimized__I(this$1)]);
  var i = 0;
  while (true) {
    var x = current;
    var x$2 = $m_sci_Nil$();
    if ((!((x !== null) && x.equals__O__Z(x$2)))) {
      arr.u[i] = $as_sc_Seq(current.head__O());
      var this$2 = current;
      current = this$2.tail__sci_List();
      i = ((1 + i) | 0)
    } else {
      break
    }
  };
  var j = arr.u.length;
  while ((j > 0)) {
    j = (((-1) + j) | 0);
    var frag = arr.u[j];
    var i$2 = 0;
    while ((i$2 < frag.length__I())) {
      $as_Ljapgolly_scalajs_react_vdom_TagMod(frag.apply__I__O(i$2)).applyTo__Ljapgolly_scalajs_react_vdom_Builder__V(b);
      i$2 = ((1 + i$2) | 0)
    }
  }
});
$c_Ljapgolly_scalajs_react_vdom_ReactTagOf.prototype.applyTo__Ljapgolly_scalajs_react_vdom_Builder__V = (function(b) {
  b.appendChild__Ljapgolly_scalajs_react_ReactNode__V(this.render__Ljapgolly_scalajs_react_ReactElement())
});
var $d_Ljapgolly_scalajs_react_vdom_ReactTagOf = new $TypeData().initClass({
  Ljapgolly_scalajs_react_vdom_ReactTagOf: 0
}, false, "japgolly.scalajs.react.vdom.ReactTagOf", {
  Ljapgolly_scalajs_react_vdom_ReactTagOf: 1,
  O: 1,
  Ljapgolly_scalajs_react_vdom_DomFrag: 1,
  Ljapgolly_scalajs_react_vdom_Frag: 1,
  Ljapgolly_scalajs_react_vdom_TagMod: 1
});
$c_Ljapgolly_scalajs_react_vdom_ReactTagOf.prototype.$classData = $d_Ljapgolly_scalajs_react_vdom_ReactTagOf;
/** @constructor */
function $c_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$() {
  $c_Ljapgolly_scalajs_react_vdom_package$Base.call(this)
}
$c_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$.prototype = new $h_Ljapgolly_scalajs_react_vdom_package$Base();
$c_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$.prototype.constructor = $c_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$;
/** @constructor */
function $h_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$.prototype = $c_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$.prototype;
$c_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$.prototype.init___ = (function() {
  $c_Ljapgolly_scalajs_react_vdom_Implicits.prototype.init___.call(this);
  return this
});
var $d_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$ = new $TypeData().initClass({
  Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$: 0
}, false, "japgolly.scalajs.react.vdom.package$prefix_$less$up$", {
  Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$: 1,
  Ljapgolly_scalajs_react_vdom_package$Base: 1,
  Ljapgolly_scalajs_react_vdom_Implicits: 1,
  Ljapgolly_scalajs_react_vdom_LowPri: 1,
  O: 1
});
$c_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$.prototype.$classData = $d_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$;
var $n_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$ = (void 0);
function $m_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$() {
  if ((!$n_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$)) {
    $n_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$ = new $c_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$().init___()
  };
  return $n_Ljapgolly_scalajs_react_vdom_package$prefix$und$less$up$
}
function $is_T(obj) {
  return ((typeof obj) === "string")
}
function $as_T(obj) {
  return (($is_T(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "java.lang.String"))
}
function $isArrayOf_T(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.T)))
}
function $asArrayOf_T(obj, depth) {
  return (($isArrayOf_T(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Ljava.lang.String;", depth))
}
var $d_T = new $TypeData().initClass({
  T: 0
}, false, "java.lang.String", {
  T: 1,
  O: 1,
  Ljava_io_Serializable: 1,
  jl_CharSequence: 1,
  jl_Comparable: 1
}, (void 0), (void 0), $is_T);
var $d_jl_Byte = new $TypeData().initClass({
  jl_Byte: 0
}, false, "java.lang.Byte", {
  jl_Byte: 1,
  jl_Number: 1,
  O: 1,
  Ljava_io_Serializable: 1,
  jl_Comparable: 1
}, (void 0), (void 0), (function(x) {
  return $isByte(x)
}));
/** @constructor */
function $c_jl_CloneNotSupportedException() {
  $c_jl_Exception.call(this)
}
$c_jl_CloneNotSupportedException.prototype = new $h_jl_Exception();
$c_jl_CloneNotSupportedException.prototype.constructor = $c_jl_CloneNotSupportedException;
/** @constructor */
function $h_jl_CloneNotSupportedException() {
  /*<skip>*/
}
$h_jl_CloneNotSupportedException.prototype = $c_jl_CloneNotSupportedException.prototype;
$c_jl_CloneNotSupportedException.prototype.init___ = (function() {
  $c_jl_Throwable.prototype.init___T__jl_Throwable.call(this, null, null);
  return this
});
var $d_jl_CloneNotSupportedException = new $TypeData().initClass({
  jl_CloneNotSupportedException: 0
}, false, "java.lang.CloneNotSupportedException", {
  jl_CloneNotSupportedException: 1,
  jl_Exception: 1,
  jl_Throwable: 1,
  O: 1,
  Ljava_io_Serializable: 1
});
$c_jl_CloneNotSupportedException.prototype.$classData = $d_jl_CloneNotSupportedException;
function $isArrayOf_jl_Double(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.jl_Double)))
}
function $asArrayOf_jl_Double(obj, depth) {
  return (($isArrayOf_jl_Double(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Ljava.lang.Double;", depth))
}
var $d_jl_Double = new $TypeData().initClass({
  jl_Double: 0
}, false, "java.lang.Double", {
  jl_Double: 1,
  jl_Number: 1,
  O: 1,
  Ljava_io_Serializable: 1,
  jl_Comparable: 1
}, (void 0), (void 0), (function(x) {
  return ((typeof x) === "number")
}));
var $d_jl_Float = new $TypeData().initClass({
  jl_Float: 0
}, false, "java.lang.Float", {
  jl_Float: 1,
  jl_Number: 1,
  O: 1,
  Ljava_io_Serializable: 1,
  jl_Comparable: 1
}, (void 0), (void 0), (function(x) {
  return $isFloat(x)
}));
var $d_jl_Integer = new $TypeData().initClass({
  jl_Integer: 0
}, false, "java.lang.Integer", {
  jl_Integer: 1,
  jl_Number: 1,
  O: 1,
  Ljava_io_Serializable: 1,
  jl_Comparable: 1
}, (void 0), (void 0), (function(x) {
  return $isInt(x)
}));
function $isArrayOf_jl_Long(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.jl_Long)))
}
function $asArrayOf_jl_Long(obj, depth) {
  return (($isArrayOf_jl_Long(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Ljava.lang.Long;", depth))
}
var $d_jl_Long = new $TypeData().initClass({
  jl_Long: 0
}, false, "java.lang.Long", {
  jl_Long: 1,
  jl_Number: 1,
  O: 1,
  Ljava_io_Serializable: 1,
  jl_Comparable: 1
}, (void 0), (void 0), (function(x) {
  return $is_sjsr_RuntimeLong(x)
}));
/** @constructor */
function $c_jl_RuntimeException() {
  $c_jl_Exception.call(this)
}
$c_jl_RuntimeException.prototype = new $h_jl_Exception();
$c_jl_RuntimeException.prototype.constructor = $c_jl_RuntimeException;
/** @constructor */
function $h_jl_RuntimeException() {
  /*<skip>*/
}
$h_jl_RuntimeException.prototype = $c_jl_RuntimeException.prototype;
$c_jl_RuntimeException.prototype.init___T = (function(s) {
  $c_jl_Throwable.prototype.init___T__jl_Throwable.call(this, s, null);
  return this
});
var $d_jl_RuntimeException = new $TypeData().initClass({
  jl_RuntimeException: 0
}, false, "java.lang.RuntimeException", {
  jl_RuntimeException: 1,
  jl_Exception: 1,
  jl_Throwable: 1,
  O: 1,
  Ljava_io_Serializable: 1
});
$c_jl_RuntimeException.prototype.$classData = $d_jl_RuntimeException;
var $d_jl_Short = new $TypeData().initClass({
  jl_Short: 0
}, false, "java.lang.Short", {
  jl_Short: 1,
  jl_Number: 1,
  O: 1,
  Ljava_io_Serializable: 1,
  jl_Comparable: 1
}, (void 0), (void 0), (function(x) {
  return $isShort(x)
}));
/** @constructor */
function $c_jl_StringBuilder() {
  $c_O.call(this);
  this.content$1 = null
}
$c_jl_StringBuilder.prototype = new $h_O();
$c_jl_StringBuilder.prototype.constructor = $c_jl_StringBuilder;
/** @constructor */
function $h_jl_StringBuilder() {
  /*<skip>*/
}
$h_jl_StringBuilder.prototype = $c_jl_StringBuilder.prototype;
$c_jl_StringBuilder.prototype.init___ = (function() {
  $c_jl_StringBuilder.prototype.init___T.call(this, "");
  return this
});
$c_jl_StringBuilder.prototype.append__T__jl_StringBuilder = (function(s) {
  this.content$1 = (("" + this.content$1) + ((s === null) ? "null" : s));
  return this
});
$c_jl_StringBuilder.prototype.subSequence__I__I__jl_CharSequence = (function(start, end) {
  var thiz = this.content$1;
  return $as_T(thiz.substring(start, end))
});
$c_jl_StringBuilder.prototype.toString__T = (function() {
  return this.content$1
});
$c_jl_StringBuilder.prototype.append__O__jl_StringBuilder = (function(obj) {
  return ((obj === null) ? this.append__T__jl_StringBuilder(null) : this.append__T__jl_StringBuilder($objectToString(obj)))
});
$c_jl_StringBuilder.prototype.init___I = (function(initialCapacity) {
  $c_jl_StringBuilder.prototype.init___T.call(this, "");
  return this
});
$c_jl_StringBuilder.prototype.append__jl_CharSequence__I__I__jl_StringBuilder = (function(csq, start, end) {
  return ((csq === null) ? this.append__jl_CharSequence__I__I__jl_StringBuilder("null", start, end) : this.append__T__jl_StringBuilder($objectToString($charSequenceSubSequence(csq, start, end))))
});
$c_jl_StringBuilder.prototype.append__C__jl_StringBuilder = (function(c) {
  return this.append__T__jl_StringBuilder($as_T($g.String.fromCharCode(c)))
});
$c_jl_StringBuilder.prototype.init___T = (function(content) {
  this.content$1 = content;
  return this
});
var $d_jl_StringBuilder = new $TypeData().initClass({
  jl_StringBuilder: 0
}, false, "java.lang.StringBuilder", {
  jl_StringBuilder: 1,
  O: 1,
  jl_CharSequence: 1,
  jl_Appendable: 1,
  Ljava_io_Serializable: 1
});
$c_jl_StringBuilder.prototype.$classData = $d_jl_StringBuilder;
/** @constructor */
function $c_s_Predef$$eq$colon$eq() {
  $c_O.call(this)
}
$c_s_Predef$$eq$colon$eq.prototype = new $h_O();
$c_s_Predef$$eq$colon$eq.prototype.constructor = $c_s_Predef$$eq$colon$eq;
/** @constructor */
function $h_s_Predef$$eq$colon$eq() {
  /*<skip>*/
}
$h_s_Predef$$eq$colon$eq.prototype = $c_s_Predef$$eq$colon$eq.prototype;
$c_s_Predef$$eq$colon$eq.prototype.toString__T = (function() {
  return "<function1>"
});
/** @constructor */
function $c_s_Predef$$less$colon$less() {
  $c_O.call(this)
}
$c_s_Predef$$less$colon$less.prototype = new $h_O();
$c_s_Predef$$less$colon$less.prototype.constructor = $c_s_Predef$$less$colon$less;
/** @constructor */
function $h_s_Predef$$less$colon$less() {
  /*<skip>*/
}
$h_s_Predef$$less$colon$less.prototype = $c_s_Predef$$less$colon$less.prototype;
$c_s_Predef$$less$colon$less.prototype.toString__T = (function() {
  return "<function1>"
});
/** @constructor */
function $c_s_math_Equiv$() {
  $c_O.call(this)
}
$c_s_math_Equiv$.prototype = new $h_O();
$c_s_math_Equiv$.prototype.constructor = $c_s_math_Equiv$;
/** @constructor */
function $h_s_math_Equiv$() {
  /*<skip>*/
}
$h_s_math_Equiv$.prototype = $c_s_math_Equiv$.prototype;
$c_s_math_Equiv$.prototype.init___ = (function() {
  return this
});
var $d_s_math_Equiv$ = new $TypeData().initClass({
  s_math_Equiv$: 0
}, false, "scala.math.Equiv$", {
  s_math_Equiv$: 1,
  O: 1,
  s_math_LowPriorityEquiv: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_s_math_Equiv$.prototype.$classData = $d_s_math_Equiv$;
var $n_s_math_Equiv$ = (void 0);
function $m_s_math_Equiv$() {
  if ((!$n_s_math_Equiv$)) {
    $n_s_math_Equiv$ = new $c_s_math_Equiv$().init___()
  };
  return $n_s_math_Equiv$
}
/** @constructor */
function $c_s_math_Ordering$() {
  $c_O.call(this)
}
$c_s_math_Ordering$.prototype = new $h_O();
$c_s_math_Ordering$.prototype.constructor = $c_s_math_Ordering$;
/** @constructor */
function $h_s_math_Ordering$() {
  /*<skip>*/
}
$h_s_math_Ordering$.prototype = $c_s_math_Ordering$.prototype;
$c_s_math_Ordering$.prototype.init___ = (function() {
  return this
});
var $d_s_math_Ordering$ = new $TypeData().initClass({
  s_math_Ordering$: 0
}, false, "scala.math.Ordering$", {
  s_math_Ordering$: 1,
  O: 1,
  s_math_LowPriorityOrderingImplicits: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_s_math_Ordering$.prototype.$classData = $d_s_math_Ordering$;
var $n_s_math_Ordering$ = (void 0);
function $m_s_math_Ordering$() {
  if ((!$n_s_math_Ordering$)) {
    $n_s_math_Ordering$ = new $c_s_math_Ordering$().init___()
  };
  return $n_s_math_Ordering$
}
/** @constructor */
function $c_s_reflect_NoManifest$() {
  $c_O.call(this)
}
$c_s_reflect_NoManifest$.prototype = new $h_O();
$c_s_reflect_NoManifest$.prototype.constructor = $c_s_reflect_NoManifest$;
/** @constructor */
function $h_s_reflect_NoManifest$() {
  /*<skip>*/
}
$h_s_reflect_NoManifest$.prototype = $c_s_reflect_NoManifest$.prototype;
$c_s_reflect_NoManifest$.prototype.init___ = (function() {
  return this
});
$c_s_reflect_NoManifest$.prototype.toString__T = (function() {
  return "<?>"
});
var $d_s_reflect_NoManifest$ = new $TypeData().initClass({
  s_reflect_NoManifest$: 0
}, false, "scala.reflect.NoManifest$", {
  s_reflect_NoManifest$: 1,
  O: 1,
  s_reflect_OptManifest: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_s_reflect_NoManifest$.prototype.$classData = $d_s_reflect_NoManifest$;
var $n_s_reflect_NoManifest$ = (void 0);
function $m_s_reflect_NoManifest$() {
  if ((!$n_s_reflect_NoManifest$)) {
    $n_s_reflect_NoManifest$ = new $c_s_reflect_NoManifest$().init___()
  };
  return $n_s_reflect_NoManifest$
}
/** @constructor */
function $c_sc_AbstractIterator() {
  $c_O.call(this)
}
$c_sc_AbstractIterator.prototype = new $h_O();
$c_sc_AbstractIterator.prototype.constructor = $c_sc_AbstractIterator;
/** @constructor */
function $h_sc_AbstractIterator() {
  /*<skip>*/
}
$h_sc_AbstractIterator.prototype = $c_sc_AbstractIterator.prototype;
$c_sc_AbstractIterator.prototype.isEmpty__Z = (function() {
  return $s_sc_Iterator$class__isEmpty__sc_Iterator__Z(this)
});
$c_sc_AbstractIterator.prototype.toString__T = (function() {
  return $s_sc_Iterator$class__toString__sc_Iterator__T(this)
});
$c_sc_AbstractIterator.prototype.foreach__F1__V = (function(f) {
  $s_sc_Iterator$class__foreach__sc_Iterator__F1__V(this, f)
});
/** @constructor */
function $c_scg_SetFactory() {
  $c_scg_GenSetFactory.call(this)
}
$c_scg_SetFactory.prototype = new $h_scg_GenSetFactory();
$c_scg_SetFactory.prototype.constructor = $c_scg_SetFactory;
/** @constructor */
function $h_scg_SetFactory() {
  /*<skip>*/
}
$h_scg_SetFactory.prototype = $c_scg_SetFactory.prototype;
/** @constructor */
function $c_sci_Map$() {
  $c_scg_ImmutableMapFactory.call(this)
}
$c_sci_Map$.prototype = new $h_scg_ImmutableMapFactory();
$c_sci_Map$.prototype.constructor = $c_sci_Map$;
/** @constructor */
function $h_sci_Map$() {
  /*<skip>*/
}
$h_sci_Map$.prototype = $c_sci_Map$.prototype;
$c_sci_Map$.prototype.init___ = (function() {
  return this
});
var $d_sci_Map$ = new $TypeData().initClass({
  sci_Map$: 0
}, false, "scala.collection.immutable.Map$", {
  sci_Map$: 1,
  scg_ImmutableMapFactory: 1,
  scg_MapFactory: 1,
  scg_GenMapFactory: 1,
  O: 1
});
$c_sci_Map$.prototype.$classData = $d_sci_Map$;
var $n_sci_Map$ = (void 0);
function $m_sci_Map$() {
  if ((!$n_sci_Map$)) {
    $n_sci_Map$ = new $c_sci_Map$().init___()
  };
  return $n_sci_Map$
}
/** @constructor */
function $c_sjsr_RuntimeLong() {
  $c_jl_Number.call(this);
  this.lo$2 = 0;
  this.hi$2 = 0
}
$c_sjsr_RuntimeLong.prototype = new $h_jl_Number();
$c_sjsr_RuntimeLong.prototype.constructor = $c_sjsr_RuntimeLong;
/** @constructor */
function $h_sjsr_RuntimeLong() {
  /*<skip>*/
}
$h_sjsr_RuntimeLong.prototype = $c_sjsr_RuntimeLong.prototype;
$c_sjsr_RuntimeLong.prototype.longValue__J = (function() {
  return $uJ(this)
});
$c_sjsr_RuntimeLong.prototype.$$bar__sjsr_RuntimeLong__sjsr_RuntimeLong = (function(b) {
  return new $c_sjsr_RuntimeLong().init___I__I((this.lo$2 | b.lo$2), (this.hi$2 | b.hi$2))
});
$c_sjsr_RuntimeLong.prototype.$$greater$eq__sjsr_RuntimeLong__Z = (function(b) {
  var ahi = this.hi$2;
  var bhi = b.hi$2;
  return ((ahi === bhi) ? (((-2147483648) ^ this.lo$2) >= ((-2147483648) ^ b.lo$2)) : (ahi > bhi))
});
$c_sjsr_RuntimeLong.prototype.byteValue__B = (function() {
  return ((this.lo$2 << 24) >> 24)
});
$c_sjsr_RuntimeLong.prototype.equals__O__Z = (function(that) {
  if ($is_sjsr_RuntimeLong(that)) {
    var x2 = $as_sjsr_RuntimeLong(that);
    return ((this.lo$2 === x2.lo$2) && (this.hi$2 === x2.hi$2))
  } else {
    return false
  }
});
$c_sjsr_RuntimeLong.prototype.$$less__sjsr_RuntimeLong__Z = (function(b) {
  var ahi = this.hi$2;
  var bhi = b.hi$2;
  return ((ahi === bhi) ? (((-2147483648) ^ this.lo$2) < ((-2147483648) ^ b.lo$2)) : (ahi < bhi))
});
$c_sjsr_RuntimeLong.prototype.$$times__sjsr_RuntimeLong__sjsr_RuntimeLong = (function(b) {
  var alo = this.lo$2;
  var blo = b.lo$2;
  return new $c_sjsr_RuntimeLong().init___I__I($imul(alo, blo), $m_sjsr_RuntimeLong$().scala$scalajs$runtime$RuntimeLong$$timesHi__I__I__I__I__I(alo, this.hi$2, blo, b.hi$2))
});
$c_sjsr_RuntimeLong.prototype.init___I__I__I = (function(l, m, h) {
  $c_sjsr_RuntimeLong.prototype.init___I__I.call(this, (l | (m << 22)), ((m >> 10) | (h << 12)));
  return this
});
$c_sjsr_RuntimeLong.prototype.$$percent__sjsr_RuntimeLong__sjsr_RuntimeLong = (function(b) {
  var this$1 = $m_sjsr_RuntimeLong$();
  var lo = this$1.remainderImpl__I__I__I__I__I(this.lo$2, this.hi$2, b.lo$2, b.hi$2);
  return new $c_sjsr_RuntimeLong().init___I__I(lo, this$1.scala$scalajs$runtime$RuntimeLong$$hiReturn$f)
});
$c_sjsr_RuntimeLong.prototype.toString__T = (function() {
  return $m_sjsr_RuntimeLong$().scala$scalajs$runtime$RuntimeLong$$toString__I__I__T(this.lo$2, this.hi$2)
});
$c_sjsr_RuntimeLong.prototype.init___I__I = (function(lo, hi) {
  this.lo$2 = lo;
  this.hi$2 = hi;
  return this
});
$c_sjsr_RuntimeLong.prototype.compareTo__O__I = (function(x$1) {
  var that = $as_sjsr_RuntimeLong(x$1);
  return $m_sjsr_RuntimeLong$().scala$scalajs$runtime$RuntimeLong$$compare__I__I__I__I__I(this.lo$2, this.hi$2, that.lo$2, that.hi$2)
});
$c_sjsr_RuntimeLong.prototype.$$less$eq__sjsr_RuntimeLong__Z = (function(b) {
  var ahi = this.hi$2;
  var bhi = b.hi$2;
  return ((ahi === bhi) ? (((-2147483648) ^ this.lo$2) <= ((-2147483648) ^ b.lo$2)) : (ahi < bhi))
});
$c_sjsr_RuntimeLong.prototype.$$amp__sjsr_RuntimeLong__sjsr_RuntimeLong = (function(b) {
  return new $c_sjsr_RuntimeLong().init___I__I((this.lo$2 & b.lo$2), (this.hi$2 & b.hi$2))
});
$c_sjsr_RuntimeLong.prototype.$$greater$greater$greater__I__sjsr_RuntimeLong = (function(n) {
  return new $c_sjsr_RuntimeLong().init___I__I((((32 & n) === 0) ? (((this.lo$2 >>> n) | 0) | ((this.hi$2 << 1) << ((31 - n) | 0))) : ((this.hi$2 >>> n) | 0)), (((32 & n) === 0) ? ((this.hi$2 >>> n) | 0) : 0))
});
$c_sjsr_RuntimeLong.prototype.$$greater__sjsr_RuntimeLong__Z = (function(b) {
  var ahi = this.hi$2;
  var bhi = b.hi$2;
  return ((ahi === bhi) ? (((-2147483648) ^ this.lo$2) > ((-2147483648) ^ b.lo$2)) : (ahi > bhi))
});
$c_sjsr_RuntimeLong.prototype.$$less$less__I__sjsr_RuntimeLong = (function(n) {
  return new $c_sjsr_RuntimeLong().init___I__I((((32 & n) === 0) ? (this.lo$2 << n) : 0), (((32 & n) === 0) ? (((((this.lo$2 >>> 1) | 0) >>> ((31 - n) | 0)) | 0) | (this.hi$2 << n)) : (this.lo$2 << n)))
});
$c_sjsr_RuntimeLong.prototype.init___I = (function(value) {
  $c_sjsr_RuntimeLong.prototype.init___I__I.call(this, value, (value >> 31));
  return this
});
$c_sjsr_RuntimeLong.prototype.toInt__I = (function() {
  return this.lo$2
});
$c_sjsr_RuntimeLong.prototype.notEquals__sjsr_RuntimeLong__Z = (function(b) {
  return (!((this.lo$2 === b.lo$2) && (this.hi$2 === b.hi$2)))
});
$c_sjsr_RuntimeLong.prototype.unary$und$minus__sjsr_RuntimeLong = (function() {
  var lo = this.lo$2;
  var hi = this.hi$2;
  return new $c_sjsr_RuntimeLong().init___I__I(((-lo) | 0), ((lo !== 0) ? (~hi) : ((-hi) | 0)))
});
$c_sjsr_RuntimeLong.prototype.$$plus__sjsr_RuntimeLong__sjsr_RuntimeLong = (function(b) {
  var alo = this.lo$2;
  var ahi = this.hi$2;
  var bhi = b.hi$2;
  var lo = ((alo + b.lo$2) | 0);
  return new $c_sjsr_RuntimeLong().init___I__I(lo, ((((-2147483648) ^ lo) < ((-2147483648) ^ alo)) ? ((1 + ((ahi + bhi) | 0)) | 0) : ((ahi + bhi) | 0)))
});
$c_sjsr_RuntimeLong.prototype.shortValue__S = (function() {
  return ((this.lo$2 << 16) >> 16)
});
$c_sjsr_RuntimeLong.prototype.$$greater$greater__I__sjsr_RuntimeLong = (function(n) {
  return new $c_sjsr_RuntimeLong().init___I__I((((32 & n) === 0) ? (((this.lo$2 >>> n) | 0) | ((this.hi$2 << 1) << ((31 - n) | 0))) : (this.hi$2 >> n)), (((32 & n) === 0) ? (this.hi$2 >> n) : (this.hi$2 >> 31)))
});
$c_sjsr_RuntimeLong.prototype.toDouble__D = (function() {
  return $m_sjsr_RuntimeLong$().scala$scalajs$runtime$RuntimeLong$$toDouble__I__I__D(this.lo$2, this.hi$2)
});
$c_sjsr_RuntimeLong.prototype.$$div__sjsr_RuntimeLong__sjsr_RuntimeLong = (function(b) {
  var this$1 = $m_sjsr_RuntimeLong$();
  var lo = this$1.divideImpl__I__I__I__I__I(this.lo$2, this.hi$2, b.lo$2, b.hi$2);
  return new $c_sjsr_RuntimeLong().init___I__I(lo, this$1.scala$scalajs$runtime$RuntimeLong$$hiReturn$f)
});
$c_sjsr_RuntimeLong.prototype.doubleValue__D = (function() {
  return $m_sjsr_RuntimeLong$().scala$scalajs$runtime$RuntimeLong$$toDouble__I__I__D(this.lo$2, this.hi$2)
});
$c_sjsr_RuntimeLong.prototype.hashCode__I = (function() {
  return (this.lo$2 ^ this.hi$2)
});
$c_sjsr_RuntimeLong.prototype.intValue__I = (function() {
  return this.lo$2
});
$c_sjsr_RuntimeLong.prototype.unary$und$tilde__sjsr_RuntimeLong = (function() {
  return new $c_sjsr_RuntimeLong().init___I__I((~this.lo$2), (~this.hi$2))
});
$c_sjsr_RuntimeLong.prototype.compareTo__jl_Long__I = (function(that) {
  return $m_sjsr_RuntimeLong$().scala$scalajs$runtime$RuntimeLong$$compare__I__I__I__I__I(this.lo$2, this.hi$2, that.lo$2, that.hi$2)
});
$c_sjsr_RuntimeLong.prototype.floatValue__F = (function() {
  return $fround($m_sjsr_RuntimeLong$().scala$scalajs$runtime$RuntimeLong$$toDouble__I__I__D(this.lo$2, this.hi$2))
});
$c_sjsr_RuntimeLong.prototype.$$minus__sjsr_RuntimeLong__sjsr_RuntimeLong = (function(b) {
  var alo = this.lo$2;
  var ahi = this.hi$2;
  var bhi = b.hi$2;
  var lo = ((alo - b.lo$2) | 0);
  return new $c_sjsr_RuntimeLong().init___I__I(lo, ((((-2147483648) ^ lo) > ((-2147483648) ^ alo)) ? (((-1) + ((ahi - bhi) | 0)) | 0) : ((ahi - bhi) | 0)))
});
$c_sjsr_RuntimeLong.prototype.$$up__sjsr_RuntimeLong__sjsr_RuntimeLong = (function(b) {
  return new $c_sjsr_RuntimeLong().init___I__I((this.lo$2 ^ b.lo$2), (this.hi$2 ^ b.hi$2))
});
$c_sjsr_RuntimeLong.prototype.equals__sjsr_RuntimeLong__Z = (function(b) {
  return ((this.lo$2 === b.lo$2) && (this.hi$2 === b.hi$2))
});
function $is_sjsr_RuntimeLong(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.sjsr_RuntimeLong)))
}
function $as_sjsr_RuntimeLong(obj) {
  return (($is_sjsr_RuntimeLong(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "scala.scalajs.runtime.RuntimeLong"))
}
function $isArrayOf_sjsr_RuntimeLong(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.sjsr_RuntimeLong)))
}
function $asArrayOf_sjsr_RuntimeLong(obj, depth) {
  return (($isArrayOf_sjsr_RuntimeLong(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Lscala.scalajs.runtime.RuntimeLong;", depth))
}
var $d_sjsr_RuntimeLong = new $TypeData().initClass({
  sjsr_RuntimeLong: 0
}, false, "scala.scalajs.runtime.RuntimeLong", {
  sjsr_RuntimeLong: 1,
  jl_Number: 1,
  O: 1,
  Ljava_io_Serializable: 1,
  jl_Comparable: 1
});
$c_sjsr_RuntimeLong.prototype.$classData = $d_sjsr_RuntimeLong;
/** @constructor */
function $c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$japgolly$scalajs$react$ReactComponentB$Builder$$onWillMountFn$1$2() {
  $c_sr_AbstractFunction1.call(this);
  this.f$15$f = null
}
$c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$japgolly$scalajs$react$ReactComponentB$Builder$$onWillMountFn$1$2.prototype = new $h_sr_AbstractFunction1();
$c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$japgolly$scalajs$react$ReactComponentB$Builder$$onWillMountFn$1$2.prototype.constructor = $c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$japgolly$scalajs$react$ReactComponentB$Builder$$onWillMountFn$1$2;
/** @constructor */
function $h_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$japgolly$scalajs$react$ReactComponentB$Builder$$onWillMountFn$1$2() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$japgolly$scalajs$react$ReactComponentB$Builder$$onWillMountFn$1$2.prototype = $c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$japgolly$scalajs$react$ReactComponentB$Builder$$onWillMountFn$1$2.prototype;
$c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$japgolly$scalajs$react$ReactComponentB$Builder$$onWillMountFn$1$2.prototype.apply__O__O = (function(v1) {
  return this.apply__F1__F1($as_F1(v1))
});
$c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$japgolly$scalajs$react$ReactComponentB$Builder$$onWillMountFn$1$2.prototype.apply__F1__F1 = (function(g) {
  return new $c_sjsr_AnonFunction1().init___sjs_js_Function1((function(arg$outer, g$1) {
    return (function($$$) {
      g$1.apply__O__O($$$);
      arg$outer.f$15$f.apply__O__O($$$)
    })
  })(this, g))
});
$c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$japgolly$scalajs$react$ReactComponentB$Builder$$onWillMountFn$1$2.prototype.init___Ljapgolly_scalajs_react_ReactComponentB$Builder__F1 = (function($$outer, f$15) {
  this.f$15$f = f$15;
  return this
});
var $d_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$japgolly$scalajs$react$ReactComponentB$Builder$$onWillMountFn$1$2 = new $TypeData().initClass({
  Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$japgolly$scalajs$react$ReactComponentB$Builder$$onWillMountFn$1$2: 0
}, false, "japgolly.scalajs.react.ReactComponentB$Builder$$anonfun$japgolly$scalajs$react$ReactComponentB$Builder$$onWillMountFn$1$2", {
  Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$japgolly$scalajs$react$ReactComponentB$Builder$$onWillMountFn$1$2: 1,
  sr_AbstractFunction1: 1,
  O: 1,
  F1: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$japgolly$scalajs$react$ReactComponentB$Builder$$onWillMountFn$1$2.prototype.$classData = $d_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$japgolly$scalajs$react$ReactComponentB$Builder$$onWillMountFn$1$2;
/** @constructor */
function $c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnP$1$1() {
  $c_sr_AbstractFunction1.call(this);
  this.spec$1$2 = null;
  this.a$2$f = null;
  this.name$2$2 = null
}
$c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnP$1$1.prototype = new $h_sr_AbstractFunction1();
$c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnP$1$1.prototype.constructor = $c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnP$1$1;
/** @constructor */
function $h_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnP$1$1() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnP$1$1.prototype = $c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnP$1$1.prototype;
$c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnP$1$1.prototype.apply__O__O = (function(v1) {
  this.apply__F1__V($as_F1(v1))
});
$c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnP$1$1.prototype.apply__F1__V = (function(f) {
  var g = new $c_sjsr_AnonFunction3().init___sjs_js_Function3((function(arg$outer, f$17) {
    return (function($$$, p$2, s$2) {
      var $$this = $as_Ljapgolly_scalajs_react_CallbackTo(f$17.apply__O__O(arg$outer.a$2$f.apply__O__O__O($$$, p$2.v))).japgolly$scalajs$react$CallbackTo$$f$1;
      return $$this.apply__O()
    })
  })(this, f));
  this.spec$1$2[this.name$2$2] = (function(f$1) {
    return (function(arg1, arg2) {
      return f$1.apply__O__O__O__O(this, arg1, arg2)
    })
  })(g)
});
$c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnP$1$1.prototype.init___Ljapgolly_scalajs_react_ReactComponentB$Builder__sjs_js_Dictionary__F2__T = (function($$outer, spec$1, a$2, name$2) {
  this.spec$1$2 = spec$1;
  this.a$2$f = a$2;
  this.name$2$2 = name$2;
  return this
});
var $d_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnP$1$1 = new $TypeData().initClass({
  Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnP$1$1: 0
}, false, "japgolly.scalajs.react.ReactComponentB$Builder$$anonfun$setFnP$1$1", {
  Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnP$1$1: 1,
  sr_AbstractFunction1: 1,
  O: 1,
  F1: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnP$1$1.prototype.$classData = $d_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnP$1$1;
/** @constructor */
function $c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnPS$1$1() {
  $c_sr_AbstractFunction1.call(this);
  this.spec$1$2 = null;
  this.a$1$f = null;
  this.name$1$2 = null
}
$c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnPS$1$1.prototype = new $h_sr_AbstractFunction1();
$c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnPS$1$1.prototype.constructor = $c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnPS$1$1;
/** @constructor */
function $h_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnPS$1$1() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnPS$1$1.prototype = $c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnPS$1$1.prototype;
$c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnPS$1$1.prototype.apply__O__O = (function(v1) {
  this.apply__F1__V($as_F1(v1))
});
$c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnPS$1$1.prototype.apply__F1__V = (function(f) {
  var g = new $c_sjsr_AnonFunction3().init___sjs_js_Function3((function(arg$outer, f$16) {
    return (function($$$, p$2, s$2) {
      var $$this = $as_Ljapgolly_scalajs_react_CallbackTo(f$16.apply__O__O(arg$outer.a$1$f.apply__O__O__O__O($$$, p$2.v, s$2.v))).japgolly$scalajs$react$CallbackTo$$f$1;
      return $$this.apply__O()
    })
  })(this, f));
  this.spec$1$2[this.name$1$2] = (function(f$1) {
    return (function(arg1, arg2) {
      return f$1.apply__O__O__O__O(this, arg1, arg2)
    })
  })(g)
});
$c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnPS$1$1.prototype.init___Ljapgolly_scalajs_react_ReactComponentB$Builder__sjs_js_Dictionary__F3__T = (function($$outer, spec$1, a$1, name$1) {
  this.spec$1$2 = spec$1;
  this.a$1$f = a$1;
  this.name$1$2 = name$1;
  return this
});
var $d_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnPS$1$1 = new $TypeData().initClass({
  Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnPS$1$1: 0
}, false, "japgolly.scalajs.react.ReactComponentB$Builder$$anonfun$setFnPS$1$1", {
  Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnPS$1$1: 1,
  sr_AbstractFunction1: 1,
  O: 1,
  F1: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnPS$1$1.prototype.$classData = $d_Ljapgolly_scalajs_react_ReactComponentB$Builder$$anonfun$setFnPS$1$1;
/** @constructor */
function $c_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle() {
  $c_O.call(this);
  this.configureSpec$1 = null;
  this.getDefaultProps$1 = null;
  this.componentWillMount$1 = null;
  this.componentDidMount$1 = null;
  this.componentWillUnmount$1 = null;
  this.componentWillUpdate$1 = null;
  this.componentDidUpdate$1 = null;
  this.componentWillReceiveProps$1 = null;
  this.shouldComponentUpdate$1 = null
}
$c_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle.prototype.constructor = $c_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle;
/** @constructor */
function $h_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle.prototype = $c_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle.prototype;
$c_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle.prototype.productPrefix__T = (function() {
  return "LifeCycle"
});
$c_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle.prototype.init___sjs_js_UndefOr__sjs_js_UndefOr__sjs_js_UndefOr__sjs_js_UndefOr__sjs_js_UndefOr__sjs_js_UndefOr__sjs_js_UndefOr__sjs_js_UndefOr__sjs_js_UndefOr = (function(configureSpec, getDefaultProps, componentWillMount, componentDidMount, componentWillUnmount, componentWillUpdate, componentDidUpdate, componentWillReceiveProps, shouldComponentUpdate) {
  this.configureSpec$1 = configureSpec;
  this.getDefaultProps$1 = getDefaultProps;
  this.componentWillMount$1 = componentWillMount;
  this.componentDidMount$1 = componentDidMount;
  this.componentWillUnmount$1 = componentWillUnmount;
  this.componentWillUpdate$1 = componentWillUpdate;
  this.componentDidUpdate$1 = componentDidUpdate;
  this.componentWillReceiveProps$1 = componentWillReceiveProps;
  this.shouldComponentUpdate$1 = shouldComponentUpdate;
  return this
});
$c_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle.prototype.productArity__I = (function() {
  return 9
});
$c_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle.prototype.equals__O__Z = (function(x$1) {
  if ((this === x$1)) {
    return true
  } else if ($is_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle(x$1)) {
    var LifeCycle$1 = $as_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle(x$1);
    return (((((((($m_sr_BoxesRunTime$().equals__O__O__Z(this.configureSpec$1, LifeCycle$1.configureSpec$1) && $m_sr_BoxesRunTime$().equals__O__O__Z(this.getDefaultProps$1, LifeCycle$1.getDefaultProps$1)) && $m_sr_BoxesRunTime$().equals__O__O__Z(this.componentWillMount$1, LifeCycle$1.componentWillMount$1)) && $m_sr_BoxesRunTime$().equals__O__O__Z(this.componentDidMount$1, LifeCycle$1.componentDidMount$1)) && $m_sr_BoxesRunTime$().equals__O__O__Z(this.componentWillUnmount$1, LifeCycle$1.componentWillUnmount$1)) && $m_sr_BoxesRunTime$().equals__O__O__Z(this.componentWillUpdate$1, LifeCycle$1.componentWillUpdate$1)) && $m_sr_BoxesRunTime$().equals__O__O__Z(this.componentDidUpdate$1, LifeCycle$1.componentDidUpdate$1)) && $m_sr_BoxesRunTime$().equals__O__O__Z(this.componentWillReceiveProps$1, LifeCycle$1.componentWillReceiveProps$1)) && $m_sr_BoxesRunTime$().equals__O__O__Z(this.shouldComponentUpdate$1, LifeCycle$1.shouldComponentUpdate$1))
  } else {
    return false
  }
});
$c_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle.prototype.productElement__I__O = (function(x$1) {
  switch (x$1) {
    case 0: {
      return this.configureSpec$1;
      break
    }
    case 1: {
      return this.getDefaultProps$1;
      break
    }
    case 2: {
      return this.componentWillMount$1;
      break
    }
    case 3: {
      return this.componentDidMount$1;
      break
    }
    case 4: {
      return this.componentWillUnmount$1;
      break
    }
    case 5: {
      return this.componentWillUpdate$1;
      break
    }
    case 6: {
      return this.componentDidUpdate$1;
      break
    }
    case 7: {
      return this.componentWillReceiveProps$1;
      break
    }
    case 8: {
      return this.shouldComponentUpdate$1;
      break
    }
    default: {
      throw new $c_jl_IndexOutOfBoundsException().init___T(("" + x$1))
    }
  }
});
$c_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle.prototype.toString__T = (function() {
  return $m_sr_ScalaRunTime$().$$undtoString__s_Product__T(this)
});
$c_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle.prototype.hashCode__I = (function() {
  var this$2 = $m_s_util_hashing_MurmurHash3$();
  return this$2.productHash__s_Product__I__I(this, (-889275714))
});
$c_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle.prototype.productIterator__sc_Iterator = (function() {
  return new $c_sr_ScalaRunTime$$anon$1().init___s_Product(this)
});
function $is_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.Ljapgolly_scalajs_react_ReactComponentB$LifeCycle)))
}
function $as_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle(obj) {
  return (($is_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "japgolly.scalajs.react.ReactComponentB$LifeCycle"))
}
function $isArrayOf_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.Ljapgolly_scalajs_react_ReactComponentB$LifeCycle)))
}
function $asArrayOf_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle(obj, depth) {
  return (($isArrayOf_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Ljapgolly.scalajs.react.ReactComponentB$LifeCycle;", depth))
}
var $d_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle = new $TypeData().initClass({
  Ljapgolly_scalajs_react_ReactComponentB$LifeCycle: 0
}, false, "japgolly.scalajs.react.ReactComponentB$LifeCycle", {
  Ljapgolly_scalajs_react_ReactComponentB$LifeCycle: 1,
  O: 1,
  s_Product: 1,
  s_Equals: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle.prototype.$classData = $d_Ljapgolly_scalajs_react_ReactComponentB$LifeCycle;
/** @constructor */
function $c_Ljapgolly_scalajs_react_vdom_HtmlTags$input$() {
  $c_Ljapgolly_scalajs_react_vdom_ReactTagOf.call(this);
  this.type$2 = null;
  this.checkbox$2 = null;
  this.text$2 = null;
  this.bitmap$0$2 = false
}
$c_Ljapgolly_scalajs_react_vdom_HtmlTags$input$.prototype = new $h_Ljapgolly_scalajs_react_vdom_ReactTagOf();
$c_Ljapgolly_scalajs_react_vdom_HtmlTags$input$.prototype.constructor = $c_Ljapgolly_scalajs_react_vdom_HtmlTags$input$;
/** @constructor */
function $h_Ljapgolly_scalajs_react_vdom_HtmlTags$input$() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_vdom_HtmlTags$input$.prototype = $c_Ljapgolly_scalajs_react_vdom_HtmlTags$input$.prototype;
$c_Ljapgolly_scalajs_react_vdom_HtmlTags$input$.prototype.init___Ljapgolly_scalajs_react_vdom_HtmlTags = (function($$outer) {
  var jsx$1 = $m_sci_Nil$();
  var e = new $c_Ljapgolly_scalajs_react_vdom_Namespace().init___T($m_Ljapgolly_scalajs_react_vdom_NamespaceHtml$().implicitNamespace$1);
  $c_Ljapgolly_scalajs_react_vdom_ReactTagOf.prototype.init___T__sci_List__T.call(this, "input", jsx$1, e.uri$1);
  this.type$2 = new $c_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic().init___T("type");
  this.text$2 = this.withType__T__Ljapgolly_scalajs_react_vdom_ReactTagOf("text");
  return this
});
$c_Ljapgolly_scalajs_react_vdom_HtmlTags$input$.prototype.withType__T__Ljapgolly_scalajs_react_vdom_ReactTagOf = (function(t) {
  var this$1 = this.type$2;
  var t$1 = $m_Ljapgolly_scalajs_react_vdom_ReactAttr$ValueType$().string$1;
  return this.apply__sc_Seq__Ljapgolly_scalajs_react_vdom_ReactTagOf(new $c_sjs_js_WrappedArray().init___sjs_js_Array([new $c_Ljapgolly_scalajs_react_vdom_ReactAttr$NameAndValue().init___T__O__F2(this$1.name$1, t, t$1)]))
});
var $d_Ljapgolly_scalajs_react_vdom_HtmlTags$input$ = new $TypeData().initClass({
  Ljapgolly_scalajs_react_vdom_HtmlTags$input$: 0
}, false, "japgolly.scalajs.react.vdom.HtmlTags$input$", {
  Ljapgolly_scalajs_react_vdom_HtmlTags$input$: 1,
  Ljapgolly_scalajs_react_vdom_ReactTagOf: 1,
  O: 1,
  Ljapgolly_scalajs_react_vdom_DomFrag: 1,
  Ljapgolly_scalajs_react_vdom_Frag: 1,
  Ljapgolly_scalajs_react_vdom_TagMod: 1
});
$c_Ljapgolly_scalajs_react_vdom_HtmlTags$input$.prototype.$classData = $d_Ljapgolly_scalajs_react_vdom_HtmlTags$input$;
/** @constructor */
function $c_Ljapgolly_scalajs_react_vdom_Namespace() {
  $c_O.call(this);
  this.uri$1 = null
}
$c_Ljapgolly_scalajs_react_vdom_Namespace.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_vdom_Namespace.prototype.constructor = $c_Ljapgolly_scalajs_react_vdom_Namespace;
/** @constructor */
function $h_Ljapgolly_scalajs_react_vdom_Namespace() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_vdom_Namespace.prototype = $c_Ljapgolly_scalajs_react_vdom_Namespace.prototype;
$c_Ljapgolly_scalajs_react_vdom_Namespace.prototype.productPrefix__T = (function() {
  return "Namespace"
});
$c_Ljapgolly_scalajs_react_vdom_Namespace.prototype.productArity__I = (function() {
  return 1
});
$c_Ljapgolly_scalajs_react_vdom_Namespace.prototype.equals__O__Z = (function(x$1) {
  return $m_Ljapgolly_scalajs_react_vdom_Namespace$().equals$extension__T__O__Z(this.uri$1, x$1)
});
$c_Ljapgolly_scalajs_react_vdom_Namespace.prototype.productElement__I__O = (function(x$1) {
  return $m_Ljapgolly_scalajs_react_vdom_Namespace$().productElement$extension__T__I__O(this.uri$1, x$1)
});
$c_Ljapgolly_scalajs_react_vdom_Namespace.prototype.toString__T = (function() {
  return $m_Ljapgolly_scalajs_react_vdom_Namespace$().toString$extension__T__T(this.uri$1)
});
$c_Ljapgolly_scalajs_react_vdom_Namespace.prototype.init___T = (function(uri) {
  this.uri$1 = uri;
  return this
});
$c_Ljapgolly_scalajs_react_vdom_Namespace.prototype.hashCode__I = (function() {
  var $$this = this.uri$1;
  return $m_sjsr_RuntimeString$().hashCode__T__I($$this)
});
$c_Ljapgolly_scalajs_react_vdom_Namespace.prototype.productIterator__sc_Iterator = (function() {
  return $m_Ljapgolly_scalajs_react_vdom_Namespace$().productIterator$extension__T__sc_Iterator(this.uri$1)
});
function $is_Ljapgolly_scalajs_react_vdom_Namespace(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.Ljapgolly_scalajs_react_vdom_Namespace)))
}
function $as_Ljapgolly_scalajs_react_vdom_Namespace(obj) {
  return (($is_Ljapgolly_scalajs_react_vdom_Namespace(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "japgolly.scalajs.react.vdom.Namespace"))
}
function $isArrayOf_Ljapgolly_scalajs_react_vdom_Namespace(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.Ljapgolly_scalajs_react_vdom_Namespace)))
}
function $asArrayOf_Ljapgolly_scalajs_react_vdom_Namespace(obj, depth) {
  return (($isArrayOf_Ljapgolly_scalajs_react_vdom_Namespace(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Ljapgolly.scalajs.react.vdom.Namespace;", depth))
}
var $d_Ljapgolly_scalajs_react_vdom_Namespace = new $TypeData().initClass({
  Ljapgolly_scalajs_react_vdom_Namespace: 0
}, false, "japgolly.scalajs.react.vdom.Namespace", {
  Ljapgolly_scalajs_react_vdom_Namespace: 1,
  O: 1,
  s_Product: 1,
  s_Equals: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_Ljapgolly_scalajs_react_vdom_Namespace.prototype.$classData = $d_Ljapgolly_scalajs_react_vdom_Namespace;
/** @constructor */
function $c_Ljapgolly_scalajs_react_vdom_Namespace$() {
  $c_sr_AbstractFunction1.call(this)
}
$c_Ljapgolly_scalajs_react_vdom_Namespace$.prototype = new $h_sr_AbstractFunction1();
$c_Ljapgolly_scalajs_react_vdom_Namespace$.prototype.constructor = $c_Ljapgolly_scalajs_react_vdom_Namespace$;
/** @constructor */
function $h_Ljapgolly_scalajs_react_vdom_Namespace$() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_vdom_Namespace$.prototype = $c_Ljapgolly_scalajs_react_vdom_Namespace$.prototype;
$c_Ljapgolly_scalajs_react_vdom_Namespace$.prototype.init___ = (function() {
  return this
});
$c_Ljapgolly_scalajs_react_vdom_Namespace$.prototype.apply__O__O = (function(v1) {
  var uri = $as_T(v1);
  return new $c_Ljapgolly_scalajs_react_vdom_Namespace().init___T(uri)
});
$c_Ljapgolly_scalajs_react_vdom_Namespace$.prototype.equals$extension__T__O__Z = (function($$this, x$1) {
  if ($is_Ljapgolly_scalajs_react_vdom_Namespace(x$1)) {
    var Namespace$1 = ((x$1 === null) ? null : $as_Ljapgolly_scalajs_react_vdom_Namespace(x$1).uri$1);
    return ($$this === Namespace$1)
  } else {
    return false
  }
});
$c_Ljapgolly_scalajs_react_vdom_Namespace$.prototype.toString$extension__T__T = (function($$this) {
  return $m_sr_ScalaRunTime$().$$undtoString__s_Product__T(new $c_Ljapgolly_scalajs_react_vdom_Namespace().init___T($$this))
});
$c_Ljapgolly_scalajs_react_vdom_Namespace$.prototype.productIterator$extension__T__sc_Iterator = (function($$this) {
  var x = new $c_Ljapgolly_scalajs_react_vdom_Namespace().init___T($$this);
  return new $c_sr_ScalaRunTime$$anon$1().init___s_Product(x)
});
$c_Ljapgolly_scalajs_react_vdom_Namespace$.prototype.toString__T = (function() {
  return "Namespace"
});
$c_Ljapgolly_scalajs_react_vdom_Namespace$.prototype.productElement$extension__T__I__O = (function($$this, x$1) {
  switch (x$1) {
    case 0: {
      return $$this;
      break
    }
    default: {
      throw new $c_jl_IndexOutOfBoundsException().init___T(("" + x$1))
    }
  }
});
var $d_Ljapgolly_scalajs_react_vdom_Namespace$ = new $TypeData().initClass({
  Ljapgolly_scalajs_react_vdom_Namespace$: 0
}, false, "japgolly.scalajs.react.vdom.Namespace$", {
  Ljapgolly_scalajs_react_vdom_Namespace$: 1,
  sr_AbstractFunction1: 1,
  O: 1,
  F1: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_Ljapgolly_scalajs_react_vdom_Namespace$.prototype.$classData = $d_Ljapgolly_scalajs_react_vdom_Namespace$;
var $n_Ljapgolly_scalajs_react_vdom_Namespace$ = (void 0);
function $m_Ljapgolly_scalajs_react_vdom_Namespace$() {
  if ((!$n_Ljapgolly_scalajs_react_vdom_Namespace$)) {
    $n_Ljapgolly_scalajs_react_vdom_Namespace$ = new $c_Ljapgolly_scalajs_react_vdom_Namespace$().init___()
  };
  return $n_Ljapgolly_scalajs_react_vdom_Namespace$
}
/** @constructor */
function $c_Ljapgolly_scalajs_react_vdom_package$Tags$() {
  $c_O.call(this);
  this.big$1 = null;
  this.dialog$1 = null;
  this.menuitem$1 = null;
  this.html$1 = null;
  this.head$1 = null;
  this.base$1 = null;
  this.link$1 = null;
  this.meta$1 = null;
  this.script$1 = null;
  this.body$1 = null;
  this.h1$1 = null;
  this.h2$1 = null;
  this.h3$1 = null;
  this.h4$1 = null;
  this.h5$1 = null;
  this.h6$1 = null;
  this.header$1 = null;
  this.footer$1 = null;
  this.p$1 = null;
  this.hr$1 = null;
  this.pre$1 = null;
  this.blockquote$1 = null;
  this.ol$1 = null;
  this.ul$1 = null;
  this.li$1 = null;
  this.dl$1 = null;
  this.dt$1 = null;
  this.dd$1 = null;
  this.figure$1 = null;
  this.figcaption$1 = null;
  this.div$1 = null;
  this.a$1 = null;
  this.em$1 = null;
  this.strong$1 = null;
  this.small$1 = null;
  this.s$1 = null;
  this.cite$1 = null;
  this.code$1 = null;
  this.sub$1 = null;
  this.sup$1 = null;
  this.i$1 = null;
  this.b$1 = null;
  this.u$1 = null;
  this.span$1 = null;
  this.br$1 = null;
  this.wbr$1 = null;
  this.ins$1 = null;
  this.del$1 = null;
  this.img$1 = null;
  this.iframe$1 = null;
  this.embed$1 = null;
  this.object$1 = null;
  this.param$1 = null;
  this.video$1 = null;
  this.audio$1 = null;
  this.source$1 = null;
  this.track$1 = null;
  this.canvas$1 = null;
  this.map$1 = null;
  this.area$1 = null;
  this.table$1 = null;
  this.caption$1 = null;
  this.colgroup$1 = null;
  this.col$1 = null;
  this.tbody$1 = null;
  this.thead$1 = null;
  this.tfoot$1 = null;
  this.tr$1 = null;
  this.td$1 = null;
  this.th$1 = null;
  this.form$1 = null;
  this.fieldset$1 = null;
  this.legend$1 = null;
  this.label$1 = null;
  this.button$1 = null;
  this.select$1 = null;
  this.datalist$1 = null;
  this.optgroup$1 = null;
  this.option$1 = null;
  this.textarea$1 = null;
  this.titleTag$1 = null;
  this.styleTag$1 = null;
  this.noscript$1 = null;
  this.section$1 = null;
  this.nav$1 = null;
  this.article$1 = null;
  this.aside$1 = null;
  this.address$1 = null;
  this.main$1 = null;
  this.q$1 = null;
  this.dfn$1 = null;
  this.abbr$1 = null;
  this.data$1 = null;
  this.time$1 = null;
  this.var$1 = null;
  this.samp$1 = null;
  this.kbd$1 = null;
  this.math$1 = null;
  this.mark$1 = null;
  this.ruby$1 = null;
  this.rt$1 = null;
  this.rp$1 = null;
  this.bdi$1 = null;
  this.bdo$1 = null;
  this.keygen$1 = null;
  this.output$1 = null;
  this.progress$1 = null;
  this.meter$1 = null;
  this.details$1 = null;
  this.summary$1 = null;
  this.command$1 = null;
  this.menu$1 = null;
  this.bitmap$0$1 = $m_sjsr_RuntimeLong$().Zero__sjsr_RuntimeLong();
  this.bitmap$1$1 = $m_sjsr_RuntimeLong$().Zero__sjsr_RuntimeLong();
  this.input$module$1 = null
}
$c_Ljapgolly_scalajs_react_vdom_package$Tags$.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_vdom_package$Tags$.prototype.constructor = $c_Ljapgolly_scalajs_react_vdom_package$Tags$;
/** @constructor */
function $h_Ljapgolly_scalajs_react_vdom_package$Tags$() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_vdom_package$Tags$.prototype = $c_Ljapgolly_scalajs_react_vdom_package$Tags$.prototype;
$c_Ljapgolly_scalajs_react_vdom_package$Tags$.prototype.init___ = (function() {
  $n_Ljapgolly_scalajs_react_vdom_package$Tags$ = this;
  $s_Ljapgolly_scalajs_react_vdom_HtmlTags$class__$$init$__Ljapgolly_scalajs_react_vdom_HtmlTags__V(this);
  return this
});
$c_Ljapgolly_scalajs_react_vdom_package$Tags$.prototype.input__Ljapgolly_scalajs_react_vdom_HtmlTags$input$ = (function() {
  return ((this.input$module$1 === null) ? this.input$lzycompute__p1__Ljapgolly_scalajs_react_vdom_HtmlTags$input$() : this.input$module$1)
});
$c_Ljapgolly_scalajs_react_vdom_package$Tags$.prototype.input$lzycompute__p1__Ljapgolly_scalajs_react_vdom_HtmlTags$input$ = (function() {
  if ((this.input$module$1 === null)) {
    this.input$module$1 = new $c_Ljapgolly_scalajs_react_vdom_HtmlTags$input$().init___Ljapgolly_scalajs_react_vdom_HtmlTags(this)
  };
  return this.input$module$1
});
var $d_Ljapgolly_scalajs_react_vdom_package$Tags$ = new $TypeData().initClass({
  Ljapgolly_scalajs_react_vdom_package$Tags$: 0
}, false, "japgolly.scalajs.react.vdom.package$Tags$", {
  Ljapgolly_scalajs_react_vdom_package$Tags$: 1,
  O: 1,
  Ljapgolly_scalajs_react_vdom_package$JustTags: 1,
  Ljapgolly_scalajs_react_vdom_package$Tags: 1,
  Ljapgolly_scalajs_react_vdom_HtmlTags: 1,
  Ljapgolly_scalajs_react_vdom_Extra$Tags: 1
});
$c_Ljapgolly_scalajs_react_vdom_package$Tags$.prototype.$classData = $d_Ljapgolly_scalajs_react_vdom_package$Tags$;
var $n_Ljapgolly_scalajs_react_vdom_package$Tags$ = (void 0);
function $m_Ljapgolly_scalajs_react_vdom_package$Tags$() {
  if ((!$n_Ljapgolly_scalajs_react_vdom_package$Tags$)) {
    $n_Ljapgolly_scalajs_react_vdom_package$Tags$ = new $c_Ljapgolly_scalajs_react_vdom_package$Tags$().init___()
  };
  return $n_Ljapgolly_scalajs_react_vdom_package$Tags$
}
/** @constructor */
function $c_jl_ArithmeticException() {
  $c_jl_RuntimeException.call(this)
}
$c_jl_ArithmeticException.prototype = new $h_jl_RuntimeException();
$c_jl_ArithmeticException.prototype.constructor = $c_jl_ArithmeticException;
/** @constructor */
function $h_jl_ArithmeticException() {
  /*<skip>*/
}
$h_jl_ArithmeticException.prototype = $c_jl_ArithmeticException.prototype;
$c_jl_ArithmeticException.prototype.init___T = (function(s) {
  $c_jl_Throwable.prototype.init___T__jl_Throwable.call(this, s, null);
  return this
});
var $d_jl_ArithmeticException = new $TypeData().initClass({
  jl_ArithmeticException: 0
}, false, "java.lang.ArithmeticException", {
  jl_ArithmeticException: 1,
  jl_RuntimeException: 1,
  jl_Exception: 1,
  jl_Throwable: 1,
  O: 1,
  Ljava_io_Serializable: 1
});
$c_jl_ArithmeticException.prototype.$classData = $d_jl_ArithmeticException;
/** @constructor */
function $c_jl_ClassCastException() {
  $c_jl_RuntimeException.call(this)
}
$c_jl_ClassCastException.prototype = new $h_jl_RuntimeException();
$c_jl_ClassCastException.prototype.constructor = $c_jl_ClassCastException;
/** @constructor */
function $h_jl_ClassCastException() {
  /*<skip>*/
}
$h_jl_ClassCastException.prototype = $c_jl_ClassCastException.prototype;
$c_jl_ClassCastException.prototype.init___T = (function(s) {
  $c_jl_Throwable.prototype.init___T__jl_Throwable.call(this, s, null);
  return this
});
var $d_jl_ClassCastException = new $TypeData().initClass({
  jl_ClassCastException: 0
}, false, "java.lang.ClassCastException", {
  jl_ClassCastException: 1,
  jl_RuntimeException: 1,
  jl_Exception: 1,
  jl_Throwable: 1,
  O: 1,
  Ljava_io_Serializable: 1
});
$c_jl_ClassCastException.prototype.$classData = $d_jl_ClassCastException;
/** @constructor */
function $c_jl_IllegalArgumentException() {
  $c_jl_RuntimeException.call(this)
}
$c_jl_IllegalArgumentException.prototype = new $h_jl_RuntimeException();
$c_jl_IllegalArgumentException.prototype.constructor = $c_jl_IllegalArgumentException;
/** @constructor */
function $h_jl_IllegalArgumentException() {
  /*<skip>*/
}
$h_jl_IllegalArgumentException.prototype = $c_jl_IllegalArgumentException.prototype;
$c_jl_IllegalArgumentException.prototype.init___ = (function() {
  $c_jl_Throwable.prototype.init___T__jl_Throwable.call(this, null, null);
  return this
});
$c_jl_IllegalArgumentException.prototype.init___T = (function(s) {
  $c_jl_Throwable.prototype.init___T__jl_Throwable.call(this, s, null);
  return this
});
var $d_jl_IllegalArgumentException = new $TypeData().initClass({
  jl_IllegalArgumentException: 0
}, false, "java.lang.IllegalArgumentException", {
  jl_IllegalArgumentException: 1,
  jl_RuntimeException: 1,
  jl_Exception: 1,
  jl_Throwable: 1,
  O: 1,
  Ljava_io_Serializable: 1
});
$c_jl_IllegalArgumentException.prototype.$classData = $d_jl_IllegalArgumentException;
/** @constructor */
function $c_jl_IllegalStateException() {
  $c_jl_RuntimeException.call(this)
}
$c_jl_IllegalStateException.prototype = new $h_jl_RuntimeException();
$c_jl_IllegalStateException.prototype.constructor = $c_jl_IllegalStateException;
/** @constructor */
function $h_jl_IllegalStateException() {
  /*<skip>*/
}
$h_jl_IllegalStateException.prototype = $c_jl_IllegalStateException.prototype;
$c_jl_IllegalStateException.prototype.init___T = (function(s) {
  $c_jl_Throwable.prototype.init___T__jl_Throwable.call(this, s, null);
  return this
});
var $d_jl_IllegalStateException = new $TypeData().initClass({
  jl_IllegalStateException: 0
}, false, "java.lang.IllegalStateException", {
  jl_IllegalStateException: 1,
  jl_RuntimeException: 1,
  jl_Exception: 1,
  jl_Throwable: 1,
  O: 1,
  Ljava_io_Serializable: 1
});
$c_jl_IllegalStateException.prototype.$classData = $d_jl_IllegalStateException;
/** @constructor */
function $c_jl_IndexOutOfBoundsException() {
  $c_jl_RuntimeException.call(this)
}
$c_jl_IndexOutOfBoundsException.prototype = new $h_jl_RuntimeException();
$c_jl_IndexOutOfBoundsException.prototype.constructor = $c_jl_IndexOutOfBoundsException;
/** @constructor */
function $h_jl_IndexOutOfBoundsException() {
  /*<skip>*/
}
$h_jl_IndexOutOfBoundsException.prototype = $c_jl_IndexOutOfBoundsException.prototype;
$c_jl_IndexOutOfBoundsException.prototype.init___T = (function(s) {
  $c_jl_Throwable.prototype.init___T__jl_Throwable.call(this, s, null);
  return this
});
var $d_jl_IndexOutOfBoundsException = new $TypeData().initClass({
  jl_IndexOutOfBoundsException: 0
}, false, "java.lang.IndexOutOfBoundsException", {
  jl_IndexOutOfBoundsException: 1,
  jl_RuntimeException: 1,
  jl_Exception: 1,
  jl_Throwable: 1,
  O: 1,
  Ljava_io_Serializable: 1
});
$c_jl_IndexOutOfBoundsException.prototype.$classData = $d_jl_IndexOutOfBoundsException;
/** @constructor */
function $c_jl_NullPointerException() {
  $c_jl_RuntimeException.call(this)
}
$c_jl_NullPointerException.prototype = new $h_jl_RuntimeException();
$c_jl_NullPointerException.prototype.constructor = $c_jl_NullPointerException;
/** @constructor */
function $h_jl_NullPointerException() {
  /*<skip>*/
}
$h_jl_NullPointerException.prototype = $c_jl_NullPointerException.prototype;
$c_jl_NullPointerException.prototype.init___ = (function() {
  $c_jl_Throwable.prototype.init___T__jl_Throwable.call(this, null, null);
  return this
});
var $d_jl_NullPointerException = new $TypeData().initClass({
  jl_NullPointerException: 0
}, false, "java.lang.NullPointerException", {
  jl_NullPointerException: 1,
  jl_RuntimeException: 1,
  jl_Exception: 1,
  jl_Throwable: 1,
  O: 1,
  Ljava_io_Serializable: 1
});
$c_jl_NullPointerException.prototype.$classData = $d_jl_NullPointerException;
/** @constructor */
function $c_jl_UnsupportedOperationException() {
  $c_jl_RuntimeException.call(this)
}
$c_jl_UnsupportedOperationException.prototype = new $h_jl_RuntimeException();
$c_jl_UnsupportedOperationException.prototype.constructor = $c_jl_UnsupportedOperationException;
/** @constructor */
function $h_jl_UnsupportedOperationException() {
  /*<skip>*/
}
$h_jl_UnsupportedOperationException.prototype = $c_jl_UnsupportedOperationException.prototype;
$c_jl_UnsupportedOperationException.prototype.init___T = (function(s) {
  $c_jl_Throwable.prototype.init___T__jl_Throwable.call(this, s, null);
  return this
});
var $d_jl_UnsupportedOperationException = new $TypeData().initClass({
  jl_UnsupportedOperationException: 0
}, false, "java.lang.UnsupportedOperationException", {
  jl_UnsupportedOperationException: 1,
  jl_RuntimeException: 1,
  jl_Exception: 1,
  jl_Throwable: 1,
  O: 1,
  Ljava_io_Serializable: 1
});
$c_jl_UnsupportedOperationException.prototype.$classData = $d_jl_UnsupportedOperationException;
/** @constructor */
function $c_ju_NoSuchElementException() {
  $c_jl_RuntimeException.call(this)
}
$c_ju_NoSuchElementException.prototype = new $h_jl_RuntimeException();
$c_ju_NoSuchElementException.prototype.constructor = $c_ju_NoSuchElementException;
/** @constructor */
function $h_ju_NoSuchElementException() {
  /*<skip>*/
}
$h_ju_NoSuchElementException.prototype = $c_ju_NoSuchElementException.prototype;
$c_ju_NoSuchElementException.prototype.init___T = (function(s) {
  $c_jl_Throwable.prototype.init___T__jl_Throwable.call(this, s, null);
  return this
});
var $d_ju_NoSuchElementException = new $TypeData().initClass({
  ju_NoSuchElementException: 0
}, false, "java.util.NoSuchElementException", {
  ju_NoSuchElementException: 1,
  jl_RuntimeException: 1,
  jl_Exception: 1,
  jl_Throwable: 1,
  O: 1,
  Ljava_io_Serializable: 1
});
$c_ju_NoSuchElementException.prototype.$classData = $d_ju_NoSuchElementException;
/** @constructor */
function $c_s_MatchError() {
  $c_jl_RuntimeException.call(this);
  this.obj$4 = null;
  this.objString$4 = null;
  this.bitmap$0$4 = false
}
$c_s_MatchError.prototype = new $h_jl_RuntimeException();
$c_s_MatchError.prototype.constructor = $c_s_MatchError;
/** @constructor */
function $h_s_MatchError() {
  /*<skip>*/
}
$h_s_MatchError.prototype = $c_s_MatchError.prototype;
$c_s_MatchError.prototype.objString$lzycompute__p4__T = (function() {
  if ((!this.bitmap$0$4)) {
    this.objString$4 = ((this.obj$4 === null) ? "null" : this.liftedTree1$1__p4__T());
    this.bitmap$0$4 = true
  };
  return this.objString$4
});
$c_s_MatchError.prototype.ofClass$1__p4__T = (function() {
  var this$1 = this.obj$4;
  return ("of class " + $objectGetClass(this$1).getName__T())
});
$c_s_MatchError.prototype.liftedTree1$1__p4__T = (function() {
  try {
    return ((($objectToString(this.obj$4) + " (") + this.ofClass$1__p4__T()) + ")")
  } catch (e) {
    var e$2 = $m_sjsr_package$().wrapJavaScriptException__O__jl_Throwable(e);
    if ((e$2 !== null)) {
      return ("an instance " + this.ofClass$1__p4__T())
    } else {
      throw e
    }
  }
});
$c_s_MatchError.prototype.getMessage__T = (function() {
  return this.objString__p4__T()
});
$c_s_MatchError.prototype.objString__p4__T = (function() {
  return ((!this.bitmap$0$4) ? this.objString$lzycompute__p4__T() : this.objString$4)
});
$c_s_MatchError.prototype.init___O = (function(obj) {
  this.obj$4 = obj;
  $c_jl_Throwable.prototype.init___T__jl_Throwable.call(this, null, null);
  return this
});
var $d_s_MatchError = new $TypeData().initClass({
  s_MatchError: 0
}, false, "scala.MatchError", {
  s_MatchError: 1,
  jl_RuntimeException: 1,
  jl_Exception: 1,
  jl_Throwable: 1,
  O: 1,
  Ljava_io_Serializable: 1
});
$c_s_MatchError.prototype.$classData = $d_s_MatchError;
/** @constructor */
function $c_s_Option() {
  $c_O.call(this)
}
$c_s_Option.prototype = new $h_O();
$c_s_Option.prototype.constructor = $c_s_Option;
/** @constructor */
function $h_s_Option() {
  /*<skip>*/
}
$h_s_Option.prototype = $c_s_Option.prototype;
$c_s_Option.prototype.isDefined__Z = (function() {
  return (!this.isEmpty__Z())
});
function $is_s_Option(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.s_Option)))
}
function $as_s_Option(obj) {
  return (($is_s_Option(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "scala.Option"))
}
function $isArrayOf_s_Option(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.s_Option)))
}
function $asArrayOf_s_Option(obj, depth) {
  return (($isArrayOf_s_Option(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Lscala.Option;", depth))
}
/** @constructor */
function $c_s_Predef$$anon$1() {
  $c_s_Predef$$less$colon$less.call(this)
}
$c_s_Predef$$anon$1.prototype = new $h_s_Predef$$less$colon$less();
$c_s_Predef$$anon$1.prototype.constructor = $c_s_Predef$$anon$1;
/** @constructor */
function $h_s_Predef$$anon$1() {
  /*<skip>*/
}
$h_s_Predef$$anon$1.prototype = $c_s_Predef$$anon$1.prototype;
$c_s_Predef$$anon$1.prototype.init___ = (function() {
  return this
});
$c_s_Predef$$anon$1.prototype.apply__O__O = (function(x) {
  return x
});
var $d_s_Predef$$anon$1 = new $TypeData().initClass({
  s_Predef$$anon$1: 0
}, false, "scala.Predef$$anon$1", {
  s_Predef$$anon$1: 1,
  s_Predef$$less$colon$less: 1,
  O: 1,
  F1: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_s_Predef$$anon$1.prototype.$classData = $d_s_Predef$$anon$1;
/** @constructor */
function $c_s_Predef$$anon$2() {
  $c_s_Predef$$eq$colon$eq.call(this)
}
$c_s_Predef$$anon$2.prototype = new $h_s_Predef$$eq$colon$eq();
$c_s_Predef$$anon$2.prototype.constructor = $c_s_Predef$$anon$2;
/** @constructor */
function $h_s_Predef$$anon$2() {
  /*<skip>*/
}
$h_s_Predef$$anon$2.prototype = $c_s_Predef$$anon$2.prototype;
$c_s_Predef$$anon$2.prototype.init___ = (function() {
  return this
});
$c_s_Predef$$anon$2.prototype.apply__O__O = (function(x) {
  return x
});
var $d_s_Predef$$anon$2 = new $TypeData().initClass({
  s_Predef$$anon$2: 0
}, false, "scala.Predef$$anon$2", {
  s_Predef$$anon$2: 1,
  s_Predef$$eq$colon$eq: 1,
  O: 1,
  F1: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_s_Predef$$anon$2.prototype.$classData = $d_s_Predef$$anon$2;
/** @constructor */
function $c_s_StringContext() {
  $c_O.call(this);
  this.parts$1 = null
}
$c_s_StringContext.prototype = new $h_O();
$c_s_StringContext.prototype.constructor = $c_s_StringContext;
/** @constructor */
function $h_s_StringContext() {
  /*<skip>*/
}
$h_s_StringContext.prototype = $c_s_StringContext.prototype;
$c_s_StringContext.prototype.productPrefix__T = (function() {
  return "StringContext"
});
$c_s_StringContext.prototype.productArity__I = (function() {
  return 1
});
$c_s_StringContext.prototype.equals__O__Z = (function(x$1) {
  if ((this === x$1)) {
    return true
  } else if ($is_s_StringContext(x$1)) {
    var StringContext$1 = $as_s_StringContext(x$1);
    var x = this.parts$1;
    var x$2 = StringContext$1.parts$1;
    return ((x === null) ? (x$2 === null) : x.equals__O__Z(x$2))
  } else {
    return false
  }
});
$c_s_StringContext.prototype.productElement__I__O = (function(x$1) {
  switch (x$1) {
    case 0: {
      return this.parts$1;
      break
    }
    default: {
      throw new $c_jl_IndexOutOfBoundsException().init___T(("" + x$1))
    }
  }
});
$c_s_StringContext.prototype.toString__T = (function() {
  return $m_sr_ScalaRunTime$().$$undtoString__s_Product__T(this)
});
$c_s_StringContext.prototype.checkLengths__sc_Seq__V = (function(args) {
  if ((this.parts$1.length__I() !== ((1 + args.length__I()) | 0))) {
    throw new $c_jl_IllegalArgumentException().init___T((((("wrong number of arguments (" + args.length__I()) + ") for interpolated string with ") + this.parts$1.length__I()) + " parts"))
  }
});
$c_s_StringContext.prototype.s__sc_Seq__T = (function(args) {
  var f = (function($this) {
    return (function(str$2) {
      var str = $as_T(str$2);
      var this$1 = $m_s_StringContext$();
      return this$1.treatEscapes0__p1__T__Z__T(str, false)
    })
  })(this);
  this.checkLengths__sc_Seq__V(args);
  var pi = this.parts$1.iterator__sc_Iterator();
  var ai = args.iterator__sc_Iterator();
  var arg1 = pi.next__O();
  var bldr = new $c_jl_StringBuilder().init___T($as_T(f(arg1)));
  while (ai.hasNext__Z()) {
    bldr.append__O__jl_StringBuilder(ai.next__O());
    var arg1$1 = pi.next__O();
    bldr.append__T__jl_StringBuilder($as_T(f(arg1$1)))
  };
  return bldr.content$1
});
$c_s_StringContext.prototype.init___sc_Seq = (function(parts) {
  this.parts$1 = parts;
  return this
});
$c_s_StringContext.prototype.hashCode__I = (function() {
  var this$2 = $m_s_util_hashing_MurmurHash3$();
  return this$2.productHash__s_Product__I__I(this, (-889275714))
});
$c_s_StringContext.prototype.productIterator__sc_Iterator = (function() {
  return new $c_sr_ScalaRunTime$$anon$1().init___s_Product(this)
});
function $is_s_StringContext(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.s_StringContext)))
}
function $as_s_StringContext(obj) {
  return (($is_s_StringContext(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "scala.StringContext"))
}
function $isArrayOf_s_StringContext(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.s_StringContext)))
}
function $asArrayOf_s_StringContext(obj, depth) {
  return (($isArrayOf_s_StringContext(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Lscala.StringContext;", depth))
}
var $d_s_StringContext = new $TypeData().initClass({
  s_StringContext: 0
}, false, "scala.StringContext", {
  s_StringContext: 1,
  O: 1,
  s_Product: 1,
  s_Equals: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_s_StringContext.prototype.$classData = $d_s_StringContext;
/** @constructor */
function $c_s_util_control_BreakControl() {
  $c_jl_Throwable.call(this)
}
$c_s_util_control_BreakControl.prototype = new $h_jl_Throwable();
$c_s_util_control_BreakControl.prototype.constructor = $c_s_util_control_BreakControl;
/** @constructor */
function $h_s_util_control_BreakControl() {
  /*<skip>*/
}
$h_s_util_control_BreakControl.prototype = $c_s_util_control_BreakControl.prototype;
$c_s_util_control_BreakControl.prototype.init___ = (function() {
  $c_jl_Throwable.prototype.init___T__jl_Throwable.call(this, null, null);
  return this
});
$c_s_util_control_BreakControl.prototype.fillInStackTrace__jl_Throwable = (function() {
  return $s_s_util_control_NoStackTrace$class__fillInStackTrace__s_util_control_NoStackTrace__jl_Throwable(this)
});
var $d_s_util_control_BreakControl = new $TypeData().initClass({
  s_util_control_BreakControl: 0
}, false, "scala.util.control.BreakControl", {
  s_util_control_BreakControl: 1,
  jl_Throwable: 1,
  O: 1,
  Ljava_io_Serializable: 1,
  s_util_control_ControlThrowable: 1,
  s_util_control_NoStackTrace: 1
});
$c_s_util_control_BreakControl.prototype.$classData = $d_s_util_control_BreakControl;
/** @constructor */
function $c_sc_Iterable$() {
  $c_scg_GenTraversableFactory.call(this)
}
$c_sc_Iterable$.prototype = new $h_scg_GenTraversableFactory();
$c_sc_Iterable$.prototype.constructor = $c_sc_Iterable$;
/** @constructor */
function $h_sc_Iterable$() {
  /*<skip>*/
}
$h_sc_Iterable$.prototype = $c_sc_Iterable$.prototype;
$c_sc_Iterable$.prototype.init___ = (function() {
  $c_scg_GenTraversableFactory.prototype.init___.call(this);
  return this
});
var $d_sc_Iterable$ = new $TypeData().initClass({
  sc_Iterable$: 0
}, false, "scala.collection.Iterable$", {
  sc_Iterable$: 1,
  scg_GenTraversableFactory: 1,
  scg_GenericCompanion: 1,
  O: 1,
  scg_TraversableFactory: 1,
  scg_GenericSeqCompanion: 1
});
$c_sc_Iterable$.prototype.$classData = $d_sc_Iterable$;
var $n_sc_Iterable$ = (void 0);
function $m_sc_Iterable$() {
  if ((!$n_sc_Iterable$)) {
    $n_sc_Iterable$ = new $c_sc_Iterable$().init___()
  };
  return $n_sc_Iterable$
}
/** @constructor */
function $c_sc_Iterator$$anon$2() {
  $c_sc_AbstractIterator.call(this)
}
$c_sc_Iterator$$anon$2.prototype = new $h_sc_AbstractIterator();
$c_sc_Iterator$$anon$2.prototype.constructor = $c_sc_Iterator$$anon$2;
/** @constructor */
function $h_sc_Iterator$$anon$2() {
  /*<skip>*/
}
$h_sc_Iterator$$anon$2.prototype = $c_sc_Iterator$$anon$2.prototype;
$c_sc_Iterator$$anon$2.prototype.init___ = (function() {
  return this
});
$c_sc_Iterator$$anon$2.prototype.next__O = (function() {
  this.next__sr_Nothing$()
});
$c_sc_Iterator$$anon$2.prototype.next__sr_Nothing$ = (function() {
  throw new $c_ju_NoSuchElementException().init___T("next on empty iterator")
});
$c_sc_Iterator$$anon$2.prototype.hasNext__Z = (function() {
  return false
});
var $d_sc_Iterator$$anon$2 = new $TypeData().initClass({
  sc_Iterator$$anon$2: 0
}, false, "scala.collection.Iterator$$anon$2", {
  sc_Iterator$$anon$2: 1,
  sc_AbstractIterator: 1,
  O: 1,
  sc_Iterator: 1,
  sc_TraversableOnce: 1,
  sc_GenTraversableOnce: 1
});
$c_sc_Iterator$$anon$2.prototype.$classData = $d_sc_Iterator$$anon$2;
/** @constructor */
function $c_sc_LinearSeqLike$$anon$1() {
  $c_sc_AbstractIterator.call(this);
  this.these$2 = null
}
$c_sc_LinearSeqLike$$anon$1.prototype = new $h_sc_AbstractIterator();
$c_sc_LinearSeqLike$$anon$1.prototype.constructor = $c_sc_LinearSeqLike$$anon$1;
/** @constructor */
function $h_sc_LinearSeqLike$$anon$1() {
  /*<skip>*/
}
$h_sc_LinearSeqLike$$anon$1.prototype = $c_sc_LinearSeqLike$$anon$1.prototype;
$c_sc_LinearSeqLike$$anon$1.prototype.init___sc_LinearSeqLike = (function($$outer) {
  this.these$2 = $$outer;
  return this
});
$c_sc_LinearSeqLike$$anon$1.prototype.next__O = (function() {
  if (this.hasNext__Z()) {
    var result = this.these$2.head__O();
    var this$1 = this.these$2;
    this.these$2 = this$1.tail__sci_List();
    return result
  } else {
    return $m_sc_Iterator$().empty$1.next__O()
  }
});
$c_sc_LinearSeqLike$$anon$1.prototype.hasNext__Z = (function() {
  return (!this.these$2.isEmpty__Z())
});
var $d_sc_LinearSeqLike$$anon$1 = new $TypeData().initClass({
  sc_LinearSeqLike$$anon$1: 0
}, false, "scala.collection.LinearSeqLike$$anon$1", {
  sc_LinearSeqLike$$anon$1: 1,
  sc_AbstractIterator: 1,
  O: 1,
  sc_Iterator: 1,
  sc_TraversableOnce: 1,
  sc_GenTraversableOnce: 1
});
$c_sc_LinearSeqLike$$anon$1.prototype.$classData = $d_sc_LinearSeqLike$$anon$1;
/** @constructor */
function $c_sc_Traversable$() {
  $c_scg_GenTraversableFactory.call(this);
  this.breaks$3 = null
}
$c_sc_Traversable$.prototype = new $h_scg_GenTraversableFactory();
$c_sc_Traversable$.prototype.constructor = $c_sc_Traversable$;
/** @constructor */
function $h_sc_Traversable$() {
  /*<skip>*/
}
$h_sc_Traversable$.prototype = $c_sc_Traversable$.prototype;
$c_sc_Traversable$.prototype.init___ = (function() {
  $c_scg_GenTraversableFactory.prototype.init___.call(this);
  $n_sc_Traversable$ = this;
  this.breaks$3 = new $c_s_util_control_Breaks().init___();
  return this
});
var $d_sc_Traversable$ = new $TypeData().initClass({
  sc_Traversable$: 0
}, false, "scala.collection.Traversable$", {
  sc_Traversable$: 1,
  scg_GenTraversableFactory: 1,
  scg_GenericCompanion: 1,
  O: 1,
  scg_TraversableFactory: 1,
  scg_GenericSeqCompanion: 1
});
$c_sc_Traversable$.prototype.$classData = $d_sc_Traversable$;
var $n_sc_Traversable$ = (void 0);
function $m_sc_Traversable$() {
  if ((!$n_sc_Traversable$)) {
    $n_sc_Traversable$ = new $c_sc_Traversable$().init___()
  };
  return $n_sc_Traversable$
}
/** @constructor */
function $c_scg_ImmutableSetFactory() {
  $c_scg_SetFactory.call(this)
}
$c_scg_ImmutableSetFactory.prototype = new $h_scg_SetFactory();
$c_scg_ImmutableSetFactory.prototype.constructor = $c_scg_ImmutableSetFactory;
/** @constructor */
function $h_scg_ImmutableSetFactory() {
  /*<skip>*/
}
$h_scg_ImmutableSetFactory.prototype = $c_scg_ImmutableSetFactory.prototype;
/** @constructor */
function $c_sr_ScalaRunTime$$anon$1() {
  $c_sc_AbstractIterator.call(this);
  this.c$2 = 0;
  this.cmax$2 = 0;
  this.x$2$2 = null
}
$c_sr_ScalaRunTime$$anon$1.prototype = new $h_sc_AbstractIterator();
$c_sr_ScalaRunTime$$anon$1.prototype.constructor = $c_sr_ScalaRunTime$$anon$1;
/** @constructor */
function $h_sr_ScalaRunTime$$anon$1() {
  /*<skip>*/
}
$h_sr_ScalaRunTime$$anon$1.prototype = $c_sr_ScalaRunTime$$anon$1.prototype;
$c_sr_ScalaRunTime$$anon$1.prototype.next__O = (function() {
  var result = this.x$2$2.productElement__I__O(this.c$2);
  this.c$2 = ((1 + this.c$2) | 0);
  return result
});
$c_sr_ScalaRunTime$$anon$1.prototype.init___s_Product = (function(x$2) {
  this.x$2$2 = x$2;
  this.c$2 = 0;
  this.cmax$2 = x$2.productArity__I();
  return this
});
$c_sr_ScalaRunTime$$anon$1.prototype.hasNext__Z = (function() {
  return (this.c$2 < this.cmax$2)
});
var $d_sr_ScalaRunTime$$anon$1 = new $TypeData().initClass({
  sr_ScalaRunTime$$anon$1: 0
}, false, "scala.runtime.ScalaRunTime$$anon$1", {
  sr_ScalaRunTime$$anon$1: 1,
  sc_AbstractIterator: 1,
  O: 1,
  sc_Iterator: 1,
  sc_TraversableOnce: 1,
  sc_GenTraversableOnce: 1
});
$c_sr_ScalaRunTime$$anon$1.prototype.$classData = $d_sr_ScalaRunTime$$anon$1;
/** @constructor */
function $c_Ljapgolly_scalajs_react_ComponentDidUpdate() {
  $c_Ljapgolly_scalajs_react_LifecycleInput.call(this);
  this.$$$2 = null;
  this.prevProps$2 = null;
  this.prevState$2 = null
}
$c_Ljapgolly_scalajs_react_ComponentDidUpdate.prototype = new $h_Ljapgolly_scalajs_react_LifecycleInput();
$c_Ljapgolly_scalajs_react_ComponentDidUpdate.prototype.constructor = $c_Ljapgolly_scalajs_react_ComponentDidUpdate;
/** @constructor */
function $h_Ljapgolly_scalajs_react_ComponentDidUpdate() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_ComponentDidUpdate.prototype = $c_Ljapgolly_scalajs_react_ComponentDidUpdate.prototype;
$c_Ljapgolly_scalajs_react_ComponentDidUpdate.prototype.productPrefix__T = (function() {
  return "ComponentDidUpdate"
});
$c_Ljapgolly_scalajs_react_ComponentDidUpdate.prototype.productArity__I = (function() {
  return 3
});
$c_Ljapgolly_scalajs_react_ComponentDidUpdate.prototype.equals__O__Z = (function(x$1) {
  if ((this === x$1)) {
    return true
  } else if ($is_Ljapgolly_scalajs_react_ComponentDidUpdate(x$1)) {
    var ComponentDidUpdate$1 = $as_Ljapgolly_scalajs_react_ComponentDidUpdate(x$1);
    return (($m_sr_BoxesRunTime$().equals__O__O__Z(this.$$$2, ComponentDidUpdate$1.$$$2) && $m_sr_BoxesRunTime$().equals__O__O__Z(this.prevProps$2, ComponentDidUpdate$1.prevProps$2)) && $m_sr_BoxesRunTime$().equals__O__O__Z(this.prevState$2, ComponentDidUpdate$1.prevState$2))
  } else {
    return false
  }
});
$c_Ljapgolly_scalajs_react_ComponentDidUpdate.prototype.productElement__I__O = (function(x$1) {
  switch (x$1) {
    case 0: {
      return this.$$$2;
      break
    }
    case 1: {
      return this.prevProps$2;
      break
    }
    case 2: {
      return this.prevState$2;
      break
    }
    default: {
      throw new $c_jl_IndexOutOfBoundsException().init___T(("" + x$1))
    }
  }
});
$c_Ljapgolly_scalajs_react_ComponentDidUpdate.prototype.toString__T = (function() {
  return $m_sr_ScalaRunTime$().$$undtoString__s_Product__T(this)
});
$c_Ljapgolly_scalajs_react_ComponentDidUpdate.prototype.hashCode__I = (function() {
  var this$2 = $m_s_util_hashing_MurmurHash3$();
  return this$2.productHash__s_Product__I__I(this, (-889275714))
});
$c_Ljapgolly_scalajs_react_ComponentDidUpdate.prototype.productIterator__sc_Iterator = (function() {
  return new $c_sr_ScalaRunTime$$anon$1().init___s_Product(this)
});
$c_Ljapgolly_scalajs_react_ComponentDidUpdate.prototype.init___Ljapgolly_scalajs_react_CompScope$DuringCallbackM__O__O = (function($$, prevProps, prevState) {
  this.$$$2 = $$;
  this.prevProps$2 = prevProps;
  this.prevState$2 = prevState;
  return this
});
function $is_Ljapgolly_scalajs_react_ComponentDidUpdate(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.Ljapgolly_scalajs_react_ComponentDidUpdate)))
}
function $as_Ljapgolly_scalajs_react_ComponentDidUpdate(obj) {
  return (($is_Ljapgolly_scalajs_react_ComponentDidUpdate(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "japgolly.scalajs.react.ComponentDidUpdate"))
}
function $isArrayOf_Ljapgolly_scalajs_react_ComponentDidUpdate(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.Ljapgolly_scalajs_react_ComponentDidUpdate)))
}
function $asArrayOf_Ljapgolly_scalajs_react_ComponentDidUpdate(obj, depth) {
  return (($isArrayOf_Ljapgolly_scalajs_react_ComponentDidUpdate(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Ljapgolly.scalajs.react.ComponentDidUpdate;", depth))
}
var $d_Ljapgolly_scalajs_react_ComponentDidUpdate = new $TypeData().initClass({
  Ljapgolly_scalajs_react_ComponentDidUpdate: 0
}, false, "japgolly.scalajs.react.ComponentDidUpdate", {
  Ljapgolly_scalajs_react_ComponentDidUpdate: 1,
  Ljapgolly_scalajs_react_LifecycleInput: 1,
  O: 1,
  s_Product: 1,
  s_Equals: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_Ljapgolly_scalajs_react_ComponentDidUpdate.prototype.$classData = $d_Ljapgolly_scalajs_react_ComponentDidUpdate;
/** @constructor */
function $c_Ljapgolly_scalajs_react_ComponentWillReceiveProps() {
  $c_Ljapgolly_scalajs_react_LifecycleInput.call(this);
  this.$$$2 = null;
  this.nextProps$2 = null
}
$c_Ljapgolly_scalajs_react_ComponentWillReceiveProps.prototype = new $h_Ljapgolly_scalajs_react_LifecycleInput();
$c_Ljapgolly_scalajs_react_ComponentWillReceiveProps.prototype.constructor = $c_Ljapgolly_scalajs_react_ComponentWillReceiveProps;
/** @constructor */
function $h_Ljapgolly_scalajs_react_ComponentWillReceiveProps() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_ComponentWillReceiveProps.prototype = $c_Ljapgolly_scalajs_react_ComponentWillReceiveProps.prototype;
$c_Ljapgolly_scalajs_react_ComponentWillReceiveProps.prototype.productPrefix__T = (function() {
  return "ComponentWillReceiveProps"
});
$c_Ljapgolly_scalajs_react_ComponentWillReceiveProps.prototype.productArity__I = (function() {
  return 2
});
$c_Ljapgolly_scalajs_react_ComponentWillReceiveProps.prototype.equals__O__Z = (function(x$1) {
  if ((this === x$1)) {
    return true
  } else if ($is_Ljapgolly_scalajs_react_ComponentWillReceiveProps(x$1)) {
    var ComponentWillReceiveProps$1 = $as_Ljapgolly_scalajs_react_ComponentWillReceiveProps(x$1);
    return ($m_sr_BoxesRunTime$().equals__O__O__Z(this.$$$2, ComponentWillReceiveProps$1.$$$2) && $m_sr_BoxesRunTime$().equals__O__O__Z(this.nextProps$2, ComponentWillReceiveProps$1.nextProps$2))
  } else {
    return false
  }
});
$c_Ljapgolly_scalajs_react_ComponentWillReceiveProps.prototype.productElement__I__O = (function(x$1) {
  switch (x$1) {
    case 0: {
      return this.$$$2;
      break
    }
    case 1: {
      return this.nextProps$2;
      break
    }
    default: {
      throw new $c_jl_IndexOutOfBoundsException().init___T(("" + x$1))
    }
  }
});
$c_Ljapgolly_scalajs_react_ComponentWillReceiveProps.prototype.toString__T = (function() {
  return $m_sr_ScalaRunTime$().$$undtoString__s_Product__T(this)
});
$c_Ljapgolly_scalajs_react_ComponentWillReceiveProps.prototype.hashCode__I = (function() {
  var this$2 = $m_s_util_hashing_MurmurHash3$();
  return this$2.productHash__s_Product__I__I(this, (-889275714))
});
$c_Ljapgolly_scalajs_react_ComponentWillReceiveProps.prototype.productIterator__sc_Iterator = (function() {
  return new $c_sr_ScalaRunTime$$anon$1().init___s_Product(this)
});
$c_Ljapgolly_scalajs_react_ComponentWillReceiveProps.prototype.init___Ljapgolly_scalajs_react_CompScope$DuringCallbackM__O = (function($$, nextProps) {
  this.$$$2 = $$;
  this.nextProps$2 = nextProps;
  return this
});
function $is_Ljapgolly_scalajs_react_ComponentWillReceiveProps(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.Ljapgolly_scalajs_react_ComponentWillReceiveProps)))
}
function $as_Ljapgolly_scalajs_react_ComponentWillReceiveProps(obj) {
  return (($is_Ljapgolly_scalajs_react_ComponentWillReceiveProps(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "japgolly.scalajs.react.ComponentWillReceiveProps"))
}
function $isArrayOf_Ljapgolly_scalajs_react_ComponentWillReceiveProps(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.Ljapgolly_scalajs_react_ComponentWillReceiveProps)))
}
function $asArrayOf_Ljapgolly_scalajs_react_ComponentWillReceiveProps(obj, depth) {
  return (($isArrayOf_Ljapgolly_scalajs_react_ComponentWillReceiveProps(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Ljapgolly.scalajs.react.ComponentWillReceiveProps;", depth))
}
var $d_Ljapgolly_scalajs_react_ComponentWillReceiveProps = new $TypeData().initClass({
  Ljapgolly_scalajs_react_ComponentWillReceiveProps: 0
}, false, "japgolly.scalajs.react.ComponentWillReceiveProps", {
  Ljapgolly_scalajs_react_ComponentWillReceiveProps: 1,
  Ljapgolly_scalajs_react_LifecycleInput: 1,
  O: 1,
  s_Product: 1,
  s_Equals: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_Ljapgolly_scalajs_react_ComponentWillReceiveProps.prototype.$classData = $d_Ljapgolly_scalajs_react_ComponentWillReceiveProps;
/** @constructor */
function $c_Ljapgolly_scalajs_react_ComponentWillUpdate() {
  $c_Ljapgolly_scalajs_react_LifecycleInput.call(this);
  this.$$$2 = null;
  this.nextProps$2 = null;
  this.nextState$2 = null
}
$c_Ljapgolly_scalajs_react_ComponentWillUpdate.prototype = new $h_Ljapgolly_scalajs_react_LifecycleInput();
$c_Ljapgolly_scalajs_react_ComponentWillUpdate.prototype.constructor = $c_Ljapgolly_scalajs_react_ComponentWillUpdate;
/** @constructor */
function $h_Ljapgolly_scalajs_react_ComponentWillUpdate() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_ComponentWillUpdate.prototype = $c_Ljapgolly_scalajs_react_ComponentWillUpdate.prototype;
$c_Ljapgolly_scalajs_react_ComponentWillUpdate.prototype.init___Ljapgolly_scalajs_react_CompScope$WillUpdate__O__O = (function($$, nextProps, nextState) {
  this.$$$2 = $$;
  this.nextProps$2 = nextProps;
  this.nextState$2 = nextState;
  return this
});
$c_Ljapgolly_scalajs_react_ComponentWillUpdate.prototype.productPrefix__T = (function() {
  return "ComponentWillUpdate"
});
$c_Ljapgolly_scalajs_react_ComponentWillUpdate.prototype.productArity__I = (function() {
  return 3
});
$c_Ljapgolly_scalajs_react_ComponentWillUpdate.prototype.equals__O__Z = (function(x$1) {
  if ((this === x$1)) {
    return true
  } else if ($is_Ljapgolly_scalajs_react_ComponentWillUpdate(x$1)) {
    var ComponentWillUpdate$1 = $as_Ljapgolly_scalajs_react_ComponentWillUpdate(x$1);
    return (($m_sr_BoxesRunTime$().equals__O__O__Z(this.$$$2, ComponentWillUpdate$1.$$$2) && $m_sr_BoxesRunTime$().equals__O__O__Z(this.nextProps$2, ComponentWillUpdate$1.nextProps$2)) && $m_sr_BoxesRunTime$().equals__O__O__Z(this.nextState$2, ComponentWillUpdate$1.nextState$2))
  } else {
    return false
  }
});
$c_Ljapgolly_scalajs_react_ComponentWillUpdate.prototype.productElement__I__O = (function(x$1) {
  switch (x$1) {
    case 0: {
      return this.$$$2;
      break
    }
    case 1: {
      return this.nextProps$2;
      break
    }
    case 2: {
      return this.nextState$2;
      break
    }
    default: {
      throw new $c_jl_IndexOutOfBoundsException().init___T(("" + x$1))
    }
  }
});
$c_Ljapgolly_scalajs_react_ComponentWillUpdate.prototype.toString__T = (function() {
  return $m_sr_ScalaRunTime$().$$undtoString__s_Product__T(this)
});
$c_Ljapgolly_scalajs_react_ComponentWillUpdate.prototype.hashCode__I = (function() {
  var this$2 = $m_s_util_hashing_MurmurHash3$();
  return this$2.productHash__s_Product__I__I(this, (-889275714))
});
$c_Ljapgolly_scalajs_react_ComponentWillUpdate.prototype.productIterator__sc_Iterator = (function() {
  return new $c_sr_ScalaRunTime$$anon$1().init___s_Product(this)
});
function $is_Ljapgolly_scalajs_react_ComponentWillUpdate(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.Ljapgolly_scalajs_react_ComponentWillUpdate)))
}
function $as_Ljapgolly_scalajs_react_ComponentWillUpdate(obj) {
  return (($is_Ljapgolly_scalajs_react_ComponentWillUpdate(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "japgolly.scalajs.react.ComponentWillUpdate"))
}
function $isArrayOf_Ljapgolly_scalajs_react_ComponentWillUpdate(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.Ljapgolly_scalajs_react_ComponentWillUpdate)))
}
function $asArrayOf_Ljapgolly_scalajs_react_ComponentWillUpdate(obj, depth) {
  return (($isArrayOf_Ljapgolly_scalajs_react_ComponentWillUpdate(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Ljapgolly.scalajs.react.ComponentWillUpdate;", depth))
}
var $d_Ljapgolly_scalajs_react_ComponentWillUpdate = new $TypeData().initClass({
  Ljapgolly_scalajs_react_ComponentWillUpdate: 0
}, false, "japgolly.scalajs.react.ComponentWillUpdate", {
  Ljapgolly_scalajs_react_ComponentWillUpdate: 1,
  Ljapgolly_scalajs_react_LifecycleInput: 1,
  O: 1,
  s_Product: 1,
  s_Equals: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_Ljapgolly_scalajs_react_ComponentWillUpdate.prototype.$classData = $d_Ljapgolly_scalajs_react_ComponentWillUpdate;
/** @constructor */
function $c_Ljapgolly_scalajs_react_ShouldComponentUpdate() {
  $c_Ljapgolly_scalajs_react_LifecycleInput.call(this);
  this.$$$2 = null;
  this.nextProps$2 = null;
  this.nextState$2 = null
}
$c_Ljapgolly_scalajs_react_ShouldComponentUpdate.prototype = new $h_Ljapgolly_scalajs_react_LifecycleInput();
$c_Ljapgolly_scalajs_react_ShouldComponentUpdate.prototype.constructor = $c_Ljapgolly_scalajs_react_ShouldComponentUpdate;
/** @constructor */
function $h_Ljapgolly_scalajs_react_ShouldComponentUpdate() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_ShouldComponentUpdate.prototype = $c_Ljapgolly_scalajs_react_ShouldComponentUpdate.prototype;
$c_Ljapgolly_scalajs_react_ShouldComponentUpdate.prototype.productPrefix__T = (function() {
  return "ShouldComponentUpdate"
});
$c_Ljapgolly_scalajs_react_ShouldComponentUpdate.prototype.productArity__I = (function() {
  return 3
});
$c_Ljapgolly_scalajs_react_ShouldComponentUpdate.prototype.equals__O__Z = (function(x$1) {
  if ((this === x$1)) {
    return true
  } else if ($is_Ljapgolly_scalajs_react_ShouldComponentUpdate(x$1)) {
    var ShouldComponentUpdate$1 = $as_Ljapgolly_scalajs_react_ShouldComponentUpdate(x$1);
    return (($m_sr_BoxesRunTime$().equals__O__O__Z(this.$$$2, ShouldComponentUpdate$1.$$$2) && $m_sr_BoxesRunTime$().equals__O__O__Z(this.nextProps$2, ShouldComponentUpdate$1.nextProps$2)) && $m_sr_BoxesRunTime$().equals__O__O__Z(this.nextState$2, ShouldComponentUpdate$1.nextState$2))
  } else {
    return false
  }
});
$c_Ljapgolly_scalajs_react_ShouldComponentUpdate.prototype.productElement__I__O = (function(x$1) {
  switch (x$1) {
    case 0: {
      return this.$$$2;
      break
    }
    case 1: {
      return this.nextProps$2;
      break
    }
    case 2: {
      return this.nextState$2;
      break
    }
    default: {
      throw new $c_jl_IndexOutOfBoundsException().init___T(("" + x$1))
    }
  }
});
$c_Ljapgolly_scalajs_react_ShouldComponentUpdate.prototype.toString__T = (function() {
  return $m_sr_ScalaRunTime$().$$undtoString__s_Product__T(this)
});
$c_Ljapgolly_scalajs_react_ShouldComponentUpdate.prototype.hashCode__I = (function() {
  var this$2 = $m_s_util_hashing_MurmurHash3$();
  return this$2.productHash__s_Product__I__I(this, (-889275714))
});
$c_Ljapgolly_scalajs_react_ShouldComponentUpdate.prototype.productIterator__sc_Iterator = (function() {
  return new $c_sr_ScalaRunTime$$anon$1().init___s_Product(this)
});
$c_Ljapgolly_scalajs_react_ShouldComponentUpdate.prototype.init___Ljapgolly_scalajs_react_CompScope$DuringCallbackM__O__O = (function($$, nextProps, nextState) {
  this.$$$2 = $$;
  this.nextProps$2 = nextProps;
  this.nextState$2 = nextState;
  return this
});
function $is_Ljapgolly_scalajs_react_ShouldComponentUpdate(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.Ljapgolly_scalajs_react_ShouldComponentUpdate)))
}
function $as_Ljapgolly_scalajs_react_ShouldComponentUpdate(obj) {
  return (($is_Ljapgolly_scalajs_react_ShouldComponentUpdate(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "japgolly.scalajs.react.ShouldComponentUpdate"))
}
function $isArrayOf_Ljapgolly_scalajs_react_ShouldComponentUpdate(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.Ljapgolly_scalajs_react_ShouldComponentUpdate)))
}
function $asArrayOf_Ljapgolly_scalajs_react_ShouldComponentUpdate(obj, depth) {
  return (($isArrayOf_Ljapgolly_scalajs_react_ShouldComponentUpdate(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Ljapgolly.scalajs.react.ShouldComponentUpdate;", depth))
}
var $d_Ljapgolly_scalajs_react_ShouldComponentUpdate = new $TypeData().initClass({
  Ljapgolly_scalajs_react_ShouldComponentUpdate: 0
}, false, "japgolly.scalajs.react.ShouldComponentUpdate", {
  Ljapgolly_scalajs_react_ShouldComponentUpdate: 1,
  Ljapgolly_scalajs_react_LifecycleInput: 1,
  O: 1,
  s_Product: 1,
  s_Equals: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_Ljapgolly_scalajs_react_ShouldComponentUpdate.prototype.$classData = $d_Ljapgolly_scalajs_react_ShouldComponentUpdate;
/** @constructor */
function $c_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic() {
  $c_O.call(this);
  this.name$1 = null
}
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic.prototype.constructor = $c_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic;
/** @constructor */
function $h_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic.prototype = $c_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic.prototype;
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic.prototype.productPrefix__T = (function() {
  return "Generic"
});
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic.prototype.productArity__I = (function() {
  return 1
});
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic.prototype.equals__O__Z = (function(x$1) {
  if ((this === x$1)) {
    return true
  } else if ($is_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic(x$1)) {
    var Generic$1 = $as_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic(x$1);
    return (this.name$1 === Generic$1.name$1)
  } else {
    return false
  }
});
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic.prototype.$$colon$eq__O__F2__Ljapgolly_scalajs_react_vdom_TagMod = (function(a, t) {
  return new $c_Ljapgolly_scalajs_react_vdom_ReactAttr$NameAndValue().init___T__O__F2(this.name$1, a, t)
});
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic.prototype.productElement__I__O = (function(x$1) {
  switch (x$1) {
    case 0: {
      return this.name$1;
      break
    }
    default: {
      throw new $c_jl_IndexOutOfBoundsException().init___T(("" + x$1))
    }
  }
});
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic.prototype.toString__T = (function() {
  return $m_sr_ScalaRunTime$().$$undtoString__s_Product__T(this)
});
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic.prototype.init___T = (function(name) {
  this.name$1 = name;
  $m_Ljapgolly_scalajs_react_vdom_Escaping$().assertValidAttrName__T__V(name);
  return this
});
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic.prototype.hashCode__I = (function() {
  var this$2 = $m_s_util_hashing_MurmurHash3$();
  return this$2.productHash__s_Product__I__I(this, (-889275714))
});
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic.prototype.productIterator__sc_Iterator = (function() {
  return new $c_sr_ScalaRunTime$$anon$1().init___s_Product(this)
});
function $is_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.Ljapgolly_scalajs_react_vdom_ReactAttr$Generic)))
}
function $as_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic(obj) {
  return (($is_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "japgolly.scalajs.react.vdom.ReactAttr$Generic"))
}
function $isArrayOf_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.Ljapgolly_scalajs_react_vdom_ReactAttr$Generic)))
}
function $asArrayOf_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic(obj, depth) {
  return (($isArrayOf_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Ljapgolly.scalajs.react.vdom.ReactAttr$Generic;", depth))
}
var $d_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic = new $TypeData().initClass({
  Ljapgolly_scalajs_react_vdom_ReactAttr$Generic: 0
}, false, "japgolly.scalajs.react.vdom.ReactAttr$Generic", {
  Ljapgolly_scalajs_react_vdom_ReactAttr$Generic: 1,
  O: 1,
  Ljapgolly_scalajs_react_vdom_ReactAttr: 1,
  s_Product: 1,
  s_Equals: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic.prototype.$classData = $d_Ljapgolly_scalajs_react_vdom_ReactAttr$Generic;
/** @constructor */
function $c_Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$() {
  $c_O.call(this)
}
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$.prototype.constructor = $c_Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$;
/** @constructor */
function $h_Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$.prototype = $c_Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$.prototype;
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$.prototype.init___ = (function() {
  return this
});
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$.prototype.productPrefix__T = (function() {
  return "Ref"
});
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$.prototype.productArity__I = (function() {
  return 0
});
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$.prototype.$$colon$eq__O__F2__Ljapgolly_scalajs_react_vdom_TagMod = (function(a, t) {
  return new $c_Ljapgolly_scalajs_react_vdom_ReactAttr$NameAndValue().init___T__O__F2("ref", a, t)
});
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$.prototype.productElement__I__O = (function(x$1) {
  throw new $c_jl_IndexOutOfBoundsException().init___T(("" + x$1))
});
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$.prototype.toString__T = (function() {
  return "Ref"
});
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$.prototype.hashCode__I = (function() {
  return 82035
});
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$.prototype.productIterator__sc_Iterator = (function() {
  return new $c_sr_ScalaRunTime$$anon$1().init___s_Product(this)
});
var $d_Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$ = new $TypeData().initClass({
  Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$: 0
}, false, "japgolly.scalajs.react.vdom.ReactAttr$Ref$", {
  Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$: 1,
  O: 1,
  Ljapgolly_scalajs_react_vdom_ReactAttr: 1,
  s_Product: 1,
  s_Equals: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$.prototype.$classData = $d_Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$;
var $n_Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$ = (void 0);
function $m_Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$() {
  if ((!$n_Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$)) {
    $n_Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$ = new $c_Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$().init___()
  };
  return $n_Ljapgolly_scalajs_react_vdom_ReactAttr$Ref$
}
/** @constructor */
function $c_Ljapgolly_scalajs_react_vdom_package$Attrs$() {
  $c_O.call(this);
  this.background$1 = null;
  this.backgroundRepeat$1 = null;
  this.backgroundPosition$1 = null;
  this.backgroundColor$1 = null;
  this.backgroundImage$1 = null;
  this.borderTopColor$1 = null;
  this.borderStyle$1 = null;
  this.borderTopStyle$1 = null;
  this.borderRightStyle$1 = null;
  this.borderRightWidth$1 = null;
  this.borderTopRightRadius$1 = null;
  this.borderBottomLeftRadius$1 = null;
  this.borderRightColor$1 = null;
  this.borderBottom$1 = null;
  this.border$1 = null;
  this.borderBottomWidth$1 = null;
  this.borderLeftColor$1 = null;
  this.borderBottomColor$1 = null;
  this.borderLeft$1 = null;
  this.borderLeftStyle$1 = null;
  this.borderRight$1 = null;
  this.borderBottomStyle$1 = null;
  this.borderLeftWidth$1 = null;
  this.borderTopWidth$1 = null;
  this.borderTop$1 = null;
  this.borderRadius$1 = null;
  this.borderWidth$1 = null;
  this.borderBottomRightRadius$1 = null;
  this.borderTopLeftRadius$1 = null;
  this.borderColor$1 = null;
  this.opacity$1 = null;
  this.maxWidth$1 = null;
  this.overflow$1 = null;
  this.height$1 = null;
  this.paddingRight$1 = null;
  this.paddingTop$1 = null;
  this.paddingLeft$1 = null;
  this.padding$1 = null;
  this.paddingBottom$1 = null;
  this.right$1 = null;
  this.lineHeight$1 = null;
  this.left$1 = null;
  this.listStyle$1 = null;
  this.overflowY$1 = null;
  this.boxShadow$1 = null;
  this.fontSizeAdjust$1 = null;
  this.fontFamily$1 = null;
  this.font$1 = null;
  this.fontFeatureSettings$1 = null;
  this.marginBottom$1 = null;
  this.marginRight$1 = null;
  this.marginTop$1 = null;
  this.marginLeft$1 = null;
  this.top$1 = null;
  this.width$1 = null;
  this.bottom$1 = null;
  this.letterSpacing$1 = null;
  this.maxHeight$1 = null;
  this.minWidth$1 = null;
  this.minHeight$1 = null;
  this.outline$1 = null;
  this.outlineStyle$1 = null;
  this.overflowX$1 = null;
  this.textAlignLast$1 = null;
  this.textAlign$1 = null;
  this.textIndent$1 = null;
  this.textShadow$1 = null;
  this.wordSpacing$1 = null;
  this.zIndex$1 = null;
  this.animationDirection$1 = null;
  this.animationDuration$1 = null;
  this.animationName$1 = null;
  this.animationFillMode$1 = null;
  this.animationIterationCount$1 = null;
  this.animationDelay$1 = null;
  this.animationTimingFunction$1 = null;
  this.animationPlayState$1 = null;
  this.animation$1 = null;
  this.columnCount$1 = null;
  this.columnGap$1 = null;
  this.columnRule$1 = null;
  this.columnWidth$1 = null;
  this.columnRuleColor$1 = null;
  this.contentStyle$1 = null;
  this.counterIncrement$1 = null;
  this.counterReset$1 = null;
  this.orphans$1 = null;
  this.widows$1 = null;
  this.pageBreakAfter$1 = null;
  this.pageBreakInside$1 = null;
  this.pageBreakBefore$1 = null;
  this.perspective$1 = null;
  this.perspectiveOrigin$1 = null;
  this.transitionDelay$1 = null;
  this.transition$1 = null;
  this.transitionTimingFunction$1 = null;
  this.transitionDuration$1 = null;
  this.transitionProperty$1 = null;
  this.transform$1 = null;
  this.flex$1 = null;
  this.flexBasis$1 = null;
  this.flexGrow$1 = null;
  this.flexShrink$1 = null;
  this.transformOrigin$1 = null;
  this.className$1 = null;
  this.cls$1 = null;
  this.class$1 = null;
  this.colSpan$1 = null;
  this.rowSpan$1 = null;
  this.htmlFor$1 = null;
  this.ref$1 = null;
  this.key$1 = null;
  this.draggable$1 = null;
  this.onBeforeInput$1 = null;
  this.onCompositionEnd$1 = null;
  this.onCompositionStart$1 = null;
  this.onCompositionUpdate$1 = null;
  this.onContextMenu$1 = null;
  this.onCopy$1 = null;
  this.onCut$1 = null;
  this.onDrag$1 = null;
  this.onDragStart$1 = null;
  this.onDragEnd$1 = null;
  this.onDragEnter$1 = null;
  this.onDragOver$1 = null;
  this.onDragLeave$1 = null;
  this.onDragExit$1 = null;
  this.onDrop$1 = null;
  this.onInput$1 = null;
  this.onPaste$1 = null;
  this.onWheel$1 = null;
  this.acceptCharset$1 = null;
  this.accessKey$1 = null;
  this.allowFullScreen$1 = null;
  this.allowTransparency$1 = null;
  this.async$1 = null;
  this.autoCapitalize$1 = null;
  this.autoCorrect$1 = null;
  this.autoPlay$1 = null;
  this.cellPadding$1 = null;
  this.cellSpacing$1 = null;
  this.classID$1 = null;
  this.contentEditable$1 = null;
  this.contextMenu$1 = null;
  this.controls$1 = null;
  this.coords$1 = null;
  this.crossOrigin$1 = null;
  this.dateTime$1 = null;
  this.defer$1 = null;
  this.defaultValue$1 = null;
  this.dir$1 = null;
  this.download$1 = null;
  this.encType$1 = null;
  this.formAction$1 = null;
  this.formEncType$1 = null;
  this.formMethod$1 = null;
  this.formNoValidate$1 = null;
  this.formTarget$1 = null;
  this.frameBorder$1 = null;
  this.headers$1 = null;
  this.hrefLang$1 = null;
  this.icon$1 = null;
  this.itemProp$1 = null;
  this.itemScope$1 = null;
  this.itemType$1 = null;
  this.list$1 = null;
  this.loop$1 = null;
  this.manifest$1 = null;
  this.marginHeight$1 = null;
  this.marginWidth$1 = null;
  this.maxLength$1 = null;
  this.mediaGroup$1 = null;
  this.multiple$1 = null;
  this.muted$1 = null;
  this.noValidate$1 = null;
  this.open$1 = null;
  this.poster$1 = null;
  this.preload$1 = null;
  this.radioGroup$1 = null;
  this.sandbox$1 = null;
  this.scope$1 = null;
  this.scrolling$1 = null;
  this.seamless$1 = null;
  this.selected$1 = null;
  this.shape$1 = null;
  this.sizes$1 = null;
  this.srcDoc$1 = null;
  this.srcSet$1 = null;
  this.step$1 = null;
  this.useMap$1 = null;
  this.wmode$1 = null;
  this.dangerouslySetInnerHtmlAttr$1 = null;
  this.href$1 = null;
  this.action$1 = null;
  this.method$1 = null;
  this.id$1 = null;
  this.target$1 = null;
  this.name$1 = null;
  this.alt$1 = null;
  this.onBlur$1 = null;
  this.onChange$1 = null;
  this.onClick$1 = null;
  this.onDblClick$1 = null;
  this.onError$1 = null;
  this.onFocus$1 = null;
  this.onKeyDown$1 = null;
  this.onKeyUp$1 = null;
  this.onKeyPress$1 = null;
  this.onLoad$1 = null;
  this.onMouseDown$1 = null;
  this.onMouseEnter$1 = null;
  this.onMouseLeave$1 = null;
  this.onMouseMove$1 = null;
  this.onMouseOut$1 = null;
  this.onMouseOver$1 = null;
  this.onMouseUp$1 = null;
  this.onTouchCancel$1 = null;
  this.onTouchEnd$1 = null;
  this.onTouchMove$1 = null;
  this.onTouchStart$1 = null;
  this.onSelect$1 = null;
  this.onScroll$1 = null;
  this.onSubmit$1 = null;
  this.onReset$1 = null;
  this.rel$1 = null;
  this.src$1 = null;
  this.style$1 = null;
  this.title$1 = null;
  this.type$1 = null;
  this.tpe$1 = null;
  this.xmlns$1 = null;
  this.lang$1 = null;
  this.placeholder$1 = null;
  this.spellCheck$1 = null;
  this.value$1 = null;
  this.accept$1 = null;
  this.autoComplete$1 = null;
  this.autoFocus$1 = null;
  this.checked$1 = null;
  this.charset$1 = null;
  this.disabled$1 = null;
  this.for$1 = null;
  this.readOnly$1 = null;
  this.required$1 = null;
  this.rows$1 = null;
  this.cols$1 = null;
  this.size$1 = null;
  this.tabIndex$1 = null;
  this.role$1 = null;
  this.contentAttr$1 = null;
  this.httpEquiv$1 = null;
  this.media$1 = null;
  this.scoped$1 = null;
  this.high$1 = null;
  this.low$1 = null;
  this.optimum$1 = null;
  this.min$1 = null;
  this.max$1 = null;
  this.unselectable$1 = null;
  this.capture$1 = null;
  this.challenge$1 = null;
  this.inputMode$1 = null;
  this.is$1 = null;
  this.keyParams$1 = null;
  this.keyType$1 = null;
  this.minLength$1 = null;
  this.summaryAttr$1 = null;
  this.wrap$1 = null;
  this.autoSave$1 = null;
  this.results$1 = null;
  this.security$1 = null;
  this.onAbort$1 = null;
  this.onCanPlay$1 = null;
  this.onCanPlayThrough$1 = null;
  this.onDurationChange$1 = null;
  this.onEmptied$1 = null;
  this.onEncrypted$1 = null;
  this.onEnded$1 = null;
  this.onLoadedData$1 = null;
  this.onLoadedMetadata$1 = null;
  this.onLoadStart$1 = null;
  this.onPause$1 = null;
  this.onPlay$1 = null;
  this.onPlaying$1 = null;
  this.onProgress$1 = null;
  this.onRateChange$1 = null;
  this.onSeeked$1 = null;
  this.onSeeking$1 = null;
  this.onStalled$1 = null;
  this.onSuspend$1 = null;
  this.onTimeUpdate$1 = null;
  this.onVolumeChange$1 = null;
  this.onWaiting$1 = null;
  this.srcLang$1 = null;
  this.default$1 = null;
  this.kind$1 = null;
  this.integrity$1 = null;
  this.reversed$1 = null;
  this.nonce$1 = null;
  this.citeAttr$1 = null;
  this.profile$1 = null;
  this.onAnimationStart$1 = null;
  this.onAnimationEnd$1 = null;
  this.onAnimationIteration$1 = null;
  this.onTransitionEnd$1 = null;
  this.onInvalid$1 = null;
  this.backgroundAttachment$module$1 = null;
  this.bitmap$0$1 = $m_sjsr_RuntimeLong$().Zero__sjsr_RuntimeLong();
  this.backgroundOrigin$module$1 = null;
  this.backgroundClip$module$1 = null;
  this.backgroundSize$module$1 = null;
  this.borderCollapse$module$1 = null;
  this.borderSpacing$module$1 = null;
  this.boxSizing$module$1 = null;
  this.color$module$1 = null;
  this.clip$module$1 = null;
  this.cursor$module$1 = null;
  this.float$module$1 = null;
  this.direction$module$1 = null;
  this.display$module$1 = null;
  this.pointerEvents$module$1 = null;
  this.listStyleImage$module$1 = null;
  this.listStylePosition$module$1 = null;
  this.wordWrap$module$1 = null;
  this.verticalAlign$module$1 = null;
  this.mask$module$1 = null;
  this.emptyCells$module$1 = null;
  this.listStyleType$module$1 = null;
  this.captionSide$module$1 = null;
  this.position$module$1 = null;
  this.quotes$module$1 = null;
  this.tableLayout$module$1 = null;
  this.fontSize$module$1 = null;
  this.fontWeight$module$1 = null;
  this.fontStyle$module$1 = null;
  this.clear$module$1 = null;
  this.margin$module$1 = null;
  this.outlineWidth$module$1 = null;
  this.outlineColor$module$1 = null;
  this.bitmap$1$1 = $m_sjsr_RuntimeLong$().Zero__sjsr_RuntimeLong();
  this.textDecoration$module$1 = null;
  this.textOverflow$module$1 = null;
  this.textUnderlinePosition$module$1 = null;
  this.textTransform$module$1 = null;
  this.visibility$module$1 = null;
  this.whiteSpace$module$1 = null;
  this.backfaceVisibility$module$1 = null;
  this.columns$module$1 = null;
  this.columnFill$module$1 = null;
  this.columnSpan$module$1 = null;
  this.columnRuleWidth$module$1 = null;
  this.columnRuleStyle$module$1 = null;
  this.alignContent$module$1 = null;
  this.alignSelf$module$1 = null;
  this.flexWrap$module$1 = null;
  this.alignItems$module$1 = null;
  this.justifyContent$module$1 = null;
  this.flexDirection$module$1 = null;
  this.transformStyle$module$1 = null;
  this.unicodeBidi$module$1 = null;
  this.wordBreak$module$1 = null;
  this.bitmap$2$1 = $m_sjsr_RuntimeLong$().Zero__sjsr_RuntimeLong();
  this.bitmap$3$1 = $m_sjsr_RuntimeLong$().Zero__sjsr_RuntimeLong();
  this.aria$module$1 = null;
  this.bitmap$4$1 = $m_sjsr_RuntimeLong$().Zero__sjsr_RuntimeLong()
}
$c_Ljapgolly_scalajs_react_vdom_package$Attrs$.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_vdom_package$Attrs$.prototype.constructor = $c_Ljapgolly_scalajs_react_vdom_package$Attrs$;
/** @constructor */
function $h_Ljapgolly_scalajs_react_vdom_package$Attrs$() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_vdom_package$Attrs$.prototype = $c_Ljapgolly_scalajs_react_vdom_package$Attrs$.prototype;
$c_Ljapgolly_scalajs_react_vdom_package$Attrs$.prototype.init___ = (function() {
  $n_Ljapgolly_scalajs_react_vdom_package$Attrs$ = this;
  $s_Ljapgolly_scalajs_react_vdom_HtmlAttrs$class__$$init$__Ljapgolly_scalajs_react_vdom_HtmlAttrs__V(this);
  $s_Ljapgolly_scalajs_react_vdom_Extra$Attrs$class__$$init$__Ljapgolly_scalajs_react_vdom_Extra$Attrs__V(this);
  return this
});
var $d_Ljapgolly_scalajs_react_vdom_package$Attrs$ = new $TypeData().initClass({
  Ljapgolly_scalajs_react_vdom_package$Attrs$: 0
}, false, "japgolly.scalajs.react.vdom.package$Attrs$", {
  Ljapgolly_scalajs_react_vdom_package$Attrs$: 1,
  O: 1,
  Ljapgolly_scalajs_react_vdom_package$JustAttrs: 1,
  Ljapgolly_scalajs_react_vdom_package$Attrs: 1,
  Ljapgolly_scalajs_react_vdom_HtmlAttrs: 1,
  Ljapgolly_scalajs_react_vdom_Extra$Attrs: 1,
  Ljapgolly_scalajs_react_vdom_HtmlStyles: 1
});
$c_Ljapgolly_scalajs_react_vdom_package$Attrs$.prototype.$classData = $d_Ljapgolly_scalajs_react_vdom_package$Attrs$;
var $n_Ljapgolly_scalajs_react_vdom_package$Attrs$ = (void 0);
function $m_Ljapgolly_scalajs_react_vdom_package$Attrs$() {
  if ((!$n_Ljapgolly_scalajs_react_vdom_package$Attrs$)) {
    $n_Ljapgolly_scalajs_react_vdom_package$Attrs$ = new $c_Ljapgolly_scalajs_react_vdom_package$Attrs$().init___()
  };
  return $n_Ljapgolly_scalajs_react_vdom_package$Attrs$
}
/** @constructor */
function $c_T2() {
  $c_O.call(this);
  this.$$und1$f = null;
  this.$$und2$f = null
}
$c_T2.prototype = new $h_O();
$c_T2.prototype.constructor = $c_T2;
/** @constructor */
function $h_T2() {
  /*<skip>*/
}
$h_T2.prototype = $c_T2.prototype;
$c_T2.prototype.productPrefix__T = (function() {
  return "Tuple2"
});
$c_T2.prototype.productArity__I = (function() {
  return 2
});
$c_T2.prototype.equals__O__Z = (function(x$1) {
  if ((this === x$1)) {
    return true
  } else if ($is_T2(x$1)) {
    var Tuple2$1 = $as_T2(x$1);
    return ($m_sr_BoxesRunTime$().equals__O__O__Z(this.$$und1$f, Tuple2$1.$$und1$f) && $m_sr_BoxesRunTime$().equals__O__O__Z(this.$$und2$f, Tuple2$1.$$und2$f))
  } else {
    return false
  }
});
$c_T2.prototype.productElement__I__O = (function(n) {
  return $s_s_Product2$class__productElement__s_Product2__I__O(this, n)
});
$c_T2.prototype.init___O__O = (function(_1, _2) {
  this.$$und1$f = _1;
  this.$$und2$f = _2;
  return this
});
$c_T2.prototype.toString__T = (function() {
  return (((("(" + this.$$und1$f) + ",") + this.$$und2$f) + ")")
});
$c_T2.prototype.hashCode__I = (function() {
  var this$2 = $m_s_util_hashing_MurmurHash3$();
  return this$2.productHash__s_Product__I__I(this, (-889275714))
});
$c_T2.prototype.productIterator__sc_Iterator = (function() {
  return new $c_sr_ScalaRunTime$$anon$1().init___s_Product(this)
});
function $is_T2(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.T2)))
}
function $as_T2(obj) {
  return (($is_T2(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "scala.Tuple2"))
}
function $isArrayOf_T2(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.T2)))
}
function $asArrayOf_T2(obj, depth) {
  return (($isArrayOf_T2(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Lscala.Tuple2;", depth))
}
var $d_T2 = new $TypeData().initClass({
  T2: 0
}, false, "scala.Tuple2", {
  T2: 1,
  O: 1,
  s_Product2: 1,
  s_Product: 1,
  s_Equals: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_T2.prototype.$classData = $d_T2;
/** @constructor */
function $c_s_None$() {
  $c_s_Option.call(this)
}
$c_s_None$.prototype = new $h_s_Option();
$c_s_None$.prototype.constructor = $c_s_None$;
/** @constructor */
function $h_s_None$() {
  /*<skip>*/
}
$h_s_None$.prototype = $c_s_None$.prototype;
$c_s_None$.prototype.init___ = (function() {
  return this
});
$c_s_None$.prototype.productPrefix__T = (function() {
  return "None"
});
$c_s_None$.prototype.productArity__I = (function() {
  return 0
});
$c_s_None$.prototype.isEmpty__Z = (function() {
  return true
});
$c_s_None$.prototype.get__O = (function() {
  this.get__sr_Nothing$()
});
$c_s_None$.prototype.productElement__I__O = (function(x$1) {
  throw new $c_jl_IndexOutOfBoundsException().init___T(("" + x$1))
});
$c_s_None$.prototype.toString__T = (function() {
  return "None"
});
$c_s_None$.prototype.get__sr_Nothing$ = (function() {
  throw new $c_ju_NoSuchElementException().init___T("None.get")
});
$c_s_None$.prototype.hashCode__I = (function() {
  return 2433880
});
$c_s_None$.prototype.productIterator__sc_Iterator = (function() {
  return new $c_sr_ScalaRunTime$$anon$1().init___s_Product(this)
});
var $d_s_None$ = new $TypeData().initClass({
  s_None$: 0
}, false, "scala.None$", {
  s_None$: 1,
  s_Option: 1,
  O: 1,
  s_Product: 1,
  s_Equals: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_s_None$.prototype.$classData = $d_s_None$;
var $n_s_None$ = (void 0);
function $m_s_None$() {
  if ((!$n_s_None$)) {
    $n_s_None$ = new $c_s_None$().init___()
  };
  return $n_s_None$
}
/** @constructor */
function $c_s_Some() {
  $c_s_Option.call(this);
  this.x$2 = null
}
$c_s_Some.prototype = new $h_s_Option();
$c_s_Some.prototype.constructor = $c_s_Some;
/** @constructor */
function $h_s_Some() {
  /*<skip>*/
}
$h_s_Some.prototype = $c_s_Some.prototype;
$c_s_Some.prototype.productPrefix__T = (function() {
  return "Some"
});
$c_s_Some.prototype.productArity__I = (function() {
  return 1
});
$c_s_Some.prototype.equals__O__Z = (function(x$1) {
  if ((this === x$1)) {
    return true
  } else if ($is_s_Some(x$1)) {
    var Some$1 = $as_s_Some(x$1);
    return $m_sr_BoxesRunTime$().equals__O__O__Z(this.x$2, Some$1.x$2)
  } else {
    return false
  }
});
$c_s_Some.prototype.isEmpty__Z = (function() {
  return false
});
$c_s_Some.prototype.productElement__I__O = (function(x$1) {
  switch (x$1) {
    case 0: {
      return this.x$2;
      break
    }
    default: {
      throw new $c_jl_IndexOutOfBoundsException().init___T(("" + x$1))
    }
  }
});
$c_s_Some.prototype.get__O = (function() {
  return this.x$2
});
$c_s_Some.prototype.toString__T = (function() {
  return $m_sr_ScalaRunTime$().$$undtoString__s_Product__T(this)
});
$c_s_Some.prototype.init___O = (function(x) {
  this.x$2 = x;
  return this
});
$c_s_Some.prototype.hashCode__I = (function() {
  var this$2 = $m_s_util_hashing_MurmurHash3$();
  return this$2.productHash__s_Product__I__I(this, (-889275714))
});
$c_s_Some.prototype.productIterator__sc_Iterator = (function() {
  return new $c_sr_ScalaRunTime$$anon$1().init___s_Product(this)
});
function $is_s_Some(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.s_Some)))
}
function $as_s_Some(obj) {
  return (($is_s_Some(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "scala.Some"))
}
function $isArrayOf_s_Some(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.s_Some)))
}
function $asArrayOf_s_Some(obj, depth) {
  return (($isArrayOf_s_Some(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Lscala.Some;", depth))
}
var $d_s_Some = new $TypeData().initClass({
  s_Some: 0
}, false, "scala.Some", {
  s_Some: 1,
  s_Option: 1,
  O: 1,
  s_Product: 1,
  s_Equals: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_s_Some.prototype.$classData = $d_s_Some;
/** @constructor */
function $c_s_StringContext$InvalidEscapeException() {
  $c_jl_IllegalArgumentException.call(this);
  this.index$5 = 0
}
$c_s_StringContext$InvalidEscapeException.prototype = new $h_jl_IllegalArgumentException();
$c_s_StringContext$InvalidEscapeException.prototype.constructor = $c_s_StringContext$InvalidEscapeException;
/** @constructor */
function $h_s_StringContext$InvalidEscapeException() {
  /*<skip>*/
}
$h_s_StringContext$InvalidEscapeException.prototype = $c_s_StringContext$InvalidEscapeException.prototype;
$c_s_StringContext$InvalidEscapeException.prototype.init___T__I = (function(str, index) {
  this.index$5 = index;
  var jsx$3 = new $c_s_StringContext().init___sc_Seq(new $c_sjs_js_WrappedArray().init___sjs_js_Array(["invalid escape ", " index ", " in \"", "\". Use \\\\\\\\ for literal \\\\."]));
  $m_s_Predef$().require__Z__V(((index >= 0) && (index < $uI(str.length))));
  if ((index === (((-1) + $uI(str.length)) | 0))) {
    var jsx$1 = "at terminal"
  } else {
    var jsx$2 = new $c_s_StringContext().init___sc_Seq(new $c_sjs_js_WrappedArray().init___sjs_js_Array(["'\\\\", "' not one of ", " at"]));
    var index$1 = ((1 + index) | 0);
    var c = (65535 & $uI(str.charCodeAt(index$1)));
    var jsx$1 = jsx$2.s__sc_Seq__T(new $c_sjs_js_WrappedArray().init___sjs_js_Array([new $c_jl_Character().init___C(c), "[\\b, \\t, \\n, \\f, \\r, \\\\, \\\", \\']"]))
  };
  var s = jsx$3.s__sc_Seq__T(new $c_sjs_js_WrappedArray().init___sjs_js_Array([jsx$1, index, str]));
  $c_jl_Throwable.prototype.init___T__jl_Throwable.call(this, s, null);
  return this
});
var $d_s_StringContext$InvalidEscapeException = new $TypeData().initClass({
  s_StringContext$InvalidEscapeException: 0
}, false, "scala.StringContext$InvalidEscapeException", {
  s_StringContext$InvalidEscapeException: 1,
  jl_IllegalArgumentException: 1,
  jl_RuntimeException: 1,
  jl_Exception: 1,
  jl_Throwable: 1,
  O: 1,
  Ljava_io_Serializable: 1
});
$c_s_StringContext$InvalidEscapeException.prototype.$classData = $d_s_StringContext$InvalidEscapeException;
/** @constructor */
function $c_scg_SeqFactory() {
  $c_scg_GenSeqFactory.call(this)
}
$c_scg_SeqFactory.prototype = new $h_scg_GenSeqFactory();
$c_scg_SeqFactory.prototype.constructor = $c_scg_SeqFactory;
/** @constructor */
function $h_scg_SeqFactory() {
  /*<skip>*/
}
$h_scg_SeqFactory.prototype = $c_scg_SeqFactory.prototype;
/** @constructor */
function $c_sci_Set$() {
  $c_scg_ImmutableSetFactory.call(this)
}
$c_sci_Set$.prototype = new $h_scg_ImmutableSetFactory();
$c_sci_Set$.prototype.constructor = $c_sci_Set$;
/** @constructor */
function $h_sci_Set$() {
  /*<skip>*/
}
$h_sci_Set$.prototype = $c_sci_Set$.prototype;
$c_sci_Set$.prototype.init___ = (function() {
  return this
});
var $d_sci_Set$ = new $TypeData().initClass({
  sci_Set$: 0
}, false, "scala.collection.immutable.Set$", {
  sci_Set$: 1,
  scg_ImmutableSetFactory: 1,
  scg_SetFactory: 1,
  scg_GenSetFactory: 1,
  scg_GenericCompanion: 1,
  O: 1,
  scg_GenericSeqCompanion: 1
});
$c_sci_Set$.prototype.$classData = $d_sci_Set$;
var $n_sci_Set$ = (void 0);
function $m_sci_Set$() {
  if ((!$n_sci_Set$)) {
    $n_sci_Set$ = new $c_sci_Set$().init___()
  };
  return $n_sci_Set$
}
/** @constructor */
function $c_sci_VectorIterator() {
  $c_sc_AbstractIterator.call(this);
  this.endIndex$2 = 0;
  this.blockIndex$2 = 0;
  this.lo$2 = 0;
  this.endLo$2 = 0;
  this.$$undhasNext$2 = false;
  this.depth$2 = 0;
  this.display0$2 = null;
  this.display1$2 = null;
  this.display2$2 = null;
  this.display3$2 = null;
  this.display4$2 = null;
  this.display5$2 = null
}
$c_sci_VectorIterator.prototype = new $h_sc_AbstractIterator();
$c_sci_VectorIterator.prototype.constructor = $c_sci_VectorIterator;
/** @constructor */
function $h_sci_VectorIterator() {
  /*<skip>*/
}
$h_sci_VectorIterator.prototype = $c_sci_VectorIterator.prototype;
$c_sci_VectorIterator.prototype.next__O = (function() {
  if ((!this.$$undhasNext$2)) {
    throw new $c_ju_NoSuchElementException().init___T("reached iterator end")
  };
  var res = this.display0$2.u[this.lo$2];
  this.lo$2 = ((1 + this.lo$2) | 0);
  if ((this.lo$2 === this.endLo$2)) {
    if ((((this.blockIndex$2 + this.lo$2) | 0) < this.endIndex$2)) {
      var newBlockIndex = ((32 + this.blockIndex$2) | 0);
      var xor = (this.blockIndex$2 ^ newBlockIndex);
      $s_sci_VectorPointer$class__gotoNextBlockStart__sci_VectorPointer__I__I__V(this, newBlockIndex, xor);
      this.blockIndex$2 = newBlockIndex;
      var x = ((this.endIndex$2 - this.blockIndex$2) | 0);
      this.endLo$2 = ((x < 32) ? x : 32);
      this.lo$2 = 0
    } else {
      this.$$undhasNext$2 = false
    }
  };
  return res
});
$c_sci_VectorIterator.prototype.display3__AO = (function() {
  return this.display3$2
});
$c_sci_VectorIterator.prototype.depth__I = (function() {
  return this.depth$2
});
$c_sci_VectorIterator.prototype.display5$und$eq__AO__V = (function(x$1) {
  this.display5$2 = x$1
});
$c_sci_VectorIterator.prototype.init___I__I = (function(_startIndex, endIndex) {
  this.endIndex$2 = endIndex;
  this.blockIndex$2 = ((-32) & _startIndex);
  this.lo$2 = (31 & _startIndex);
  var x = ((endIndex - this.blockIndex$2) | 0);
  this.endLo$2 = ((x < 32) ? x : 32);
  this.$$undhasNext$2 = (((this.blockIndex$2 + this.lo$2) | 0) < endIndex);
  return this
});
$c_sci_VectorIterator.prototype.display0__AO = (function() {
  return this.display0$2
});
$c_sci_VectorIterator.prototype.display4__AO = (function() {
  return this.display4$2
});
$c_sci_VectorIterator.prototype.display2$und$eq__AO__V = (function(x$1) {
  this.display2$2 = x$1
});
$c_sci_VectorIterator.prototype.display1$und$eq__AO__V = (function(x$1) {
  this.display1$2 = x$1
});
$c_sci_VectorIterator.prototype.hasNext__Z = (function() {
  return this.$$undhasNext$2
});
$c_sci_VectorIterator.prototype.display4$und$eq__AO__V = (function(x$1) {
  this.display4$2 = x$1
});
$c_sci_VectorIterator.prototype.display1__AO = (function() {
  return this.display1$2
});
$c_sci_VectorIterator.prototype.display5__AO = (function() {
  return this.display5$2
});
$c_sci_VectorIterator.prototype.depth$und$eq__I__V = (function(x$1) {
  this.depth$2 = x$1
});
$c_sci_VectorIterator.prototype.display2__AO = (function() {
  return this.display2$2
});
$c_sci_VectorIterator.prototype.display0$und$eq__AO__V = (function(x$1) {
  this.display0$2 = x$1
});
$c_sci_VectorIterator.prototype.display3$und$eq__AO__V = (function(x$1) {
  this.display3$2 = x$1
});
var $d_sci_VectorIterator = new $TypeData().initClass({
  sci_VectorIterator: 0
}, false, "scala.collection.immutable.VectorIterator", {
  sci_VectorIterator: 1,
  sc_AbstractIterator: 1,
  O: 1,
  sc_Iterator: 1,
  sc_TraversableOnce: 1,
  sc_GenTraversableOnce: 1,
  sci_VectorPointer: 1
});
$c_sci_VectorIterator.prototype.$classData = $d_sci_VectorIterator;
/** @constructor */
function $c_sjsr_UndefinedBehaviorError() {
  $c_jl_Error.call(this)
}
$c_sjsr_UndefinedBehaviorError.prototype = new $h_jl_Error();
$c_sjsr_UndefinedBehaviorError.prototype.constructor = $c_sjsr_UndefinedBehaviorError;
/** @constructor */
function $h_sjsr_UndefinedBehaviorError() {
  /*<skip>*/
}
$h_sjsr_UndefinedBehaviorError.prototype = $c_sjsr_UndefinedBehaviorError.prototype;
$c_sjsr_UndefinedBehaviorError.prototype.fillInStackTrace__jl_Throwable = (function() {
  return $c_jl_Throwable.prototype.fillInStackTrace__jl_Throwable.call(this)
});
$c_sjsr_UndefinedBehaviorError.prototype.init___jl_Throwable = (function(cause) {
  $c_sjsr_UndefinedBehaviorError.prototype.init___T__jl_Throwable.call(this, ("An undefined behavior was detected" + ((cause === null) ? "" : (": " + cause.getMessage__T()))), cause);
  return this
});
$c_sjsr_UndefinedBehaviorError.prototype.init___T__jl_Throwable = (function(message, cause) {
  $c_jl_Throwable.prototype.init___T__jl_Throwable.call(this, message, cause);
  return this
});
var $d_sjsr_UndefinedBehaviorError = new $TypeData().initClass({
  sjsr_UndefinedBehaviorError: 0
}, false, "scala.scalajs.runtime.UndefinedBehaviorError", {
  sjsr_UndefinedBehaviorError: 1,
  jl_Error: 1,
  jl_Throwable: 1,
  O: 1,
  Ljava_io_Serializable: 1,
  s_util_control_ControlThrowable: 1,
  s_util_control_NoStackTrace: 1
});
$c_sjsr_UndefinedBehaviorError.prototype.$classData = $d_sjsr_UndefinedBehaviorError;
/** @constructor */
function $c_Ljapgolly_scalajs_react_CompState$ReadCallbackWriteCallback() {
  $c_O.call(this);
  this.$$$1 = null;
  this.a$1 = null
}
$c_Ljapgolly_scalajs_react_CompState$ReadCallbackWriteCallback.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_CompState$ReadCallbackWriteCallback.prototype.constructor = $c_Ljapgolly_scalajs_react_CompState$ReadCallbackWriteCallback;
/** @constructor */
function $h_Ljapgolly_scalajs_react_CompState$ReadCallbackWriteCallback() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_CompState$ReadCallbackWriteCallback.prototype = $c_Ljapgolly_scalajs_react_CompState$ReadCallbackWriteCallback.prototype;
$c_Ljapgolly_scalajs_react_CompState$ReadCallbackWriteCallback.prototype.init___O__Ljapgolly_scalajs_react_CompState$Accessor = (function($$, a) {
  this.$$$1 = $$;
  this.a$1 = a;
  return this
});
$c_Ljapgolly_scalajs_react_CompState$ReadCallbackWriteCallback.prototype.a__Ljapgolly_scalajs_react_CompState$Accessor = (function() {
  return this.a$1
});
$c_Ljapgolly_scalajs_react_CompState$ReadCallbackWriteCallback.prototype.$$__O = (function() {
  return this.$$$1
});
var $d_Ljapgolly_scalajs_react_CompState$ReadCallbackWriteCallback = new $TypeData().initClass({
  Ljapgolly_scalajs_react_CompState$ReadCallbackWriteCallback: 0
}, false, "japgolly.scalajs.react.CompState$ReadCallbackWriteCallback", {
  Ljapgolly_scalajs_react_CompState$ReadCallbackWriteCallback: 1,
  O: 1,
  Ljapgolly_scalajs_react_CompState$ReadCallbackWriteCallbackOps: 1,
  Ljapgolly_scalajs_react_CompState$ReadCallbackOps: 1,
  Ljapgolly_scalajs_react_CompState$ZoomOps: 1,
  Ljapgolly_scalajs_react_CompState$BaseOps: 1,
  Ljapgolly_scalajs_react_CompState$WriteCallbackOps: 1,
  Ljapgolly_scalajs_react_CompState$WriteOps: 1
});
$c_Ljapgolly_scalajs_react_CompState$ReadCallbackWriteCallback.prototype.$classData = $d_Ljapgolly_scalajs_react_CompState$ReadCallbackWriteCallback;
/** @constructor */
function $c_Ljapgolly_scalajs_react_CompState$ReadDirectWriteCallback() {
  $c_O.call(this);
  this.$$$1 = null;
  this.a$1 = null
}
$c_Ljapgolly_scalajs_react_CompState$ReadDirectWriteCallback.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_CompState$ReadDirectWriteCallback.prototype.constructor = $c_Ljapgolly_scalajs_react_CompState$ReadDirectWriteCallback;
/** @constructor */
function $h_Ljapgolly_scalajs_react_CompState$ReadDirectWriteCallback() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_CompState$ReadDirectWriteCallback.prototype = $c_Ljapgolly_scalajs_react_CompState$ReadDirectWriteCallback.prototype;
$c_Ljapgolly_scalajs_react_CompState$ReadDirectWriteCallback.prototype.init___O__Ljapgolly_scalajs_react_CompState$Accessor = (function($$, a) {
  this.$$$1 = $$;
  this.a$1 = a;
  return this
});
$c_Ljapgolly_scalajs_react_CompState$ReadDirectWriteCallback.prototype.a__Ljapgolly_scalajs_react_CompState$Accessor = (function() {
  return this.a$1
});
$c_Ljapgolly_scalajs_react_CompState$ReadDirectWriteCallback.prototype.$$__O = (function() {
  return this.$$$1
});
var $d_Ljapgolly_scalajs_react_CompState$ReadDirectWriteCallback = new $TypeData().initClass({
  Ljapgolly_scalajs_react_CompState$ReadDirectWriteCallback: 0
}, false, "japgolly.scalajs.react.CompState$ReadDirectWriteCallback", {
  Ljapgolly_scalajs_react_CompState$ReadDirectWriteCallback: 1,
  O: 1,
  Ljapgolly_scalajs_react_CompState$ReadDirectWriteCallbackOps: 1,
  Ljapgolly_scalajs_react_CompState$ReadDirectOps: 1,
  Ljapgolly_scalajs_react_CompState$ZoomOps: 1,
  Ljapgolly_scalajs_react_CompState$BaseOps: 1,
  Ljapgolly_scalajs_react_CompState$WriteCallbackOps: 1,
  Ljapgolly_scalajs_react_CompState$WriteOps: 1
});
$c_Ljapgolly_scalajs_react_CompState$ReadDirectWriteCallback.prototype.$classData = $d_Ljapgolly_scalajs_react_CompState$ReadDirectWriteCallback;
/** @constructor */
function $c_sc_Seq$() {
  $c_scg_SeqFactory.call(this)
}
$c_sc_Seq$.prototype = new $h_scg_SeqFactory();
$c_sc_Seq$.prototype.constructor = $c_sc_Seq$;
/** @constructor */
function $h_sc_Seq$() {
  /*<skip>*/
}
$h_sc_Seq$.prototype = $c_sc_Seq$.prototype;
$c_sc_Seq$.prototype.init___ = (function() {
  $c_scg_GenTraversableFactory.prototype.init___.call(this);
  return this
});
var $d_sc_Seq$ = new $TypeData().initClass({
  sc_Seq$: 0
}, false, "scala.collection.Seq$", {
  sc_Seq$: 1,
  scg_SeqFactory: 1,
  scg_GenSeqFactory: 1,
  scg_GenTraversableFactory: 1,
  scg_GenericCompanion: 1,
  O: 1,
  scg_TraversableFactory: 1,
  scg_GenericSeqCompanion: 1
});
$c_sc_Seq$.prototype.$classData = $d_sc_Seq$;
var $n_sc_Seq$ = (void 0);
function $m_sc_Seq$() {
  if ((!$n_sc_Seq$)) {
    $n_sc_Seq$ = new $c_sc_Seq$().init___()
  };
  return $n_sc_Seq$
}
/** @constructor */
function $c_scg_IndexedSeqFactory() {
  $c_scg_SeqFactory.call(this)
}
$c_scg_IndexedSeqFactory.prototype = new $h_scg_SeqFactory();
$c_scg_IndexedSeqFactory.prototype.constructor = $c_scg_IndexedSeqFactory;
/** @constructor */
function $h_scg_IndexedSeqFactory() {
  /*<skip>*/
}
$h_scg_IndexedSeqFactory.prototype = $c_scg_IndexedSeqFactory.prototype;
/** @constructor */
function $c_Ljapgolly_scalajs_react_vdom_ReactNodeFrag() {
  $c_O.call(this);
  this.render$1 = null
}
$c_Ljapgolly_scalajs_react_vdom_ReactNodeFrag.prototype = new $h_O();
$c_Ljapgolly_scalajs_react_vdom_ReactNodeFrag.prototype.constructor = $c_Ljapgolly_scalajs_react_vdom_ReactNodeFrag;
/** @constructor */
function $h_Ljapgolly_scalajs_react_vdom_ReactNodeFrag() {
  /*<skip>*/
}
$h_Ljapgolly_scalajs_react_vdom_ReactNodeFrag.prototype = $c_Ljapgolly_scalajs_react_vdom_ReactNodeFrag.prototype;
$c_Ljapgolly_scalajs_react_vdom_ReactNodeFrag.prototype.productPrefix__T = (function() {
  return "ReactNodeFrag"
});
$c_Ljapgolly_scalajs_react_vdom_ReactNodeFrag.prototype.productArity__I = (function() {
  return 1
});
$c_Ljapgolly_scalajs_react_vdom_ReactNodeFrag.prototype.equals__O__Z = (function(x$1) {
  if ((this === x$1)) {
    return true
  } else if ($is_Ljapgolly_scalajs_react_vdom_ReactNodeFrag(x$1)) {
    var ReactNodeFrag$1 = $as_Ljapgolly_scalajs_react_vdom_ReactNodeFrag(x$1);
    return $m_sr_BoxesRunTime$().equals__O__O__Z(this.render$1, ReactNodeFrag$1.render$1)
  } else {
    return false
  }
});
$c_Ljapgolly_scalajs_react_vdom_ReactNodeFrag.prototype.productElement__I__O = (function(x$1) {
  switch (x$1) {
    case 0: {
      return this.render$1;
      break
    }
    default: {
      throw new $c_jl_IndexOutOfBoundsException().init___T(("" + x$1))
    }
  }
});
$c_Ljapgolly_scalajs_react_vdom_ReactNodeFrag.prototype.toString__T = (function() {
  return $m_sr_ScalaRunTime$().$$undtoString__s_Product__T(this)
});
$c_Ljapgolly_scalajs_react_vdom_ReactNodeFrag.prototype.hashCode__I = (function() {
  var this$2 = $m_s_util_hashing_MurmurHash3$();
  return this$2.productHash__s_Product__I__I(this, (-889275714))
});
$c_Ljapgolly_scalajs_react_vdom_ReactNodeFrag.prototype.init___Ljapgolly_scalajs_react_ReactNode = (function(render) {
  this.render$1 = render;
  return this
});
$c_Ljapgolly_scalajs_react_vdom_ReactNodeFrag.prototype.productIterator__sc_Iterator = (function() {
  return new $c_sr_ScalaRunTime$$anon$1().init___s_Product(this)
});
$c_Ljapgolly_scalajs_react_vdom_ReactNodeFrag.prototype.applyTo__Ljapgolly_scalajs_react_vdom_Builder__V = (function(b) {
  b.appendChild__Ljapgolly_scalajs_react_ReactNode__V(this.render$1)
});
function $is_Ljapgolly_scalajs_react_vdom_ReactNodeFrag(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.Ljapgolly_scalajs_react_vdom_ReactNodeFrag)))
}
function $as_Ljapgolly_scalajs_react_vdom_ReactNodeFrag(obj) {
  return (($is_Ljapgolly_scalajs_react_vdom_ReactNodeFrag(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "japgolly.scalajs.react.vdom.ReactNodeFrag"))
}
function $isArrayOf_Ljapgolly_scalajs_react_vdom_ReactNodeFrag(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.Ljapgolly_scalajs_react_vdom_ReactNodeFrag)))
}
function $asArrayOf_Ljapgolly_scalajs_react_vdom_ReactNodeFrag(obj, depth) {
  return (($isArrayOf_Ljapgolly_scalajs_react_vdom_ReactNodeFrag(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Ljapgolly.scalajs.react.vdom.ReactNodeFrag;", depth))
}
var $d_Ljapgolly_scalajs_react_vdom_ReactNodeFrag = new $TypeData().initClass({
  Ljapgolly_scalajs_react_vdom_ReactNodeFrag: 0
}, false, "japgolly.scalajs.react.vdom.ReactNodeFrag", {
  Ljapgolly_scalajs_react_vdom_ReactNodeFrag: 1,
  O: 1,
  Ljapgolly_scalajs_react_vdom_DomFrag: 1,
  Ljapgolly_scalajs_react_vdom_Frag: 1,
  Ljapgolly_scalajs_react_vdom_TagMod: 1,
  s_Product: 1,
  s_Equals: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_Ljapgolly_scalajs_react_vdom_ReactNodeFrag.prototype.$classData = $d_Ljapgolly_scalajs_react_vdom_ReactNodeFrag;
/** @constructor */
function $c_s_reflect_AnyValManifest() {
  $c_O.call(this);
  this.toString$1 = null
}
$c_s_reflect_AnyValManifest.prototype = new $h_O();
$c_s_reflect_AnyValManifest.prototype.constructor = $c_s_reflect_AnyValManifest;
/** @constructor */
function $h_s_reflect_AnyValManifest() {
  /*<skip>*/
}
$h_s_reflect_AnyValManifest.prototype = $c_s_reflect_AnyValManifest.prototype;
$c_s_reflect_AnyValManifest.prototype.equals__O__Z = (function(that) {
  return (this === that)
});
$c_s_reflect_AnyValManifest.prototype.toString__T = (function() {
  return this.toString$1
});
$c_s_reflect_AnyValManifest.prototype.hashCode__I = (function() {
  return $systemIdentityHashCode(this)
});
/** @constructor */
function $c_s_reflect_ManifestFactory$ClassTypeManifest() {
  $c_O.call(this);
  this.prefix$1 = null;
  this.runtimeClass1$1 = null;
  this.typeArguments$1 = null
}
$c_s_reflect_ManifestFactory$ClassTypeManifest.prototype = new $h_O();
$c_s_reflect_ManifestFactory$ClassTypeManifest.prototype.constructor = $c_s_reflect_ManifestFactory$ClassTypeManifest;
/** @constructor */
function $h_s_reflect_ManifestFactory$ClassTypeManifest() {
  /*<skip>*/
}
$h_s_reflect_ManifestFactory$ClassTypeManifest.prototype = $c_s_reflect_ManifestFactory$ClassTypeManifest.prototype;
/** @constructor */
function $c_sc_IndexedSeq$() {
  $c_scg_IndexedSeqFactory.call(this);
  this.ReusableCBF$6 = null
}
$c_sc_IndexedSeq$.prototype = new $h_scg_IndexedSeqFactory();
$c_sc_IndexedSeq$.prototype.constructor = $c_sc_IndexedSeq$;
/** @constructor */
function $h_sc_IndexedSeq$() {
  /*<skip>*/
}
$h_sc_IndexedSeq$.prototype = $c_sc_IndexedSeq$.prototype;
$c_sc_IndexedSeq$.prototype.init___ = (function() {
  $c_scg_GenTraversableFactory.prototype.init___.call(this);
  $n_sc_IndexedSeq$ = this;
  this.ReusableCBF$6 = new $c_sc_IndexedSeq$$anon$1().init___();
  return this
});
var $d_sc_IndexedSeq$ = new $TypeData().initClass({
  sc_IndexedSeq$: 0
}, false, "scala.collection.IndexedSeq$", {
  sc_IndexedSeq$: 1,
  scg_IndexedSeqFactory: 1,
  scg_SeqFactory: 1,
  scg_GenSeqFactory: 1,
  scg_GenTraversableFactory: 1,
  scg_GenericCompanion: 1,
  O: 1,
  scg_TraversableFactory: 1,
  scg_GenericSeqCompanion: 1
});
$c_sc_IndexedSeq$.prototype.$classData = $d_sc_IndexedSeq$;
var $n_sc_IndexedSeq$ = (void 0);
function $m_sc_IndexedSeq$() {
  if ((!$n_sc_IndexedSeq$)) {
    $n_sc_IndexedSeq$ = new $c_sc_IndexedSeq$().init___()
  };
  return $n_sc_IndexedSeq$
}
/** @constructor */
function $c_sc_IndexedSeqLike$Elements() {
  $c_sc_AbstractIterator.call(this);
  this.end$2 = 0;
  this.index$2 = 0;
  this.$$outer$f = null
}
$c_sc_IndexedSeqLike$Elements.prototype = new $h_sc_AbstractIterator();
$c_sc_IndexedSeqLike$Elements.prototype.constructor = $c_sc_IndexedSeqLike$Elements;
/** @constructor */
function $h_sc_IndexedSeqLike$Elements() {
  /*<skip>*/
}
$h_sc_IndexedSeqLike$Elements.prototype = $c_sc_IndexedSeqLike$Elements.prototype;
$c_sc_IndexedSeqLike$Elements.prototype.next__O = (function() {
  if ((this.index$2 >= this.end$2)) {
    $m_sc_Iterator$().empty$1.next__O()
  };
  var x = this.$$outer$f.apply__I__O(this.index$2);
  this.index$2 = ((1 + this.index$2) | 0);
  return x
});
$c_sc_IndexedSeqLike$Elements.prototype.init___sc_IndexedSeqLike__I__I = (function($$outer, start, end) {
  this.end$2 = end;
  if (($$outer === null)) {
    throw $m_sjsr_package$().unwrapJavaScriptException__jl_Throwable__O(null)
  } else {
    this.$$outer$f = $$outer
  };
  this.index$2 = start;
  return this
});
$c_sc_IndexedSeqLike$Elements.prototype.hasNext__Z = (function() {
  return (this.index$2 < this.end$2)
});
var $d_sc_IndexedSeqLike$Elements = new $TypeData().initClass({
  sc_IndexedSeqLike$Elements: 0
}, false, "scala.collection.IndexedSeqLike$Elements", {
  sc_IndexedSeqLike$Elements: 1,
  sc_AbstractIterator: 1,
  O: 1,
  sc_Iterator: 1,
  sc_TraversableOnce: 1,
  sc_GenTraversableOnce: 1,
  sc_BufferedIterator: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_sc_IndexedSeqLike$Elements.prototype.$classData = $d_sc_IndexedSeqLike$Elements;
/** @constructor */
function $c_sjs_js_JavaScriptException() {
  $c_jl_RuntimeException.call(this);
  this.exception$4 = null
}
$c_sjs_js_JavaScriptException.prototype = new $h_jl_RuntimeException();
$c_sjs_js_JavaScriptException.prototype.constructor = $c_sjs_js_JavaScriptException;
/** @constructor */
function $h_sjs_js_JavaScriptException() {
  /*<skip>*/
}
$h_sjs_js_JavaScriptException.prototype = $c_sjs_js_JavaScriptException.prototype;
$c_sjs_js_JavaScriptException.prototype.productPrefix__T = (function() {
  return "JavaScriptException"
});
$c_sjs_js_JavaScriptException.prototype.productArity__I = (function() {
  return 1
});
$c_sjs_js_JavaScriptException.prototype.fillInStackTrace__jl_Throwable = (function() {
  var e = this.exception$4;
  this.stackdata = e;
  return this
});
$c_sjs_js_JavaScriptException.prototype.equals__O__Z = (function(x$1) {
  if ((this === x$1)) {
    return true
  } else if ($is_sjs_js_JavaScriptException(x$1)) {
    var JavaScriptException$1 = $as_sjs_js_JavaScriptException(x$1);
    return $m_sr_BoxesRunTime$().equals__O__O__Z(this.exception$4, JavaScriptException$1.exception$4)
  } else {
    return false
  }
});
$c_sjs_js_JavaScriptException.prototype.productElement__I__O = (function(x$1) {
  switch (x$1) {
    case 0: {
      return this.exception$4;
      break
    }
    default: {
      throw new $c_jl_IndexOutOfBoundsException().init___T(("" + x$1))
    }
  }
});
$c_sjs_js_JavaScriptException.prototype.getMessage__T = (function() {
  return $objectToString(this.exception$4)
});
$c_sjs_js_JavaScriptException.prototype.init___O = (function(exception) {
  this.exception$4 = exception;
  $c_jl_Throwable.prototype.init___T__jl_Throwable.call(this, null, null);
  return this
});
$c_sjs_js_JavaScriptException.prototype.hashCode__I = (function() {
  var this$2 = $m_s_util_hashing_MurmurHash3$();
  return this$2.productHash__s_Product__I__I(this, (-889275714))
});
$c_sjs_js_JavaScriptException.prototype.productIterator__sc_Iterator = (function() {
  return new $c_sr_ScalaRunTime$$anon$1().init___s_Product(this)
});
function $is_sjs_js_JavaScriptException(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.sjs_js_JavaScriptException)))
}
function $as_sjs_js_JavaScriptException(obj) {
  return (($is_sjs_js_JavaScriptException(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "scala.scalajs.js.JavaScriptException"))
}
function $isArrayOf_sjs_js_JavaScriptException(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.sjs_js_JavaScriptException)))
}
function $asArrayOf_sjs_js_JavaScriptException(obj, depth) {
  return (($isArrayOf_sjs_js_JavaScriptException(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Lscala.scalajs.js.JavaScriptException;", depth))
}
var $d_sjs_js_JavaScriptException = new $TypeData().initClass({
  sjs_js_JavaScriptException: 0
}, false, "scala.scalajs.js.JavaScriptException", {
  sjs_js_JavaScriptException: 1,
  jl_RuntimeException: 1,
  jl_Exception: 1,
  jl_Throwable: 1,
  O: 1,
  Ljava_io_Serializable: 1,
  s_Product: 1,
  s_Equals: 1,
  s_Serializable: 1
});
$c_sjs_js_JavaScriptException.prototype.$classData = $d_sjs_js_JavaScriptException;
/** @constructor */
function $c_s_reflect_ManifestFactory$BooleanManifest$() {
  $c_s_reflect_AnyValManifest.call(this)
}
$c_s_reflect_ManifestFactory$BooleanManifest$.prototype = new $h_s_reflect_AnyValManifest();
$c_s_reflect_ManifestFactory$BooleanManifest$.prototype.constructor = $c_s_reflect_ManifestFactory$BooleanManifest$;
/** @constructor */
function $h_s_reflect_ManifestFactory$BooleanManifest$() {
  /*<skip>*/
}
$h_s_reflect_ManifestFactory$BooleanManifest$.prototype = $c_s_reflect_ManifestFactory$BooleanManifest$.prototype;
$c_s_reflect_ManifestFactory$BooleanManifest$.prototype.init___ = (function() {
  this.toString$1 = "Boolean";
  return this
});
var $d_s_reflect_ManifestFactory$BooleanManifest$ = new $TypeData().initClass({
  s_reflect_ManifestFactory$BooleanManifest$: 0
}, false, "scala.reflect.ManifestFactory$BooleanManifest$", {
  s_reflect_ManifestFactory$BooleanManifest$: 1,
  s_reflect_AnyValManifest: 1,
  O: 1,
  s_reflect_Manifest: 1,
  s_reflect_ClassTag: 1,
  s_reflect_ClassManifestDeprecatedApis: 1,
  s_reflect_OptManifest: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1,
  s_Equals: 1
});
$c_s_reflect_ManifestFactory$BooleanManifest$.prototype.$classData = $d_s_reflect_ManifestFactory$BooleanManifest$;
var $n_s_reflect_ManifestFactory$BooleanManifest$ = (void 0);
function $m_s_reflect_ManifestFactory$BooleanManifest$() {
  if ((!$n_s_reflect_ManifestFactory$BooleanManifest$)) {
    $n_s_reflect_ManifestFactory$BooleanManifest$ = new $c_s_reflect_ManifestFactory$BooleanManifest$().init___()
  };
  return $n_s_reflect_ManifestFactory$BooleanManifest$
}
/** @constructor */
function $c_s_reflect_ManifestFactory$ByteManifest$() {
  $c_s_reflect_AnyValManifest.call(this)
}
$c_s_reflect_ManifestFactory$ByteManifest$.prototype = new $h_s_reflect_AnyValManifest();
$c_s_reflect_ManifestFactory$ByteManifest$.prototype.constructor = $c_s_reflect_ManifestFactory$ByteManifest$;
/** @constructor */
function $h_s_reflect_ManifestFactory$ByteManifest$() {
  /*<skip>*/
}
$h_s_reflect_ManifestFactory$ByteManifest$.prototype = $c_s_reflect_ManifestFactory$ByteManifest$.prototype;
$c_s_reflect_ManifestFactory$ByteManifest$.prototype.init___ = (function() {
  this.toString$1 = "Byte";
  return this
});
var $d_s_reflect_ManifestFactory$ByteManifest$ = new $TypeData().initClass({
  s_reflect_ManifestFactory$ByteManifest$: 0
}, false, "scala.reflect.ManifestFactory$ByteManifest$", {
  s_reflect_ManifestFactory$ByteManifest$: 1,
  s_reflect_AnyValManifest: 1,
  O: 1,
  s_reflect_Manifest: 1,
  s_reflect_ClassTag: 1,
  s_reflect_ClassManifestDeprecatedApis: 1,
  s_reflect_OptManifest: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1,
  s_Equals: 1
});
$c_s_reflect_ManifestFactory$ByteManifest$.prototype.$classData = $d_s_reflect_ManifestFactory$ByteManifest$;
var $n_s_reflect_ManifestFactory$ByteManifest$ = (void 0);
function $m_s_reflect_ManifestFactory$ByteManifest$() {
  if ((!$n_s_reflect_ManifestFactory$ByteManifest$)) {
    $n_s_reflect_ManifestFactory$ByteManifest$ = new $c_s_reflect_ManifestFactory$ByteManifest$().init___()
  };
  return $n_s_reflect_ManifestFactory$ByteManifest$
}
/** @constructor */
function $c_s_reflect_ManifestFactory$CharManifest$() {
  $c_s_reflect_AnyValManifest.call(this)
}
$c_s_reflect_ManifestFactory$CharManifest$.prototype = new $h_s_reflect_AnyValManifest();
$c_s_reflect_ManifestFactory$CharManifest$.prototype.constructor = $c_s_reflect_ManifestFactory$CharManifest$;
/** @constructor */
function $h_s_reflect_ManifestFactory$CharManifest$() {
  /*<skip>*/
}
$h_s_reflect_ManifestFactory$CharManifest$.prototype = $c_s_reflect_ManifestFactory$CharManifest$.prototype;
$c_s_reflect_ManifestFactory$CharManifest$.prototype.init___ = (function() {
  this.toString$1 = "Char";
  return this
});
var $d_s_reflect_ManifestFactory$CharManifest$ = new $TypeData().initClass({
  s_reflect_ManifestFactory$CharManifest$: 0
}, false, "scala.reflect.ManifestFactory$CharManifest$", {
  s_reflect_ManifestFactory$CharManifest$: 1,
  s_reflect_AnyValManifest: 1,
  O: 1,
  s_reflect_Manifest: 1,
  s_reflect_ClassTag: 1,
  s_reflect_ClassManifestDeprecatedApis: 1,
  s_reflect_OptManifest: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1,
  s_Equals: 1
});
$c_s_reflect_ManifestFactory$CharManifest$.prototype.$classData = $d_s_reflect_ManifestFactory$CharManifest$;
var $n_s_reflect_ManifestFactory$CharManifest$ = (void 0);
function $m_s_reflect_ManifestFactory$CharManifest$() {
  if ((!$n_s_reflect_ManifestFactory$CharManifest$)) {
    $n_s_reflect_ManifestFactory$CharManifest$ = new $c_s_reflect_ManifestFactory$CharManifest$().init___()
  };
  return $n_s_reflect_ManifestFactory$CharManifest$
}
/** @constructor */
function $c_s_reflect_ManifestFactory$DoubleManifest$() {
  $c_s_reflect_AnyValManifest.call(this)
}
$c_s_reflect_ManifestFactory$DoubleManifest$.prototype = new $h_s_reflect_AnyValManifest();
$c_s_reflect_ManifestFactory$DoubleManifest$.prototype.constructor = $c_s_reflect_ManifestFactory$DoubleManifest$;
/** @constructor */
function $h_s_reflect_ManifestFactory$DoubleManifest$() {
  /*<skip>*/
}
$h_s_reflect_ManifestFactory$DoubleManifest$.prototype = $c_s_reflect_ManifestFactory$DoubleManifest$.prototype;
$c_s_reflect_ManifestFactory$DoubleManifest$.prototype.init___ = (function() {
  this.toString$1 = "Double";
  return this
});
var $d_s_reflect_ManifestFactory$DoubleManifest$ = new $TypeData().initClass({
  s_reflect_ManifestFactory$DoubleManifest$: 0
}, false, "scala.reflect.ManifestFactory$DoubleManifest$", {
  s_reflect_ManifestFactory$DoubleManifest$: 1,
  s_reflect_AnyValManifest: 1,
  O: 1,
  s_reflect_Manifest: 1,
  s_reflect_ClassTag: 1,
  s_reflect_ClassManifestDeprecatedApis: 1,
  s_reflect_OptManifest: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1,
  s_Equals: 1
});
$c_s_reflect_ManifestFactory$DoubleManifest$.prototype.$classData = $d_s_reflect_ManifestFactory$DoubleManifest$;
var $n_s_reflect_ManifestFactory$DoubleManifest$ = (void 0);
function $m_s_reflect_ManifestFactory$DoubleManifest$() {
  if ((!$n_s_reflect_ManifestFactory$DoubleManifest$)) {
    $n_s_reflect_ManifestFactory$DoubleManifest$ = new $c_s_reflect_ManifestFactory$DoubleManifest$().init___()
  };
  return $n_s_reflect_ManifestFactory$DoubleManifest$
}
/** @constructor */
function $c_s_reflect_ManifestFactory$FloatManifest$() {
  $c_s_reflect_AnyValManifest.call(this)
}
$c_s_reflect_ManifestFactory$FloatManifest$.prototype = new $h_s_reflect_AnyValManifest();
$c_s_reflect_ManifestFactory$FloatManifest$.prototype.constructor = $c_s_reflect_ManifestFactory$FloatManifest$;
/** @constructor */
function $h_s_reflect_ManifestFactory$FloatManifest$() {
  /*<skip>*/
}
$h_s_reflect_ManifestFactory$FloatManifest$.prototype = $c_s_reflect_ManifestFactory$FloatManifest$.prototype;
$c_s_reflect_ManifestFactory$FloatManifest$.prototype.init___ = (function() {
  this.toString$1 = "Float";
  return this
});
var $d_s_reflect_ManifestFactory$FloatManifest$ = new $TypeData().initClass({
  s_reflect_ManifestFactory$FloatManifest$: 0
}, false, "scala.reflect.ManifestFactory$FloatManifest$", {
  s_reflect_ManifestFactory$FloatManifest$: 1,
  s_reflect_AnyValManifest: 1,
  O: 1,
  s_reflect_Manifest: 1,
  s_reflect_ClassTag: 1,
  s_reflect_ClassManifestDeprecatedApis: 1,
  s_reflect_OptManifest: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1,
  s_Equals: 1
});
$c_s_reflect_ManifestFactory$FloatManifest$.prototype.$classData = $d_s_reflect_ManifestFactory$FloatManifest$;
var $n_s_reflect_ManifestFactory$FloatManifest$ = (void 0);
function $m_s_reflect_ManifestFactory$FloatManifest$() {
  if ((!$n_s_reflect_ManifestFactory$FloatManifest$)) {
    $n_s_reflect_ManifestFactory$FloatManifest$ = new $c_s_reflect_ManifestFactory$FloatManifest$().init___()
  };
  return $n_s_reflect_ManifestFactory$FloatManifest$
}
/** @constructor */
function $c_s_reflect_ManifestFactory$IntManifest$() {
  $c_s_reflect_AnyValManifest.call(this)
}
$c_s_reflect_ManifestFactory$IntManifest$.prototype = new $h_s_reflect_AnyValManifest();
$c_s_reflect_ManifestFactory$IntManifest$.prototype.constructor = $c_s_reflect_ManifestFactory$IntManifest$;
/** @constructor */
function $h_s_reflect_ManifestFactory$IntManifest$() {
  /*<skip>*/
}
$h_s_reflect_ManifestFactory$IntManifest$.prototype = $c_s_reflect_ManifestFactory$IntManifest$.prototype;
$c_s_reflect_ManifestFactory$IntManifest$.prototype.init___ = (function() {
  this.toString$1 = "Int";
  return this
});
var $d_s_reflect_ManifestFactory$IntManifest$ = new $TypeData().initClass({
  s_reflect_ManifestFactory$IntManifest$: 0
}, false, "scala.reflect.ManifestFactory$IntManifest$", {
  s_reflect_ManifestFactory$IntManifest$: 1,
  s_reflect_AnyValManifest: 1,
  O: 1,
  s_reflect_Manifest: 1,
  s_reflect_ClassTag: 1,
  s_reflect_ClassManifestDeprecatedApis: 1,
  s_reflect_OptManifest: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1,
  s_Equals: 1
});
$c_s_reflect_ManifestFactory$IntManifest$.prototype.$classData = $d_s_reflect_ManifestFactory$IntManifest$;
var $n_s_reflect_ManifestFactory$IntManifest$ = (void 0);
function $m_s_reflect_ManifestFactory$IntManifest$() {
  if ((!$n_s_reflect_ManifestFactory$IntManifest$)) {
    $n_s_reflect_ManifestFactory$IntManifest$ = new $c_s_reflect_ManifestFactory$IntManifest$().init___()
  };
  return $n_s_reflect_ManifestFactory$IntManifest$
}
/** @constructor */
function $c_s_reflect_ManifestFactory$LongManifest$() {
  $c_s_reflect_AnyValManifest.call(this)
}
$c_s_reflect_ManifestFactory$LongManifest$.prototype = new $h_s_reflect_AnyValManifest();
$c_s_reflect_ManifestFactory$LongManifest$.prototype.constructor = $c_s_reflect_ManifestFactory$LongManifest$;
/** @constructor */
function $h_s_reflect_ManifestFactory$LongManifest$() {
  /*<skip>*/
}
$h_s_reflect_ManifestFactory$LongManifest$.prototype = $c_s_reflect_ManifestFactory$LongManifest$.prototype;
$c_s_reflect_ManifestFactory$LongManifest$.prototype.init___ = (function() {
  this.toString$1 = "Long";
  return this
});
var $d_s_reflect_ManifestFactory$LongManifest$ = new $TypeData().initClass({
  s_reflect_ManifestFactory$LongManifest$: 0
}, false, "scala.reflect.ManifestFactory$LongManifest$", {
  s_reflect_ManifestFactory$LongManifest$: 1,
  s_reflect_AnyValManifest: 1,
  O: 1,
  s_reflect_Manifest: 1,
  s_reflect_ClassTag: 1,
  s_reflect_ClassManifestDeprecatedApis: 1,
  s_reflect_OptManifest: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1,
  s_Equals: 1
});
$c_s_reflect_ManifestFactory$LongManifest$.prototype.$classData = $d_s_reflect_ManifestFactory$LongManifest$;
var $n_s_reflect_ManifestFactory$LongManifest$ = (void 0);
function $m_s_reflect_ManifestFactory$LongManifest$() {
  if ((!$n_s_reflect_ManifestFactory$LongManifest$)) {
    $n_s_reflect_ManifestFactory$LongManifest$ = new $c_s_reflect_ManifestFactory$LongManifest$().init___()
  };
  return $n_s_reflect_ManifestFactory$LongManifest$
}
/** @constructor */
function $c_s_reflect_ManifestFactory$PhantomManifest() {
  $c_s_reflect_ManifestFactory$ClassTypeManifest.call(this);
  this.toString$2 = null
}
$c_s_reflect_ManifestFactory$PhantomManifest.prototype = new $h_s_reflect_ManifestFactory$ClassTypeManifest();
$c_s_reflect_ManifestFactory$PhantomManifest.prototype.constructor = $c_s_reflect_ManifestFactory$PhantomManifest;
/** @constructor */
function $h_s_reflect_ManifestFactory$PhantomManifest() {
  /*<skip>*/
}
$h_s_reflect_ManifestFactory$PhantomManifest.prototype = $c_s_reflect_ManifestFactory$PhantomManifest.prototype;
$c_s_reflect_ManifestFactory$PhantomManifest.prototype.equals__O__Z = (function(that) {
  return (this === that)
});
$c_s_reflect_ManifestFactory$PhantomManifest.prototype.toString__T = (function() {
  return this.toString$2
});
$c_s_reflect_ManifestFactory$PhantomManifest.prototype.hashCode__I = (function() {
  return $systemIdentityHashCode(this)
});
/** @constructor */
function $c_s_reflect_ManifestFactory$ShortManifest$() {
  $c_s_reflect_AnyValManifest.call(this)
}
$c_s_reflect_ManifestFactory$ShortManifest$.prototype = new $h_s_reflect_AnyValManifest();
$c_s_reflect_ManifestFactory$ShortManifest$.prototype.constructor = $c_s_reflect_ManifestFactory$ShortManifest$;
/** @constructor */
function $h_s_reflect_ManifestFactory$ShortManifest$() {
  /*<skip>*/
}
$h_s_reflect_ManifestFactory$ShortManifest$.prototype = $c_s_reflect_ManifestFactory$ShortManifest$.prototype;
$c_s_reflect_ManifestFactory$ShortManifest$.prototype.init___ = (function() {
  this.toString$1 = "Short";
  return this
});
var $d_s_reflect_ManifestFactory$ShortManifest$ = new $TypeData().initClass({
  s_reflect_ManifestFactory$ShortManifest$: 0
}, false, "scala.reflect.ManifestFactory$ShortManifest$", {
  s_reflect_ManifestFactory$ShortManifest$: 1,
  s_reflect_AnyValManifest: 1,
  O: 1,
  s_reflect_Manifest: 1,
  s_reflect_ClassTag: 1,
  s_reflect_ClassManifestDeprecatedApis: 1,
  s_reflect_OptManifest: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1,
  s_Equals: 1
});
$c_s_reflect_ManifestFactory$ShortManifest$.prototype.$classData = $d_s_reflect_ManifestFactory$ShortManifest$;
var $n_s_reflect_ManifestFactory$ShortManifest$ = (void 0);
function $m_s_reflect_ManifestFactory$ShortManifest$() {
  if ((!$n_s_reflect_ManifestFactory$ShortManifest$)) {
    $n_s_reflect_ManifestFactory$ShortManifest$ = new $c_s_reflect_ManifestFactory$ShortManifest$().init___()
  };
  return $n_s_reflect_ManifestFactory$ShortManifest$
}
/** @constructor */
function $c_s_reflect_ManifestFactory$UnitManifest$() {
  $c_s_reflect_AnyValManifest.call(this)
}
$c_s_reflect_ManifestFactory$UnitManifest$.prototype = new $h_s_reflect_AnyValManifest();
$c_s_reflect_ManifestFactory$UnitManifest$.prototype.constructor = $c_s_reflect_ManifestFactory$UnitManifest$;
/** @constructor */
function $h_s_reflect_ManifestFactory$UnitManifest$() {
  /*<skip>*/
}
$h_s_reflect_ManifestFactory$UnitManifest$.prototype = $c_s_reflect_ManifestFactory$UnitManifest$.prototype;
$c_s_reflect_ManifestFactory$UnitManifest$.prototype.init___ = (function() {
  this.toString$1 = "Unit";
  return this
});
var $d_s_reflect_ManifestFactory$UnitManifest$ = new $TypeData().initClass({
  s_reflect_ManifestFactory$UnitManifest$: 0
}, false, "scala.reflect.ManifestFactory$UnitManifest$", {
  s_reflect_ManifestFactory$UnitManifest$: 1,
  s_reflect_AnyValManifest: 1,
  O: 1,
  s_reflect_Manifest: 1,
  s_reflect_ClassTag: 1,
  s_reflect_ClassManifestDeprecatedApis: 1,
  s_reflect_OptManifest: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1,
  s_Equals: 1
});
$c_s_reflect_ManifestFactory$UnitManifest$.prototype.$classData = $d_s_reflect_ManifestFactory$UnitManifest$;
var $n_s_reflect_ManifestFactory$UnitManifest$ = (void 0);
function $m_s_reflect_ManifestFactory$UnitManifest$() {
  if ((!$n_s_reflect_ManifestFactory$UnitManifest$)) {
    $n_s_reflect_ManifestFactory$UnitManifest$ = new $c_s_reflect_ManifestFactory$UnitManifest$().init___()
  };
  return $n_s_reflect_ManifestFactory$UnitManifest$
}
/** @constructor */
function $c_sci_List$() {
  $c_scg_SeqFactory.call(this);
  this.partialNotApplied$5 = null
}
$c_sci_List$.prototype = new $h_scg_SeqFactory();
$c_sci_List$.prototype.constructor = $c_sci_List$;
/** @constructor */
function $h_sci_List$() {
  /*<skip>*/
}
$h_sci_List$.prototype = $c_sci_List$.prototype;
$c_sci_List$.prototype.init___ = (function() {
  $c_scg_GenTraversableFactory.prototype.init___.call(this);
  $n_sci_List$ = this;
  this.partialNotApplied$5 = new $c_sci_List$$anon$1().init___();
  return this
});
var $d_sci_List$ = new $TypeData().initClass({
  sci_List$: 0
}, false, "scala.collection.immutable.List$", {
  sci_List$: 1,
  scg_SeqFactory: 1,
  scg_GenSeqFactory: 1,
  scg_GenTraversableFactory: 1,
  scg_GenericCompanion: 1,
  O: 1,
  scg_TraversableFactory: 1,
  scg_GenericSeqCompanion: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_sci_List$.prototype.$classData = $d_sci_List$;
var $n_sci_List$ = (void 0);
function $m_sci_List$() {
  if ((!$n_sci_List$)) {
    $n_sci_List$ = new $c_sci_List$().init___()
  };
  return $n_sci_List$
}
/** @constructor */
function $c_sci_Stream$() {
  $c_scg_SeqFactory.call(this)
}
$c_sci_Stream$.prototype = new $h_scg_SeqFactory();
$c_sci_Stream$.prototype.constructor = $c_sci_Stream$;
/** @constructor */
function $h_sci_Stream$() {
  /*<skip>*/
}
$h_sci_Stream$.prototype = $c_sci_Stream$.prototype;
$c_sci_Stream$.prototype.init___ = (function() {
  $c_scg_GenTraversableFactory.prototype.init___.call(this);
  return this
});
var $d_sci_Stream$ = new $TypeData().initClass({
  sci_Stream$: 0
}, false, "scala.collection.immutable.Stream$", {
  sci_Stream$: 1,
  scg_SeqFactory: 1,
  scg_GenSeqFactory: 1,
  scg_GenTraversableFactory: 1,
  scg_GenericCompanion: 1,
  O: 1,
  scg_TraversableFactory: 1,
  scg_GenericSeqCompanion: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_sci_Stream$.prototype.$classData = $d_sci_Stream$;
var $n_sci_Stream$ = (void 0);
function $m_sci_Stream$() {
  if ((!$n_sci_Stream$)) {
    $n_sci_Stream$ = new $c_sci_Stream$().init___()
  };
  return $n_sci_Stream$
}
/** @constructor */
function $c_s_reflect_ManifestFactory$AnyManifest$() {
  $c_s_reflect_ManifestFactory$PhantomManifest.call(this)
}
$c_s_reflect_ManifestFactory$AnyManifest$.prototype = new $h_s_reflect_ManifestFactory$PhantomManifest();
$c_s_reflect_ManifestFactory$AnyManifest$.prototype.constructor = $c_s_reflect_ManifestFactory$AnyManifest$;
/** @constructor */
function $h_s_reflect_ManifestFactory$AnyManifest$() {
  /*<skip>*/
}
$h_s_reflect_ManifestFactory$AnyManifest$.prototype = $c_s_reflect_ManifestFactory$AnyManifest$.prototype;
$c_s_reflect_ManifestFactory$AnyManifest$.prototype.init___ = (function() {
  this.toString$2 = "Any";
  var prefix = $m_s_None$();
  var typeArguments = $m_sci_Nil$();
  this.prefix$1 = prefix;
  this.runtimeClass1$1 = $d_O.getClassOf();
  this.typeArguments$1 = typeArguments;
  return this
});
var $d_s_reflect_ManifestFactory$AnyManifest$ = new $TypeData().initClass({
  s_reflect_ManifestFactory$AnyManifest$: 0
}, false, "scala.reflect.ManifestFactory$AnyManifest$", {
  s_reflect_ManifestFactory$AnyManifest$: 1,
  s_reflect_ManifestFactory$PhantomManifest: 1,
  s_reflect_ManifestFactory$ClassTypeManifest: 1,
  O: 1,
  s_reflect_Manifest: 1,
  s_reflect_ClassTag: 1,
  s_reflect_ClassManifestDeprecatedApis: 1,
  s_reflect_OptManifest: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1,
  s_Equals: 1
});
$c_s_reflect_ManifestFactory$AnyManifest$.prototype.$classData = $d_s_reflect_ManifestFactory$AnyManifest$;
var $n_s_reflect_ManifestFactory$AnyManifest$ = (void 0);
function $m_s_reflect_ManifestFactory$AnyManifest$() {
  if ((!$n_s_reflect_ManifestFactory$AnyManifest$)) {
    $n_s_reflect_ManifestFactory$AnyManifest$ = new $c_s_reflect_ManifestFactory$AnyManifest$().init___()
  };
  return $n_s_reflect_ManifestFactory$AnyManifest$
}
/** @constructor */
function $c_s_reflect_ManifestFactory$AnyValManifest$() {
  $c_s_reflect_ManifestFactory$PhantomManifest.call(this)
}
$c_s_reflect_ManifestFactory$AnyValManifest$.prototype = new $h_s_reflect_ManifestFactory$PhantomManifest();
$c_s_reflect_ManifestFactory$AnyValManifest$.prototype.constructor = $c_s_reflect_ManifestFactory$AnyValManifest$;
/** @constructor */
function $h_s_reflect_ManifestFactory$AnyValManifest$() {
  /*<skip>*/
}
$h_s_reflect_ManifestFactory$AnyValManifest$.prototype = $c_s_reflect_ManifestFactory$AnyValManifest$.prototype;
$c_s_reflect_ManifestFactory$AnyValManifest$.prototype.init___ = (function() {
  this.toString$2 = "AnyVal";
  var prefix = $m_s_None$();
  var typeArguments = $m_sci_Nil$();
  this.prefix$1 = prefix;
  this.runtimeClass1$1 = $d_O.getClassOf();
  this.typeArguments$1 = typeArguments;
  return this
});
var $d_s_reflect_ManifestFactory$AnyValManifest$ = new $TypeData().initClass({
  s_reflect_ManifestFactory$AnyValManifest$: 0
}, false, "scala.reflect.ManifestFactory$AnyValManifest$", {
  s_reflect_ManifestFactory$AnyValManifest$: 1,
  s_reflect_ManifestFactory$PhantomManifest: 1,
  s_reflect_ManifestFactory$ClassTypeManifest: 1,
  O: 1,
  s_reflect_Manifest: 1,
  s_reflect_ClassTag: 1,
  s_reflect_ClassManifestDeprecatedApis: 1,
  s_reflect_OptManifest: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1,
  s_Equals: 1
});
$c_s_reflect_ManifestFactory$AnyValManifest$.prototype.$classData = $d_s_reflect_ManifestFactory$AnyValManifest$;
var $n_s_reflect_ManifestFactory$AnyValManifest$ = (void 0);
function $m_s_reflect_ManifestFactory$AnyValManifest$() {
  if ((!$n_s_reflect_ManifestFactory$AnyValManifest$)) {
    $n_s_reflect_ManifestFactory$AnyValManifest$ = new $c_s_reflect_ManifestFactory$AnyValManifest$().init___()
  };
  return $n_s_reflect_ManifestFactory$AnyValManifest$
}
/** @constructor */
function $c_s_reflect_ManifestFactory$NothingManifest$() {
  $c_s_reflect_ManifestFactory$PhantomManifest.call(this)
}
$c_s_reflect_ManifestFactory$NothingManifest$.prototype = new $h_s_reflect_ManifestFactory$PhantomManifest();
$c_s_reflect_ManifestFactory$NothingManifest$.prototype.constructor = $c_s_reflect_ManifestFactory$NothingManifest$;
/** @constructor */
function $h_s_reflect_ManifestFactory$NothingManifest$() {
  /*<skip>*/
}
$h_s_reflect_ManifestFactory$NothingManifest$.prototype = $c_s_reflect_ManifestFactory$NothingManifest$.prototype;
$c_s_reflect_ManifestFactory$NothingManifest$.prototype.init___ = (function() {
  this.toString$2 = "Nothing";
  var prefix = $m_s_None$();
  var typeArguments = $m_sci_Nil$();
  this.prefix$1 = prefix;
  this.runtimeClass1$1 = $d_sr_Nothing$.getClassOf();
  this.typeArguments$1 = typeArguments;
  return this
});
var $d_s_reflect_ManifestFactory$NothingManifest$ = new $TypeData().initClass({
  s_reflect_ManifestFactory$NothingManifest$: 0
}, false, "scala.reflect.ManifestFactory$NothingManifest$", {
  s_reflect_ManifestFactory$NothingManifest$: 1,
  s_reflect_ManifestFactory$PhantomManifest: 1,
  s_reflect_ManifestFactory$ClassTypeManifest: 1,
  O: 1,
  s_reflect_Manifest: 1,
  s_reflect_ClassTag: 1,
  s_reflect_ClassManifestDeprecatedApis: 1,
  s_reflect_OptManifest: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1,
  s_Equals: 1
});
$c_s_reflect_ManifestFactory$NothingManifest$.prototype.$classData = $d_s_reflect_ManifestFactory$NothingManifest$;
var $n_s_reflect_ManifestFactory$NothingManifest$ = (void 0);
function $m_s_reflect_ManifestFactory$NothingManifest$() {
  if ((!$n_s_reflect_ManifestFactory$NothingManifest$)) {
    $n_s_reflect_ManifestFactory$NothingManifest$ = new $c_s_reflect_ManifestFactory$NothingManifest$().init___()
  };
  return $n_s_reflect_ManifestFactory$NothingManifest$
}
/** @constructor */
function $c_s_reflect_ManifestFactory$NullManifest$() {
  $c_s_reflect_ManifestFactory$PhantomManifest.call(this)
}
$c_s_reflect_ManifestFactory$NullManifest$.prototype = new $h_s_reflect_ManifestFactory$PhantomManifest();
$c_s_reflect_ManifestFactory$NullManifest$.prototype.constructor = $c_s_reflect_ManifestFactory$NullManifest$;
/** @constructor */
function $h_s_reflect_ManifestFactory$NullManifest$() {
  /*<skip>*/
}
$h_s_reflect_ManifestFactory$NullManifest$.prototype = $c_s_reflect_ManifestFactory$NullManifest$.prototype;
$c_s_reflect_ManifestFactory$NullManifest$.prototype.init___ = (function() {
  this.toString$2 = "Null";
  var prefix = $m_s_None$();
  var typeArguments = $m_sci_Nil$();
  this.prefix$1 = prefix;
  this.runtimeClass1$1 = $d_sr_Null$.getClassOf();
  this.typeArguments$1 = typeArguments;
  return this
});
var $d_s_reflect_ManifestFactory$NullManifest$ = new $TypeData().initClass({
  s_reflect_ManifestFactory$NullManifest$: 0
}, false, "scala.reflect.ManifestFactory$NullManifest$", {
  s_reflect_ManifestFactory$NullManifest$: 1,
  s_reflect_ManifestFactory$PhantomManifest: 1,
  s_reflect_ManifestFactory$ClassTypeManifest: 1,
  O: 1,
  s_reflect_Manifest: 1,
  s_reflect_ClassTag: 1,
  s_reflect_ClassManifestDeprecatedApis: 1,
  s_reflect_OptManifest: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1,
  s_Equals: 1
});
$c_s_reflect_ManifestFactory$NullManifest$.prototype.$classData = $d_s_reflect_ManifestFactory$NullManifest$;
var $n_s_reflect_ManifestFactory$NullManifest$ = (void 0);
function $m_s_reflect_ManifestFactory$NullManifest$() {
  if ((!$n_s_reflect_ManifestFactory$NullManifest$)) {
    $n_s_reflect_ManifestFactory$NullManifest$ = new $c_s_reflect_ManifestFactory$NullManifest$().init___()
  };
  return $n_s_reflect_ManifestFactory$NullManifest$
}
/** @constructor */
function $c_s_reflect_ManifestFactory$ObjectManifest$() {
  $c_s_reflect_ManifestFactory$PhantomManifest.call(this)
}
$c_s_reflect_ManifestFactory$ObjectManifest$.prototype = new $h_s_reflect_ManifestFactory$PhantomManifest();
$c_s_reflect_ManifestFactory$ObjectManifest$.prototype.constructor = $c_s_reflect_ManifestFactory$ObjectManifest$;
/** @constructor */
function $h_s_reflect_ManifestFactory$ObjectManifest$() {
  /*<skip>*/
}
$h_s_reflect_ManifestFactory$ObjectManifest$.prototype = $c_s_reflect_ManifestFactory$ObjectManifest$.prototype;
$c_s_reflect_ManifestFactory$ObjectManifest$.prototype.init___ = (function() {
  this.toString$2 = "Object";
  var prefix = $m_s_None$();
  var typeArguments = $m_sci_Nil$();
  this.prefix$1 = prefix;
  this.runtimeClass1$1 = $d_O.getClassOf();
  this.typeArguments$1 = typeArguments;
  return this
});
var $d_s_reflect_ManifestFactory$ObjectManifest$ = new $TypeData().initClass({
  s_reflect_ManifestFactory$ObjectManifest$: 0
}, false, "scala.reflect.ManifestFactory$ObjectManifest$", {
  s_reflect_ManifestFactory$ObjectManifest$: 1,
  s_reflect_ManifestFactory$PhantomManifest: 1,
  s_reflect_ManifestFactory$ClassTypeManifest: 1,
  O: 1,
  s_reflect_Manifest: 1,
  s_reflect_ClassTag: 1,
  s_reflect_ClassManifestDeprecatedApis: 1,
  s_reflect_OptManifest: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1,
  s_Equals: 1
});
$c_s_reflect_ManifestFactory$ObjectManifest$.prototype.$classData = $d_s_reflect_ManifestFactory$ObjectManifest$;
var $n_s_reflect_ManifestFactory$ObjectManifest$ = (void 0);
function $m_s_reflect_ManifestFactory$ObjectManifest$() {
  if ((!$n_s_reflect_ManifestFactory$ObjectManifest$)) {
    $n_s_reflect_ManifestFactory$ObjectManifest$ = new $c_s_reflect_ManifestFactory$ObjectManifest$().init___()
  };
  return $n_s_reflect_ManifestFactory$ObjectManifest$
}
function $is_sc_GenSeq(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.sc_GenSeq)))
}
function $as_sc_GenSeq(obj) {
  return (($is_sc_GenSeq(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "scala.collection.GenSeq"))
}
function $isArrayOf_sc_GenSeq(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.sc_GenSeq)))
}
function $asArrayOf_sc_GenSeq(obj, depth) {
  return (($isArrayOf_sc_GenSeq(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Lscala.collection.GenSeq;", depth))
}
/** @constructor */
function $c_sci_Vector$() {
  $c_scg_IndexedSeqFactory.call(this);
  this.NIL$6 = null;
  this.Log2ConcatFaster$6 = 0;
  this.TinyAppendFaster$6 = 0
}
$c_sci_Vector$.prototype = new $h_scg_IndexedSeqFactory();
$c_sci_Vector$.prototype.constructor = $c_sci_Vector$;
/** @constructor */
function $h_sci_Vector$() {
  /*<skip>*/
}
$h_sci_Vector$.prototype = $c_sci_Vector$.prototype;
$c_sci_Vector$.prototype.init___ = (function() {
  $c_scg_GenTraversableFactory.prototype.init___.call(this);
  $n_sci_Vector$ = this;
  this.NIL$6 = new $c_sci_Vector().init___I__I__I(0, 0, 0);
  return this
});
var $d_sci_Vector$ = new $TypeData().initClass({
  sci_Vector$: 0
}, false, "scala.collection.immutable.Vector$", {
  sci_Vector$: 1,
  scg_IndexedSeqFactory: 1,
  scg_SeqFactory: 1,
  scg_GenSeqFactory: 1,
  scg_GenTraversableFactory: 1,
  scg_GenericCompanion: 1,
  O: 1,
  scg_TraversableFactory: 1,
  scg_GenericSeqCompanion: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_sci_Vector$.prototype.$classData = $d_sci_Vector$;
var $n_sci_Vector$ = (void 0);
function $m_sci_Vector$() {
  if ((!$n_sci_Vector$)) {
    $n_sci_Vector$ = new $c_sci_Vector$().init___()
  };
  return $n_sci_Vector$
}
/** @constructor */
function $c_sc_AbstractTraversable() {
  $c_O.call(this)
}
$c_sc_AbstractTraversable.prototype = new $h_O();
$c_sc_AbstractTraversable.prototype.constructor = $c_sc_AbstractTraversable;
/** @constructor */
function $h_sc_AbstractTraversable() {
  /*<skip>*/
}
$h_sc_AbstractTraversable.prototype = $c_sc_AbstractTraversable.prototype;
$c_sc_AbstractTraversable.prototype.repr__O = (function() {
  return this
});
$c_sc_AbstractTraversable.prototype.stringPrefix__T = (function() {
  return $s_sc_TraversableLike$class__stringPrefix__sc_TraversableLike__T(this)
});
/** @constructor */
function $c_sc_AbstractIterable() {
  $c_sc_AbstractTraversable.call(this)
}
$c_sc_AbstractIterable.prototype = new $h_sc_AbstractTraversable();
$c_sc_AbstractIterable.prototype.constructor = $c_sc_AbstractIterable;
/** @constructor */
function $h_sc_AbstractIterable() {
  /*<skip>*/
}
$h_sc_AbstractIterable.prototype = $c_sc_AbstractIterable.prototype;
$c_sc_AbstractIterable.prototype.sameElements__sc_GenIterable__Z = (function(that) {
  return $s_sc_IterableLike$class__sameElements__sc_IterableLike__sc_GenIterable__Z(this, that)
});
$c_sc_AbstractIterable.prototype.foreach__F1__V = (function(f) {
  var this$1 = this.iterator__sc_Iterator();
  $s_sc_Iterator$class__foreach__sc_Iterator__F1__V(this$1, f)
});
/** @constructor */
function $c_sci_StringOps() {
  $c_O.call(this);
  this.repr$1 = null
}
$c_sci_StringOps.prototype = new $h_O();
$c_sci_StringOps.prototype.constructor = $c_sci_StringOps;
/** @constructor */
function $h_sci_StringOps() {
  /*<skip>*/
}
$h_sci_StringOps.prototype = $c_sci_StringOps.prototype;
$c_sci_StringOps.prototype.apply__I__O = (function(idx) {
  var $$this = this.repr$1;
  var c = (65535 & $uI($$this.charCodeAt(idx)));
  return new $c_jl_Character().init___C(c)
});
$c_sci_StringOps.prototype.lengthCompare__I__I = (function(len) {
  return $s_sc_IndexedSeqOptimized$class__lengthCompare__sc_IndexedSeqOptimized__I__I(this, len)
});
$c_sci_StringOps.prototype.sameElements__sc_GenIterable__Z = (function(that) {
  return $s_sc_IndexedSeqOptimized$class__sameElements__sc_IndexedSeqOptimized__sc_GenIterable__Z(this, that)
});
$c_sci_StringOps.prototype.isEmpty__Z = (function() {
  return $s_sc_IndexedSeqOptimized$class__isEmpty__sc_IndexedSeqOptimized__Z(this)
});
$c_sci_StringOps.prototype.equals__O__Z = (function(x$1) {
  return $m_sci_StringOps$().equals$extension__T__O__Z(this.repr$1, x$1)
});
$c_sci_StringOps.prototype.toString__T = (function() {
  var $$this = this.repr$1;
  return $$this
});
$c_sci_StringOps.prototype.foreach__F1__V = (function(f) {
  $s_sc_IndexedSeqOptimized$class__foreach__sc_IndexedSeqOptimized__F1__V(this, f)
});
$c_sci_StringOps.prototype.iterator__sc_Iterator = (function() {
  var $$this = this.repr$1;
  return new $c_sc_IndexedSeqLike$Elements().init___sc_IndexedSeqLike__I__I(this, 0, $uI($$this.length))
});
$c_sci_StringOps.prototype.length__I = (function() {
  var $$this = this.repr$1;
  return $uI($$this.length)
});
$c_sci_StringOps.prototype.repr__O = (function() {
  return this.repr$1
});
$c_sci_StringOps.prototype.init___T = (function(repr) {
  this.repr$1 = repr;
  return this
});
$c_sci_StringOps.prototype.hashCode__I = (function() {
  var $$this = this.repr$1;
  return $m_sjsr_RuntimeString$().hashCode__T__I($$this)
});
$c_sci_StringOps.prototype.stringPrefix__T = (function() {
  return $s_sc_TraversableLike$class__stringPrefix__sc_TraversableLike__T(this)
});
function $is_sci_StringOps(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.sci_StringOps)))
}
function $as_sci_StringOps(obj) {
  return (($is_sci_StringOps(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "scala.collection.immutable.StringOps"))
}
function $isArrayOf_sci_StringOps(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.sci_StringOps)))
}
function $asArrayOf_sci_StringOps(obj, depth) {
  return (($isArrayOf_sci_StringOps(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Lscala.collection.immutable.StringOps;", depth))
}
var $d_sci_StringOps = new $TypeData().initClass({
  sci_StringOps: 0
}, false, "scala.collection.immutable.StringOps", {
  sci_StringOps: 1,
  O: 1,
  sci_StringLike: 1,
  sc_IndexedSeqOptimized: 1,
  sc_IndexedSeqLike: 1,
  sc_SeqLike: 1,
  sc_IterableLike: 1,
  s_Equals: 1,
  sc_TraversableLike: 1,
  scg_HasNewBuilder: 1,
  scg_FilterMonadic: 1,
  sc_TraversableOnce: 1,
  sc_GenTraversableOnce: 1,
  sc_GenTraversableLike: 1,
  sc_Parallelizable: 1,
  sc_GenIterableLike: 1,
  sc_GenSeqLike: 1,
  s_math_Ordered: 1,
  jl_Comparable: 1
});
$c_sci_StringOps.prototype.$classData = $d_sci_StringOps;
function $is_sc_Seq(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.sc_Seq)))
}
function $as_sc_Seq(obj) {
  return (($is_sc_Seq(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "scala.collection.Seq"))
}
function $isArrayOf_sc_Seq(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.sc_Seq)))
}
function $asArrayOf_sc_Seq(obj, depth) {
  return (($isArrayOf_sc_Seq(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Lscala.collection.Seq;", depth))
}
var $d_sc_Seq = new $TypeData().initClass({
  sc_Seq: 0
}, true, "scala.collection.Seq", {
  sc_Seq: 1,
  s_PartialFunction: 1,
  F1: 1,
  sc_Iterable: 1,
  sc_Traversable: 1,
  sc_TraversableLike: 1,
  scg_HasNewBuilder: 1,
  scg_FilterMonadic: 1,
  sc_TraversableOnce: 1,
  sc_GenTraversableOnce: 1,
  sc_GenTraversableLike: 1,
  sc_Parallelizable: 1,
  sc_GenTraversable: 1,
  scg_GenericTraversableTemplate: 1,
  sc_GenIterable: 1,
  sc_GenIterableLike: 1,
  sc_IterableLike: 1,
  s_Equals: 1,
  sc_GenSeq: 1,
  sc_GenSeqLike: 1,
  sc_SeqLike: 1
});
function $is_sjs_js_ArrayOps(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.sjs_js_ArrayOps)))
}
function $as_sjs_js_ArrayOps(obj) {
  return (($is_sjs_js_ArrayOps(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "scala.scalajs.js.ArrayOps"))
}
function $isArrayOf_sjs_js_ArrayOps(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.sjs_js_ArrayOps)))
}
function $asArrayOf_sjs_js_ArrayOps(obj, depth) {
  return (($isArrayOf_sjs_js_ArrayOps(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Lscala.scalajs.js.ArrayOps;", depth))
}
function $is_sc_IndexedSeq(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.sc_IndexedSeq)))
}
function $as_sc_IndexedSeq(obj) {
  return (($is_sc_IndexedSeq(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "scala.collection.IndexedSeq"))
}
function $isArrayOf_sc_IndexedSeq(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.sc_IndexedSeq)))
}
function $asArrayOf_sc_IndexedSeq(obj, depth) {
  return (($isArrayOf_sc_IndexedSeq(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Lscala.collection.IndexedSeq;", depth))
}
function $is_sc_LinearSeq(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.sc_LinearSeq)))
}
function $as_sc_LinearSeq(obj) {
  return (($is_sc_LinearSeq(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "scala.collection.LinearSeq"))
}
function $isArrayOf_sc_LinearSeq(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.sc_LinearSeq)))
}
function $asArrayOf_sc_LinearSeq(obj, depth) {
  return (($isArrayOf_sc_LinearSeq(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Lscala.collection.LinearSeq;", depth))
}
/** @constructor */
function $c_sc_AbstractSeq() {
  $c_sc_AbstractIterable.call(this)
}
$c_sc_AbstractSeq.prototype = new $h_sc_AbstractIterable();
$c_sc_AbstractSeq.prototype.constructor = $c_sc_AbstractSeq;
/** @constructor */
function $h_sc_AbstractSeq() {
  /*<skip>*/
}
$h_sc_AbstractSeq.prototype = $c_sc_AbstractSeq.prototype;
$c_sc_AbstractSeq.prototype.equals__O__Z = (function(that) {
  return $s_sc_GenSeqLike$class__equals__sc_GenSeqLike__O__Z(this, that)
});
$c_sc_AbstractSeq.prototype.isEmpty__Z = (function() {
  return $s_sc_SeqLike$class__isEmpty__sc_SeqLike__Z(this)
});
$c_sc_AbstractSeq.prototype.toString__T = (function() {
  return $s_sc_TraversableLike$class__toString__sc_TraversableLike__T(this)
});
/** @constructor */
function $c_scm_AbstractSeq() {
  $c_sc_AbstractSeq.call(this)
}
$c_scm_AbstractSeq.prototype = new $h_sc_AbstractSeq();
$c_scm_AbstractSeq.prototype.constructor = $c_scm_AbstractSeq;
/** @constructor */
function $h_scm_AbstractSeq() {
  /*<skip>*/
}
$h_scm_AbstractSeq.prototype = $c_scm_AbstractSeq.prototype;
/** @constructor */
function $c_sci_List() {
  $c_sc_AbstractSeq.call(this)
}
$c_sci_List.prototype = new $h_sc_AbstractSeq();
$c_sci_List.prototype.constructor = $c_sci_List;
/** @constructor */
function $h_sci_List() {
  /*<skip>*/
}
$h_sci_List.prototype = $c_sci_List.prototype;
$c_sci_List.prototype.apply__I__O = (function(n) {
  return $s_sc_LinearSeqOptimized$class__apply__sc_LinearSeqOptimized__I__O(this, n)
});
$c_sci_List.prototype.lengthCompare__I__I = (function(len) {
  return $s_sc_LinearSeqOptimized$class__lengthCompare__sc_LinearSeqOptimized__I__I(this, len)
});
$c_sci_List.prototype.apply__O__O = (function(v1) {
  var n = $uI(v1);
  return $s_sc_LinearSeqOptimized$class__apply__sc_LinearSeqOptimized__I__O(this, n)
});
$c_sci_List.prototype.sameElements__sc_GenIterable__Z = (function(that) {
  return $s_sc_LinearSeqOptimized$class__sameElements__sc_LinearSeqOptimized__sc_GenIterable__Z(this, that)
});
$c_sci_List.prototype.foreach__F1__V = (function(f) {
  var these = this;
  while ((!these.isEmpty__Z())) {
    f.apply__O__O(these.head__O());
    var this$1 = these;
    these = this$1.tail__sci_List()
  }
});
$c_sci_List.prototype.iterator__sc_Iterator = (function() {
  return new $c_sc_LinearSeqLike$$anon$1().init___sc_LinearSeqLike(this)
});
$c_sci_List.prototype.drop__I__sci_List = (function(n) {
  var these = this;
  var count = n;
  while (((!these.isEmpty__Z()) && (count > 0))) {
    var this$1 = these;
    these = this$1.tail__sci_List();
    count = (((-1) + count) | 0)
  };
  return these
});
$c_sci_List.prototype.length__I = (function() {
  return $s_sc_LinearSeqOptimized$class__length__sc_LinearSeqOptimized__I(this)
});
$c_sci_List.prototype.hashCode__I = (function() {
  return $m_s_util_hashing_MurmurHash3$().seqHash__sc_Seq__I(this)
});
$c_sci_List.prototype.stringPrefix__T = (function() {
  return "List"
});
function $is_sci_List(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.sci_List)))
}
function $as_sci_List(obj) {
  return (($is_sci_List(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "scala.collection.immutable.List"))
}
function $isArrayOf_sci_List(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.sci_List)))
}
function $asArrayOf_sci_List(obj, depth) {
  return (($isArrayOf_sci_List(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Lscala.collection.immutable.List;", depth))
}
/** @constructor */
function $c_sci_Vector() {
  $c_sc_AbstractSeq.call(this);
  this.startIndex$4 = 0;
  this.endIndex$4 = 0;
  this.focus$4 = 0;
  this.dirty$4 = false;
  this.depth$4 = 0;
  this.display0$4 = null;
  this.display1$4 = null;
  this.display2$4 = null;
  this.display3$4 = null;
  this.display4$4 = null;
  this.display5$4 = null
}
$c_sci_Vector.prototype = new $h_sc_AbstractSeq();
$c_sci_Vector.prototype.constructor = $c_sci_Vector;
/** @constructor */
function $h_sci_Vector() {
  /*<skip>*/
}
$h_sci_Vector.prototype = $c_sci_Vector.prototype;
$c_sci_Vector.prototype.checkRangeConvert__p4__I__I = (function(index) {
  var idx = ((index + this.startIndex$4) | 0);
  if (((index >= 0) && (idx < this.endIndex$4))) {
    return idx
  } else {
    throw new $c_jl_IndexOutOfBoundsException().init___T(("" + index))
  }
});
$c_sci_Vector.prototype.display3__AO = (function() {
  return this.display3$4
});
$c_sci_Vector.prototype.apply__I__O = (function(index) {
  var idx = this.checkRangeConvert__p4__I__I(index);
  var xor = (idx ^ this.focus$4);
  return $s_sci_VectorPointer$class__getElem__sci_VectorPointer__I__I__O(this, idx, xor)
});
$c_sci_Vector.prototype.depth__I = (function() {
  return this.depth$4
});
$c_sci_Vector.prototype.lengthCompare__I__I = (function(len) {
  return ((this.length__I() - len) | 0)
});
$c_sci_Vector.prototype.apply__O__O = (function(v1) {
  return this.apply__I__O($uI(v1))
});
$c_sci_Vector.prototype.initIterator__sci_VectorIterator__V = (function(s) {
  var depth = this.depth$4;
  $s_sci_VectorPointer$class__initFrom__sci_VectorPointer__sci_VectorPointer__I__V(s, this, depth);
  if (this.dirty$4) {
    var index = this.focus$4;
    $s_sci_VectorPointer$class__stabilize__sci_VectorPointer__I__V(s, index)
  };
  if ((s.depth$2 > 1)) {
    var index$1 = this.startIndex$4;
    var xor = (this.startIndex$4 ^ this.focus$4);
    $s_sci_VectorPointer$class__gotoPos__sci_VectorPointer__I__I__V(s, index$1, xor)
  }
});
$c_sci_Vector.prototype.init___I__I__I = (function(startIndex, endIndex, focus) {
  this.startIndex$4 = startIndex;
  this.endIndex$4 = endIndex;
  this.focus$4 = focus;
  this.dirty$4 = false;
  return this
});
$c_sci_Vector.prototype.display5$und$eq__AO__V = (function(x$1) {
  this.display5$4 = x$1
});
$c_sci_Vector.prototype.display0__AO = (function() {
  return this.display0$4
});
$c_sci_Vector.prototype.display4__AO = (function() {
  return this.display4$4
});
$c_sci_Vector.prototype.display2$und$eq__AO__V = (function(x$1) {
  this.display2$4 = x$1
});
$c_sci_Vector.prototype.iterator__sc_Iterator = (function() {
  return this.iterator__sci_VectorIterator()
});
$c_sci_Vector.prototype.display1$und$eq__AO__V = (function(x$1) {
  this.display1$4 = x$1
});
$c_sci_Vector.prototype.display4$und$eq__AO__V = (function(x$1) {
  this.display4$4 = x$1
});
$c_sci_Vector.prototype.length__I = (function() {
  return ((this.endIndex$4 - this.startIndex$4) | 0)
});
$c_sci_Vector.prototype.display1__AO = (function() {
  return this.display1$4
});
$c_sci_Vector.prototype.display5__AO = (function() {
  return this.display5$4
});
$c_sci_Vector.prototype.iterator__sci_VectorIterator = (function() {
  var s = new $c_sci_VectorIterator().init___I__I(this.startIndex$4, this.endIndex$4);
  this.initIterator__sci_VectorIterator__V(s);
  return s
});
$c_sci_Vector.prototype.hashCode__I = (function() {
  return $m_s_util_hashing_MurmurHash3$().seqHash__sc_Seq__I(this)
});
$c_sci_Vector.prototype.depth$und$eq__I__V = (function(x$1) {
  this.depth$4 = x$1
});
$c_sci_Vector.prototype.display2__AO = (function() {
  return this.display2$4
});
$c_sci_Vector.prototype.display0$und$eq__AO__V = (function(x$1) {
  this.display0$4 = x$1
});
$c_sci_Vector.prototype.display3$und$eq__AO__V = (function(x$1) {
  this.display3$4 = x$1
});
var $d_sci_Vector = new $TypeData().initClass({
  sci_Vector: 0
}, false, "scala.collection.immutable.Vector", {
  sci_Vector: 1,
  sc_AbstractSeq: 1,
  sc_AbstractIterable: 1,
  sc_AbstractTraversable: 1,
  O: 1,
  sc_Traversable: 1,
  sc_TraversableLike: 1,
  scg_HasNewBuilder: 1,
  scg_FilterMonadic: 1,
  sc_TraversableOnce: 1,
  sc_GenTraversableOnce: 1,
  sc_GenTraversableLike: 1,
  sc_Parallelizable: 1,
  sc_GenTraversable: 1,
  scg_GenericTraversableTemplate: 1,
  sc_Iterable: 1,
  sc_GenIterable: 1,
  sc_GenIterableLike: 1,
  sc_IterableLike: 1,
  s_Equals: 1,
  sc_Seq: 1,
  s_PartialFunction: 1,
  F1: 1,
  sc_GenSeq: 1,
  sc_GenSeqLike: 1,
  sc_SeqLike: 1,
  sci_IndexedSeq: 1,
  sci_Seq: 1,
  sci_Iterable: 1,
  sci_Traversable: 1,
  s_Immutable: 1,
  sc_IndexedSeq: 1,
  sc_IndexedSeqLike: 1,
  sci_VectorPointer: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1,
  sc_CustomParallelizable: 1
});
$c_sci_Vector.prototype.$classData = $d_sci_Vector;
/** @constructor */
function $c_sci_$colon$colon() {
  $c_sci_List.call(this);
  this.head$5 = null;
  this.tl$5 = null
}
$c_sci_$colon$colon.prototype = new $h_sci_List();
$c_sci_$colon$colon.prototype.constructor = $c_sci_$colon$colon;
/** @constructor */
function $h_sci_$colon$colon() {
  /*<skip>*/
}
$h_sci_$colon$colon.prototype = $c_sci_$colon$colon.prototype;
$c_sci_$colon$colon.prototype.productPrefix__T = (function() {
  return "::"
});
$c_sci_$colon$colon.prototype.head__O = (function() {
  return this.head$5
});
$c_sci_$colon$colon.prototype.productArity__I = (function() {
  return 2
});
$c_sci_$colon$colon.prototype.isEmpty__Z = (function() {
  return false
});
$c_sci_$colon$colon.prototype.tail__sci_List = (function() {
  return this.tl$5
});
$c_sci_$colon$colon.prototype.productElement__I__O = (function(x$1) {
  switch (x$1) {
    case 0: {
      return this.head$5;
      break
    }
    case 1: {
      return this.tl$5;
      break
    }
    default: {
      throw new $c_jl_IndexOutOfBoundsException().init___T(("" + x$1))
    }
  }
});
$c_sci_$colon$colon.prototype.init___O__sci_List = (function(head, tl) {
  this.head$5 = head;
  this.tl$5 = tl;
  return this
});
$c_sci_$colon$colon.prototype.productIterator__sc_Iterator = (function() {
  return new $c_sr_ScalaRunTime$$anon$1().init___s_Product(this)
});
var $d_sci_$colon$colon = new $TypeData().initClass({
  sci_$colon$colon: 0
}, false, "scala.collection.immutable.$colon$colon", {
  sci_$colon$colon: 1,
  sci_List: 1,
  sc_AbstractSeq: 1,
  sc_AbstractIterable: 1,
  sc_AbstractTraversable: 1,
  O: 1,
  sc_Traversable: 1,
  sc_TraversableLike: 1,
  scg_HasNewBuilder: 1,
  scg_FilterMonadic: 1,
  sc_TraversableOnce: 1,
  sc_GenTraversableOnce: 1,
  sc_GenTraversableLike: 1,
  sc_Parallelizable: 1,
  sc_GenTraversable: 1,
  scg_GenericTraversableTemplate: 1,
  sc_Iterable: 1,
  sc_GenIterable: 1,
  sc_GenIterableLike: 1,
  sc_IterableLike: 1,
  s_Equals: 1,
  sc_Seq: 1,
  s_PartialFunction: 1,
  F1: 1,
  sc_GenSeq: 1,
  sc_GenSeqLike: 1,
  sc_SeqLike: 1,
  sci_LinearSeq: 1,
  sci_Seq: 1,
  sci_Iterable: 1,
  sci_Traversable: 1,
  s_Immutable: 1,
  sc_LinearSeq: 1,
  sc_LinearSeqLike: 1,
  s_Product: 1,
  sc_LinearSeqOptimized: 1,
  Ljava_io_Serializable: 1,
  s_Serializable: 1
});
$c_sci_$colon$colon.prototype.$classData = $d_sci_$colon$colon;
/** @constructor */
function $c_sci_Nil$() {
  $c_sci_List.call(this)
}
$c_sci_Nil$.prototype = new $h_sci_List();
$c_sci_Nil$.prototype.constructor = $c_sci_Nil$;
/** @constructor */
function $h_sci_Nil$() {
  /*<skip>*/
}
$h_sci_Nil$.prototype = $c_sci_Nil$.prototype;
$c_sci_Nil$.prototype.init___ = (function() {
  return this
});
$c_sci_Nil$.prototype.head__O = (function() {
  this.head__sr_Nothing$()
});
$c_sci_Nil$.prototype.productPrefix__T = (function() {
  return "Nil"
});
$c_sci_Nil$.prototype.productArity__I = (function() {
  return 0
});
$c_sci_Nil$.prototype.equals__O__Z = (function(that) {
  if ($is_sc_GenSeq(that)) {
    var x2 = $as_sc_GenSeq(that);
    return x2.isEmpty__Z()
  } else {
    return false
  }
});
$c_sci_Nil$.prototype.tail__sci_List = (function() {
  throw new $c_jl_UnsupportedOperationException().init___T("tail of empty list")
});
$c_sci_Nil$.prototype.isEmpty__Z = (function() {
  return true
});
$c_sci_Nil$.prototype.productElement__I__O = (function(x$1) {
  throw new $c_jl_IndexOutOfBoundsException().init___T(("" + x$1))
});
$c_sci_Nil$.prototype.head__sr_Nothing$ = (function() {
  throw new $c_ju_NoSuchElementException().init___T("head of empty list")
});
$c_sci_Nil$.prototype.productIterator__sc_Iterator = (function() {
  return new $c_sr_ScalaRunTime$$anon$1().init___s_Product(this)
});
var $d_sci_Nil$ = new $TypeData().initClass({
  sci_Nil$: 0
}, false, "scala.collection.immutable.Nil$", {
  sci_Nil$: 1,
  sci_List: 1,
  sc_AbstractSeq: 1,
  sc_AbstractIterable: 1,
  sc_AbstractTraversable: 1,
  O: 1,
  sc_Traversable: 1,
  sc_TraversableLike: 1,
  scg_HasNewBuilder: 1,
  scg_FilterMonadic: 1,
  sc_TraversableOnce: 1,
  sc_GenTraversableOnce: 1,
  sc_GenTraversableLike: 1,
  sc_Parallelizable: 1,
  sc_GenTraversable: 1,
  scg_GenericTraversableTemplate: 1,
  sc_Iterable: 1,
  sc_GenIterable: 1,
  sc_GenIterableLike: 1,
  sc_IterableLike: 1,
  s_Equals: 1,
  sc_Seq: 1,
  s_PartialFunction: 1,
  F1: 1,
  sc_GenSeq: 1,
  sc_GenSeqLike: 1,
  sc_SeqLike: 1,
  sci_LinearSeq: 1,
  sci_Seq: 1,
  sci_Iterable: 1,
  sci_Traversable: 1,
  s_Immutable: 1,
  sc_LinearSeq: 1,
  sc_LinearSeqLike: 1,
  s_Product: 1,
  sc_LinearSeqOptimized: 1,
  Ljava_io_Serializable: 1,
  s_Serializable: 1
});
$c_sci_Nil$.prototype.$classData = $d_sci_Nil$;
var $n_sci_Nil$ = (void 0);
function $m_sci_Nil$() {
  if ((!$n_sci_Nil$)) {
    $n_sci_Nil$ = new $c_sci_Nil$().init___()
  };
  return $n_sci_Nil$
}
/** @constructor */
function $c_scm_AbstractBuffer() {
  $c_scm_AbstractSeq.call(this)
}
$c_scm_AbstractBuffer.prototype = new $h_scm_AbstractSeq();
$c_scm_AbstractBuffer.prototype.constructor = $c_scm_AbstractBuffer;
/** @constructor */
function $h_scm_AbstractBuffer() {
  /*<skip>*/
}
$h_scm_AbstractBuffer.prototype = $c_scm_AbstractBuffer.prototype;
/** @constructor */
function $c_scm_StringBuilder() {
  $c_scm_AbstractSeq.call(this);
  this.underlying$5 = null
}
$c_scm_StringBuilder.prototype = new $h_scm_AbstractSeq();
$c_scm_StringBuilder.prototype.constructor = $c_scm_StringBuilder;
/** @constructor */
function $h_scm_StringBuilder() {
  /*<skip>*/
}
$h_scm_StringBuilder.prototype = $c_scm_StringBuilder.prototype;
$c_scm_StringBuilder.prototype.init___ = (function() {
  $c_scm_StringBuilder.prototype.init___I__T.call(this, 16, "");
  return this
});
$c_scm_StringBuilder.prototype.apply__I__O = (function(idx) {
  var this$1 = this.underlying$5;
  var thiz = this$1.content$1;
  var c = (65535 & $uI(thiz.charCodeAt(idx)));
  return new $c_jl_Character().init___C(c)
});
$c_scm_StringBuilder.prototype.lengthCompare__I__I = (function(len) {
  return $s_sc_IndexedSeqOptimized$class__lengthCompare__sc_IndexedSeqOptimized__I__I(this, len)
});
$c_scm_StringBuilder.prototype.apply__O__O = (function(v1) {
  var index = $uI(v1);
  var this$1 = this.underlying$5;
  var thiz = this$1.content$1;
  var c = (65535 & $uI(thiz.charCodeAt(index)));
  return new $c_jl_Character().init___C(c)
});
$c_scm_StringBuilder.prototype.sameElements__sc_GenIterable__Z = (function(that) {
  return $s_sc_IndexedSeqOptimized$class__sameElements__sc_IndexedSeqOptimized__sc_GenIterable__Z(this, that)
});
$c_scm_StringBuilder.prototype.isEmpty__Z = (function() {
  return $s_sc_IndexedSeqOptimized$class__isEmpty__sc_IndexedSeqOptimized__Z(this)
});
$c_scm_StringBuilder.prototype.subSequence__I__I__jl_CharSequence = (function(start, end) {
  var this$1 = this.underlying$5;
  var thiz = this$1.content$1;
  return $as_T(thiz.substring(start, end))
});
$c_scm_StringBuilder.prototype.toString__T = (function() {
  var this$1 = this.underlying$5;
  return this$1.content$1
});
$c_scm_StringBuilder.prototype.foreach__F1__V = (function(f) {
  $s_sc_IndexedSeqOptimized$class__foreach__sc_IndexedSeqOptimized__F1__V(this, f)
});
$c_scm_StringBuilder.prototype.append__T__scm_StringBuilder = (function(s) {
  this.underlying$5.append__T__jl_StringBuilder(s);
  return this
});
$c_scm_StringBuilder.prototype.iterator__sc_Iterator = (function() {
  var this$1 = this.underlying$5;
  var thiz = this$1.content$1;
  return new $c_sc_IndexedSeqLike$Elements().init___sc_IndexedSeqLike__I__I(this, 0, $uI(thiz.length))
});
$c_scm_StringBuilder.prototype.init___I__T = (function(initCapacity, initValue) {
  $c_scm_StringBuilder.prototype.init___jl_StringBuilder.call(this, new $c_jl_StringBuilder().init___I((($uI(initValue.length) + initCapacity) | 0)).append__T__jl_StringBuilder(initValue));
  return this
});
$c_scm_StringBuilder.prototype.length__I = (function() {
  var this$1 = this.underlying$5;
  var thiz = this$1.content$1;
  return $uI(thiz.length)
});
$c_scm_StringBuilder.prototype.init___jl_StringBuilder = (function(underlying) {
  this.underlying$5 = underlying;
  return this
});
$c_scm_StringBuilder.prototype.append__O__scm_StringBuilder = (function(x) {
  this.underlying$5.append__T__jl_StringBuilder($m_sjsr_RuntimeString$().valueOf__O__T(x));
  return this
});
$c_scm_StringBuilder.prototype.hashCode__I = (function() {
  return $m_s_util_hashing_MurmurHash3$().seqHash__sc_Seq__I(this)
});
var $d_scm_StringBuilder = new $TypeData().initClass({
  scm_StringBuilder: 0
}, false, "scala.collection.mutable.StringBuilder", {
  scm_StringBuilder: 1,
  scm_AbstractSeq: 1,
  sc_AbstractSeq: 1,
  sc_AbstractIterable: 1,
  sc_AbstractTraversable: 1,
  O: 1,
  sc_Traversable: 1,
  sc_TraversableLike: 1,
  scg_HasNewBuilder: 1,
  scg_FilterMonadic: 1,
  sc_TraversableOnce: 1,
  sc_GenTraversableOnce: 1,
  sc_GenTraversableLike: 1,
  sc_Parallelizable: 1,
  sc_GenTraversable: 1,
  scg_GenericTraversableTemplate: 1,
  sc_Iterable: 1,
  sc_GenIterable: 1,
  sc_GenIterableLike: 1,
  sc_IterableLike: 1,
  s_Equals: 1,
  sc_Seq: 1,
  s_PartialFunction: 1,
  F1: 1,
  sc_GenSeq: 1,
  sc_GenSeqLike: 1,
  sc_SeqLike: 1,
  scm_Seq: 1,
  scm_Iterable: 1,
  scm_Traversable: 1,
  s_Mutable: 1,
  scm_SeqLike: 1,
  scm_Cloneable: 1,
  s_Cloneable: 1,
  jl_Cloneable: 1,
  jl_CharSequence: 1,
  scm_IndexedSeq: 1,
  sc_IndexedSeq: 1,
  sc_IndexedSeqLike: 1,
  scm_IndexedSeqLike: 1,
  sci_StringLike: 1,
  sc_IndexedSeqOptimized: 1,
  s_math_Ordered: 1,
  jl_Comparable: 1,
  scm_Builder: 1,
  scg_Growable: 1,
  scg_Clearable: 1,
  s_Serializable: 1,
  Ljava_io_Serializable: 1
});
$c_scm_StringBuilder.prototype.$classData = $d_scm_StringBuilder;
/** @constructor */
function $c_sjs_js_WrappedArray() {
  $c_scm_AbstractBuffer.call(this);
  this.array$6 = null
}
$c_sjs_js_WrappedArray.prototype = new $h_scm_AbstractBuffer();
$c_sjs_js_WrappedArray.prototype.constructor = $c_sjs_js_WrappedArray;
/** @constructor */
function $h_sjs_js_WrappedArray() {
  /*<skip>*/
}
$h_sjs_js_WrappedArray.prototype = $c_sjs_js_WrappedArray.prototype;
$c_sjs_js_WrappedArray.prototype.lengthCompare__I__I = (function(len) {
  return $s_sc_IndexedSeqOptimized$class__lengthCompare__sc_IndexedSeqOptimized__I__I(this, len)
});
$c_sjs_js_WrappedArray.prototype.apply__I__O = (function(index) {
  return this.array$6[index]
});
$c_sjs_js_WrappedArray.prototype.apply__O__O = (function(v1) {
  var index = $uI(v1);
  return this.array$6[index]
});
$c_sjs_js_WrappedArray.prototype.sameElements__sc_GenIterable__Z = (function(that) {
  return $s_sc_IndexedSeqOptimized$class__sameElements__sc_IndexedSeqOptimized__sc_GenIterable__Z(this, that)
});
$c_sjs_js_WrappedArray.prototype.isEmpty__Z = (function() {
  return $s_sc_IndexedSeqOptimized$class__isEmpty__sc_IndexedSeqOptimized__Z(this)
});
$c_sjs_js_WrappedArray.prototype.foreach__F1__V = (function(f) {
  $s_sc_IndexedSeqOptimized$class__foreach__sc_IndexedSeqOptimized__F1__V(this, f)
});
$c_sjs_js_WrappedArray.prototype.iterator__sc_Iterator = (function() {
  return new $c_sc_IndexedSeqLike$Elements().init___sc_IndexedSeqLike__I__I(this, 0, $uI(this.array$6.length))
});
$c_sjs_js_WrappedArray.prototype.length__I = (function() {
  return $uI(this.array$6.length)
});
$c_sjs_js_WrappedArray.prototype.hashCode__I = (function() {
  return $m_s_util_hashing_MurmurHash3$().seqHash__sc_Seq__I(this)
});
$c_sjs_js_WrappedArray.prototype.init___sjs_js_Array = (function(array) {
  this.array$6 = array;
  return this
});
$c_sjs_js_WrappedArray.prototype.stringPrefix__T = (function() {
  return "WrappedArray"
});
function $is_sjs_js_WrappedArray(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.sjs_js_WrappedArray)))
}
function $as_sjs_js_WrappedArray(obj) {
  return (($is_sjs_js_WrappedArray(obj) || (obj === null)) ? obj : $throwClassCastException(obj, "scala.scalajs.js.WrappedArray"))
}
function $isArrayOf_sjs_js_WrappedArray(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.sjs_js_WrappedArray)))
}
function $asArrayOf_sjs_js_WrappedArray(obj, depth) {
  return (($isArrayOf_sjs_js_WrappedArray(obj, depth) || (obj === null)) ? obj : $throwArrayCastException(obj, "Lscala.scalajs.js.WrappedArray;", depth))
}
var $d_sjs_js_WrappedArray = new $TypeData().initClass({
  sjs_js_WrappedArray: 0
}, false, "scala.scalajs.js.WrappedArray", {
  sjs_js_WrappedArray: 1,
  scm_AbstractBuffer: 1,
  scm_AbstractSeq: 1,
  sc_AbstractSeq: 1,
  sc_AbstractIterable: 1,
  sc_AbstractTraversable: 1,
  O: 1,
  sc_Traversable: 1,
  sc_TraversableLike: 1,
  scg_HasNewBuilder: 1,
  scg_FilterMonadic: 1,
  sc_TraversableOnce: 1,
  sc_GenTraversableOnce: 1,
  sc_GenTraversableLike: 1,
  sc_Parallelizable: 1,
  sc_GenTraversable: 1,
  scg_GenericTraversableTemplate: 1,
  sc_Iterable: 1,
  sc_GenIterable: 1,
  sc_GenIterableLike: 1,
  sc_IterableLike: 1,
  s_Equals: 1,
  sc_Seq: 1,
  s_PartialFunction: 1,
  F1: 1,
  sc_GenSeq: 1,
  sc_GenSeqLike: 1,
  sc_SeqLike: 1,
  scm_Seq: 1,
  scm_Iterable: 1,
  scm_Traversable: 1,
  s_Mutable: 1,
  scm_SeqLike: 1,
  scm_Cloneable: 1,
  s_Cloneable: 1,
  jl_Cloneable: 1,
  scm_Buffer: 1,
  scm_BufferLike: 1,
  scg_Growable: 1,
  scg_Clearable: 1,
  scg_Shrinkable: 1,
  sc_script_Scriptable: 1,
  scg_Subtractable: 1,
  scm_IndexedSeq: 1,
  sc_IndexedSeq: 1,
  sc_IndexedSeqLike: 1,
  scm_IndexedSeqLike: 1,
  scm_ArrayLike: 1,
  scm_IndexedSeqOptimized: 1,
  sc_IndexedSeqOptimized: 1,
  scm_Builder: 1
});
$c_sjs_js_WrappedArray.prototype.$classData = $d_sjs_js_WrappedArray;
}).call(this);
//# sourceMappingURL=sp-example-widget.js.map

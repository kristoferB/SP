package sp.domain.logic

import org.threeten.bp._
import sp.domain._
import play.api.libs.json._
import julienrf.json.derived

object JsonLogic extends JsonImplicit

trait JsonImplicit extends JsonDerived {


  def writeJson[T](x: T)(implicit fjs: JSWrites[T]) = {
    Json.stringify(Json.toJson(x))
  }




  // testing not using derive macros to limit the compiled size
  // due macro expansion. Especially in scala js.

  import play.api.libs.json.Reads._ // Custom validation helpers
  import play.api.libs.functional.syntax._

  def extrObj[T](json: JsValue, t: String, reader: Reads[T]) = {
    val conv = (JsPath \ "isa").read[String](verifying[String](_ == t)).reads(json)
    conv.flatMap(t => reader.reads(json))
  }



  // StateEvaluator and Updater

  implicit lazy val readASSIGN: Reads[ASSIGN] = new Reads[ASSIGN] {
    def reads(json: JsValue): JsResult[ASSIGN] = {
      val rr = (JsPath \ "id").read[ID].map(id => ASSIGN(id))
      extrObj(json, "ASSIGN", rr)
    }
  }
  implicit lazy val writeASSIGN: Writes[ASSIGN] = new Writes[ASSIGN] {
    override def writes(o: ASSIGN): SPValue =  Json.obj(
      "isa"->"ASSIGN",
      "id" -> o.id
    )
  }
  implicit lazy val readDECR: Reads[DECR] = new Reads[DECR] {
    def reads(json: JsValue): JsResult[DECR] = {
      val rr = (JsPath \ "n").read[Int].map(x => DECR(x))
      extrObj(json, "DECR", rr)
    }
  }
  implicit lazy val writeDECR: Writes[DECR] = new Writes[DECR] {
    override def writes(o: DECR): SPValue = Json.obj(
      "isa"->"DECR",
      "n" -> o.n
    )
  }
  implicit lazy val readINCR: Reads[INCR] = new Reads[INCR] {
    def reads(json: JsValue): JsResult[INCR] = {
      val rr = (JsPath \ "n").read[Int].map(x => INCR(x))
      extrObj(json, "INCR", rr)
    }
  }
  implicit lazy val writeINCR: Writes[INCR] = new Writes[INCR] {
    override def writes(o: INCR): SPValue = Json.obj(
      "isa"->"INCR",
      "n" -> o.n
    )
  }
  implicit lazy val readValueHolder: Reads[ValueHolder] = new Reads[ValueHolder] {
    def reads(json: JsValue): JsResult[ValueHolder] = {
      val rr = (JsPath \ "v").read[SPValue].map(x => ValueHolder(x))
      extrObj(json, "ValueHolder", rr)
    }
  }
  implicit lazy val writeValueHolder: Writes[ValueHolder] = new Writes[ValueHolder] {
    override def writes(o: ValueHolder): SPValue = Json.obj(
      "isa"->"ValueHolder",
      "v" -> o.v
    )
  }
  implicit lazy val readSVIDEval: Reads[SVIDEval] = new Reads[SVIDEval] {
    def reads(json: JsValue): JsResult[SVIDEval] = {
      val rr = (JsPath \ "id").read[ID].map(id => SVIDEval(id))
      extrObj(json, "SVIDEval", rr)
    }
  }
  implicit lazy val writeSVIDEval: Writes[SVIDEval] = new Writes[SVIDEval] {
    override def writes(o: SVIDEval): SPValue = Json.obj(
      "isa"->"SVIDEval",
      "id" -> o.id
    )
  }
  implicit lazy val readStateUpd: Reads[StateUpdater] = new JSReads[StateUpdater] {
    override def reads(json: SPValue): JsResult[StateUpdater] = {
      json.validate[ValueHolder] orElse
        json.validate[INCR] orElse
        json.validate[DECR] orElse
        json.validate[ASSIGN]
    }
  }
  implicit lazy val writeStateUpd: Writes[StateUpdater] = new Writes[StateUpdater] {
    override def writes(o: StateUpdater): SPValue = {
      o match {
        case x: ValueHolder => Json.toJson(x)(writeValueHolder)
        case x: INCR => Json.toJson(x)(writeINCR)
        case x: DECR => Json.toJson(x)(writeDECR)
        case x: ASSIGN => Json.toJson(x)(writeASSIGN)
      }
    }
  }

  implicit lazy val readStateEval: Reads[StateEvaluator] = new JSReads[StateEvaluator] {
    override def reads(json: SPValue): JsResult[StateEvaluator] = {
      json.validate[ValueHolder] orElse
        json.validate[SVIDEval]
    }
  }
  implicit lazy val writeStateEval: Writes[StateEvaluator] = new Writes[StateEvaluator] {
    override def writes(o: StateEvaluator): SPValue = {
      o match {
        case x: ValueHolder => Json.toJson(x)(writeValueHolder)
        case x: SVIDEval => Json.toJson(x)(writeSVIDEval)
      }
    }
  }



  // Propositions and conditions

  def rr[T](make: (StateEvaluator, StateEvaluator) => T) = {
    val left = (JsPath \ "left").read[StateEvaluator]
    val right = (JsPath \ "right").read[StateEvaluator]
    left.flatMap(l => right.map(r => make(l, r)))
  }
  def ww(isa: String, left: StateEvaluator, right: StateEvaluator) = {
    Json.obj(
      "isa"->isa,
      "left"->left,
      "right"->right
    )
  }

  implicit lazy val readEQ: Reads[EQ] = new Reads[EQ] {
    def reads(json: JsValue): JsResult[EQ] = {
      extrObj(json, "EQ", rr(EQ.apply))
    }
  }
  implicit lazy val writeEQ: Writes[EQ] = new Writes[EQ] {
    override def writes(o: EQ): SPValue = {
      ww("EQ", o.left, o.right)
    }
  }
  implicit lazy val readNEQ: Reads[NEQ] = new Reads[NEQ] {
    def reads(json: JsValue): JsResult[NEQ] = {
      extrObj(json, "NEQ", rr(NEQ.apply))
    }
  }
  implicit lazy val writeNEQ: Writes[NEQ] = new Writes[NEQ] {
    override def writes(o: NEQ): SPValue = {
      ww("NEQ", o.left, o.right)
    }
  }
  implicit lazy val readGREQ: Reads[GREQ] = new Reads[GREQ] {
    def reads(json: JsValue): JsResult[GREQ] = {
      extrObj(json, "GREQ", rr(GREQ.apply))
    }
  }
  implicit lazy val writeGREQ: Writes[GREQ] = new Writes[GREQ] {
    override def writes(o: GREQ): SPValue = {
      ww("GREQ", o.left, o.right)
    }
  }
  implicit lazy val readLEEQ: Reads[LEEQ] = new Reads[LEEQ] {
    def reads(json: JsValue): JsResult[LEEQ] = {
      extrObj(json, "LEEQ", rr(LEEQ.apply))
    }
  }
  implicit lazy val writeLEEQ: Writes[LEEQ] = new Writes[LEEQ] {
    override def writes(o: LEEQ): SPValue = {
      ww("LEEQ", o.left, o.right)
    }
  }
  implicit lazy val readGR: Reads[GR] = new Reads[GR] {
    def reads(json: JsValue): JsResult[GR] = {
      extrObj(json, "GR", rr(GR.apply))
    }
  }
  implicit lazy val writeGR: Writes[GR] = new Writes[GR] {
    override def writes(o: GR): SPValue = {
      ww("GR", o.left, o.right)
    }
  }
  implicit lazy val readLE: Reads[LE] = new Reads[LE] {
    def reads(json: JsValue): JsResult[LE] = {
      extrObj(json, "LE", rr(LE.apply))
    }
  }
  implicit lazy val writeLE: Writes[LE] = new Writes[LE] {
    override def writes(o: LE): SPValue = {
      ww("LE", o.left, o.right)
    }
  }
  implicit lazy val readPropEval: Reads[PropositionEvaluator] = new JSReads[PropositionEvaluator] {
    override def reads(json: SPValue): JsResult[PropositionEvaluator] = {
        json.validate[EQ] orElse
        json.validate[NEQ] orElse
        json.validate[GREQ] orElse
        json.validate[LEEQ] orElse
        json.validate[GR] orElse
        json.validate[LE]
    }
  }
  implicit lazy val writePropEval: Writes[PropositionEvaluator] = new Writes[PropositionEvaluator] {
    override def writes(o: PropositionEvaluator): SPValue = {
      o match {
        case x: EQ => Json.toJson(x)(writeEQ)
        case x: NEQ => Json.toJson(x)(writeNEQ)
        case x: GREQ => Json.toJson(x)(writeGREQ)
        case x: LEEQ => Json.toJson(x)(writeLEEQ)
        case x: GR => Json.toJson(x)(writeGR)
        case x: LE => Json.toJson(x)(writeLE)
      }
    }
  }


  def extrObj2[T](json: JsValue, t: String, reader: Reads[T]) = {
    val conv = (JsPath \ "isa").read[String](verifying[String](_ == t)).reads(json)
    conv.flatMap(t => reader.reads(json))
  }


  implicit lazy val propW: Writes[Proposition] =  WriteProp
  implicit lazy val readAND: Reads[AND] = new Reads[AND] {
    def reads(json: JsValue): JsResult[AND] = {
      val t = (JsPath \ "isa").read[String](verifying[String](_ == "AND")).reads(json)
      val res =  (JsPath \ "props").lazyRead(implicitly[Reads[List[Proposition]]]).reads(json)
      t.flatMap(x => res.map(xs => AND(xs)))
    }
  }
  implicit lazy val writeAND: Writes[AND] = new Writes[AND] {
    override def writes(o: AND): SPValue = Json.obj(
      "isa"->"AND",
      "props"->o.props
    )
  }
  implicit lazy val readOR: Reads[OR] = new Reads[OR] {
    def reads(json: JsValue): JsResult[OR] = {
      val t = (JsPath \ "isa").read[String](verifying[String](_ == "OR")).reads(json)
      val res =  (JsPath \ "props").lazyRead(implicitly[Reads[List[Proposition]]]).reads(json)
      t.flatMap(x => res.map(xs => OR(xs)))
    }
  }
  implicit lazy val writeOR: Writes[OR] = new Writes[OR] {
    override def writes(o: OR): SPValue = Json.obj(
      "isa"->"OR",
      "props"->o.props
    )
  }

  implicit lazy val readNOT: Reads[NOT] = new Reads[NOT] {
    def reads(json: JsValue): JsResult[NOT] = {
      val t = (JsPath \ "isa").read[String](verifying[String](_ == "NOT")).reads(json)
      val res =  (JsPath \ "p").lazyRead(implicitly[Reads[Proposition]]).reads(json)
      t.flatMap(x => res.map(xs => NOT(xs)))
    }
  }
  implicit lazy val writeNOT: Writes[NOT] = new Writes[NOT] {
    override def writes(o: NOT): SPValue = Json.obj(
      "isa"->"NOT",
      "p"->o.p
    )
  }

  def parseMe(x: SPValue): JsResult[Proposition] = {
    val res = x.asOpt[String].flatMap{ p =>
      Proposition.parseStr(p)
    }
    res match {
      case Some(ok) => JsSuccess[Proposition](ok)
      case None => JsError("Could not convert proposition from a string ")
    }
  }

  implicit lazy val readProp: Reads[Proposition] = new JSReads[Proposition] {
    override def reads(json: SPValue): JsResult[Proposition] = {
      json.validate[PropositionEvaluator] orElse
        json.validate[AND] orElse
        json.validate[OR] orElse
        json.validate[NOT] orElse
        (JsPath \ "isa").read[String](verifying[String](_ == "AlwaysTrue")).reads(json).map(x => AlwaysTrue) orElse
        (JsPath \ "isa").read[String](verifying[String](_ == "AlwaysFalse")).reads(json).map(x => AlwaysFalse) orElse
        parseMe(json)
    }
  }
  implicit object WriteProp extends Writes[Proposition]  {
    override def writes(o: Proposition): SPValue = {
      o match {
        case x: PropositionEvaluator => Json.toJson(x)(writePropEval)
        case x: AND => Json.toJson(x)(writeAND)
        case x: OR => Json.toJson(x)(writeOR)
        case x: NOT => Json.toJson(x)(writeNOT)
        case AlwaysTrue => Json.obj("isa"-> "AlwaysTrue")
        case AlwaysFalse => Json.obj("isa"-> "AlwaysFalse")
      }
    }
  }




  implicit lazy val actionsRead: JSReads[Action] = (
      (JsPath \ "id").read[ID] and
        (JsPath \ "value").read[StateUpdater]
    )(Action.apply _)

  implicit lazy val actionsWrites: JSWrites[Action] = (
    (JsPath \ "id").write[ID] and
      (JsPath \ "value").write[StateUpdater]
    )(unlift(Action.unapply))

  implicit lazy val condsRead: JSReads[Condition] = (
    (JsPath \ "guard").read[Proposition] and
    (JsPath \ "action").readWithDefault[List[Action]](List()) and
    (JsPath \ "attributes").readWithDefault[SPAttributes](SPAttributes.empty)
  )(Condition.apply _)
  implicit lazy val condsWrites: JSWrites[Condition] = (
    (JsPath \ "guard").write[Proposition] and
      (JsPath \ "action").write[List[Action]]and
      (JsPath \ "attributes").write[SPAttributes]
    )(unlift(Condition.unapply))




  // SOPs


  implicit lazy val sopW: Writes[SOP] =  WriteSOP
  def createTheSOP[T](json: JsValue, t: String, make: (List[SOP], ID) => T) = {
    val isa = (JsPath \ "isa").read[String](verifying[String](_ == t)).reads(json)
    val res =  (JsPath \ "sop").lazyRead(implicitly[Reads[List[SOP]]]).reads(json)
    val id =  (JsPath \ "nodeID").read[ID].reads(json).getOrElse(ID.newID)
    isa.flatMap(x => res.map(xs => make(xs, id)))
  }
  def wwSOP(isa: String, sop: List[SOP], nodeID: ID) = {
    Json.obj(
      "isa"->isa,
      "sop"->sop,
      "nodeID"->nodeID
    )
  }

  implicit lazy val readParallel: Reads[Parallel] = new Reads[Parallel] {
    def reads(json: JsValue): JsResult[Parallel] = {
      createTheSOP(json, "Parallel", Parallel.apply)
    }
  }
  implicit lazy val writeParallel: Writes[Parallel] = new Writes[Parallel] {
    override def writes(o: Parallel): SPValue = {
      wwSOP("Parallel", o.sop, o.nodeID)
    }
  }
  implicit lazy val readAlternative: Reads[Alternative] = new Reads[Alternative] {
    def reads(json: JsValue): JsResult[Alternative] = {
      createTheSOP(json, "Alternative", Alternative.apply)
    }
  }
  implicit lazy val writeAlternative: Writes[Alternative] = new Writes[Alternative] {
    override def writes(o: Alternative): SPValue = {
      wwSOP("Alternative", o.sop, o.nodeID)
    }
  }
  implicit lazy val readArbitrary: Reads[Arbitrary] = new Reads[Arbitrary] {
    def reads(json: JsValue): JsResult[Arbitrary] = {
      createTheSOP(json, "Arbitrary", Arbitrary.apply)
    }
  }
  implicit lazy val writeArbitrary: Writes[Arbitrary] = new Writes[Arbitrary] {
    override def writes(o: Arbitrary): SPValue = {
      wwSOP("Arbitrary", o.sop, o.nodeID)
    }
  }
  implicit lazy val readSequence: Reads[Sequence] = new Reads[Sequence] {
    def reads(json: JsValue): JsResult[Sequence] = {
      createTheSOP(json, "Sequence", Sequence.apply)
    }
  }
  implicit lazy val writeSequence: Writes[Sequence] = new Writes[Sequence] {
    override def writes(o: Sequence): SPValue = {
      wwSOP("Sequence", o.sop, o.nodeID)
    }
  }
  implicit lazy val readSometimeSequence: Reads[SometimeSequence] = new Reads[SometimeSequence] {
    def reads(json: JsValue): JsResult[SometimeSequence] = {
      createTheSOP(json, "SometimeSequence", SometimeSequence.apply)
    }
  }
  implicit lazy val writeSometimeSequence: Writes[SometimeSequence] = new Writes[SometimeSequence] {
    override def writes(o: SometimeSequence): SPValue = {
      wwSOP("SometimeSequence", o.sop, o.nodeID)
    }
  }
  implicit lazy val readOther: Reads[Other] = new Reads[Other] {
    def reads(json: JsValue): JsResult[Other] = {
      createTheSOP(json, "Other", Other.apply)
    }
  }
  implicit lazy val writeOther: Writes[Other] = new Writes[Other] {
    override def writes(o: Other): SPValue = {
      wwSOP("Other", o.sop, o.nodeID)
    }
  }
  implicit lazy val readOperationNode: Reads[OperationNode] = new Reads[OperationNode] {
    def reads(json: JsValue): JsResult[OperationNode] = {
      val isa = (JsPath \ "isa").read[String](verifying[String](_ == "OperationNode")).reads(json)
      val res =  (JsPath \ "sop").lazyRead(implicitly[Reads[List[SOP]]]).reads(json).getOrElse(List[SOP]())
      val id =  (JsPath \ "nodeID").read[ID].reads(json).getOrElse(ID.newID)
      val op =  (JsPath \ "operation").read[ID].reads(json)
      val conds =  (JsPath \ "conditions").read[List[Condition]].reads(json).getOrElse(List())
      isa.flatMap(x => op.map(o => OperationNode(o, conds, res, id)))
    }
  }
  implicit lazy val writeOperationNode: Writes[OperationNode] = new Writes[OperationNode] {
    override def writes(o: OperationNode): SPValue = Json.obj(
      "isa"->"OperationNode",
      "operation" -> o.operation,
      "conditions"-> o.conditions,
      "sop"-> o.sop,
      "nodeID"->o.nodeID
    )
  }



  implicit lazy val readSOP: Reads[SOP] = new JSReads[SOP] {
    override def reads(json: SPValue): JsResult[SOP] = {
        json.validate[Parallel] orElse
        json.validate[Alternative] orElse
        json.validate[Arbitrary] orElse
        json.validate[Sequence] orElse
        json.validate[SometimeSequence] orElse
        json.validate[Other] orElse
        json.validate[OperationNode] orElse
        (JsPath \ "isa").read[String](verifying[String](_ == "EmptySOP")).reads(json).map(x => EmptySOP)
    }
  }
  implicit object WriteSOP extends Writes[SOP]  {
    override def writes(o: SOP): SPValue = {
      o match {
        case x: Parallel => Json.toJson(x)(writeParallel)
        case x: Alternative => Json.toJson(x)(writeAlternative)
        case x: Arbitrary => Json.toJson(x)(writeArbitrary)
        case x: Sequence => Json.toJson(x)(writeSequence)
        case x: SometimeSequence => Json.toJson(x)(writeSometimeSequence)
        case x: Other => Json.toJson(x)(writeOther)
        case x: OperationNode => Json.toJson(x)(writeOperationNode)
        case EmptySOP => Json.obj("isa"-> "EmptySOP")
      }
    }
  }




  // IDAble
  implicit lazy val structRead: JSReads[StructNode] = (
    (JsPath \ "item").read[ID] and
      (JsPath \ "parent").readNullableWithDefault[ID](None) and
      (JsPath \ "nodeID").read[ID] and
      (JsPath \ "attributes").readWithDefault[SPAttributes](SPAttributes.empty)
  )(StructNode.apply _)

  implicit lazy val structwrites: JSWrites[StructNode] = (
    (JsPath \ "item").write[ID] and
      (JsPath \ "parent").writeNullable[ID] and
      (JsPath \ "nodeID").write[ID] and
      (JsPath \ "attributes").write[SPAttributes](SPAttributes.empty)
    )(unlift(StructNode.unapply))
  //

  implicit lazy val stateReads: JSReads[Map[ID, SPValue]] = new JSReads[Map[ID, SPValue]] {
    override def reads(json: JsValue): JsResult[Map[ID, SPValue]] = {
      json.validate[Map[String, SPValue]].map(xs => xs.collect{case (k, v) if ID.isID(k) => ID.makeID(k).get -> v})
    }
  }
  implicit lazy val stateWrites: JSWrites[Map[ID, SPValue]] = new OWrites[Map[ID, SPValue]] {
    override def writes(xs: Map[ID, SPValue]): JsObject = {
      val toFixedMap = xs.map{case (k, v) => k.toString -> v}
      JsObject(toFixedMap)
    }
  }

  def extrIDable(isa: String, json: SPValue) = {
    val conv = (JsPath \ "isa").read[String](verifying[String](_ == isa)).reads(json)
    val name = (JsPath \ "name").read[String].reads(json)
    val id = (JsPath \ "id").read[ID].reads(json).getOrElse(ID.newID)
    val attr = (JsPath \ "attributes").read[SPAttributes].reads(json).getOrElse(SPAttributes.empty)
    conv.flatMap(i => name.map(n => (n, id, attr)))
  }

  def wwIDAble(isa: String, extra: SPAttributes, x: IDAble): JsObject = {
    val first = Json.obj(
      "isa"-> isa,
      "name"-> x.name)
    val last = Json.obj(
      "attributes"-> x.attributes,
      "id"-> x.id
    )

    first ++ extra ++ last
  }

  implicit lazy val readOperation: Reads[Operation] = new Reads[Operation] {
    def reads(json: JsValue): JsResult[Operation] = {
      val idable = extrIDable("Operation", json)
      val cond = (JsPath \ "conditions").read[List[Condition]].reads(json).getOrElse(List())
      idable.map{case (n, id, attr) => Operation(n, cond, attr, id)}
    }
  }
  implicit lazy val writeOperation: Writes[Operation] = new Writes[Operation] {
    override def writes(o: Operation): SPValue = {
      wwIDAble("Operation", SPAttributes("conditions"->o.conditions), o)
    }
  }
  implicit lazy val readThing: Reads[Thing] = new Reads[Thing] {
    def reads(json: JsValue): JsResult[Thing] = {
      val idable = extrIDable("Thing", json)
      idable.map{case (n, id, attr) => Thing(n, attr, id)}
    }
  }
  implicit lazy val writeThing: Writes[Thing] = new Writes[Thing] {
    override def writes(o: Thing): SPValue = {
      wwIDAble("Thing", SPAttributes.empty, o)
    }
  }
  implicit lazy val readSOPSpec: Reads[SOPSpec] = new Reads[SOPSpec] {
    def reads(json: JsValue): JsResult[SOPSpec] = {
      val idable = extrIDable("SOPSpec", json)
      val sop = (JsPath \ "sop").read[List[SOP]].reads(json).getOrElse(List())
      idable.map{case (n, id, attr) => SOPSpec(n, sop, attr, id)}
    }
  }
  implicit lazy val writeSOPSpec: Writes[SOPSpec] = new Writes[SOPSpec] {
    override def writes(o: SOPSpec): SPValue = {
      wwIDAble("SOPSpec", SPAttributes("sop"->o.sop), o)
    }
  }
  implicit lazy val readSPSpec: Reads[SPSpec] = new Reads[SPSpec] {
    def reads(json: JsValue): JsResult[SPSpec] = {
      val idable = extrIDable("SPSpec", json)
      idable.map{case (n, id, attr) => SPSpec(n, attr, id)}
    }
  }
  implicit lazy val writeSPSpec: Writes[SPSpec] = new Writes[SPSpec] {
    override def writes(o: SPSpec): SPValue = {
      wwIDAble("SPSpec", SPAttributes.empty, o)
    }
  }
  implicit lazy val readSPResult: Reads[SPResult] = new Reads[SPResult] {
    def reads(json: JsValue): JsResult[SPResult] = {
      val idable = extrIDable("SPResult", json)
      idable.map{case (n, id, attr) => SPResult(n, attr, id)}
    }
  }
  implicit lazy val writeSPResult: Writes[SPResult] = new Writes[SPResult] {
    override def writes(o: SPResult): SPValue = {
      wwIDAble("SPResult", SPAttributes.empty, o)
    }
  }
  implicit lazy val readSPState: Reads[SPState] = new Reads[SPState] {
    def reads(json: JsValue): JsResult[SPState] = {
      val idable = extrIDable("SPState", json)
      val state = (JsPath \ "state").read[Map[ID, SPValue]].reads(json).getOrElse(Map())
      idable.map{case (n, id, attr) => SPState(n, state, attr, id)}
    }
  }
  implicit lazy val writeSPState: Writes[SPState] = new Writes[SPState] {
    override def writes(o: SPState): SPValue = {
      wwIDAble("SPState", SPAttributes("state"->o.state), o)
    }
  }
  implicit lazy val readStruct: Reads[Struct] = new Reads[Struct] {
    def reads(json: JsValue): JsResult[Struct] = {
      val idable = extrIDable("Struct", json)
      val items = (JsPath \ "items").read[List[StructNode]].reads(json).getOrElse(List())
      idable.map{case (n, id, attr) => Struct(n, items, attr, id)}
    }
  }
  implicit lazy val writeStruct: Writes[Struct] = new Writes[Struct] {
    override def writes(o: Struct): SPValue = {
      wwIDAble("Struct", SPAttributes("items"->o.items), o)
    }
  }

  implicit lazy val readIDAble: Reads[IDAble] = new JSReads[IDAble] {
    override def reads(json: SPValue): JsResult[IDAble] = {
        json.validate[Operation] orElse
        json.validate[Thing] orElse
        json.validate[SOPSpec] orElse
        json.validate[SPSpec] orElse
        json.validate[SPResult] orElse
        json.validate[SPState] orElse
        json.validate[Struct]
    }
  }
  implicit object WriteIDAble extends Writes[IDAble]  {
    override def writes(o: IDAble): SPValue = {
      o match {
        case x: Operation=> Json.toJson(x)(writeOperation)
        case x: Thing=> Json.toJson(x)(writeThing)
        case x: SOPSpec=> Json.toJson(x)(writeSOPSpec)
        case x: SPSpec=> Json.toJson(x)(writeSPSpec)
        case x: SPResult=> Json.toJson(x)(writeSPResult)
        case x: SPState=> Json.toJson(x)(writeSPState)
        case x: Struct=> Json.toJson(x)(writeStruct)
      }
    }
  }



  // Extra formats

  val dateF = format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  implicit lazy val javatimeF = new Format[ZonedDateTime] {
    override def reads(json: JsValue): JsResult[ZonedDateTime] = {
      json.validate[String].map(ZonedDateTime.parse(_, dateF))
    }

    override def writes(o: ZonedDateTime): JsValue = {
      Json.toJson(o.format(dateF))
    }

  }

  implicit lazy val ididReads: JSReads[Map[ID, ID]] = new JSReads[Map[ID, ID]] {
    override def reads(json: JsValue): JsResult[Map[ID, ID]] = {
      json.validate[Map[String, String]].map(xs => xs.collect{case (k, v) if ID.isID(k) && ID.isID(v) => ID.makeID(k).get -> ID.makeID(v).get})
    }
  }
  implicit lazy val ididWrites: JSWrites[Map[ID, ID]] = new OWrites[Map[ID, ID]] {
    override def writes(xs: Map[ID, ID]): JsObject = {
      val toFixedMap = xs.map{case (k, v) => k.toString -> SPValue(v)}
      JsObject(toFixedMap)
    }
  }




//  implicit lazy val stUpdf1 = Json.format[ASSIGN]
//  implicit lazy val stUpdf2 = Json.format[DECR]
//  implicit lazy val stUpdf3 = Json.format[INCR]
//  implicit lazy val stUpdf4 = Json.format[ValueHolder]
//  implicit lazy val stUpdf5 = Json.format[SVIDEval]
//  implicit lazy val stUpdfR = __.read[ValueHolder].map(x => x:StateUpdater)
//  implicit lazy val stUpdfW = Writes[StateUpdater]{
//    case x: ASSIGN => Json.toJson(x)
//  }
//  implicit lazy val stEvR = __.read[SVIDEval].map(x => x:StateEvaluator)
//  implicit lazy val stEvW = Writes[StateEvaluator]{
//    case x: SVIDEval => Json.toJson(x)
//  }
//
//
//  implicit lazy val propEvF1 = Json.format[EQ]
//  implicit lazy val propEvF2 = Json.format[NEQ]
//  implicit lazy val propEvF3 = Json.format[GREQ]
//  implicit lazy val propEvF4 = Json.format[LEEQ]
//  implicit lazy val propEvF5 = Json.format[GR]
//  implicit lazy val propEvF6 = Json.format[LE]
//  implicit lazy val propEvR = __.read[EQ].map(x => x:PropositionEvaluator)
//  implicit lazy val propEvW = Writes[PropositionEvaluator]{
//    case x: EQ => Json.toJson(x)
//  }
//
//  implicit lazy val propR = __.read[PropositionEvaluator].map(x => x:Proposition)
//  implicit lazy val propW = Writes[Proposition]{
//    case x: PropositionEvaluator => Json.toJson(x)
//  }
//
//
//  implicit lazy val actionFormat = Json.format[Action]
//  implicit lazy val conditionFormat = Json.format[Condition]




//  implicit lazy val stateEvWrites: JSWrites[StateEvaluator] = deriveWriteISA[StateEvaluator]
//  implicit lazy val stateEvReads: JSReads[StateEvaluator] = deriveReadISA[StateEvaluator]
//  implicit lazy val stateUpdRead: JSReads[StateUpdater] = deriveReadISA[StateUpdater]
//  implicit lazy val stateUpdWrites: JSWrites[StateUpdater] = deriveWriteISA[StateUpdater]
//  implicit lazy val propsRead: JSReads[Proposition] = deriveReadISA[Proposition]
//  implicit lazy val propsWrites: JSWrites[Proposition] = deriveWriteISA[Proposition]
//  implicit lazy val actionsRead: JSReads[Action] = Json.reads[Action]
//  implicit lazy val actionsWrites: JSWrites[Action] = Json.writes[Action]
//  implicit lazy val condsRead: JSReads[Condition] = Json.reads[Condition]
//  implicit lazy val condsWrites: JSWrites[Condition] = Json.writes[Condition]
//  implicit lazy val sopsRead: JSReads[SOP] = deriveReadISA[SOP]
//  implicit lazy val sopsWrites: JSWrites[SOP] = deriveWriteISA[SOP]
//  implicit lazy val structRead: JSReads[StructNode] = Json.reads[StructNode]
//  implicit lazy val structrites: JSWrites[StructNode] = Json.writes[StructNode]
////
//
//  implicit lazy val stateReads: JSReads[Map[ID, SPValue]] = new JSReads[Map[ID, SPValue]] {
//    override def reads(json: JsValue): JsResult[Map[ID, SPValue]] = {
//      json.validate[Map[String, SPValue]].map(xs => xs.collect{case (k, v) if ID.isID(k) => ID.makeID(k).get -> v})
//    }
//  }
//  implicit lazy val stateWrites: JSWrites[Map[ID, SPValue]] = new OWrites[Map[ID, SPValue]] {
//    override def writes(xs: Map[ID, SPValue]): JsObject = {
//      val toFixedMap = xs.map{case (k, v) => k.toString -> v}
//      JsObject(toFixedMap)
//    }
//  }

//  implicit lazy val opsWrites: JSReads[Operation] = Json.reads[Operation]
//  implicit lazy val opsReads: JSWrites[Operation] = Json.writes[Operation]
//  implicit lazy val thingsWrites: JSReads[Thing] = Json.reads[Thing]
//  implicit lazy val thingsReads: JSWrites[Thing] = Json.writes[Thing]
//  implicit lazy val sopSpecWrites: JSReads[SOPSpec] = Json.reads[SOPSpec]
//  implicit lazy val sopSpecReads: JSWrites[SOPSpec] = Json.writes[SOPSpec]
//  implicit lazy val spSpecWrites: JSReads[SPSpec] = Json.reads[SPSpec]
//  implicit lazy val spSpecReads: JSWrites[SPSpec] = Json.writes[SPSpec]
//  implicit lazy val spResWrites: JSReads[SPResult] = Json.reads[SPResult]
//  implicit lazy val spResReads: JSWrites[SPResult] = Json.writes[SPResult]
//  implicit lazy val spStateWrites: JSReads[SPState] = Json.reads[SPState]
//  implicit lazy val spStateReads: JSWrites[SPState] = Json.writes[SPState]
//  implicit lazy val structWrites: JSReads[Struct] = Json.reads[Struct]
//  implicit lazy val structReads: JSWrites[Struct] = Json.writes[Struct]
//  implicit lazy val idableWrites: JSFormat[IDAble] = deriveFormatISA[IDAble]
  //implicit lazy val idableReads: JSWrites[IDAble] = Json.writes[IDAble]

  //implicit def toJsAttribWrapper[T](field: T)(implicit w: JSWrites[T]): AttributeWrapper = Json.toJsFieldJsValueWrapper(field)(w)



//  implicit val idAbleR = __.read[Operation].map(x => x:IDAble) orElse
//    __.read[Thing].map(x => x:IDAble) orElse
//    __.read[SOPSpec].map(x => x:IDAble) orElse
//    __.read[SPSpec].map(x => x:IDAble) orElse
//    __.read[SPResult].map(x => x:IDAble) orElse
//    __.read[SPState].map(x => x:IDAble) orElse
//    __.read[Struct].map(x => x:IDAble)
//
//  implicit val idAbleW = Writes[IDAble]{
//    case x: Operation => Json.toJson(x)
//    case x: SOPSpec => Json.toJson(x)
//    case x: SPSpec => Json.toJson(x)
//    case x: SPResult => Json.toJson(x)
//    case x: SPState => Json.toJson(x)
//    case x: Struct => Json.toJson(x)
//    case x: Thing => Json.toJson(x)
//  }




}





trait JsonDerived{



  import play.api.libs.json.{OFormat, OWrites, Reads}
  import shapeless.Lazy
  import julienrf.json.derived._



  val derive: Json.type = Json

  def deriveCaseObject[A](implicit derivedReads: Lazy[DerivedReads[A]], derivedOWrites: Lazy[DerivedOWrites[A]]): OFormat[A] =
    OFormat(derivedReads.value.reads(TypeTagReads.nested, NameAdapter.identity), derivedOWrites.value.owrites(TypeTagOWrites.nested, NameAdapter.identity))




  //def spFormat[A]: OFormat[A] =  derived.oformat[A]()
  val jsonISA = (__ \ "isa").format[String]


  def deriveFormatSimple[A](implicit derivedReads: Lazy[DerivedReads[A]], derivedOWrites: Lazy[DerivedOWrites[A]]): OFormat[A] =
    OFormat(derivedReads.value.reads(TypeTagReads.nested, NameAdapter.identity), derivedOWrites.value.owrites(TypeTagOWrites.nested, NameAdapter.identity))

  def deriveFormatISA[A](implicit derivedReads: Lazy[DerivedReads[A]], derivedOWrites: Lazy[DerivedOWrites[A]]): OFormat[A] =
    OFormat(derivedReads.value.reads(TypeTagReads.flat(jsonISA), NameAdapter.identity), derivedOWrites.value.owrites(TypeTagOWrites.flat(jsonISA), NameAdapter.identity))

 def deriveReadISA[A](implicit derivedReads: Lazy[DerivedReads[A]]): Reads[A] =
    derivedReads.value.reads(TypeTagReads.flat(jsonISA), NameAdapter.identity)

 def deriveWriteISA[A](implicit derivedOWrites: Lazy[DerivedOWrites[A]]): OWrites[A] =
     derivedOWrites.value.owrites(TypeTagOWrites.flat(jsonISA), NameAdapter.identity)




  // testing simplified
  //def deriveSPFormatClass[T] = Json.format




}



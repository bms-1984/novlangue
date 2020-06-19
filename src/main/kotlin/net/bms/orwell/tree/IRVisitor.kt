package net.bms.orwell.tree

import me.tomassetti.kllvm.*
import net.bms.orwell.*
import net.bms.orwell.llvm.FloatComparison

class IRVisitor(private val func: FunctionBuilder, private var block: BlockBuilder, private val finally: Boolean = false) {
    constructor(func: FunctionBuilder): this(func, func.entryBlock())
    constructor(func: FunctionBuilder, finally: Boolean): this(func, func.entryBlock(), finally)
    fun visit(node: Node?): Value = when(node) {
        is AdditionNode -> visit(node)
        is SubtractionNode -> visit(node)
        is MultiplicationNode -> visit(node)
        is DivisionNode -> visit(node)
        is NegateNode -> visit(node)
        is NumberNode -> visit(node)
        is ValNode -> visit(node)
        is FunCallNode -> visit(node)
        is FunDefNode -> visit(node)
        is LimitedIfNode -> visit(node)
        is CompNode -> visit(node)
        is BodyNode -> visit(node)
        is MasterNode -> visit(node)
        else -> Null(VoidType)
    }
    private fun visit(node: MasterNode): Value {
        node.prog.forEach { visit(it) }
        if (finally)
            block.addInstruction(ReturnInt(0))
        return Null(VoidType)
    }
    private fun visit(node: BodyNode): Value = visit(node, block)
    private fun visit(node: BodyNode?, block: BlockBuilder): Value {
        node?.list?.forEach{ IRVisitor(func, block).visit(OrwellVisitor().visit(it)) }
        block.addInstruction(JumpInstruction(Label("post")))
        return Null(VoidType)
    }
    private fun visit(node: CompNode): Value =
        block.tempValue(FloatComparison(node.type!!, visit(node.left), visit(node.right))).reference()
    private fun visit(node: LimitedIfNode): Value {
        if (node is IfNode) {
            val comp = visit(node.comp)
            val yes = func.createBlock("true")
            visit(node.`if`, yes)
            val no = func.createBlock("false")
            visit(node.`else`, no)
            val post = func.createBlock("post")
            block.addInstruction(IfInstruction(comp, yes, no))
            block = post
        }
        return Null(VoidType)
    }
    private fun visit(node: AdditionNode): Value {
        val left = visit(node.left)
        val right = visit(node.right)
        val inst = FloatAddition(left, right)
        return block.tempValue(inst).reference()
    }
    private fun visit(node: SubtractionNode): Value {
        val left = visit(node.left)
        val right = visit(node.right)
        val inst = FloatSubtraction(left, right)
        return block.tempValue(inst).reference()
    }
    private fun visit(node: MultiplicationNode): Value {
        val left = visit(node.left)
        val right = visit(node.right)
        val inst = FloatMultiplication(left, right)
        return block.tempValue(inst).reference()
    }
    private fun visit(node: DivisionNode): Value {
        val left = visit(node.left)
        val right = visit(node.right)
        val inst = FloatDivision(left, right)
        return block.tempValue(inst).reference()
    }
    private fun visit(node: NegateNode): Value = FloatConst(-(visit(node.innerNode) as FloatConst).value, FloatType)
    private fun visit(node: NumberNode): Value = FloatConst(node.value.toFloat(), FloatType)
    private fun visit(node: ValNode): Value {
        if (node.id.isEmpty())
            return visit(node.value)
        else if (node.value != null && node.isNew) {
            if (valStore.containsKey(node.id))
                println("\tWARNING: Variable ${node.id} already exists. You should not use `val` here.")
            valStore[node.id] = visit(node.value)
            // module.floatGlobalVariable(node.id, value = (valStore[node.id]!! as FloatConst).value)
            return valStore[node.id]!!
        }
        else if (node.value != null && !node.isNew) {
            if (!valStore.containsKey(node.id)) {
                println("\tERROR: The variable ${node.id} does not exist. Try using `val`.")
                return Null(VoidType)
            }
            valStore[node.id] = visit(node.value)
            return valStore[node.id]!!
        }
        else if (func.name in valStoreFun && node.id in valStoreFun[func.name]!!)
            return valStoreFun[func.name]?.get(node.id)!!
        else if (node.id in valStore)
            return valStore[node.id]!!
        else {
            println("\tERROR: The variable ${node.id} does not exist.")
            return Null(VoidType)
        }
    }
    private fun visit(node: FunCallNode): Value {
        val args = Array(node.args.size) {
            visit(node.args[it])
        }
        if(!funStore.containsKey(node.`fun`))
        {
            println("\tERROR: The function ${node.`fun`} does not exist.")
            return Null(VoidType)
        }
        if (node.args.size != valStoreFun[node.`fun`]?.size) {
            println("\tERROR: The function ${node.`fun`} takes ${valStoreFun[node.`fun`]?.size} arguments, but you supplied ${node.args.size}.")
            return Null(VoidType)
        }
        val inst = Call(FloatType, node.`fun`, *args)
        return block.tempValue(inst).reference()
    }
    private fun visit(node: FunDefNode): Value {
        if (funStore.containsKey(node.`fun`)) {
            println("\tERROR: The function ${node.`fun`} already exists.")
            return Null(VoidType)
        }
        val names = Array(node.arg.size) {
            node.arg[it].id
        }
        val funct = module.createFunction(node.`fun`, FloatType, List(node.arg.size) { FloatType })
        funStore[node.`fun`] = funct
        if (!valStoreFun.containsKey(node.`fun`))
            valStoreFun[node.`fun`] = HashMap()
        names.forEachIndexed { index, s ->
            val v = funct.paramReference(index)
            valStoreFun[node.`fun`]?.set(s, v)
        }
        val ret = IRVisitor(funct).visit(OrwellVisitor().visit(node.expr))
        val con = funct.tempValue(ConversionFloatToSignedInt(ret, I8Type))
        funct.addInstruction(Printf(funct.stringConstForContent("${node.`fun`} result: %d\n").reference(), con.reference()))
        funct.addInstruction(Return(ret))
        return ret
    }
}
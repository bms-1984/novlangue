package net.bms.orwell.tree

import me.tomassetti.kllvm.*
import net.bms.orwell.*

class IRVisitor(private val func: FunctionBuilder) {
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
        else -> Null(VoidType)
    }

    private fun visit(node: AdditionNode): Value {
        val left = visit(node.left)
        val right = visit(node.right)
        val inst = FloatAddition(left, right)
        return func.tempValue(inst).reference()
    }
    private fun visit(node: SubtractionNode): Value {
        val left = visit(node.left)
        val right = visit(node.right)
        val inst = FloatSubtraction(left, right)
        return func.tempValue(inst).reference()
    }
    private fun visit(node: MultiplicationNode): Value {
        val left = visit(node.left)
        val right = visit(node.right)
        val inst = FloatMultiplication(left, right)
        return func.tempValue(inst).reference()
    }
    private fun visit(node: DivisionNode): Value {
        val left = visit(node.left)
        val right = visit(node.right)
        val inst = FloatDivision(left, right)
        return func.tempValue(inst).reference()
    }
    private fun visit(node: NegateNode): Value = FloatConst(-(visit(node.innerNode) as FloatConst).value, FloatType)
    private fun visit(node: NumberNode): Value = FloatConst(node.value.toFloat(), FloatType)
    private fun visit(node: ValNode): Value =
        if (node.id.isEmpty())
            visit(node.value)
        else if (node.value != null) {
            valStore[node.id] = visit(node.value)
            module.floatGlobalVariable(node.id, value = (valStore[node.id] as FloatConst).value)
            valStore[node.id]!!
        }
        else if (func.name in valStoreFun && node.id in valStoreFun[func.name]!!)
            valStoreFun[func.name]?.get(node.id)!!
        else if (node.id in valStore)
            valStore[node.id]!!
        else {
            println("\tERROR: The variable ${node.id} does not exist.")
            Null(VoidType)
        }

    private fun visit(node: FunCallNode): Value {
        val args = Array(node.args.size) {
            visit(node.args[it])
        }
        val inst = Call(FloatType, node.`fun`, *args)
        return func.tempValue(inst).reference()
    }
    private fun visit(node: FunDefNode): Value {
        val names = Array(node.arg.size) {
            node.arg[it].id
        }
        val funct = module.createFunction(node.`fun`, FloatType, List(node.arg.size) { FloatType })
        funStore[node.`fun`] = funct
        if (!valStoreFun.containsKey(node.`fun`))
            valStoreFun[node.`fun`] = HashMap()
        names.withIndex().forEach {
            val v = funct.paramReference(it.index)
            valStoreFun[node.`fun`]?.set(it.value, v)
        }
        val ret = IRVisitor(funct).visit(OrwellVisitor().visit(node.expr))
        val con = funct.tempValue(ConversionFloatToSignedInt(ret, I8Type))
        funct.addInstruction(Printf(funct.stringConstForContent("Result: %d\n").reference(), con.reference()))
        funct.addInstruction(Return(ret))
        return ret
    }
}
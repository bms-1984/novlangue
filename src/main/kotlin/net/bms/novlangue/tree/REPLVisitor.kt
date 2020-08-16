package net.bms.novlangue.tree

import me.tomassetti.kllvm.DoubleConst
import me.tomassetti.kllvm.FunctionBuilder
import me.tomassetti.kllvm.Load
import me.tomassetti.kllvm.Value

/**
 * Extension of [IRVisitor] to facilitate a read-eval-print-loop
 *
 * @property func passed to [IRVisitor]
 */
class REPLVisitor(func: FunctionBuilder, helperFuncs: Boolean = false) : IRVisitor(func, helperFuncs = helperFuncs) {

    override fun visit(node: Node?): Value = when (node) {
        is InfixExpressionNode -> visit(node)
        else -> super.visit(node)
    }

    override fun visit(node: FunDefNode): Value {
        val ret = super.visit(node)
        println("\tfunction ${node.`fun`} bound")
        return ret
    }

    private fun visit(node: InfixExpressionNode): Value {
        val ret = when (node) {
            is MultiplicationNode -> DoubleConst((node.left as NumberNode).value * (node.right as NumberNode).value)
            is DivisionNode -> DoubleConst((node.left as NumberNode).value / (node.right as NumberNode).value)
            is SubtractionNode -> DoubleConst((node.left as NumberNode).value - (node.right as NumberNode).value)
            else -> DoubleConst((node.left as NumberNode).value + (node.right as NumberNode).value)
        }
        println("\t${(node.left as NumberNode).value} ${node.operator} ${(node.right as NumberNode).value} -> ${ret.value}")
        return ret
    }

    override fun visit(node: FunCallNode): Value {
        val ret = Load(super.visit(node)).value
        println("\t${node.`fun`}(...) -> $ret")
        return ret
    }
}
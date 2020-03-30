package net.bms.orwell.tree

import net.bms.orwell.OrwellFunction
import net.bms.orwell.funStore
import net.bms.orwell.valStore

object ASTVisitor {
    fun visit(node: Node?) = when(node) {
        is AdditionNode -> visit(node)
        is SubtractionNode -> visit(node)
        is MultiplicationNode -> visit(node)
        is DivisionNode -> visit(node)
        is NegateNode -> visit(node)
        is NumberNode -> visit(node)
        is ValNode -> visit(node)
        is FunCallNode -> visit(node)
        is FunDefNode -> visit(node)
        else -> Double.NaN
    }

    private fun visit(node: AdditionNode): Double = visit(node.left) + visit(node.right)
    private fun visit(node: SubtractionNode): Double = visit(node.left) - visit(node.right)
    private fun visit(node: MultiplicationNode): Double = visit(node.left) * visit(node.right)
    private fun visit(node: DivisionNode): Double = visit(node.left) / visit(node.right)
    private fun visit(node: NegateNode): Double = -visit(node.innerNode)
    private fun visit(node: NumberNode): Double = node.value
    private fun visit(node: ValNode): Double {
        val value: Any? =
            if (node.value != null) {
                visit(node.value)
            } else null
        valStore[node.id] = value
        println ("\t${node.id} -> ${valStore[node.id]}")
        return visit(node.value)
    }
    fun visitWithNoSideEffects(node: ValNode): Double {
        return visit(node.value)
    }
    private fun visit(node: FunCallNode): Double {
        if (funStore.containsKey(node.`fun`)) {
            val retNode = funStore[node.`fun`]?.runFunction(node.args)
            return visit(retNode)
        }
        return Double.NaN
    }
    private fun visit(node: FunDefNode): Double {
        funStore[node.`fun`] = OrwellFunction(node.arg, node.expr)
        return Double.NaN
    }
}
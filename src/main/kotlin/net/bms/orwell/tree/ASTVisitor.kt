package net.bms.orwell.tree

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
        var type = node.type
        if (valStore.containsKey(node.id))
            type = valStore[node.id]!!.second
        valStore[node.id] = Pair(value, type)
        println ("\t${node.id}: ${valStore[node.id]!!.second} = ${valStore[node.id]!!.first}")
        return Double.NaN
    }
}
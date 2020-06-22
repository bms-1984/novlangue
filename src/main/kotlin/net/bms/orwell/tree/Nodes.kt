package net.bms.orwell.tree

import me.tomassetti.kllvm.ComparisonType
import net.bms.orwell.llvm.FloatComparisonType

abstract class Node {
    fun toValNode(): ValNode =
        ValNode().also {
            it.id = ""
            it.value = this
        }
}

class MasterNode: Node() {
    val prog = ArrayList<Node?>()
}

abstract class InfixExpressionNode: Node() {
    abstract var left: Node
    abstract var right: Node
}

class AdditionNode: InfixExpressionNode() {
    override lateinit var left: Node
    override lateinit var right: Node
}

class SubtractionNode: InfixExpressionNode() {
    override lateinit var left: Node
    override lateinit var right: Node
}

class MultiplicationNode: InfixExpressionNode() {
    override lateinit var left: Node
    override lateinit var right: Node
}

class DivisionNode: InfixExpressionNode() {
    override lateinit var left: Node
    override lateinit var right: Node
}

class NegateNode: Node() {
    lateinit var innerNode: Node
}

class FunDefNode: Node() {
    lateinit var `fun`: String
    val arg = ArrayList<ValNode>()
    val body = ArrayList<OrwellParser.TopContext>()
    var returnExpr: OrwellParser.EContext? = null
}

class FunCallNode: Node() {
    lateinit var `fun`: String
    val args = ArrayList<ValNode>()
}

class NumberNode: Node() {
    var value: Double = 0.0
}

class ValNode: Node() {
    lateinit var id: String
    var value: Node? = null
    var isNew = false
}

open class IfNode: Node() {
    var `if`: BodyNode? = null
    var comp: CompNode? = null
    var elif = ArrayList<IfNode?>()
    var `else`: BodyNode? = null
    var isTop = false
}

class BodyNode: Node() {
    val list = ArrayList<OrwellParser.TopContext>()
}

class CompNode: Node() {
    var left: ValNode? = null
    var right: ValNode? = null
    var type: FloatComparisonType? = null
}


package net.bms.orwell.tree

abstract class Node {
    fun toValNode(): ValNode {
        val node = ValNode()
        node.id = ""
        node.value = this
        return node
    }
}

abstract class InfixExpressionNode : Node()
{
    abstract var left: Node
    abstract var right: Node
}

class AdditionNode : InfixExpressionNode()
{
    override lateinit var left: Node
    override lateinit var right: Node
}

class SubtractionNode : InfixExpressionNode()
{
    override lateinit var left: Node
    override lateinit var right: Node
}

class MultiplicationNode : InfixExpressionNode()
{
    override lateinit var left: Node
    override lateinit var right: Node
}

class DivisionNode : InfixExpressionNode()
{
    override lateinit var left: Node
    override lateinit var right: Node
}

class NegateNode : Node()
{
    lateinit var innerNode: Node
}

class FunDefNode: Node()
{
    lateinit var `fun`: String
    var arg = ArrayList<ValNode>()
    lateinit var expr: OrwellParser.EContext
}

class FunCallNode : Node()
{
    lateinit var `fun`: String
    var args = ArrayList<ValNode>()
}

class NumberNode : Node()
{
    var value: Double = 0.0
}

class ValNode: Node() {
    lateinit var id: String
    var value: Node? = null
    var isNew = false
}


package net.bms.orwell.tree

import OrwellParser
import me.tomassetti.kllvm.FloatComparisonType

/**
 * AST Node
 */
abstract class Node {
    /**
     * Converts any [Node] to a [ValNode]
     *
     * @return [ValNode] form.
     */
    fun toValNode(): ValNode =
        ValNode().apply {
            id = ""
            value = this@Node
        }
}

/**
 * Top-level node
 *
 * @property prog list of all nodes in the program.
 */
class MasterNode : Node() {
    val prog: ArrayList<Node> = ArrayList()
}

/**
 * Basic node type for all binary operators
 *
 * @property left left side.
 * @property right right side.
 */
abstract class InfixExpressionNode : Node() {
    abstract var left: Node
    abstract var right: Node
}

/**
 * Node type for '+'
 */
class AdditionNode(override var left: Node, override var right: Node) : InfixExpressionNode()

/**
 * Node type for '-'
 */
class SubtractionNode(override var left: Node, override var right: Node) : InfixExpressionNode()

/**
 * Node type for '*'
 */
class MultiplicationNode(override var left: Node, override var right: Node) : InfixExpressionNode()

/**
 * Node type for '/'
 */
class DivisionNode(override var left: Node, override var right: Node) : InfixExpressionNode()

/**
 * Node type for negative numbers
 *
 * @property innerNode the node within.
 */
class NegateNode : Node() {
    lateinit var innerNode: Node
}

/**
 * Node type for function definitions
 *
 * @property fun function name.
 * @property arg list of [ValNode]s with non-null [ValNode.id]s.
 * @property body list of lines in the function body.
 * @property returnExpr final line in the function to return, must be an expression.
 */
class FunDefNode : Node() {
    lateinit var `fun`: String
    val arg: ArrayList<ValNode> = ArrayList()
    val body: ArrayList<OrwellParser.TopContext> = ArrayList()
    var returnExpr: OrwellParser.EContext? = null
}

/**
 * Node type for function calls
 *
 * @property fun function name.
 * @property args list of [ValNode]s with non-null [ValNode.value]s
 */
class FunCallNode : Node() {
    lateinit var `fun`: String
    val args: ArrayList<ValNode> = ArrayList()
}

/**
 * Node type for all numbers
 *
 * @property value number.
 */
class NumberNode : Node() {
    var value: Double = 0.0
}

/**
 * General node type for identifiers and anything passed around as a parameter
 *
 * @property id name, if representing an identifier
 * @property value value of the node
 * @property isNew false, unless this is a variable definition
 */
class ValNode : Node() {
    var id: String = ""
    var value: Node? = null
    var isNew: Boolean = false
}

class ConditionalNode : Node() {
    lateinit var `true`: BodyNode
    var `false`: BodyNode? = null
    var chain = ArrayList<ConditionalNode>()
    var isTop = false
    lateinit var comp: CompNode
    var isLoop = false
}

/**
 * Body of a conditional
 *
 * @property list list of lines to run.
 */
class BodyNode : Node() {
    val list: ArrayList<OrwellParser.TopContext> = ArrayList()
}

/**
 * Node type for a comparison
 *
 * @property left left side.
 * @property right right side.
 * @property type which kind of comparison.
 */
class CompNode : Node() {
    var left: ValNode = ValNode()
    var right: ValNode = ValNode()
    var type: FloatComparisonType = FloatComparisonType.Equal
}


package net.bms.orwell.tree

import OrwellParser
import me.tomassetti.kllvm.ComparisonType

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
            if (this@Node is NumberNode) type = this@Node.type
            else if (this@Node is ValNode) type = this@Node.type
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
 * @property operator operator symbol.
 */
abstract class InfixExpressionNode : Node() {
    abstract var left: Node
    abstract var right: Node
    abstract var operator: String
}

/**
 * Node type for '+'
 */
class AdditionNode(override var left: Node, override var right: Node, override var operator: String = "+") :
    InfixExpressionNode()

/**
 * Node type for '-'
 */
class SubtractionNode(override var left: Node, override var right: Node, override var operator: String = "-") :
    InfixExpressionNode()

/**
 * Node type for '*'
 */
class MultiplicationNode(override var left: Node, override var right: Node, override var operator: String = "*") :
    InfixExpressionNode()

/**
 * Node type for '/'
 */
class DivisionNode(override var left: Node, override var right: Node, override var operator: String = "/") :
    InfixExpressionNode()

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
 * @property returnType type to return.
 */
class FunDefNode : Node() {
    lateinit var `fun`: String
    val arg: ArrayList<ValNode> = ArrayList()
    val body: BodyNode = BodyNode()
    var returnType: ValTypes = ValTypes.INT
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
 * @property type datatype;
 */
class NumberNode : Node() {
    var value: Double = 0.0
    var type: ValTypes = ValTypes.INT
}

/**
 * General node type for identifiers and anything passed around as a parameter
 *
 * @property id name, if representing an identifier
 * @property value value of the node
 * @property isNew false, unless this is a variable definition
 * @property type datatype
 */
class ValNode : Node() {
    var id: String = ""
    var value: Node? = null
    var isNew: Boolean = false
    var type: ValTypes = ValTypes.INT
}

/**
 * Types
 *
 * @property type text name.
 */
enum class ValTypes(val type: String) {
    /**
     * floating-point number
     */
    DOUBLE("double"),

    /**
     * integer number
     */
    INT("int"),

    /**
     * sequence of characters
     */
    STRING("string"),
}

/**
 * common node for whiles and ifs
 *
 * @property true body to execute when [comp] evaluates to true
 * @property false body to execute when [comp] evaluates to false
 * @property chain list of additional else-ifs
 * @property isTop true if this is not an elseIf
 * @property comp comparison
 * @property isLoop true if this is a while
 */
class ConditionalNode : Node() {
    lateinit var `true`: BodyNode
    var `false`: BodyNode? = null
    var chain: ArrayList<ConditionalNode> = ArrayList()
    var isTop: Boolean = false
    lateinit var comp: CompNode
    var isLoop: Boolean = false
}

/**
 * Body of a conditional
 *
 * @property list list of lines to run.
 * @property returnExpr value to return.
 */
class BodyNode : Node() {
    val list: ArrayList<OrwellParser.TopContext> = ArrayList()
    var returnExpr: OrwellParser.EContext? = null
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
    var type: ComparisonType = ComparisonType.IntEqual
}


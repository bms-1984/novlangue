package net.bms.orwell.tree

import jdk.nashorn.internal.parser.Token
import java.util.function.Function

abstract class Node

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

class FunCallNode : Node()
{
    lateinit var func: Function<Double, Double>
    lateinit var arg: Node
}

class NumberNode : Node()
{
    var value: Double = 0.0
}

class ValNode: Node() {
    lateinit var id: String
    var value: Node? = null
    var type: String? = null
}


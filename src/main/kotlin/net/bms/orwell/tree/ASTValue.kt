package net.bms.orwell.tree

open class ASTValue(private val value: Any, private val line: Int) {
    fun getValue() = value
    fun getLine() = line
}
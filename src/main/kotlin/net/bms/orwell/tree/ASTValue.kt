package net.bms.orwell.tree

class ASTValue(private val value: Any, private val line: Int) {
    fun getValue() = value
    fun getLine() = line

    override fun toString(): String {
        return "Value: $value, Line: $line"
    }
}
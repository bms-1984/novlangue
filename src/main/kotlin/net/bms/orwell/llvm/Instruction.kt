package net.bms.orwell.llvm

import me.tomassetti.kllvm.BooleanType
import me.tomassetti.kllvm.ComparisonType
import me.tomassetti.kllvm.Instruction
import me.tomassetti.kllvm.Value

data class FloatComparison(val comparisonType: FloatComparisonType, val left: Value, val right: Value) : Instruction {
    override fun IRCode() = "fcmp ${comparisonType.code} ${left.type().IRCode()} ${left.IRCode()}, ${right.IRCode()}"
    override fun type() = BooleanType
}

enum class FloatComparisonType(val code: String) {
    Equal("oeq"),
    NotEqual("one")
}
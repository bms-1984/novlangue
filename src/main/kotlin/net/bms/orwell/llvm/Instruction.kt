package net.bms.orwell.llvm

import me.tomassetti.kllvm.BooleanType
import me.tomassetti.kllvm.Instruction
import me.tomassetti.kllvm.Value

/**
 * Instruction type for comparing FloatConsts
 *
 * @property comparisonType which kind of comparison.
 * @property left left side.
 * @property right right side.
 */
data class FloatComparison(val comparisonType: FloatComparisonType, val left: Value, val right: Value) : Instruction {
    /**
     * @return Generated IR
     */
    override fun IRCode(): String =
        "fcmp ${comparisonType.code} ${left.type().IRCode()} ${left.IRCode()}, ${right.IRCode()}"

    /**
     * @return Comparison type
     */
    override fun type(): BooleanType = BooleanType
}

/**
 * Types of [FloatComparison]s
 *
 * @property code operator.
 */
enum class FloatComparisonType(val code: String) {
    /**
     * =
     */
    Equal("oeq"),

    /**
     * !=
     */
    NotEqual("one"),

    /**
     * >
     */
    GreaterThan("ogt"),

    /**
     * >=
     */
    GreaterThanOrEqual("oge"),

    /**
     * <
     */
    LessThan("olt"),

    /**
     * <=
     */
    LessThanOrEqual("ole"),
}
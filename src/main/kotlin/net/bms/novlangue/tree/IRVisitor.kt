/*
Copyright Ben M. Sutter 2020

This file is part of Novlangue.

Novlangue is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Novlangue is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Novlangue.  If not, see <https://www.gnu.org/licenses/>.
*/
package net.bms.novlangue.tree

import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef
import org.bytedeco.llvm.LLVM.LLVMBuilderRef
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

/**
 * Translator from AST to LLVM IR
 *
 * @author Ben M. Sutter
 * @since 0.1.1
 * @constructor Creates a new IRVisitor object.
 * @property func function in which the IR will reside, defaults to main.
 * @property block block in which the IR will reside, defaults to main's entry block.
 * @property finally should a return statement be appended, defaults to false.
 */
open class IRVisitor(
    private val func: LLVMValueRef,
    private val builder: LLVMBuilderRef,
    var block: LLVMBasicBlockRef = LLVM.LLVMGetEntryBasicBlock(func),
    private val finally: Boolean = false,
    private val exitBlock: LLVMBasicBlockRef? = null,
    private var tempIndex: Int = 0
) {
    internal open fun visit(node: Node?): LLVMValueRef = when (node) {
        is AdditionNode -> visit(node)
        is SubtractionNode -> visit(node)
        is MultiplicationNode -> visit(node)
        is DivisionNode -> visit(node)
        is ModuloNode -> visit(node)
        is NegateNode -> visit(node)
        is NumberNode -> visit(node)
        is ValNode -> visit(node)
        is FunCallNode -> visit(node)
        is FunDefNode -> visit(node)
        is ConditionalNode -> visit(node)
        is CompNode -> visit(node)
        is MasterNode -> visit(node)
        is BodyNode -> visit(node)
        is StringNode -> visit(node)
        else -> LLVM.LLVMConstNull(LLVM.LLVMVoidType())
    }

    /**
     * Return a temporary index
     */
    private fun getTempIndex(): Int = tempIndex++

    /**
     * Returns a unique label
     *
     * @param title optional custom string to insert into label.
     */
    private fun getUniqueID(title: String = ""): String =
        if (title.isEmpty()) "_INTERNAL_${LLVM.LLVMGetValueName(func)}_${getTempIndex()}".toUpperCase()
        else "_INTERNAL_${LLVM.LLVMGetValueName(func)}_${title}_${getTempIndex()}".toUpperCase()

    private fun mangleFunName(name: String, vararg types: LLVMTypeRef = arrayOf()): String {
        var ret = name
        types.forEach { ret += "_${LLVM.LLVMPrintTypeToString(it).toString().filter { c -> c.isLetterOrDigit() }}" }
        return ret.toUpperCase()
    }

    internal open fun visit(node: BodyNode): LLVMValueRef {
        node.list.forEach { visit(CodeVisitor().visit(it)) }
        if (node.returnExpr != null)
            LLVM.LLVMInsertIntoBuilder(
                builder,
                LLVM.LLVMBuildRet(builder, visit(CodeVisitor().visit(node.returnExpr)))
            )
        else
            LLVM.LLVMInsertIntoBuilder(
                builder,
                LLVM.LLVMBuildRet(
                    builder,
                    when (node.returnType) {
                        ValTypes.DOUBLE -> LLVM.LLVMConstReal(LLVM.LLVMDoubleType(), 0.0)
                        else -> LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, 0)
                    }
                )
            )

        return LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, 0)
    }

    internal open fun visit(node: MasterNode): LLVMValueRef {
        node.prog.forEach {
            if (it is InfixExpressionNode) LLVM.LLVMInsertIntoBuilder(builder, visit(it))
            else visit(it)
        }
        if (finally) LLVM.LLVMInsertIntoBuilder(
            builder,
            LLVM.LLVMBuildRet(
                builder,
                LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, 0)
            )
        )

        return LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, 0)
    }

    internal open fun visit(node: ConditionalNode): LLVMValueRef = TODO()

    internal open fun visit(node: CompNode): LLVMValueRef = when (node.left.type) {
        ValTypes.DOUBLE -> LLVM.LLVMBuildFCmp(builder, node.type, visit(node.left), visit(node.right), getUniqueID())
        else -> LLVM.LLVMBuildICmp(builder, node.type, visit(node.left), visit(node.right), getUniqueID())
    }

    internal open fun visit(node: AdditionNode): LLVMValueRef = when (node.left.toValNode().type) {
        ValTypes.DOUBLE -> LLVM.LLVMBuildFAdd(builder, visit(node.left), visit(node.right), "add")
        else -> LLVM.LLVMBuildAdd(builder, visit(node.left), visit(node.right), "add")
    }

    internal open fun visit(node: SubtractionNode): LLVMValueRef = when (node.left.toValNode().type) {
        ValTypes.DOUBLE -> LLVM.LLVMBuildFSub(builder, visit(node.left), visit(node.right), "sub")
        else -> LLVM.LLVMBuildSub(builder, visit(node.left), visit(node.right), "sub")
    }

    internal open fun visit(node: MultiplicationNode): LLVMValueRef = when (node.left.toValNode().type) {
        ValTypes.DOUBLE -> LLVM.LLVMBuildFMul(builder, visit(node.left), visit(node.right), "mul")
        else -> LLVM.LLVMBuildMul(builder, visit(node.left), visit(node.right), "mul")
    }

    internal open fun visit(node: DivisionNode): LLVMValueRef = when (node.left.toValNode().type) {
        ValTypes.DOUBLE -> LLVM.LLVMBuildFDiv(builder, visit(node.left), visit(node.right), "div")
        else -> LLVM.LLVMBuildSDiv(builder, visit(node.left), visit(node.right), "div")
    }

    internal open fun visit(node: ModuloNode): LLVMValueRef = when (node.left.toValNode().type) {
        ValTypes.DOUBLE -> LLVM.LLVMBuildFRem(builder, visit(node.left), visit(node.right), "mod")
        else -> LLVM.LLVMBuildSRem(builder, visit(node.left), visit(node.right), "mod")
    }

    internal open fun visit(node: NegateNode): LLVMValueRef = when (node.innerNode.toValNode().type) {
        ValTypes.DOUBLE -> LLVM.LLVMBuildFNeg(builder, visit(node.innerNode), "neg")
        else -> LLVM.LLVMBuildNeg(builder, visit(node.innerNode), "neg")
    }

    internal open fun visit(node: NumberNode): LLVMValueRef = when (node.type) {
        ValTypes.DOUBLE -> LLVM.LLVMConstReal(LLVM.LLVMDoubleType(), node.value)
        else -> LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), node.value.toLong(), 0)
    }

    internal open fun visit(node: StringNode): LLVMValueRef = TODO()

    internal open fun visit(node: ValNode): LLVMValueRef = TODO()

    internal open fun visit(node: FunCallNode): LLVMValueRef = TODO()

    internal open fun visit(node: FunDefNode): LLVMValueRef = TODO()
}

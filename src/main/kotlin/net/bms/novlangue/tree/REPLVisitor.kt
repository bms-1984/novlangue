/* (C) Ben M. Sutter 2020 */
package net.bms.novlangue.tree

import net.bms.novlangue.builder
import org.bytedeco.llvm.LLVM.LLVMValueRef

/**
 * Extension of [IRVisitor] to facilitate a read-eval-print-loop
 *
 * @property func passed to [IRVisitor]
 */
class REPLVisitor(func: LLVMValueRef, helperFuncs: Boolean = false) :
    IRVisitor(func, builder, helperFuncs = helperFuncs) {

    override fun visit(node: Node?): LLVMValueRef = when (node) {
        is InfixExpressionNode -> visit(node)
        else -> super.visit(node)
    }

    override fun visit(node: FunDefNode): LLVMValueRef {
        val ret = super.visit(node)
        println("\tfunction ${node.`fun`} bound")
        return ret
    }

    private fun visit(node: InfixExpressionNode): LLVMValueRef = TODO()

    override fun visit(node: FunCallNode): LLVMValueRef = TODO()
}

/* (C) Ben M. Sutter 2020 */
package net.bms.novlangue.tree

import net.bms.novlangue.builder
import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef
import org.bytedeco.llvm.LLVM.LLVMValueRef

/**
 * Visitor for whiles and ifs
 *
 * @property func function containing the code
 * @property entryBlock jump from here
 * @property exitBlock jump to and continue execution here
 * @property trueBlock jump here if true
 * @property falseBlock jump here if false
 */
class ConditionalVisitor(
    private val func: LLVMValueRef,
    private val entryBlock: LLVMBasicBlockRef,
    private var exitBlock: LLVMBasicBlockRef,
    private val trueBlock: LLVMBasicBlockRef,
    private val falseBlock: LLVMBasicBlockRef,
) : IRVisitor(func, builder, entryBlock) {
    internal fun visit(
        trueBody: BodyNode,
        falseBody: BodyNode?,
        comp: CompNode,
        chain: ArrayList<ConditionalNode>
    ): LLVMValueRef = TODO()

    internal fun visitLoop(trueBody: BodyNode, comp: CompNode): LLVMValueRef = TODO()

    private fun visitBlock(node: BodyNode): LLVMValueRef = TODO()
}

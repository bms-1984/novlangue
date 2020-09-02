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

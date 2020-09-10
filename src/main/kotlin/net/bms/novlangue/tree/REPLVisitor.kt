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
import org.bytedeco.llvm.LLVM.LLVMValueRef

/**
 * Extension of [IRVisitor] to facilitate a read-eval-print-loop
 *
 * @property func passed to [IRVisitor]
 */
class REPLVisitor(func: LLVMValueRef) :
    IRVisitor(func, builder) {

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

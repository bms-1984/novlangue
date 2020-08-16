package net.bms.novlangue.tree

import me.tomassetti.kllvm.BlockBuilder
import me.tomassetti.kllvm.FunctionBuilder
import me.tomassetti.kllvm.IfInstruction
import me.tomassetti.kllvm.JumpInstruction

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
    private val func: FunctionBuilder, private val entryBlock: BlockBuilder,
    private var exitBlock: BlockBuilder, private val trueBlock: BlockBuilder,
    private val falseBlock: BlockBuilder,
) : IRVisitor(func, entryBlock) {
    internal fun visit(trueBody: BodyNode, falseBody: BodyNode?, comp: CompNode, chain: ArrayList<ConditionalNode>) {
        entryBlock.addInstruction(IfInstruction(visit(comp), trueBlock, falseBlock))
        block = trueBlock
        visitBlock(trueBody)
        trueBlock.addInstruction(JumpInstruction(exitBlock.label()))

        if (chain.isNotEmpty()) {
            chain[0].chain = chain.filterIndexed { index, _ -> index != 0 } as ArrayList<ConditionalNode>
            if (chain.size == 1) chain[0].chain.clear()
            chain[0].`false` = falseBody
            block = falseBlock
            IRVisitor(func, block, exitBlock = exitBlock).visit(chain[0])
        } else {
            val falseBodyFixed = falseBody ?: BodyNode()
            block = falseBlock
            visitBlock(falseBodyFixed)
            falseBlock.addInstruction(JumpInstruction(exitBlock.label()))
        }
    }

    internal fun visitLoop(trueBody: BodyNode, comp: CompNode) {
        val testBlock = func.createBlock(getUniqueID("CONDITIONAL_TEST"))
        entryBlock.addInstruction(JumpInstruction(testBlock.label()))
        testBlock.addInstruction(IfInstruction(IRVisitor(func, testBlock).visit(comp), trueBlock, exitBlock))
        block = trueBlock
        visitBlock(trueBody)
        block.addInstruction(JumpInstruction(testBlock.label()))
    }

    private fun visitBlock(node: BodyNode) {
        if (node.list.isNotEmpty())
            node.list.forEach { visit(CodeVisitor().visitTop(it)) }
    }
}
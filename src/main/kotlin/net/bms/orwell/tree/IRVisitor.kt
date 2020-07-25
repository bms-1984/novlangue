package net.bms.orwell.tree

import me.tomassetti.kllvm.*
import net.bms.orwell.*

/**
 * Translator from AST to LLVM IR
 *
 * @author Ben M. Sutter
 * @since 0.1.1
 * @constructor Creates a new IRVisitor object.
 * @property func function in which the IR will reside, defaults to main.
 * @property block block in which the IR will reside, defaults to main's entry block.
 * @property finally should a return statement be appended, defaults to false.
 * @property helperFuncs should builtin functions be defined.
 */
open class IRVisitor(
    private val func: FunctionBuilder = mainFun, var block: BlockBuilder = func.entryBlock(),
    private val finally: Boolean = false, private val helperFuncs: Boolean = (func == mainFun && finally),
    private val exitBlock: BlockBuilder? = null
) {

    internal fun visit(node: Node?): Value = when (node) {
        is AdditionNode -> visit(node)
        is SubtractionNode -> visit(node)
        is MultiplicationNode -> visit(node)
        is DivisionNode -> visit(node)
        is NegateNode -> visit(node)
        is NumberNode -> visit(node)
        is ValNode -> visit(node)
        is FunCallNode -> visit(node)
        is FunDefNode -> visit(node)
        is ConditionalNode -> visit(node)
        is CompNode -> visit(node)
        is MasterNode -> visit(node)
        else -> Null(VoidType)
    }

    fun getUniqueID(title: String = "") =
        if (title.isEmpty()) "_INTERNAL_${func.name}_${func.tmpIndex()}"
        else "_INTERNAL_${func.name.toUpperCase()}_${title.toUpperCase()}_${func.tmpIndex()}"

    private fun visit(node: MasterNode): Value {
        if (helperFuncs) {
            val print = module.createFunction("print", FloatType, listOf(FloatType))
            funStore += print
            val variable = print.entryBlock().addVariable(FloatType, "d")
            print.entryBlock().assignVariable(variable, print.paramReference(0))
            funValStore += hashMapOf(print.name to arrayListOf(variable as LocalVariable))
            val num = print.tempValue(ConversionFloatToSignedInt(print.paramReference(0), I8Type))
            print.addInstruction(Printf(print.stringConstForContent("%d\n").reference(), num.reference()))
            print.addInstruction(Return(FloatConst(0F, FloatType)))
        }
        node.prog.forEach { visit(it) }
        if (finally) block.addInstruction(ReturnInt(0))

        return Null(VoidType)
    }

    internal open fun visit(node: ConditionalNode): Value {
        val trueBlock = func.createBlock(getUniqueID("conditional_true"))
        val exitBlock = exitBlock ?: func.createBlock(getUniqueID("conditional_exit"))

        if (node.isLoop)
            ConditionalVisitor(func, block, exitBlock, trueBlock, exitBlock).visitLoop(node.`true`, node.comp)
        else {
            val falseBlock = func.createBlock(getUniqueID("conditional_false"))
            ConditionalVisitor(func, block, exitBlock, trueBlock, falseBlock).visit(
                node.`true`,
                node.`false`,
                node.comp,
                node.chain
            )
        }

        block = exitBlock

        return Null(VoidType)
    }

    private fun visit(node: CompNode): Value =
        block.tempValue(FloatComparison(node.type, visit(node.left), visit(node.right))).reference()

    private fun visit(node: AdditionNode): Value =
        block.tempValue(FloatAddition(visit(node.left), visit(node.right))).reference()

    private fun visit(node: SubtractionNode): Value =
        block.tempValue(FloatSubtraction(visit(node.left), visit(node.right))).reference()

    private fun visit(node: MultiplicationNode): Value =
        block.tempValue(FloatMultiplication(visit(node.left), visit(node.right))).reference()

    private fun visit(node: DivisionNode): Value =
        block.tempValue(FloatDivision(visit(node.left), visit(node.right))).reference()

    private fun visit(node: NegateNode): Value = FloatConst(-(visit(node.innerNode) as FloatConst).value, FloatType)
    private fun visit(node: NumberNode): Value = FloatConst(node.value.toFloat(), FloatType)
    private fun visit(node: ValNode): Value {
        if (node.id.isEmpty()) return visit(node.value)
        else if (node.value != null && node.isNew) {
            if (valStore.any { it.name == node.id })
                println("\tWARNING: Variable ${node.id} already exists. You should not use `val` here.")
            valStore += block.addVariable(FloatType, node.id) as LocalVariable
            block.assignVariable(valStore.find { it.name == node.id }!!, visit(node.value))

            return block.tempValue(Load(valStore.find { it.name == node.id }!!.reference())).reference()
        } else if (node.value != null && !node.isNew) {
            if (!valStore.any { it.name == node.id }) {
                println("\tERROR: The variable ${node.id} does not exist. Try using `val`.")

                return Null(VoidType)
            }
            block.assignVariable(valStore.find { it.name == node.id }!!, visit(node.value))

            return block.tempValue(Load(valStore.find { it.name == node.id }!!.reference())).reference()
        } else if (func.name in funValStore && funValStore[func.name]?.any { it.name == node.id }!!)
            return block.tempValue(Load(funValStore[func.name]?.find { it.name == node.id }!!.reference())).reference()
        else if (valStore.any { it.name == node.id })
            return block.tempValue(Load(valStore.find { it.name == node.id }!!.reference())).reference()
        else {
            println("\tERROR: The variable ${node.id} does not exist.")

            return Null(VoidType)
        }
    }

    private fun visit(node: FunCallNode): Value {
        val args = Array(node.args.size) { visit(node.args[it]) }

        if (!funStore.any { it.name == node.`fun` }) {
            println("\tERROR: The function ${node.`fun`} does not exist.")

            return Null(VoidType)
        }
        if (node.args.size != funValStore[node.`fun`]?.size) {
            println(
                "\tERROR: The function ${node.`fun`} takes ${funValStore[node.`fun`]?.size} arguments, " +
                        "but you supplied ${node.args.size}."
            )

            return Null(VoidType)
        }
        val inst = Call(FloatType, node.`fun`, *args)

        return block.tempValue(inst).reference()
    }

    private fun visit(node: FunDefNode): Value {
        if (!funStore.any { it.name == node.`fun` }) {
            println("\tERROR: The function ${node.`fun`} already exists.")
            return Null(VoidType)
        }

        val names = Array(node.arg.size) { node.arg[it].id }
        val funct = module.createFunction(node.`fun`, FloatType, List(node.arg.size) { FloatType })

        funStore += funct
        if (!funValStore.containsKey(node.`fun`)) funValStore[node.`fun`] = ArrayList()
        names.forEachIndexed { index, s ->
            val variable = funct.entryBlock().addVariable(FloatType, s)
            funct.entryBlock().assignVariable(variable, funct.paramReference(index))
            funValStore[node.`fun`]?.add(variable as LocalVariable)
        }
        node.body.forEach { IRVisitor(funct).visit(OrwellVisitor().visit(it)) }
        val ret = IRVisitor(funct).visit(OrwellVisitor().visit(node.returnExpr))
        funct.addInstruction(Return(ret))

        return ret
    }
}
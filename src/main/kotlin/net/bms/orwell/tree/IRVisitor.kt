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
    private val func: FunctionBuilder, var block: BlockBuilder = func.entryBlock(),
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
        is BodyNode -> visit(node)
        else -> Null(VoidType)
    }

    fun getUniqueID(title: String = "") =
        if (title.isEmpty()) "_INTERNAL_${func.name}_${func.tmpIndex()}"
        else "_INTERNAL_${func.name.toUpperCase()}_${title.toUpperCase()}_${func.tmpIndex()}"

    private fun visit(node: BodyNode): Value {
        node.list.forEach { visit(OrwellVisitor().visit(it)) }
        if (node.returnExpr != null)
            block.addInstruction(Return(visit(OrwellVisitor().visit(node.returnExpr))))
        return Null()
    }

    private fun visit(node: MasterNode): Value {
        if (helperFuncs) {
            val print = module.createFunction("print", I32Type, listOf(I32Type))
            funStore += print
            val variable = print.entryBlock().addVariable(I32Type, "d")
            print.entryBlock().assignVariable(variable, print.paramReference(0))
            funValStore += hashMapOf(print.name to arrayListOf(variable as LocalVariable))
            print.addInstruction(
                Printf(
                    print.stringConstForContent("%d\n").reference(),
                    print.entryBlock().load(variable.reference())
                )
            )
            print.addInstruction(Return(print.entryBlock().load(variable.reference())))
        }
        node.prog.forEach { visit(it) }
        if (finally) block.addInstruction(ReturnInt(0))

        return Null()
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

        return Null()
    }

    private fun visit(node: CompNode): Value =
        block.tempValue(IntComparison(node.type, visit(node.left), visit(node.right))).reference()

    private fun visit(node: AdditionNode): Value =
        block.tempValue(IntAddition(visit(node.left), visit(node.right))).reference()

    private fun visit(node: SubtractionNode): Value =
        block.tempValue(IntSubtraction(visit(node.left), visit(node.right))).reference()

    private fun visit(node: MultiplicationNode): Value =
        block.tempValue(IntMultiplication(visit(node.left), visit(node.right))).reference()

    private fun visit(node: DivisionNode): Value =
        block.tempValue(SignedIntDivision(visit(node.left), visit(node.right))).reference()

    private fun visit(node: NegateNode): Value = IntConst(-(visit(node.innerNode) as IntConst).value, I32Type)
    private fun visit(node: NumberNode): Value = IntConst(node.value.toInt(), I32Type)
    private fun visit(node: ValNode): Value {
        if (node.id.isEmpty()) return visit(node.value)
        else if (node.value != null && node.isNew) {
            if (valStore.any { it.name == node.id })
                println("\tWARNING: Variable ${node.id} already exists. You should not use `val` here.")
            valStore +=
                when (node.type) {
                    ValTypes.INT -> block.addVariable(I32Type, node.id) as LocalVariable
                    ValTypes.DOUBLE -> block.addVariable(DoubleType, node.id) as LocalVariable
                    ValTypes.STRING -> block.addVariable(Pointer(I8Type), node.id) as LocalVariable
                }
            block.assignVariable(valStore.find { it.name == node.id }!!, visit(node.value))

            return block.tempValue(Load(valStore.find { it.name == node.id }!!.reference())).reference()
        } else if (node.value != null && !node.isNew) {
            if (!valStore.any { it.name == node.id }) {
                println("\tERROR: The variable ${node.id} does not exist. Try using `val`.")

                return Null()
            }
            block.assignVariable(valStore.find { it.name == node.id }!!, visit(node.value))

            return block.tempValue(Load(valStore.find { it.name == node.id }!!.reference())).reference()
        } else if (func.name in funValStore && funValStore[func.name]?.any { it.name == node.id }!!)
            return block.tempValue(Load(funValStore[func.name]?.find { it.name == node.id }!!.reference())).reference()
        else if (valStore.any { it.name == node.id })
            return block.tempValue(Load(valStore.find { it.name == node.id }!!.reference())).reference()
        else {
            println("\tERROR: The variable ${node.id} does not exist.")

            return Null()
        }
    }

    private fun visit(node: FunCallNode): Value {
        val args = Array(node.args.size) { visit(node.args[it]) }

        if (!funStore.any { it.name == node.`fun` }) {
            println("\tERROR: The function ${node.`fun`} does not exist.")

            return Null()
        }
        if (node.args.size != funValStore[node.`fun`]?.size) {
            println(
                "\tERROR: The function ${node.`fun`} takes ${funValStore[node.`fun`]?.size} arguments, " +
                        "but you supplied ${node.args.size}."
            )

            return Null()
        }
        node.args.forEachIndexed { index, valNode ->
            if (
                !(((funValStore[node.`fun`]
                    ?: return Null())[index].type == DoubleType && valNode.type == ValTypes.DOUBLE) ||
                        ((funValStore[node.`fun`]
                            ?: return Null())[index].type == I32Type && valNode.type == ValTypes.INT) ||
                        ((funValStore[node.`fun`]
                            ?: return Null())[index].type == Pointer(I8Type) && valNode.type == ValTypes.STRING))
            ) {
                println("\tERROR: Function argument mismatch.")
                return Null()
            }
        }

        val inst = Call(I32Type, node.`fun`, *args)
        return block.tempValue(inst).reference()
    }

    private fun visit(node: FunDefNode): Value {
        if (funStore.any { it.name == node.`fun` }) {
            println("\tERROR: The function ${node.`fun`} already exists.")
            return Null()
        }

        val names = Array(node.arg.size) { node.arg[it].id }
        val list = List(node.arg.size) {
            when (node.arg[it].type) {
                ValTypes.INT -> I32Type
                ValTypes.DOUBLE -> DoubleType
                ValTypes.STRING -> Pointer(I8Type)
            }
        }
        val funct =
            when (node.returnType) {
                ValTypes.INT -> module.createFunction(node.`fun`, I32Type, list)
                ValTypes.DOUBLE -> module.createFunction(node.`fun`, DoubleType, list)
                ValTypes.STRING -> module.createFunction(node.`fun`, Pointer(I8Type), list)
            }

        funStore += funct
        if (!funValStore.containsKey(node.`fun`)) funValStore[node.`fun`] = ArrayList()
        names.forEachIndexed { index, s ->
            val variable =
                when (node.arg[index].type) {
                    ValTypes.INT -> funct.entryBlock().addVariable(I32Type, s) as LocalVariable
                    ValTypes.DOUBLE -> funct.entryBlock().addVariable(DoubleType, s) as LocalVariable
                    ValTypes.STRING -> funct.entryBlock().addVariable(Pointer(I8Type), s) as LocalVariable
                }
            funct.entryBlock().assignVariable(variable, funct.paramReference(index))
            funValStore[node.`fun`]?.add(variable)
        }

        IRVisitor(funct).visit(node.body)

        return Null()
    }
}
package net.bms.novlangue.tree

import me.tomassetti.kllvm.*
import net.bms.novlangue.*

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

    internal open fun visit(node: Node?): Value = when (node) {
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

    /**
     * Returns a unique label
     *
     * @param title optional custom string to insert into label.
     */
    fun getUniqueID(title: String = ""): String =
        if (title.isEmpty()) "_INTERNAL_${func.name}_${func.tmpIndex()}"
        else "_INTERNAL_${func.name.toUpperCase()}_${title.toUpperCase()}_${func.tmpIndex()}"

    private fun mangleFunName(name: String, vararg types: Type = arrayOf()): String {
        var ret = name
        types.forEach { ret += "_${it.IRCode()}" }
        return ret.toUpperCase()
    }

    internal open fun visit(node: BodyNode): Value {
        node.list.forEach { visit(CodeVisitor().visit(it)) }
        if (node.returnExpr != null)
            block.addInstruction(Return(visit(CodeVisitor().visit(node.returnExpr))))
        else
            block.addInstruction(
                Return(
                    when (func.returnType) {
                        DoubleType -> DoubleConst(0.0, func.returnType)
                        else -> IntConst(0, func.returnType)
                    }
                )
            )

        return Null()
    }

    internal open fun visit(node: MasterNode): Value {
        if (helperFuncs) {
            val printInt = module.createFunction(
                mangleFunName("print", I32Type), I32Type, listOf(I32Type)
            )
            funStore += printInt
            val variableInt = printInt.entryBlock().addVariable(I32Type, "x")
            printInt.entryBlock().assignVariable(variableInt, printInt.paramReference(0))
            funValStore += hashMapOf(printInt to arrayListOf("x" to printInt.paramTypes.first()))
            printInt.addInstruction(
                Printf(
                    printInt.stringConstForContent("%d\n").reference(),
                    printInt.paramReference(0)
                )
            )
            printInt.addInstruction(ReturnInt(0))

            val printDouble = module.createFunction(
                mangleFunName("print", DoubleType), I32Type, listOf(DoubleType)
            )
            funStore += printDouble
            val variableDouble = printDouble.entryBlock().addVariable(DoubleType, "x")
            printDouble.entryBlock().assignVariable(variableDouble, printDouble.paramReference(0))
            funValStore += hashMapOf(printDouble to arrayListOf("x" to printDouble.paramTypes.first()))
            printDouble.addInstruction(
                Printf(
                    printDouble.stringConstForContent("%f\n").reference(),
                    printDouble.paramReference(0)
                )
            )
            printDouble.addInstruction(ReturnInt(0))

//            val printString = module.createFunction(
//                mangleFunName("print", Pointer(I8Type)), I32Type, listOf(Pointer(I8Type))
//            )
//            funStore += printString
//            val variableString = printString.entryBlock().addVariable(Pointer(I8Type), "x")
//            printString.entryBlock().assignVariable(variableString, printString.paramReference(0))
//            funValStore += hashMapOf(printString to arrayListOf("x" to printString.paramTypes.first()))
//            printString.addInstruction(
//                Printf(
//                    printString.stringConstForContent("%s\n").reference(),
//                    printString.paramReference(0)
//                )
//            )
//            printString.addInstruction(ReturnInt(0))
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

    internal open fun visit(node: CompNode): Value = when (node.left.type) {
        ValTypes.DOUBLE -> block.tempValue(FloatComparison(node.type, visit(node.left), visit(node.right))).reference()
        else -> block.tempValue(IntComparison(node.type, visit(node.left), visit(node.right))).reference()
    }

    internal open fun visit(node: AdditionNode): Value = when (node.left.toValNode().type) {
        ValTypes.DOUBLE -> block.tempValue(FloatAddition(visit(node.left), visit(node.right))).reference()
        else -> block.tempValue(IntAddition(visit(node.left), visit(node.right))).reference()
    }

    internal open fun visit(node: SubtractionNode): Value = when (node.left.toValNode().type) {
        ValTypes.DOUBLE -> block.tempValue(FloatSubtraction(visit(node.left), visit(node.right))).reference()
        else -> block.tempValue(IntSubtraction(visit(node.left), visit(node.right))).reference()
    }

    internal open fun visit(node: MultiplicationNode): Value = when (node.left.toValNode().type) {
        ValTypes.DOUBLE -> block.tempValue(FloatMultiplication(visit(node.left), visit(node.right))).reference()
        else -> block.tempValue(IntMultiplication(visit(node.left), visit(node.right))).reference()
    }

    internal open fun visit(node: DivisionNode): Value = when (node.left.toValNode().type) {
        ValTypes.DOUBLE -> block.tempValue(FloatDivision(visit(node.left), visit(node.right))).reference()
        else -> block.tempValue(SignedIntDivision(visit(node.left), visit(node.right))).reference()
    }

    internal open fun visit(node: NegateNode): Value = when (node.innerNode.toValNode().type) {
        ValTypes.DOUBLE -> DoubleConst(-(visit(node.innerNode) as DoubleConst).value)
        else -> IntConst(-(visit(node.innerNode) as IntConst).value, I32Type)
    }

    internal open fun visit(node: NumberNode): Value = when (node.type) {
        ValTypes.DOUBLE -> DoubleConst(node.value)
        else -> IntConst(node.value.toInt(), I32Type)
    }

    internal open fun visit(node: ValNode): Value {
        if (node.id.isEmpty()) return visit(node.value)
        else if (node.value != null && node.isNew) {
            if (valStore.any { it.name == node.id })
                println("\tWARNING: Variable ${node.id} already exists. You should not use `val` here.")
            valStore += when (node.type) {
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
        } else if (func in funValStore && funValStore[func]?.any { it.first == node.id }!!) {
            val index = funValStore[func]?.indexOf(funValStore[func]?.find {
                it.first == node.id
            })
            return index?.let { func.paramReference(it) }!!
        } else if (valStore.any { it.name == node.id })
            return block.tempValue(Load(valStore.find { it.name == node.id }!!.reference())).reference()
        else {
            println("\tERROR: The variable ${node.id} does not exist.")
            return Null()
        }
    }

    internal open fun visit(node: FunCallNode): Value {
        val args = Array(node.args.size) { visit(node.args[it]) }
        val name = mangleFunName(node.`fun`, *args.map { it.type() }.toTypedArray())

        if (!funStore.any { it.name == name }) {
            println("\tERROR: The function ${node.`fun`} does not exist.")

            return Null()
        }
        args.forEachIndexed { index, value ->
            val type = (funValStore[funStore.find { it.name == name }] ?: return Null())[index].second
            if (type != value.type()) {
                println(
                    "\tERROR: Argument ${index + 1} of function ${node.`fun`} " +
                            "should be of type ${typeNameMap[type]?.type}, " +
                            "but supplied argument is of type ${typeNameMap[value.type()]?.type}."
                )
                return Null()
            }
        }

        val inst = Call(funStore.find { it.name == name }!!.returnType, name, *args)
        return block.tempValue(inst).reference()
    }

    internal open fun visit(node: FunDefNode): Value {
        val names = Array(node.arg.size) { node.arg[it].id }
        val list = List(node.arg.size) {
            when (node.arg[it].type) {
                ValTypes.INT -> I32Type
                ValTypes.DOUBLE -> DoubleType
                ValTypes.STRING -> Pointer(I8Type)
            }
        }
        val name = mangleFunName(node.`fun`, *list.toTypedArray())

        if (funStore.any { it.name == name }) {
            println("\tERROR: The function ${node.`fun`} already exists.")
            return Null()
        }
        val funct =
            when (node.returnType) {
                ValTypes.INT -> module.createFunction(name, I32Type, list)
                ValTypes.DOUBLE -> module.createFunction(name, DoubleType, list)
                ValTypes.STRING -> module.createFunction(name, Pointer(I8Type), list)
            }

        funStore += funct
        if (!funValStore.containsKey(funct)) funValStore[funct] = ArrayList()
        names.forEachIndexed { index, s ->
            val variable =
                when (node.arg[index].type) {
                    ValTypes.INT -> funct.entryBlock().addVariable(I32Type, s)
                    ValTypes.DOUBLE -> funct.entryBlock().addVariable(DoubleType, s)
                    ValTypes.STRING -> funct.entryBlock().addVariable(Pointer(I8Type), s)
                }
            funct.entryBlock().assignVariable(variable, funct.paramReference(index))
            funValStore[funct]?.add(s to list[index])
        }

        IRVisitor(funct).visit(node.body)

        return Null()
    }
}
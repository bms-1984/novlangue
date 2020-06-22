package net.bms.orwell.tree

import me.tomassetti.kllvm.*
import net.bms.orwell.*
import net.bms.orwell.llvm.FloatComparison

class IRVisitor(private val func: FunctionBuilder = mainFun, private var block: BlockBuilder = func.entryBlock(),
                private val finally: Boolean = false, private val exit: BlockBuilder? = null,
                private val helperFuncs: Boolean = (func == mainFun && finally)) {
    fun visit(node: Node?): Value = when(node) {
        is AdditionNode -> visit(node)
        is SubtractionNode -> visit(node)
        is MultiplicationNode -> visit(node)
        is DivisionNode -> visit(node)
        is NegateNode -> visit(node)
        is NumberNode -> visit(node)
        is ValNode -> visit(node)
        is FunCallNode -> visit(node)
        is FunDefNode -> visit(node)
        is IfNode -> visit(node)
        is CompNode -> visit(node)
        is BodyNode -> visit(node)
        is MasterNode -> visit(node)
        else -> Null(VoidType)
    }
    private fun visit(node: MasterNode): Value {
        if (helperFuncs) {
            val print = module.createFunction("print", FloatType, listOf(FloatType))
            funStore[print.name] = print
            valStoreFun[print.name] = hashMapOf("s" to print.paramReference(0))
            val num = print.tempValue(ConversionFloatToSignedInt(print.paramReference(0), I8Type))
            print.addInstruction(Printf(print.stringConstForContent("%d\n").reference(), num.reference()))
            print.addInstruction(Return(FloatConst(0F, FloatType)))
        }
        node.prog.forEach { visit(it) }
        if (finally)
            block.addInstruction(ReturnInt(0))
        return Null(VoidType)
    }
    private fun visit(node: BodyNode): Value {
        node.list.forEach{ IRVisitor(func, block).visit(OrwellVisitor().visit(it)) }
        block.addInstruction(JumpInstruction(exit!!.label()))
        return Null(VoidType)
    }
    private fun visit(node: CompNode): Value = block.tempValue(FloatComparison(node.type!!, visit(node.left), visit(node.right))).reference()
    private fun visit(node: IfNode): Value {
        val comp = visit(node.comp)
        val curBlock = block
        val exit: BlockBuilder
        if (node.isTop) {
            val post = func.createBlock("post${func.tmpIndex()}")
            block = post
            exit = block
        }
        else
            exit = this.exit!!
        val yes = func.createBlock("true${func.tmpIndex()}")
        IRVisitor(func, yes, exit = exit).visit(node.`if`)
        val no = func.createBlock("false${func.tmpIndex()}")
        if (node.elif.isEmpty())
            IRVisitor(func, no, exit = exit).visit(node.`else`)
        else
            node.elif[0].also {
                it?.elif = ArrayList(node.elif.filterIndexed { index, _ -> index != 0})
                it?.`else` = node.`else`
                IRVisitor(func, no, exit = exit).visit(it!!)
            }
        curBlock.addInstruction(IfInstruction(comp, yes, no))
        return Null(VoidType)
    }
    private fun visit(node: AdditionNode): Value {
        val left = visit(node.left)
        val right = visit(node.right)
        val inst = FloatAddition(left, right)
        return block.tempValue(inst).reference()
    }
    private fun visit(node: SubtractionNode): Value {
        val left = visit(node.left)
        val right = visit(node.right)
        val inst = FloatSubtraction(left, right)
        return block.tempValue(inst).reference()
    }
    private fun visit(node: MultiplicationNode): Value {
        val left = visit(node.left)
        val right = visit(node.right)
        val inst = FloatMultiplication(left, right)
        return block.tempValue(inst).reference()
    }
    private fun visit(node: DivisionNode): Value {
        val left = visit(node.left)
        val right = visit(node.right)
        val inst = FloatDivision(left, right)
        return block.tempValue(inst).reference()
    }
    private fun visit(node: NegateNode): Value = FloatConst(-(visit(node.innerNode) as FloatConst).value, FloatType)
    private fun visit(node: NumberNode): Value = FloatConst(node.value.toFloat(), FloatType)
    private fun visit(node: ValNode): Value {
        if (node.id.isEmpty())
            return visit(node.value)
        else if (node.value != null && node.isNew) {
            if (valStore.containsKey(node.id))
                println("\tWARNING: Variable ${node.id} already exists. You should not use `val` here.")
            valStore[node.id] = visit(node.value)
            return valStore[node.id]!!
        }
        else if (node.value != null && !node.isNew) {
            if (!valStore.containsKey(node.id)) {
                println("\tERROR: The variable ${node.id} does not exist. Try using `val`.")
                return Null(VoidType)
            }
            valStore[node.id] = visit(node.value)
            return valStore[node.id]!!
        }
        else if (func.name in valStoreFun && node.id in valStoreFun[func.name]!!)
            return valStoreFun[func.name]?.get(node.id)!!
        else if (node.id in valStore)
            return valStore[node.id]!!
        else {
            println("\tERROR: The variable ${node.id} does not exist.")
            return Null(VoidType)
        }
    }
    private fun visit(node: FunCallNode): Value {
        val args = Array(node.args.size) {
            visit(node.args[it])
        }
        if(!funStore.containsKey(node.`fun`))
        {
            println("\tERROR: The function ${node.`fun`} does not exist.")
            return Null(VoidType)
        }
        if (node.args.size != valStoreFun[node.`fun`]?.size) {
            println("\tERROR: The function ${node.`fun`} takes ${valStoreFun[node.`fun`]?.size} arguments, but you supplied ${node.args.size}.")
            return Null(VoidType)
        }
        val inst = Call(FloatType, node.`fun`, *args)
        return block.tempValue(inst).reference()
    }
    private fun visit(node: FunDefNode): Value {
        if (funStore.containsKey(node.`fun`)) {
            println("\tERROR: The function ${node.`fun`} already exists.")
            return Null(VoidType)
        }
        val names = Array(node.arg.size) {
            node.arg[it].id
        }
        val funct = module.createFunction(node.`fun`, FloatType, List(node.arg.size) { FloatType })
        funStore[node.`fun`] = funct
        if (!valStoreFun.containsKey(node.`fun`))
            valStoreFun[node.`fun`] = HashMap()
        names.forEachIndexed { index, s ->
            val v = funct.paramReference(index)
            valStoreFun[node.`fun`]?.set(s, v)
        }
        node.body.forEach {
            IRVisitor(funct).visit(OrwellVisitor().visit(it))
        }
        val ret = IRVisitor(funct).visit(OrwellVisitor().visit(node.returnExpr))
        //val con = funct.tempValue(ConversionFloatToSignedInt(ret, I8Type))
        //funct.addInstruction(Printf(funct.stringConstForContent("${node.`fun`} result: %d\n").reference(), con.reference()))
        funct.addInstruction(Return(ret))
        return ret
    }
}
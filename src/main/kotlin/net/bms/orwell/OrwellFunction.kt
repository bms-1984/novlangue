package net.bms.orwell

import net.bms.orwell.tree.*

class OrwellFunction(private val args: ArrayList<ValNode>, private val expr: OrwellParser.EContext) {
    fun getArgCount() = args.size
    fun getArgN(n: Int) = try {
        args[n]
    }
    catch (e: IndexOutOfBoundsException)
    {
        null
    }

    private fun areArgsApplicable(newArgs: ArrayList<ValNode>) = newArgs.size == args.size

    fun runFunction(newArgs: ArrayList<ValNode>): Node? =
        if (!areArgsApplicable(newArgs))
            null
        else HashMap<String, ValNode>().let {
            for (i in 0 until args.size) {
                it[args[i].id] = newArgs[i].value!!.toValNode()
            }
            OrwellFunctionVisitor(it).visit(expr)
        }
}
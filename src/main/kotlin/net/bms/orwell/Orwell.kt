package net.bms.orwell

import OrwellLexer
import OrwellParser
import me.tomassetti.kllvm.*
import net.bms.orwell.tree.CodeVisitor
import net.bms.orwell.tree.IRVisitor
import net.bms.orwell.tree.REPLVisitor
import net.bms.orwell.tree.ValTypes
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.random.Random
import kotlin.random.nextUInt

/**
 * Variable storage
 */
val valStore: ArrayList<LocalVariable> = ArrayList()

/**
 * Function parameter storage
 */
val funValStore: HashMap<FunctionBuilder, ArrayList<Pair<String, Type>>> = HashMap()

/**
 * Function storage
 */
val funStore: ArrayList<FunctionBuilder> = ArrayList()

/**
 * Top-level IR representation
 */
val module: ModuleBuilder = ModuleBuilder()

/**
 * IR main function
 */
lateinit var mainFun: FunctionBuilder

internal val typeNameMap: HashMap<Type, ValTypes> = hashMapOf(
    I32Type to ValTypes.INT,
    DoubleType to ValTypes.DOUBLE,
    Pointer(I8Type) to ValTypes.STRING,
)

/**
 * Main function
 */
@ExperimentalUnsignedTypes
fun main(args: Array<String>) {
    val properties = Properties()
    properties.load(object {}.javaClass.classLoader.getResourceAsStream("orwell.properties"))
    println("Sutter's Orwell Compiler v${properties.getProperty("version")}")

    mainFun = module.createMainFunction()

    if (args.isNotEmpty()) {
        if (args.contains("-noMain"))
            mainFun = module.createFunction("__INTERNAL_${Random.nextUInt()}_", I32Type, listOf())
        val helpers = !args.contains("-noStd")

        try {
            module.addDeclaration(FunctionDeclaration("printf", I32Type, listOf(Pointer(I8Type)), varargs = true))
            FileReader(args[0]).also { reader -> runOrwell(reader, true, helpers) }.close()
            FileWriter(File(args[0]).nameWithoutExtension.plus(".ll")).apply { write(module.IRCode().trim()) }.close()
            println("Complete.")
            return
        } catch (e: FileNotFoundException) {
            println("ERROR: $e; dropping into a REPL...")
        }
        runREPL(helpers)
    } else {
        runREPL()
    }
    return
}

@Suppress("SameParameterValue")
private fun runOrwell(reader: Reader, compile: Boolean = false, helpers: Boolean = true) {
    val tree = CodeVisitor().visit(OrwellParser(CommonTokenStream(OrwellLexer(CharStreams.fromReader(reader)))).top())
    if (compile) IRVisitor(mainFun, finally = true, helperFuncs = helpers).visit(tree)
    else REPLVisitor(mainFun, helperFuncs = helpers).visit(tree)
}

private fun listBindings() {
    if (funStore.isEmpty()) {
        println("No functions have been bound.")
    } else {
        println("Functions:")
        for (f in funStore) {
            var params = ""
            f.paramTypes.forEachIndexed { index, type ->
                params += if (index == 0) type.IRCode() else ", ${type.IRCode()}"
            }
            println("${f.name}(${params}): ${f.returnType.IRCode()}")
        }
    }
}

private fun runREPL(helpers: Boolean = true) {
    println("WARNING: REPL MODE IS CURRENTLY INCOMPLETE")
    println("For assistance, use ;help.\n")
    while (true) {
        print("orwell> ")
        val line = readLine() ?: return
        if (line.isBlank()) continue
        if (line[0] == ';') {
            if (line.toLowerCase().contains("help")) {
                getHelp()
                continue
            }
            if (line.toLowerCase().contains("quit")) {
                return
            }
            if (line.toLowerCase().contains("list")) {
                listBindings()
                continue
            }
        } else runOrwell(StringReader(line), helpers = helpers)
    }
}


private fun getHelp() {
    println("Commands:")
    println(";help -- prints this help dialogue")
    println(";quit -- exits the REPL")
    println(";list -- lists current bindings")
}

package net.bms.orwell

import OrwellLexer
import OrwellParser
import me.tomassetti.kllvm.*
import net.bms.orwell.tree.IRVisitor
import net.bms.orwell.tree.OrwellVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.random.Random

/**
 * Variable storage
 */
val valStore: ArrayList<LocalVariable> = ArrayList()

/**
 * Function parameter storage
 */
val funValStore: HashMap<String, ArrayList<LocalVariable>> = HashMap()

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

/**
 * Main function
 */
fun main(args: Array<String>) {
    val properties = Properties()
    properties.load(object {}.javaClass.classLoader.getResourceAsStream("orwell.properties"))
    println("Sutter's Orwell Compiler v${properties.getProperty("version")}")

    if (args.isNotEmpty()) {
        mainFun = if (!args.contains("-noMain"))
            module.createMainFunction()
        else
            module.createFunction("__INTERNAL_${Random.nextInt()}_", I32Type, listOf())
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
    } else println("ERROR: REPL Mode is not currently available.")

    return
/* TODO:: reimplement REPL
    println("For assistance, use ;help.\n")
    while(true) {
        print("orwell> ")
        val line = readLine() ?: break
        if (line.isBlank()) continue
        if (line[0] == ';') {
            if (line.toLowerCase().contains("help"))
            {
                getHelp()
                continue
            }
            if (line.toLowerCase().contains("quit")) {
                break
            }
            if (line.toLowerCase().contains("list"))
            {
                listBindings()
                continue
            }
        }
        runOrwell(line)
    }
*/
}

@Suppress("SameParameterValue")
private fun runOrwell(reader: Reader, compile: Boolean = false, helpers: Boolean = false) {
    val tree = OrwellVisitor().visit(OrwellParser(CommonTokenStream(OrwellLexer(CharStreams.fromReader(reader)))).top())
    if (compile) IRVisitor(mainFun, finally = true, helperFuncs = helpers).visit(tree)
}

/* TODO: fix listBindings for new variable and function storage
private fun listBindings() {
    if (valStore.isEmpty()) {
        println("No variables have been bound.\n")
    }
    else {
        println("Variables:")
        for ((k, v) in valStore)
            println("$k -> ${(v as FloatConst).value}")
    }
    if (funStore.isEmpty()) {
        println("No functions have been bound.")
    }
    else {
        println("Functions:")
        for (k in funStore.keys) {
            println(k)
        }
    }
}
*/

/*
private fun getHelp() {
    println("Commands:")
    println(";help -- prints this help dialogue")
    println(";quit -- exits the REPL")
    println(";list -- lists current bindings")
}
*/

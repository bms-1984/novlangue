package net.bms.orwell

import OrwellLexer
import OrwellParser
import me.tomassetti.kllvm.*
import net.bms.orwell.tree.IRVisitor
import net.bms.orwell.tree.Node
import net.bms.orwell.tree.OrwellVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.util.*
import kotlin.collections.HashMap

val valStore = HashMap<String, Value>()
val valStoreFun = HashMap<String, HashMap<String, Value>>()
val funStore = HashMap<String, FunctionBuilder>()
val module = ModuleBuilder()
val mainFun = module.createMainFunction()

fun main(args: Array<String>) {
    val properties = Properties()
    properties.load(object {}.javaClass.classLoader.getResourceAsStream("orwell.properties"))
    println("Sutter's Orwell Compiler v${properties.getProperty("version")}")
    if (args.isNotEmpty()) {
        try {
            module.addDeclaration(FunctionDeclaration("printf", I32Type, listOf(Pointer(I8Type)), varargs = true))
            FileReader(args[0]).let { reader ->
                reader.forEachLine {
                    println(it)
                    runOrwell(it, true)
                }
                reader.close()
            }
//            println("\nFinal Bindings:")
//            listBindings()
            mainFun.addInstruction(ReturnInt(0))
            FileWriter(File(args[0]).nameWithoutExtension.plus(".ll")).let {
                it.write(module.IRCode())
                it.close()
            }
            return
        }
        catch (e: FileNotFoundException) {
            println("ERROR: $e; dropping into a REPL...")
        }
    }
    else {
        println("ERROR: REPL Mode is not currently available.")
        return
    }
//    println("For assistance, use ;help.\n")
//    while(true) {
//        print("orwell> ")
//        val line = readLine() ?: break
//        if (line.isBlank()) continue
//        if (line[0] == ';') {
//            if (line.toLowerCase().contains("help"))
//            {
//                getHelp()
//                continue
//            }
//            if (line.toLowerCase().contains("quit")) {
//                break
//            }
//            if (line.toLowerCase().contains("list"))
//            {
//                listBindings()
//                continue
//            }
//        }
//        runOrwell(line)
//    }
}

private fun runOrwell(line: String, compile: Boolean = false) {
    val lexer = OrwellLexer(CharStreams.fromString(line))
    val tokenStream = CommonTokenStream(lexer)
    val parser = OrwellParser(tokenStream)
    val tree = parser.top()
    val visitor = OrwellVisitor()
    val astree: Node?
    if (tree is OrwellParser.ExprContext) {
        astree = visitor.visitExpr(tree)
        if (compile)
            IRVisitor(mainFun).visit(astree)
    }
    else if (tree is OrwellParser.StatContext) {
        astree = visitor.visitStat(tree)
        if (compile)
            IRVisitor(mainFun).visit(astree)
    }
}

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

private fun getHelp() {
    println("Commands:")
    println(";help -- prints this help dialogue")
    println(";quit -- exits the REPL")
    println(";list -- lists current bindings")
}
package net.bms.orwell

import OrwellLexer
import OrwellParser
import net.bms.orwell.tree.ASTVisitor
import net.bms.orwell.tree.Node
import net.bms.orwell.tree.OrwellVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.util.*
import kotlin.collections.HashMap

val valStore = HashMap<String, Any?>()
val funStore = HashMap<String, OrwellFunction>()

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        try {
            FileReader(args[0]).forEachLine {
                println(it)
                runOrwell(it)
            }
            println("\nFinal Bindings:")
            listBindings()
            return
        }
        catch (e: FileNotFoundException) {
            println("ERROR: $e; dropping into a REPL...\n")
        }
    }
    val properties = Properties()
    properties.load(object {}.javaClass.classLoader.getResourceAsStream("orwell.properties"))
    println("Sutter's IMPlementation (SIMP) Orwell Interpreter v${properties.getProperty("version")} REPL")
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
}

private fun runOrwell(line: String) {
    val lexer = OrwellLexer(CharStreams.fromString(line))
    val tokenStream = CommonTokenStream(lexer)
    val parser = OrwellParser(tokenStream)
    val tree = parser.top()
    val visitor = OrwellVisitor()
    val astree: Node?
    if (tree is OrwellParser.ExprContext) {
        astree = visitor.visitExpr(tree)
        println("\t$line -> ${ASTVisitor.visit(astree)}")
    }
    else if (tree is OrwellParser.StatContext) {
        astree = visitor.visitStat(tree)
        ASTVisitor.visit(astree)
    }
}

private fun listBindings() {
    if (valStore.isEmpty()) {
        println("No variables have been bound.\n")
    }
    else {
        println("Variables:")
        for ((k, v) in valStore)
            println("$k -> $v")
    }
    if (funStore.isEmpty()) {
        println("No functions have been bound.")
    }
    else {
        println("Functions:")
        for ((k, f) in funStore) {
            print("$k(")
            for (a in 0 until f.getArgCount()) {
                if (a == f.getArgCount() - 1)
                    print("${f.getArgN(a)?.id}")
                else
                    print("${f.getArgN(a)?.id}, ")
            }
            println(")")
        }
    }
}

private fun getHelp() {
    println("Commands:")
    println(";help -- prints this help dialogue")
    println(";quit -- exits the REPL")
    println(";list -- lists current bindings")
}
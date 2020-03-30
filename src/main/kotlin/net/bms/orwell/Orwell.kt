package net.bms.orwell

import OrwellLexer
import OrwellParser
import net.bms.orwell.tree.ASTVisitor
import net.bms.orwell.tree.Node
import net.bms.orwell.tree.OrwellVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.util.*
import kotlin.collections.HashMap

val valStore = HashMap<String, Any?>()
val funStore = HashMap<String, OrwellFunction>()

fun main() {
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
                println("Commands:")
                println(";help -- prints this help dialogue")
                println(";quit -- exits the REPL")
                println(";list -- lists current bindings")
                continue
            }
            if (line.toLowerCase().contains("quit")) {
                break
            }
            if (line.toLowerCase().contains("list"))
            {
                if (valStore.isEmpty()) {
                    println("No variables have been bound.\n")
                }
                else {
                    println("Variables:")
                    for (k in valStore.keys)
                        println("$k -> ${valStore[k]}")
                    println()
                }
                if (funStore.isEmpty()) {
                    println("No functions have been bound.")
                }
                else {
                    println("Functions:")
                    for (k in funStore.keys) {
                        print("$k(")
                        val f = funStore[k]
                        for (a in 0 until f!!.getArgCount()) {
                            if (a == f.getArgCount() - 1)
                                print("${f.getArgN(a)?.id}")
                            else
                                print("${f.getArgN(a)?.id}, ")
                        }
                        println(")")
                    }
                }
                continue
            }
        }
        val lexer = OrwellLexer(CharStreams.fromString(line))
        val tokenStream = CommonTokenStream(lexer)
        val parser = OrwellParser(tokenStream)
        val tree = parser.top()
        val visitor = OrwellVisitor()
        var astree: Node?

        if (tree is OrwellParser.ExprContext) {
            astree = visitor.visitExpr(tree)
            println("\t$line -> ${ASTVisitor.visit(astree)}")
        }
        else if (tree is OrwellParser.StatContext) {
            astree = visitor.visitStat(tree)
            ASTVisitor.visit(astree)
        }

    }
}
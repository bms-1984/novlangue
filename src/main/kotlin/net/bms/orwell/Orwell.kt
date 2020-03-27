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

val valStore = HashMap<String, Pair<Any?, String?>>()

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
                continue
            }
            if (line.toLowerCase().contains("quit")) {
                break
            }
        }
        val lexer = OrwellLexer(CharStreams.fromString(line))
        val tokenStream = CommonTokenStream(lexer)
        val parser = OrwellParser(tokenStream)
        val tree = parser.top()
        var astree: Node?

        if (tree is OrwellParser.ExprContext) {
            astree = OrwellVisitor.visitExpr(tree)
            println("\t$line -> ${ASTVisitor.visit(astree)}")
        }
        else if (tree is OrwellParser.StatContext) {
            astree = OrwellVisitor.visitStat(tree)
            ASTVisitor.visit(astree)
        }

    }
}
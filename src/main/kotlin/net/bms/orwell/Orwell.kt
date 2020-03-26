package net.bms.orwell

import OrwellLexer
import OrwellParser
import net.bms.orwell.tree.AST
import net.bms.orwell.tree.ASTValue
import net.bms.orwell.tree.OrwellListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker
import java.util.*

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
//        val lexer = OrwellLexer(CharStreams.fromString(line))
//        val tokenStream = CommonTokenStream(lexer)
//        val parser = OrwellParser(tokenStream)
//        val ast = AST()
//        val listener = OrwellListener(ast)
        continue
    }
}

/**
 * Prints every [ASTValue] of an [AST] and its descendants, albeit confusingly
 *
 * @author Ben M. Sutter
 * @since 0.1.0
 * @param[ast] the tree that should be printed
 */
fun printAST(ast: AST) {
    println("NODE\nvalue: ${ast.value.getValue()}\nline: ${ast.value.getLine()}\n")
    ast.children.forEach { printAST(it) }
}
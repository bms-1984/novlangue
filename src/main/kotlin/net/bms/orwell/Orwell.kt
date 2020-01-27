package net.bms.orwell

import OrwellLexer
import OrwellParser
import net.bms.orwell.tree.AST
import net.bms.orwell.tree.OrwellListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker

private val ast = AST()
private val lexer = OrwellLexer(CharStreams.fromString("1 + 1"))
private val tokenStream = CommonTokenStream(lexer)
private val parser = OrwellParser(tokenStream)
private val listener = OrwellListener(ast)

fun main() {
    ParseTreeWalker.DEFAULT.walk(listener, parser.expr())
    printAST(ast)
}

private fun printAST(ast: AST) {
    println("NODE\nvalue: ${ast.value.getValue()}\nline: ${ast.value.getLine()}\n")
    ast.children.forEach { printAST(it) }
}
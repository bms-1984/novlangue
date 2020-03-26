package net.bms.orwell.tree

import OrwellBaseListener
import OrwellParser
import org.antlr.v4.runtime.RuleContext
import org.antlr.v4.runtime.tree.TerminalNodeImpl
//TODO: implement this -- cleanly this time
class OrwellListener(private val ast: AST): OrwellBaseListener() {
    override fun enterExpr(ctx: OrwellParser.ExprContext?) {}

    override fun exitExpr(ctx: OrwellParser.ExprContext?) {}
}
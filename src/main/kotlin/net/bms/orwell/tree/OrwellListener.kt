package net.bms.orwell.tree

import OrwellBaseListener
import OrwellParser
import org.antlr.v4.runtime.RuleContext
import org.antlr.v4.runtime.tree.TerminalNodeImpl

class OrwellListener(private val ast: AST): OrwellBaseListener() {
    override fun enterExpr(ctx: OrwellParser.ExprContext?) {
        if (ctx != null && ast.value == SpecialValues.NONE) {
            runExpr(ctx.payload, ast)
        }
    }

    private fun runExpr(payload: Any, tree: AST)
    {
        if (payload is TerminalNodeImpl) {
            val token = payload.symbol
            if (token.type != OrwellParser.PAREN_L &&
                token.type != OrwellParser.PAREN_R &&
                token.type != OrwellParser.EOF) {
                val new = AST()
                new.value = ASTValue(token.text, token.line)
                tree.addChild(new)
            }
        }
        else if (payload is RuleContext) {
            var newTree = tree
            if (tree.value == SpecialValues.NONE) {
                tree.value = SpecialValues.EXPR
            }
            else {
                val new = AST()
                new.value = SpecialValues.EXPR
                newTree = tree.addChild(new)
            }
            for (i in 0 until payload.childCount){
                runExpr(payload.getChild(i), newTree)
            }
        }
    }

    override fun exitExpr(ctx: OrwellParser.ExprContext?) {}
}
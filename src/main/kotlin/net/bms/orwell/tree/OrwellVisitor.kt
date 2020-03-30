package net.bms.orwell.tree

import OrwellBaseVisitor
import OrwellLexer
import net.bms.orwell.valStore

open class OrwellVisitor: OrwellBaseVisitor<Node>() {
    override fun visitExpr(ctx: OrwellParser.ExprContext?): Node? =
        if (ctx != null) {
            visit(ctx.e())
        } else null

    override fun visitStat(ctx: OrwellParser.StatContext?): Node? =
        if (ctx != null) {
            visit(ctx.s())
        } else null

    override fun visitVal_def(ctx: OrwellParser.Val_defContext?): Node? =
        if (ctx != null) {
            with(ValNode()) {
                (visit(ctx.name) as ValNode).let {
                    id = it.id
                    value = visit(ctx.`val`)
                    this
                }
            }
        } else null

    override fun visitVal_dec(ctx: OrwellParser.Val_decContext?): Node? =
        if (ctx != null) {
            with(ValNode()) {
                id = ctx.name.text
                value = null
                this
            }
        } else null

    override fun visitAssignment(ctx: OrwellParser.AssignmentContext?): Node? =
        if (ctx != null) {
            with(ValNode()) {
                id = ctx.name.text
                value = visit(ctx.`val`)
                this
            }
        } else null

    override fun visitNumber(ctx: OrwellParser.NumberContext?): Node? =
        if (ctx != null) {
            with(NumberNode()){
                value = ctx.`val`.text.toDouble()
                this
            }
        } else null

    override fun visitNegExpr(ctx: OrwellParser.NegExprContext?): Node? =
        if (ctx != null) {
            with(NegateNode()) {
                innerNode = visit(ctx.e())
                this
            }
        } else null

    override fun visitBinExpr(ctx: OrwellParser.BinExprContext?): Node? =
        if (ctx != null) {
            with(when (ctx.op.type) {
                OrwellLexer.OP_ADD -> {
                    AdditionNode()
                }
                OrwellLexer.OP_SUB -> {
                    SubtractionNode()
                }
                OrwellLexer.OP_DIV -> {
                    DivisionNode()
                }
                OrwellLexer.OP_MUL -> {
                    MultiplicationNode()
                }
                else -> null
            }) {
                if (this != null) {
                    left = visit(ctx.left)
                    right = visit(ctx.right)
                    this
                } else null
            }
        } else null

    override fun visitParenExpr(ctx: OrwellParser.ParenExprContext?): Node? =
        if (ctx != null) {
            visit(ctx.e())
        } else null

    override fun visitIdentifier(ctx: OrwellParser.IdentifierContext?): Node? =
        if(ctx != null) {
            with(NumberNode()) {
                if (ctx.name.text in valStore)  {
                    value = valStore[ctx.name.text] as Double
                    this
                }
                else {
                    println("\tERROR: The variable ${ctx.name.text} does not exist.")
                    value = Double.NaN
                    this
                }
            }
        } else null

    override fun visitFun_def(ctx: OrwellParser.Fun_defContext?): Node? =
        if (ctx != null) {
            with(FunDefNode()) {
                for (i in 0 until ctx.names.size) {
                    ValNode().let {
                        it.value = null
                        it.id = ctx.names[i].text
                        this.arg.add(it)
                    }
                }
                `fun` = ctx.name.text
                expr = ctx.e()
                this
            }
        } else null

    override fun visitFun_call(ctx: OrwellParser.Fun_callContext?): Node? =
        if (ctx != null) {
            with(FunCallNode()) {
                for (e in ctx.args) {
                    args.add(visit(e).toValNode())
                }
                `fun` = ctx.name.text
                this
            }
        } else null
}
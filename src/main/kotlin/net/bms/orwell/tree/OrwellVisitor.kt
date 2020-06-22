package net.bms.orwell.tree

import OrwellBaseVisitor
import OrwellLexer
import net.bms.orwell.llvm.FloatComparisonType

open class OrwellVisitor: OrwellBaseVisitor<Node>() {
    override fun visitTop(ctx: OrwellParser.TopContext?): Node =
        MasterNode().apply {
            ctx?.children?.forEach {
                prog += visit(it)
            }
        }

    override fun visitValDef(ctx: OrwellParser.ValDefContext?): Node? =
        if (ctx != null) {
            ValNode().apply {
                (visit(ctx.val_def().name) as ValNode ).let {
                    id = it.id
                    value = visit(ctx.val_def().`val`)
                    isNew = true
                }
            }
        } else null

    override fun visitVal_dec(ctx: OrwellParser.Val_decContext?): Node? =
        if (ctx != null) {
            ValNode().apply {
                id = ctx.name.text
                value = null
                isNew = true
            }
        } else null

    override fun visitValDec(ctx: OrwellParser.ValDecContext?): Node? =
        if (ctx != null) {
            ValNode().apply {
                id = ctx.val_dec().name.text
                value = null
                isNew = true
            }
        } else null

    override fun visitAssign(ctx: OrwellParser.AssignContext?): Node? =
        if (ctx != null) {
            ValNode().apply {
                id = ctx.assignment().name.text
                value = visit(ctx.assignment().`val`)
            }
        } else null

    override fun visitNumber(ctx: OrwellParser.NumberContext?): Node? =
        if (ctx != null) { NumberNode().apply{ value = ctx.`val`.text.toDouble() } } else null

    override fun visitNegExpr(ctx: OrwellParser.NegExprContext?): Node? =
        if (ctx != null) { NegateNode().apply { innerNode = visit(ctx.e()) } } else null

    override fun visitBinExpr(ctx: OrwellParser.BinExprContext?): Node? =
        if (ctx != null) {
            when (ctx.op.type) {
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
            }?.apply {
                left = visit(ctx.left)
                right = visit(ctx.right)
            }
        } else null

    override fun visitParenExpr(ctx: OrwellParser.ParenExprContext?): Node? =
        if (ctx != null) {
            visit(ctx.e())
        } else null

    override fun visitIdentifier(ctx: OrwellParser.IdentifierContext?): Node? =
        if(ctx != null) { ValNode().apply { id = ctx.name.text } } else null

    override fun visitFunDef(ctx: OrwellParser.FunDefContext?): Node? =
        if (ctx != null) {
            FunDefNode().apply {
                for (i in 0 until ctx.fun_def().names.size) {
                    ValNode().let {
                        it.value = null
                        it.id = ctx.fun_def().names[i].text
                        arg.add(it)
                    }
                }
                `fun` = ctx.fun_def().name.text
                ctx.fun_def().top().forEach {
                    body += it
                }
                returnExpr = ctx.fun_def().e()
            }
        } else null

    override fun visitFun_call(ctx: OrwellParser.Fun_callContext?): Node? =
        if (ctx != null) {
            FunCallNode().apply {
                for (e in ctx.args)
                    args.add(visit(e).toValNode())
                `fun` = ctx.name.text
            }
        } else null

    override fun visitFunCall(ctx: OrwellParser.FunCallContext?): Node? =
        if (ctx != null) {
            FunCallNode().apply {
                for (e in ctx.fun_call().args)
                    args.add(visit(e).toValNode())
                `fun` = ctx.fun_call().name.text
            }
        } else null

    override fun visitComparison(ctx: OrwellParser.ComparisonContext?): Node =
        CompNode().apply {
            left = visit(ctx?.left).toValNode()
            right = visit(ctx?.right).toValNode()
            type = when(ctx?.op?.text) {
                "==" -> FloatComparisonType.Equal
                "!=" -> FloatComparisonType.NotEqual
                ">" -> FloatComparisonType.GreaterThan
                "<" -> FloatComparisonType.LessThan
                ">=" -> FloatComparisonType.GreaterThanOrEqual
                "<=" -> FloatComparisonType.LessThanOrEqual
                else -> FloatComparisonType.Equal
            }
        }

    override fun visitIfBlock(ctx: OrwellParser.IfBlockContext?): Node =
        IfNode().apply {
            `if` = visit(ctx?.if_block()?.if_statement()) as BodyNode
            comp = visit(ctx?.if_block()?.if_statement()?.comparison()) as CompNode?
            if (ctx?.if_block()?.else_statement() != null)
                `else` = visit(ctx.if_block().else_statement()) as BodyNode?
            ctx?.if_block()?.else_if_statement()?.forEach { elif.add(visit(it) as IfNode?) }
            isTop = true
        }

    override fun visitIf_statement(ctx: OrwellParser.If_statementContext?): Node =
        BodyNode().apply {
            ctx?.top()?.forEach {
                list.add(it)
            }
        }

    override fun visitElse_if_statement(ctx: OrwellParser.Else_if_statementContext?): Node =
        IfNode().apply {
            `if` = visit(ctx?.if_statement()) as BodyNode?
            comp = visit(ctx?.if_statement()?.comparison()) as CompNode?
        }

    override fun visitElse_statement(ctx: OrwellParser.Else_statementContext?): Node =
        BodyNode().apply {
            ctx?.top()?.forEach {
                list.add(it)
            }
        }
}
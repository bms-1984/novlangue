package net.bms.orwell.tree

import OrwellBaseVisitor
import OrwellLexer
import OrwellParser
import me.tomassetti.kllvm.ComparisonType

/**
 * Creates an AST from a the ANTLR parse tree
 *
 * @author Ben M. Sutter
 * @since 0.1.0
 */
open class CodeVisitor : OrwellBaseVisitor<Node>() {
    /**
     * Whole program
     */
    override fun visitTop(ctx: OrwellParser.TopContext?): Node =
        MasterNode().apply { ctx?.children?.forEach { prog += visit(it) } }

    /**
     * Variable definition
     */
    override fun visitValDef(ctx: OrwellParser.ValDefContext?): Node =
        ValNode().apply {
            (visit(ctx?.val_def()?.name) as ValNode).let {
                id = it.id
                value = visit(ctx?.val_def()?.`val`)
                isNew = true
                type =
                    if (ctx?.val_def()?.val_dec()?.type == null) ValTypes.INT
                    else ValTypes.valueOf(ctx.val_def()?.val_dec()?.type?.text?.toUpperCase() ?: return@let)
            }
        }

    /**
     * Variable declaration inside definition
     */
    override fun visitVal_dec(ctx: OrwellParser.Val_decContext?): Node =
        ValNode().apply {
            id = (ctx?.name?.text ?: return@apply)
            value = null
            isNew = true
            type =
                if (ctx.type == null) ValTypes.INT
                else ValTypes.valueOf(ctx.type.text.toUpperCase())
        }

    /**
     * Variable declaration
     */
    override fun visitValDec(ctx: OrwellParser.ValDecContext?): Node =
        ValNode().apply {
            id = (ctx?.val_dec()?.name?.text ?: return@apply)
            value = null
            isNew = true
            type =
                if (ctx.val_dec().type == null) ValTypes.INT
                else ValTypes.valueOf(ctx.val_dec().type.text.toUpperCase())
        }

    /**
     * Variable assignment
     */
    override fun visitAssign(ctx: OrwellParser.AssignContext?): Node =
        ValNode().apply {
            id = (ctx?.assignment()?.name?.text ?: return@apply)
            value = visit(ctx.assignment()?.`val`)
        }

    /**
     * Int number
     */
    override fun visitIntNumber(ctx: OrwellParser.IntNumberContext?): Node =
        NumberNode().apply {
            value = (ctx?.`val`?.text?.toDouble() ?: return@apply)
            type = ValTypes.INT
        }

    /**
     * Float number
     */
    override fun visitFloatNumber(ctx: OrwellParser.FloatNumberContext?): Node =
        NumberNode().apply {
            value = (ctx?.`val`?.text?.toDouble() ?: return@apply)
            type = ValTypes.DOUBLE
        }

    /**
     * Negative number
     */
    override fun visitNegExpr(ctx: OrwellParser.NegExprContext?): Node =
        NegateNode().apply { innerNode = visit(ctx?.e()) }

    /**
     * Binary expression
     */
    override fun visitBinExpr(ctx: OrwellParser.BinExprContext?): Node =
        when (ctx?.op?.type) {
            OrwellLexer.OP_ADD -> {
                AdditionNode(visit(ctx.left), visit(ctx.right))
            }
            OrwellLexer.OP_SUB -> {
                SubtractionNode(visit(ctx.left), visit(ctx.right))
            }
            OrwellLexer.OP_DIV -> {
                DivisionNode(visit(ctx.left), visit(ctx.right))
            }
            OrwellLexer.OP_MUL -> {
                MultiplicationNode(visit(ctx.left), visit(ctx.right))
            }
            else -> null
        } as Node

    /**
     * Parenthetical expression
     */
    override fun visitParenExpr(ctx: OrwellParser.ParenExprContext?): Node = visit(ctx?.e())

    /**
     * Identifier
     */
    override fun visitIdentifier(ctx: OrwellParser.IdentifierContext?): Node =
        ValNode().apply { id = (ctx?.name?.text ?: return@apply) }

    /**
     * Function definition
     */
    override fun visitFunDef(ctx: OrwellParser.FunDefContext?): Node =
        FunDefNode().apply {
            for (i in 0 until (ctx?.fun_def()?.names?.size ?: return@apply)) {
                ValNode().let {
                    it.value = null
                    it.id = ctx.fun_def().names[i].text
                    it.type = ValTypes.valueOf(ctx.fun_def().types[i].text.toUpperCase())
                    arg.add(it)
                }
            }
            `fun` = ctx.fun_def().name.text
            returnType =
                if (ctx.fun_def().type == null) ValTypes.INT
                else ValTypes.valueOf(ctx.fun_def().type.text.toUpperCase())
            ctx.fun_def().top().forEachIndexed { index, topContext ->
                if (index >= ctx.fun_def().top().size - 1 && topContext.e(0) != null) body.returnExpr = topContext.e(0)
                else body.list += topContext
            }
        }

    /**
     * Function call internal
     */
    override fun visitFun_call(ctx: OrwellParser.Fun_callContext?): Node =
        FunCallNode().apply {
            for (e in ctx?.args ?: return@apply) args.add(visit(e).toValNode())
            `fun` = (ctx.name?.text ?: return@apply)
        }

    /**
     * Function call
     */
    override fun visitFunCall(ctx: OrwellParser.FunCallContext?): Node =
        FunCallNode().apply {
            for (e in ctx?.fun_call()?.args ?: return@apply) args.add(visit(e).toValNode())
            `fun` = (ctx.fun_call()?.name?.text ?: return@apply)
        }

    /**
     * Comparison
     */
    override fun visitComparison(ctx: OrwellParser.ComparisonContext?): Node =
        CompNode().apply {
            left = visit(ctx?.left).toValNode()
            right = visit(ctx?.right).toValNode()
            type = if (left.type == ValTypes.DOUBLE) when (ctx?.op?.text) {
                "!=" -> ComparisonType.FloatNotEqual
                ">" -> ComparisonType.FloatGreaterThan
                "<" -> ComparisonType.FloatLessThan
                ">=" -> ComparisonType.FloatGreaterThanOrEqual
                "<=" -> ComparisonType.FloatLessThanOrEqual
                else -> ComparisonType.FloatEqual
            }
            else when (ctx?.op?.text) {
                "!=" -> ComparisonType.IntNotEqual
                ">" -> ComparisonType.IntGreaterThan
                "<" -> ComparisonType.IntLessThan
                ">=" -> ComparisonType.IntGreaterThanOrEqual
                "<=" -> ComparisonType.IntLessThanOrEqual
                else -> ComparisonType.IntEqual
            }
        }

    /**
     * If chain
     */
    override fun visitIfBlock(ctx: OrwellParser.IfBlockContext?): Node =
        ConditionalNode().apply {
            `true` = visit(ctx?.if_block()?.if_statement()) as BodyNode
            comp = visit(ctx?.if_block()?.if_statement()?.comparison()) as CompNode
            if (ctx?.if_block()?.else_statement() != null)
                `false` = visit(ctx.if_block().else_statement()) as BodyNode
            ctx?.if_block()?.else_if_statement()?.forEach { chain.add(visit(it) as ConditionalNode) }
            isTop = true
        }

    /**
     * If statement
     */
    override fun visitIf_statement(ctx: OrwellParser.If_statementContext?): Node =
        BodyNode().apply { ctx?.top()?.forEach { list.add(it) } }

    /**
     * Else if statement
     */
    override fun visitElse_if_statement(ctx: OrwellParser.Else_if_statementContext?): Node =
        ConditionalNode().apply {
            `true` = visit(ctx?.if_statement()) as BodyNode
            comp = visit(ctx?.if_statement()?.comparison()) as CompNode
        }

    /**
     * Else statement
     */
    override fun visitElse_statement(ctx: OrwellParser.Else_statementContext?): Node =
        BodyNode().apply { ctx?.top()?.forEach { list.add(it) } }

    /**
     * While loop
     */
    override fun visitWhile(ctx: OrwellParser.WhileContext?): Node =
        ConditionalNode().apply {
            comp = visit(ctx?.while_loop()?.comparison()) as CompNode
            `true` = BodyNode().apply { ctx?.while_loop()?.top()?.forEach { list.add(it) } }
            isTop = true
            isLoop = true
        }
}
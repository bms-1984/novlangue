package net.bms.orwell.tree

class OrwellFunctionVisitor(private val args: HashMap<String, ValNode>): OrwellVisitor() {
    override fun visitIdentifier(ctx: OrwellParser.IdentifierContext?): Node? =
        if(ctx != null) {
            if (ctx.name.text in args) {
                with(NumberNode()) {
                    value = ASTVisitor.visitWithNoSideEffects(args[ctx.name.text]!!)
                    this
                }
            } else super.visitIdentifier(ctx)
        } else null
}
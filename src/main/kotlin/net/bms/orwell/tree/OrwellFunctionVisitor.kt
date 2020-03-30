package net.bms.orwell.tree

class OrwellFunctionVisitor(private val args: HashMap<String, ValNode>): OrwellVisitor() {
    override fun visitIdentifier(ctx: OrwellParser.IdentifierContext?): Node? =
        if(ctx != null) {
            val node: Node
            if (args.containsKey(ctx.name.text)) {
                node = NumberNode()
                node.value = ASTVisitor.visitWithNoSideEffects(args[ctx.name.text]!!)
                node
            } else super.visitIdentifier(ctx)
        } else null
}
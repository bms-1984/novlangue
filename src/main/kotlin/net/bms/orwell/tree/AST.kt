package net.bms.orwell.tree

import kotlin.collections.ArrayList

class AST: Collection<AST> {
    var parent: AST = this
    var value: ASTValue = SpecialValues.NONE // root node has no value
    val children = ArrayList<AST>()

    fun addChild(child: AST): AST {
        children.add(child)
        child.parent = this
        return children[children.size - 1]
    }

    fun addChildren(childList: Collection<AST>) {
        childList.forEach {
            children.add(it)
            it.parent = this
        }
    }

    fun getChild(id: Int) = children[id]

    override fun contains(element: AST): Boolean {
        var found = false
        children.forEach {
            if (it == element)
                found = true
            else if (!it.isEmpty())
                found = it.contains(element)
        }
        return found
    }

    override fun containsAll(elements: Collection<AST>): Boolean {
        if (isEmpty())
            return false
        elements.forEach {
            if(!contains(it))
                return false
        }
        return true
    }

    override fun isEmpty() = children.isEmpty()

    override fun iterator():Iterator<AST> = iterator()
    override val size: Int
        get()  {
            var num = 0
            forEach {
                num += it.size
            }
            return num
        }
}
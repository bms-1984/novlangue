/* (C) Ben M. Sutter 2020 */
package net.bms.novlangue.tree

import net.bms.novlangue.mainFun
import net.bms.novlangue.module
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef
import org.bytedeco.llvm.LLVM.LLVMBuilderRef
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

/**
 * Translator from AST to LLVM IR
 *
 * @author Ben M. Sutter
 * @since 0.1.1
 * @constructor Creates a new IRVisitor object.
 * @property func function in which the IR will reside, defaults to main.
 * @property block block in which the IR will reside, defaults to main's entry block.
 * @property finally should a return statement be appended, defaults to false.
 * @property helperFuncs should builtin functions be defined.
 */
open class IRVisitor(
    private val func: LLVMValueRef,
    private val builder: LLVMBuilderRef,
    var block: LLVMBasicBlockRef = LLVM.LLVMGetEntryBasicBlock(func),
    private val finally: Boolean = false,
    private val helperFuncs: Boolean = (func == mainFun && finally),
    private val exitBlock: LLVMBasicBlockRef? = null,
    private var tempIndex: Int = 0
) {
    internal open fun visit(node: Node?): LLVMValueRef = when (node) {
        is AdditionNode -> visit(node)
        is SubtractionNode -> visit(node)
        is MultiplicationNode -> visit(node)
        is DivisionNode -> visit(node)
        is ModuloNode -> visit(node)
        is NegateNode -> visit(node)
        is NumberNode -> visit(node)
        is ValNode -> visit(node)
        is FunCallNode -> visit(node)
        is FunDefNode -> visit(node)
        is ConditionalNode -> visit(node)
        is CompNode -> visit(node)
        is MasterNode -> visit(node)
        is BodyNode -> visit(node)
        is StringNode -> visit(node)
        else -> LLVM.LLVMConstNull(LLVM.LLVMVoidType())
    }

    /**
     * Return a temporary index
     */
    private fun getTempIndex(): Int = tempIndex++

    /**
     * Returns a unique label
     *
     * @param title optional custom string to insert into label.
     */
    fun getUniqueID(title: String = ""): String =
        if (title.isEmpty()) "_INTERNAL_${LLVM.LLVMGetValueName(func)}_${getTempIndex()}".toUpperCase()
        else "_INTERNAL_${LLVM.LLVMGetValueName(func)}_${title}_${getTempIndex()}".toUpperCase()

    private fun mangleFunName(name: String, vararg types: LLVMTypeRef = arrayOf()): String {
        var ret = name
        types.forEach { ret += "_${LLVM.LLVMPrintTypeToString(it).toString().filter { c -> c.isLetterOrDigit() }}" }
        return ret.toUpperCase()
    }

    internal open fun visit(node: BodyNode): LLVMValueRef {
        node.list.forEach { visit(CodeVisitor().visit(it)) }
        if (node.returnExpr != null)
            LLVM.LLVMInsertIntoBuilder(
                builder,
                LLVM.LLVMBuildRet(builder, visit(CodeVisitor().visit(node.returnExpr)))
            )
        else
            LLVM.LLVMInsertIntoBuilder(
                builder,
                LLVM.LLVMBuildRet(
                    builder,
                    when (node.returnType) {
                        ValTypes.DOUBLE -> LLVM.LLVMConstReal(LLVM.LLVMDoubleType(), 0.0)
                        else -> LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, 0)
                    }
                )
            )

        return LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, 0)
    }

    internal open fun visit(node: MasterNode): LLVMValueRef {
        if (helperFuncs) addHelpers()
        node.prog.forEach {
            if (it is InfixExpressionNode) LLVM.LLVMInsertIntoBuilder(builder, visit(it))
            else visit(it)
        }
        if (finally) LLVM.LLVMInsertIntoBuilder(
            builder,
            LLVM.LLVMBuildRet(
                builder,
                LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, 0)
            )
        )

        return LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, 0)
    }

    internal open fun visit(node: ConditionalNode): LLVMValueRef = TODO()

    internal open fun visit(node: CompNode): LLVMValueRef = when (node.left.type) {
        ValTypes.DOUBLE -> LLVM.LLVMBuildFCmp(builder, node.type, visit(node.left), visit(node.right), getUniqueID())
        else -> LLVM.LLVMBuildICmp(builder, node.type, visit(node.left), visit(node.right), getUniqueID())
    }

    internal open fun visit(node: AdditionNode): LLVMValueRef = when (node.left.toValNode().type) {
        ValTypes.DOUBLE -> LLVM.LLVMBuildFAdd(builder, visit(node.left), visit(node.right), "add")
        else -> LLVM.LLVMBuildAdd(builder, visit(node.left), visit(node.right), "add")
    }

    internal open fun visit(node: SubtractionNode): LLVMValueRef = when (node.left.toValNode().type) {
        ValTypes.DOUBLE -> LLVM.LLVMBuildFSub(builder, visit(node.left), visit(node.right), "sub")
        else -> LLVM.LLVMBuildSub(builder, visit(node.left), visit(node.right), "sub")
    }

    internal open fun visit(node: MultiplicationNode): LLVMValueRef = when (node.left.toValNode().type) {
        ValTypes.DOUBLE -> LLVM.LLVMBuildFMul(builder, visit(node.left), visit(node.right), "mul")
        else -> LLVM.LLVMBuildMul(builder, visit(node.left), visit(node.right), "mul")
    }

    internal open fun visit(node: DivisionNode): LLVMValueRef = when (node.left.toValNode().type) {
        ValTypes.DOUBLE -> LLVM.LLVMBuildFDiv(builder, visit(node.left), visit(node.right), "div")
        else -> LLVM.LLVMBuildSDiv(builder, visit(node.left), visit(node.right), "div")
    }

    internal open fun visit(node: ModuloNode): LLVMValueRef = when (node.left.toValNode().type) {
        ValTypes.DOUBLE -> LLVM.LLVMBuildFRem(builder, visit(node.left), visit(node.right), "mod")
        else -> LLVM.LLVMBuildSRem(builder, visit(node.left), visit(node.right), "mod")
    }

    internal open fun visit(node: NegateNode): LLVMValueRef = when (node.innerNode.toValNode().type) {
        ValTypes.DOUBLE -> LLVM.LLVMBuildFNeg(builder, visit(node.innerNode), "neg")
        else -> LLVM.LLVMBuildNeg(builder, visit(node.innerNode), "neg")
    }

    internal open fun visit(node: NumberNode): LLVMValueRef = when (node.type) {
        ValTypes.DOUBLE -> LLVM.LLVMConstReal(LLVM.LLVMDoubleType(), node.value)
        else -> LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), node.value.toLong(), 0)
    }

    internal open fun visit(node: StringNode): LLVMValueRef = TODO()

    internal open fun visit(node: ValNode): LLVMValueRef = TODO()

    internal open fun visit(node: FunCallNode): LLVMValueRef = TODO()

    internal open fun visit(node: FunDefNode): LLVMValueRef = TODO()

    private fun addHelpers() {
        val intString = LLVM.LLVMBuildGlobalStringPtr(builder, "%d\n", getUniqueID("INT_STRING"))
        val doubleString = LLVM.LLVMBuildGlobalStringPtr(builder, "%f\n", getUniqueID("DOUBLE_STRING"))
        val strString = LLVM.LLVMBuildGlobalStringPtr(builder, "%s\n", getUniqueID("STR_STRING"))
        val printInt = LLVM.LLVMAddFunction(
            module,
            mangleFunName("print", LLVM.LLVMInt32Type()),
            LLVM.LLVMFunctionType(
                LLVM.LLVMInt32Type(),
                LLVM.LLVMInt32Type(),
                1,
                0
            )
        )
        val printDouble = LLVM.LLVMAddFunction(
            module,
            mangleFunName("print", LLVM.LLVMDoubleType()),
            LLVM.LLVMFunctionType(
                LLVM.LLVMInt32Type(),
                LLVM.LLVMDoubleType(),
                1,
                0
            )
        )
        val printString = LLVM.LLVMAddFunction(
            module,
            mangleFunName("print", LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0)),
            LLVM.LLVMFunctionType(
                LLVM.LLVMInt32Type(),
                LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0),
                1,
                0
            )
        )
        LLVM.LLVMPositionBuilderAtEnd(builder, LLVM.LLVMGetEntryBasicBlock(printInt))
        LLVM.LLVMBuildCall(
            builder,
            LLVM.LLVMGetNamedFunction(module, "printf"),
            PointerPointer(intString, LLVM.LLVMGetFirstParam(printInt)),
            2,
            "call_int_printf"
        )
        LLVM.LLVMBuildRet(
            builder,
            LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, 0)
        )
        LLVM.LLVMPositionBuilderAtEnd(builder, LLVM.LLVMGetEntryBasicBlock(printDouble))
        LLVM.LLVMBuildCall(
            builder,
            LLVM.LLVMGetNamedFunction(module, "printf"),
            PointerPointer(doubleString, LLVM.LLVMGetFirstParam(printDouble)),
            2,
            "call_double_printf"
        )
        LLVM.LLVMBuildRet(
            builder,
            LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, 0)
        )
        LLVM.LLVMPositionBuilderAtEnd(builder, LLVM.LLVMGetEntryBasicBlock(printString))
        LLVM.LLVMBuildCall(
            builder,
            LLVM.LLVMGetNamedFunction(module, "printf"),
            PointerPointer(strString, LLVM.LLVMGetFirstParam(printString)),
            2,
            "call_string_printf"
        )
        LLVM.LLVMBuildRet(
            builder,
            LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, 0)
        )
        LLVM.LLVMPositionBuilderAtEnd(builder, block)
    }
}

/*
Copyright Ben M. Sutter 2020

This file is part of Novlangue.

Novlangue is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Novlangue is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Novlangue.  If not, see <https://www.gnu.org/licenses/>.
*/
package net.bms.novlangue

import NovlangueLexer
import NovlangueParser
import net.bms.novlangue.tree.CodeVisitor
import net.bms.novlangue.tree.IRVisitor
import net.bms.novlangue.tree.REPLVisitor
import net.bms.novlangue.tree.ValTypes
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.bytedeco.llvm.LLVM.LLVMBuilderRef
import org.bytedeco.llvm.LLVM.LLVMContextRef
import org.bytedeco.llvm.LLVM.LLVMModuleRef
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.Reader
import java.io.StringReader
import java.util.Properties
import kotlin.random.Random
import kotlin.random.nextUInt

/**
 * Variable storage
 */
val valStore: HashMap<String, LLVMValueRef> = HashMap()

/**
 * Local variable storage
 */
val localValStore: HashMap<String, LLVMValueRef> = HashMap()

/**
 * Top-level IR representation
 */
val context: LLVMContextRef = LLVM.LLVMGetGlobalContext()

/**
 * IR Builder
 */
val builder: LLVMBuilderRef = LLVM.LLVMCreateBuilder()

/**
 * IR Module
 */
lateinit var module: LLVMModuleRef

/**
 * IR main function
 */
lateinit var mainFun: LLVMValueRef

internal val typeNameMap: HashMap<LLVMTypeRef, ValTypes> = hashMapOf(
    LLVM.LLVMInt32Type() to ValTypes.INT,
    LLVM.LLVMDoubleType() to ValTypes.DOUBLE,
    LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0) to ValTypes.STRING,
)

/**
 * Main function
 */
@ExperimentalUnsignedTypes
fun main(args: Array<String>) {
    val properties = Properties()
    properties.load(object {}.javaClass.classLoader.getResourceAsStream("novlangue.properties"))
    println("Sutter's Novlangue Compiler v${properties.getProperty("version")}")

    if (args.isNotEmpty()) {
        val installMain = !args.contains("-noMain")
        val helpers = !args.contains("-noStd")
        if (runCompiler(File(args[0]), helpers, installMain)) println("Complete.")
        else runREPL(helpers)
    } else {
        runREPL()
    }
    return
}

/**
 * Run Novlangue to file
 */
@ExperimentalUnsignedTypes
fun runCompiler(
    input: File,
    helpers: Boolean = true,
    installMain: Boolean = true
): Boolean = try {
    module = LLVM.LLVMModuleCreateWithName(input.nameWithoutExtension)
    LLVM.LLVMSetSourceFileName(module, input.name, input.name.length.toLong())
    FileReader(input).run {
        runNovlangue(this, true, helpers, installMain)
        close()
    }
    LLVM.LLVMPrintModuleToFile(module, input.getOutputFile().path, byteArrayOf())
    true
} catch (e: FileNotFoundException) {
    println("ERROR: $e; dropping into a REPL...")
    false
}

/**
 * Returns file's output name
 */
fun File.getOutputFile(): File = File(this.nameWithoutExtension.plus(".ll"))

/**
 * Runs Novlangue
 */
@ExperimentalUnsignedTypes
fun runNovlangue(reader: Reader, compile: Boolean = false, helpers: Boolean = true, installMain: Boolean = true) {
    val tree =
        CodeVisitor().visit(NovlangueParser(CommonTokenStream(NovlangueLexer(CharStreams.fromReader(reader)))).top())
    mainFun =
        if (installMain) LLVM.LLVMAddFunction(
            module,
            "main",
            LLVM.LLVMFunctionType(LLVM.LLVMInt32Type(), LLVM.LLVMVoidType(), 0, 0)
        )
        else LLVM.LLVMAddFunction(
            module,
            "__INTERNAL_${Random.nextUInt()}_MAIN_",
            LLVM.LLVMFunctionType(LLVM.LLVMInt32Type(), LLVM.LLVMVoidType(), 0, 0)
        )
    if (helpers)
        LLVM.LLVMSetLinkage(
            LLVM.LLVMAddFunction(
                module,
                "printf",
                LLVM.LLVMFunctionType(LLVM.LLVMInt32Type(), LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0), 1, 1)
            ),
            LLVM.LLVMExternalLinkage
        )
    if (compile) IRVisitor(func = mainFun, builder = builder, finally = true).visit(tree)
    else REPLVisitor(func = mainFun).visit(tree)
}

private fun listBindings(): Nothing = TODO()

@ExperimentalUnsignedTypes
private fun runREPL(helpers: Boolean = true) {
    println("WARNING: REPL MODE IS CURRENTLY INCOMPLETE")
    println("For assistance, use ;help.\n")
    while (true) {
        print("novlangue> ")
        val line = readLine() ?: return
        if (line.isBlank()) continue
        if (line[0] == ';') {
            if (line.toLowerCase().contains("help")) {
                getHelp()
                continue
            }
            if (line.toLowerCase().contains("quit")) {
                return
            }
            if (line.toLowerCase().contains("list")) {
                // TODO: listBindings not implemented
                continue
            }
        } else runNovlangue(StringReader(line), helpers = helpers)
    }
}

private fun getHelp() {
    println("Commands:")
    println(";help -- prints this help dialogue")
    println(";quit -- exits the REPL")
    // println(";list -- lists current bindings")
}

/* (C) Ben M. Sutter 2020 */
package net.bms.novlangue

import NovlangueLexer
import NovlangueParser
import me.tomassetti.kllvm.DoubleType
import me.tomassetti.kllvm.FunctionBuilder
import me.tomassetti.kllvm.FunctionDeclaration
import me.tomassetti.kllvm.I32Type
import me.tomassetti.kllvm.I8Type
import me.tomassetti.kllvm.LocalVariable
import me.tomassetti.kllvm.ModuleBuilder
import me.tomassetti.kllvm.Pointer
import me.tomassetti.kllvm.Type
import net.bms.novlangue.tree.CodeVisitor
import net.bms.novlangue.tree.IRVisitor
import net.bms.novlangue.tree.REPLVisitor
import net.bms.novlangue.tree.ValTypes
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.io.Reader
import java.io.StringReader
import java.util.Properties
import kotlin.random.Random
import kotlin.random.nextUInt

/**
 * Variable storage
 */
val valStore: ArrayList<LocalVariable> = ArrayList()

/**
 * Function parameter storage
 */
val funValStore: HashMap<FunctionBuilder, ArrayList<Pair<String, Type>>> = HashMap()

/**
 * Function storage
 */
val funStore: ArrayList<FunctionBuilder> = ArrayList()

/**
 * Top-level IR representation
 */
val module: ModuleBuilder = ModuleBuilder()

/**
 * IR main function
 */
lateinit var mainFun: FunctionBuilder

internal val typeNameMap: HashMap<Type, ValTypes> = hashMapOf(
    I32Type to ValTypes.INT,
    DoubleType to ValTypes.DOUBLE,
    Pointer(I8Type) to ValTypes.STRING,
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
    FileReader(input).run {
        runNovlangue(this, true, helpers, installMain)
        close()
    }
    FileWriter(input.getOutputFile()).run {
        write(module.IRCode().trim())
        close()
    }
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
        if (installMain) module.createMainFunction()
        else module.createFunction("__INTERNAL_${Random.nextUInt()}_MAIN_", I32Type, listOf())
    if (helpers)
        module.addDeclaration(FunctionDeclaration("printf", I32Type, listOf(Pointer(I8Type)), varargs = true))
    if (compile) IRVisitor(mainFun, finally = true, helperFuncs = helpers).visit(tree)
    else REPLVisitor(mainFun, helperFuncs = helpers).visit(tree)
}

private fun listBindings() {
    if (funStore.isEmpty()) {
        println("No functions have been bound.")
    } else {
        println("Functions:")
        for (f in funStore) {
            var params = ""
            f.paramTypes.forEachIndexed { index, type ->
                params += if (index == 0) typeNameMap[type]?.type else ", ${typeNameMap[type]?.type}"
            }
            println("${f.name}($params): ${typeNameMap[f.returnType]?.type}")
        }
    }
}

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
                listBindings()
                continue
            }
        } else runNovlangue(StringReader(line), helpers = helpers)
    }
}

private fun getHelp() {
    println("Commands:")
    println(";help -- prints this help dialogue")
    println(";quit -- exits the REPL")
    println(";list -- lists current bindings")
}

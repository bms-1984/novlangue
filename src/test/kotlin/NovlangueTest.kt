/* (C) Ben M. Sutter 2020 */
/* Novlangue Test Suite */
import net.bms.novlangue.getOutputFile
import net.bms.novlangue.runCompiler
import java.io.File
import java.io.FileWriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class NovlangueTest {
    @ExperimentalUnsignedTypes
    @Test
    fun `Output file is created and contains basic IR`() {
        val input = File("test.sw").apply {
            createNewFile()
            deleteOnExit()
        }
        val output = input.getOutputFile().apply { deleteOnExit() }
        FileWriter(input).run {
            write("1 + 1")
            close()
        }
        if (!runCompiler(input, helpers = false, installMain = true))
            fail("Created input file does not exist... this is a filesystem issue.")
        if (!output.exists())
            fail("Output file was not created.")
        assertEquals(
            "define i32 @main(i32, i8**) {\n" +
                "    \n" +
                "    ; unnamed block\n" +
                "    %tmpValue0 = add i32 1, 1\n" +
                "    ret i32 0\n" +
                "\n" +
                "}",
            output.readText()
        )
    }
}

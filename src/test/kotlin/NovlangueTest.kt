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
        val input = File("temp.sw").apply {
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
            "; ModuleID = 'temp'${System.lineSeparator()}source_filename = \"temp.sw\"${System.lineSeparator()}${System.lineSeparator()}declare i32 @main()${System.lineSeparator()}",
            output.readText()
        )
    }
}

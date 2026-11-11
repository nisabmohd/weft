package app.sprout.habits.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Screens use named sizes only (ui/theme/Dimens.kt and the component constants), never a raw
 * `16.dp` (user, 2026-11-11: "smallest detail matters"). A raw number is allowed only where it is
 * defined: a `val Name = 16.dp` line. `0.dp` is not a size.
 */
class NoRawSizesTest {
    private val root = listOf("src/main/java/app/sprout/habits/ui", "app/src/main/java/app/sprout/habits/ui").map(::File).first { it.exists() }
    private val raw = Regex("""(?<![\w.])[0-9]+(\.[0-9]+)?\.(dp|sp)\b""")
    private val definition = Regex("""^\s*(private |internal )?(const )?val \w+(: \w+)? = """)

    @Test fun noScreenUsesARawSize() {
        val found = (root.walkTopDown() + File(root.parentFile, "widget").walkTopDown()).filter { it.extension == "kt" && it.name != "Type.kt" }.flatMap { file ->
            file.readLines().mapIndexedNotNull { i, line ->
                val code = line.substringBefore("//")
                if (raw.findAll(code).any { it.value != "0.dp" } && !definition.containsMatchIn(code) && !code.trimStart().startsWith("*")) {
                    "${file.name}:${i + 1}: ${line.trim()}"
                } else null
            }
        }.toList()
        assertTrue("Raw sizes outside a named definition:\n" + found.joinToString("\n"), found.isEmpty())
    }

    @Test fun everySpacingTokenIsOnTheGrid() {
        // The spacing scale itself: 2 dp, then multiples of 4.
        val dimens = File(root, "theme/Dimens.kt").readText()
        val spaces = Regex("""val Space(\d+) = (\d+)\.dp""").findAll(dimens).map { it.groupValues[1].toInt() to it.groupValues[2].toInt() }.toList()
        assertTrue("no Space tokens found", spaces.isNotEmpty())
        spaces.forEach { (name, value) ->
            assertTrue("Space$name is $value dp", name == value && (value == 2 || value % 4 == 0))
        }
    }
}

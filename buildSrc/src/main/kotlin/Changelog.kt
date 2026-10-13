import java.io.File

/** The entries of one version in CHANGELOG.md, without its heading. */
fun changelogSection(changelog: File, version: String): String {
    val lines = changelog.readLines()
    val start = lines.indexOfFirst { it.startsWith("## [$version]") }
    require(start >= 0) { "CHANGELOG.md has no section for $version" }
    val next = lines.subList(start + 1, lines.size).indexOfFirst { it.startsWith("## ") }
    val end = if (next < 0) lines.size else start + 1 + next
    return lines.subList(start + 1, end).joinToString("\n").trim()
}

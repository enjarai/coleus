package mod.master_bw3.coleus

import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import net.minecraft.resource.Resource
import java.util.stream.Collectors

public data class Base16Theme(
    val base00: String,
    val base01: String,
    val base02: String,
    val base03: String,
    val base04: String,
    val base05: String,
    val base06: String,
    val base07: String,
    val base08: String,
    val base09: String,
    val base0A: String,
    val base0B: String,
    val base0C: String,
    val base0D: String,
    val base0E: String,
    val base0F: String
) {

    public fun toCss(): String {
        val builder = StringBuilder()
        builder.appendLine(":root {")
        builder.appendLine("\t--base00: #$base00;")
        builder.appendLine("\t--base01: #$base01;")
        builder.appendLine("\t--base02: #$base02;")
        builder.appendLine("\t--base03: #$base03;")
        builder.appendLine("\t--base04: #$base04;")
        builder.appendLine("\t--base05: #$base05;")
        builder.appendLine("\t--base06: #$base06;")
        builder.appendLine("\t--base07: #$base07;")
        builder.appendLine("\t--base08: #$base08;")
        builder.appendLine("\t--base09: #$base09;")
        builder.appendLine("\t--base0A: #$base0A;")
        builder.appendLine("\t--base0B: #$base0B;")
        builder.appendLine("\t--base0C: #$base0C;")
        builder.appendLine("\t--base0D: #$base0D;")
        builder.appendLine("\t--base0E: #$base0E;")
        builder.appendLine("\t--base0F: #$base0F;")
        builder.appendLine("}")

        return builder.toString()
    }

    public companion object {
        @JvmStatic
        public fun fromJsonResource(resource: Resource): Base16Theme {
            val builder = GsonBuilder().create()
            return builder.fromJson(
                resource.reader.lines().collect(Collectors.joining("\n")),
                Base16Theme::class.java
            )
        }
    }
}
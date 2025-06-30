package mod.master_bw3.coleus

import j2html.tags.DomContent
import j2html.tags.UnescapedText
import net.minecraft.util.Identifier
import java.nio.file.Path

public fun interface TemplateExpander {
    public fun expand(properties: Map<String, String>, pagePath: Path, extraResourcesDir: Path ): DomContent
}

public object HtmlTemplateRegistry {
    internal val registry = HashMap<Identifier, TemplateExpander>()

    @JvmStatic
    public fun register(identifier: Identifier, expander: TemplateExpander) {
        if (registry[identifier] == null) {
            registry[identifier] = expander
        } else {
            throw IllegalArgumentException("template expander already registered for $identifier")
        }
    }

    @JvmStatic
    public fun register(identifier: Identifier, content: DomContent) {
        register(identifier) { _, _, _ -> content }
    }

    @JvmStatic
    public fun register(identifier: Identifier, content: String) {
        register(identifier) { _, _, _ -> UnescapedText(content) }
    }
}
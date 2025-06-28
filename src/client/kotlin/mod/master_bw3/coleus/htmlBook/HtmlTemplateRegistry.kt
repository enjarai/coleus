package mod.master_bw3.coleus.htmlBook

import j2html.tags.DomContent
import j2html.tags.Renderable
import j2html.tags.UnescapedText
import net.minecraft.util.Identifier
import java.nio.file.Path

typealias TemplateExpander = (properties: Map<String, String>, pagePath: Path, extraResourcesDir: Path ) -> DomContent

object HtmlTemplateRegistry {
    internal val registry = HashMap<Identifier, TemplateExpander>()

    fun register(identifier: Identifier, expander: TemplateExpander) {
        if (registry[identifier] == null) {
            registry[identifier] = expander;
        } else {
            throw IllegalArgumentException("template expander already registered for $identifier")
        }
    }

    fun register(identifier: Identifier, content: DomContent) {
        register(identifier) { _, _, _ -> content }
    }

    fun register(identifier: Identifier, content: String) {
        register(identifier) { _, _, _ -> UnescapedText(content) }
    }
}
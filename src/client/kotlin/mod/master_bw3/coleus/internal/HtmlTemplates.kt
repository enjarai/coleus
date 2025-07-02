package mod.master_bw3.coleus.internal

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import j2html.TagCreator.*
import mod.master_bw3.coleus.Components
import mod.master_bw3.coleus.HtmlTemplateRegistry
import mod.master_bw3.coleus.TemplateExpander
import net.minecraft.command.argument.ItemStringReader
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryWrapper
import net.minecraft.util.Identifier
import java.util.stream.Stream

internal val lavenderHtmlTemplates = mapOf<String, TemplateExpander>(
    "page-title" to TemplateExpander { params, _, _ ->
        h2(params["title"]!!).withClass("page-title")
    },

    "item-spotlight" to TemplateExpander { params, pagePath, resourcesDir ->
        try {
            val item = ItemStringReader(RegistryWrapper.WrapperLookup.of(Stream.of(Registries.ITEM.getReadOnlyWrapper())))
                .consume(StringReader(params["item"]!!)).item
            val id = item.key.get().value

            div(
                Components.item(
                    ItemStack(item),
                    pagePath,
                    resourcesDir.resolve("item/${id.namespace}/${id.path}.png"))
            ).withClass("item-spotlight")
        } catch (_: CommandSyntaxException) {
            p(params["item"])
        }
    },

    "horizontal-rule" to TemplateExpander {params, pagePath, resourcesDir -> hr() }
)


internal fun registerHtmlTemplates() {
    lavenderHtmlTemplates.forEach {
        HtmlTemplateRegistry.register(Identifier.of("lavender", it.key), it.value)
    }
}
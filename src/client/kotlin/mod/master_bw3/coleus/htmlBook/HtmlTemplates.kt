package mod.master_bw3.coleus.htmlBook

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import j2html.TagCreator.*
import net.minecraft.command.argument.ItemStringReader
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryWrapper
import net.minecraft.util.Identifier
import java.util.stream.Stream

internal val lavenderHtmlTemplates = mapOf<String, TemplateExpander>(
    "page-title" to { params, _, _ ->
        h1(params["title"]!!).withClass("page-title")
    },

    "item-spotlight" to { params, pagePath, resourcesDir ->
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
        } catch (e: CommandSyntaxException) {
            p(params["item"])
        }
    },

    "horizontal-rule" to {params, pagePath, resourcesDir -> hr() }
)


internal fun registerHtmlTemplates() {
    lavenderHtmlTemplates.forEach {
        HtmlTemplateRegistry.register(Identifier.of("lavender", it.key), it.value)
    }
}
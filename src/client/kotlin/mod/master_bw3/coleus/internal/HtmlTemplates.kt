package mod.master_bw3.coleus.internal

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import io.wispforest.owo.ui.component.Components
import j2html.TagCreator.*
import mod.master_bw3.coleus.Components.owo
import mod.master_bw3.coleus.Components.tooltip
import mod.master_bw3.coleus.HtmlTemplateRegistry
import mod.master_bw3.coleus.TemplateExpander
import net.minecraft.command.argument.ItemStringReader
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryWrapper
import net.minecraft.util.Identifier
import java.util.stream.Stream

internal val lavenderHtmlTemplates = mapOf<String, TemplateExpander>(
    "page-title" to TemplateExpander { params, _ ->
        h2(params["title"]!!).withClass("page-title")
    },

    "horizontal-rule" to TemplateExpander { _, _ -> hr() }
)


internal fun registerHtmlTemplates() {
    lavenderHtmlTemplates.forEach {
        HtmlTemplateRegistry.register(Identifier.of("lavender", it.key), it.value)
    }
}
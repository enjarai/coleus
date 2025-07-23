package mod.master_bw3.coleus.internal

import com.mojang.brigadier.StringReader
import io.wispforest.owo.ui.component.ItemComponent.tooltipFromItem
import io.wispforest.owo.ui.container.StackLayout
import io.wispforest.owo.ui.parsing.UIModel
import j2html.TagCreator.*
import mod.master_bw3.coleus.Components.owoWithTooltip
import mod.master_bw3.coleus.HtmlTemplateRegistry
import mod.master_bw3.coleus.TemplateExpander
import net.minecraft.client.MinecraftClient
import net.minecraft.command.argument.ItemStringReader
import net.minecraft.item.Item
import net.minecraft.util.Identifier
import java.util.UUID

internal val lavenderHtmlTemplates = mapOf<String, TemplateExpander>(
    "page-title" to TemplateExpander { params, _ ->
        h2(params["title"]!!).withClass("page-title")
    },

    "horizontal-rule" to TemplateExpander { _, _ -> hr() },

    "item-spotlight" to TemplateExpander { params, context ->
        val client = MinecraftClient.getInstance();
        val templates = client.resourceManager.getResource(Identifier.of("lavender", "owo_ui/book_components.xml"));
        val model = templates.get().inputStream.use {
            UIModel.load(it);
        }

        val component = model.expandTemplate(StackLayout::class.java, "item-spotlight", params)
        val item = ItemStringReader(client.world!!.registryManager).consume(StringReader(params["item"])).item()
        val stack = item.value().defaultStack
        val tooltip = tooltipFromItem(stack, Item.TooltipContext.create(client.world), client.player, null)

        div().withClass("item-spotlight").with(
            owoWithTooltip(component, tooltip, "lavender-book_components-item-spotlight", context.pagePath,
                context.assetsDir.resolve("lavender/book_components_item-spotlight_${UUID.randomUUID()}.png"), 500, 4)
        )
    },

)


internal fun registerHtmlTemplates() {
    lavenderHtmlTemplates.forEach {
        HtmlTemplateRegistry.register(Identifier.of("lavender", it.key), it.value)
    }
}
package mod.master_bw3.coleus.lavender.compiler

import io.wispforest.lavendermd.compiler.MarkdownCompiler
import io.wispforest.owo.ui.core.Component
import j2html.TagCreator.*
import j2html.tags.ContainerTag
import j2html.tags.DomContent
import j2html.tags.specialized.DivTag
import mod.master_bw3.coleus.ColeusClient
import mod.master_bw3.coleus.Components.owo
import mod.master_bw3.coleus.Components.tooltip
import mod.master_bw3.coleus.PageContext
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.tooltip.TooltipComponent
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Style
import net.minecraft.util.Identifier
import java.util.*
import java.util.function.UnaryOperator
import kotlin.collections.ArrayDeque
import kotlin.io.path.outputStream
import kotlin.io.path.relativeTo
import kotlin.jvm.optionals.getOrNull

public class HtmlPageCompiler(private val context: PageContext) : MarkdownCompiler<DivTag> {

    private var root: DivTag = div()
    private var nodes: ArrayDeque<ContainerTag<*>> = ArrayDeque(listOf(root))
    private val nodesTop: ContainerTag<*>
        get() = nodes.last()

    private var prevListDepth = 0
    private var listDepth = 0
    private var newParagraph = true
    private var pageIndex = 1;

    override fun visitText(text: String) {
        if (text.matches(Regex("\n+"))) {
            popToRoot()
        } else if (text.isNotBlank()) {
            if (nodes.size == 1) {
                pushTag(p())
                nodesTop.withText(text)
            } else {
                nodesTop.withText(" $text")
            }
        }
    }

    override fun visitStyle(styleFn: UnaryOperator<Style>) {
        if (nodes.size == 1) pushTag(p())

        val style = styleFn.apply(Style.EMPTY)
        var styleTag: ContainerTag<*> = span()
        nodesTop.with(styleTag)
        fun <TAG : ContainerTag<*>> appendStyle(tag: TAG) {
            styleTag.with(tag)
            styleTag = tag
        }

        if (style.isBold) {
            appendStyle(strong())
        }
        if (style.isItalic) {
            appendStyle(em())
        }
        if (style.isObfuscated) {
            appendStyle(span().withClass("obfuscated"))
        }
        if (style.isUnderlined) {
            appendStyle(u())
        }
        if (style.isStrikethrough) {
            appendStyle(s())
        }
        val color = style.color;
        if (color != null) {
            if (color.name != color.hexCode) {
                appendStyle(span().withClass(color.name))
            } else {
                appendStyle(span().withStyle("color: " + color.hexCode))
            }
        }

        val clickEvent = style.clickEvent
        val hoverText = style.hoverEvent?.getValue(HoverEvent.Action.SHOW_TEXT)
        if (clickEvent != null) {
            when (clickEvent.action) {
                ClickEvent.Action.OPEN_URL -> appendStyle(a().withHref(resolveLinkTarget(clickEvent.value)))
                else -> {}
            }
        } else if (hoverText != null) {
            val uuid = UUID.randomUUID()
            val tooltip = Tooltip.of(hoverText).getLines(MinecraftClient.getInstance()).map { TooltipComponent.of(it) }
            appendStyle(
                span().withClass("hover-text")
                    .with(
                        tooltip(tooltip, context.pagePath, context.assetsDir.resolve("hover/${uuid}.png"), 2)
                            .withClass("hover-text-tooltip")
                    )
            )
        }

        nodes.add(styleTag)
    }

    private fun resolveLinkTarget(link: String): String {
        val start = link.first()
        val linkParts = link.drop(1).split("#")
        val identifier = Identifier.tryParse(linkParts.first())
        val section = linkParts.getOrNull(1)


        return if (identifier != null && start == '^') {
            val pagePath = context.bookDir.resolve("${identifier.path}.html").relativeTo(context.pagePath.parent)
            var newLink = pagePath.toString()
            if (section != null) newLink += "#$section"

            newLink
        } else {
            link
        }
    }

    override fun visitStyleEnd() {
        nodes.removeLast()
    }

    override fun visitBlockQuote() {
        pushTag(blockquote().withClass("block-quote"))
    }

    override fun visitBlockQuoteEnd() {
        nodes.removeLast()
    }

    override fun visitHorizontalRule() {
        popToRoot()
        nodesTop.with(hr())
    }

    override fun visitImage(image: Identifier, description: String, fit: Boolean) {
        val resource = MinecraftClient.getInstance().resourceManager.getResource(image).getOrNull()
        val outPath = context.assetsDir.resolve(image.namespace).resolve(image.path)

        if (resource != null) {
            outPath.parent.toFile().mkdirs()
            resource.inputStream.use { inputStream ->
                outPath.outputStream().use { outputStream ->
                    inputStream.transferTo(outputStream)
                }
            }
        } else {
            ColeusClient.logger.atError().log("missing resource: $image")
        }

        nodesTop.with(img().withSrc(outPath.relativeTo(context.bookDir).toString()).withAlt(description))
    }

    override fun visitListItem(ordinal: OptionalInt) {
        if (prevListDepth == 0) popToRoot()
        listDepth++

        if (listDepth > prevListDepth) {
            if (ordinal.isPresent) {
                pushTag(ol().withStart(ordinal.asInt.toString()))
            } else {
                pushTag(ul())
            }
        }
        pushTag(li())
    }

    override fun visitListItemEnd() {
        nodes.removeLast()

        prevListDepth = listDepth
        listDepth--
    }

    public fun visitPageBreak() {
        pageIndex++

        popToRoot()
        nodesTop.with(div().withId(pageIndex.toString()))
    }

    public fun visitTemplate(template: (context: PageContext) -> DomContent) {
        nodesTop.with(template(context))
    }

    public fun visitComponent(component: Component, className: String, scale: Int = 2) {
        val outDir = context.assetsDir.resolve("component")
        outDir.parent.toFile().mkdirs()
        val uuid = UUID.randomUUID()

        pushTag(div().withClass("embedded-component-container $className"))
        nodesTop.with(
            owo(
                component,
                context.pagePath,
                outDir.resolve("${uuid}.png"),
                500,
                scale
            ).withClass("embedded-component")
        )
        component.tooltip()?.let { tooltip ->
            nodesTop.with(
                tooltip(tooltip, context.pagePath, outDir.resolve("${uuid}-tooltip.png"), 2)
                    .withClass("embedded-component-tooltip")
            )
        }
        nodes.removeLast()
    }

    override fun compile(): DivTag {
        return root
    }

    override fun name(): String {
        return "coleus_html"
    }

    private fun <TAG : ContainerTag<*>> pushTag(tag: TAG): TAG {
        nodesTop.with(tag)
        nodes.add(tag)
        return tag
    }

    private fun popToRoot() {
        nodes.clear()
        nodes.add(root)
        newParagraph = true
    }


}
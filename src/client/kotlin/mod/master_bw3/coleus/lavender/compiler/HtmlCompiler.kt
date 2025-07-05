package mod.master_bw3.coleus.lavender.compiler

import io.wispforest.lavendermd.compiler.MarkdownCompiler
import io.wispforest.owo.ui.core.Component
import j2html.TagCreator.*
import j2html.tags.ContainerTag
import j2html.tags.DomContent
import j2html.tags.specialized.DivTag
import mod.master_bw3.coleus.Components.owo
import mod.master_bw3.coleus.Components.tooltip
import net.minecraft.text.ClickEvent
import net.minecraft.text.Style
import net.minecraft.util.Identifier
import java.nio.file.Path
import java.util.*
import java.util.function.UnaryOperator
import kotlin.collections.ArrayDeque
import kotlin.io.path.relativeTo

public class HtmlCompiler(private val pagePath: Path, private val rootDir: Path, private val extraResourcesDir: Path) : MarkdownCompiler<DivTag> {

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
            if (nodes.size == 1) pushTag(p())
            nodesTop.withText(text)
        }
    }

    override fun visitStyle(styleFn: UnaryOperator<Style>) {
        if (nodes.size == 1) pushTag(p())
        val style = styleFn.apply(Style.EMPTY)
        if (style.isBold) {
            pushTag(strong())
        }
        if (style.isItalic) {
            pushTag(em())
        }
        if (style.isObfuscated) {
            pushTag(span().withClass("obfuscated"))
        }
        if (style.isUnderlined) {
            pushTag(u())
        }
        if (style.isStrikethrough) {
            pushTag(s())
        }
        val color = style.color;
        if (color != null) {
            if (color.name != color.hexCode) {
                pushTag(span().withClass(color.name))
            } else {
                pushTag(span().withStyle("color: " + color.hexCode))
            }
        }
        style.clickEvent?.let { clickEvent ->

            when (clickEvent.action) {
                ClickEvent.Action.OPEN_URL -> pushTag(a().withHref(resolveLinkTarget(clickEvent.value)))
                else -> {}
            }
        }
//        style.hoverEvent?.getValue(HoverEvent.Action.SHOW_TEXT)?.let { text ->
//            pushTag(span().with(Components.text(text).withClass("hover-text")))
//        }
    }

    private fun resolveLinkTarget(link: String): String {
        val start = link.first()
        val linkParts = link.drop(1).split("#")
        val identifier = Identifier.tryParse(linkParts.first())
        val section = linkParts.getOrNull(1)


        return if (identifier != null && start == '^') {
            val pagePath = rootDir.resolve("${identifier.path}.html").relativeTo(pagePath.parent)
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
        nodesTop.with(img().withSrc("/${image.namespace}/${image.path}").withAlt(description))
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

    public fun visitTemplate(template: (pagePath: Path, extraResourcesDir: Path) -> DomContent) {
        nodesTop.with(template(pagePath, extraResourcesDir))
    }

    public fun visitComponent(component: Component, className: String, scale: Int = 2) {
        val outDir = extraResourcesDir.resolve("component")
        outDir.parent.toFile().mkdirs()
        val uuid = UUID.randomUUID()

        pushTag(div().withClass("embedded-component-container $className"))
        nodesTop.with(
            owo(
                component,
                pagePath,
                outDir.resolve("${uuid}.png"),
                500,
                scale
            ).withClass("embedded-component")
        )
        component.tooltip()?.let { tooltip ->
            nodesTop.with(
                tooltip(tooltip, pagePath, outDir.resolve("${uuid}-tooltip.png"), 2)
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
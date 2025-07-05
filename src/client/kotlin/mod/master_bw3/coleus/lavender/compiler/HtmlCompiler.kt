package mod.master_bw3.coleus.lavender.compiler

import io.wispforest.lavendermd.compiler.MarkdownCompiler
import io.wispforest.owo.ui.core.Component
import j2html.TagCreator.*
import j2html.tags.ContainerTag
import j2html.tags.DomContent
import j2html.tags.specialized.DivTag
import mod.master_bw3.coleus.Components.owo
import net.minecraft.text.Style
import net.minecraft.util.Identifier
import java.nio.file.Path
import java.util.*
import java.util.function.UnaryOperator
import kotlin.collections.ArrayDeque

public class HtmlCompiler(private val pagePath: Path, private val extraResourcesDir: Path) : MarkdownCompiler<DivTag> {

    private var root: DivTag = div()
    private var nodes: ArrayDeque<ContainerTag<*>> = ArrayDeque(listOf(root))
    private val nodesTop: ContainerTag<*>
        get() = nodes.last()

    private var prevListDepth = 0
    private var listDepth = 0

    private var newParagraph = true

    override fun visitText(text: String) {
        if (text.matches(Regex("\n+")))  {
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
            pushTag(span().withClass("underlined"))
        }
        if (style.isStrikethrough) {
            pushTag(span().withClass("underlined"))
        }
        if (style.color != null) {
            pushTag(span().withStyle("color: ${style.color!!.hexCode}"))
        }
    }

    override fun visitStyleEnd() {
        nodes.removeLast()
    }

    override fun visitBlockQuote() {
        pushTag(div().withClass("blockQuote"))
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
    }

    public fun visitTemplate(template: (pagePath: Path, extraResourcesDir: Path) -> DomContent) {
        nodesTop.with(template(pagePath, extraResourcesDir))
    }

    public fun visitComponent(component: Component, scale: Int = 1) {
        val outDir = extraResourcesDir.resolve("component").resolve("${UUID.randomUUID()}.png")
        outDir.parent.toFile().mkdirs()
        nodesTop.with(owo(component, pagePath, outDir, 500, scale))
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
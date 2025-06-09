package mod.master_bw3.coleus.lavender.compiler

import io.wispforest.lavendermd.compiler.MarkdownCompiler
import j2html.TagCreator.*
import j2html.rendering.IndentedHtml
import j2html.tags.ContainerTag
import j2html.tags.specialized.DivTag
import j2html.tags.specialized.HtmlTag
import net.minecraft.text.Style
import net.minecraft.util.Identifier
import java.util.*
import java.util.function.UnaryOperator
import kotlin.collections.ArrayDeque
import kotlin.math.min

class HtmlCompiler<T : Appendable>(private val out: T) : MarkdownCompiler<T> {

    private var root: ContainerTag<*> = div()
    private var nodes: ArrayDeque<ContainerTag<*>> = ArrayDeque(listOf(root))
    private val nodesTop: ContainerTag<*>
        get() = nodes.last()

    private var listDepth = 0
    private var emptyLineCount = 0

    override fun visitText(text: String) {
        text.split("\n").forEach { line ->
            if (line.isBlank()) {
                if (emptyLineCount < 2) {
                    nodesTop.with(br())
                }
                emptyLineCount++
            } else {
                emptyLineCount = 0;

                if (listDepth > 0) {
                    nodesTop.with(li(line))
                } else {
                    nodesTop.withText(line)
                }
            }
        }
    }

    override fun visitStyle(styleFn: UnaryOperator<Style>) {
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
        nodesTop.with(div().withClass("horizontalRule"))
    }

    override fun visitImage(image: Identifier, description: String, fit: Boolean) {
        nodesTop.with(img().withSrc("/${image.namespace}/${image.path}").withAlt(description))
    }

    override fun visitListItem(ordinal: OptionalInt) {
        if (ordinal.isPresent) {
            pushTag(ol().withStart(ordinal.asInt.toString()))
        } else {
            pushTag(ul())
        }
        listDepth++
    }

    override fun visitListItemEnd() {
        nodes.removeLast()
        listDepth--
    }

    override fun compile(): T {
        return root.render(IndentedHtml.into(out))
    }

    override fun name(): String {
        return "coleus_html"
    }

    private fun <TAG : ContainerTag<*>> pushTag(tag: TAG): TAG {
        nodesTop.with(tag)
        nodes.add(tag)
        return tag
    }

}
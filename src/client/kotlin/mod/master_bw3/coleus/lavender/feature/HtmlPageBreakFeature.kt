package mod.master_bw3.coleus.lavender.feature

import io.wispforest.lavendermd.Lexer
import io.wispforest.lavendermd.MarkdownFeature
import io.wispforest.lavendermd.MarkdownFeature.NodeRegistrar
import io.wispforest.lavendermd.MarkdownFeature.TokenRegistrar
import io.wispforest.lavendermd.Parser
import io.wispforest.lavendermd.compiler.MarkdownCompiler
import mod.master_bw3.coleus.lavender.compiler.HtmlPageCompiler

public class HtmlPageBreakFeature : MarkdownFeature {
    override fun name(): String {
        return "html_page_breaks"
    }

    override fun supportsCompiler(compiler: MarkdownCompiler<*>?): Boolean {
        return compiler is HtmlPageCompiler
    }

    override fun registerTokens(registrar: TokenRegistrar) {
        registrar.registerToken({ nibbler, tokens ->
            if (!nibbler.expect(-1, '\n') || !nibbler.expect(-2, '\n')) return@registerToken false
            if (!nibbler.tryConsume(";;;;;\n\n")) return@registerToken false

            tokens.add(PageBreakToken())
            true
        }, ';')
    }

    override fun registerNodes(registrar: NodeRegistrar) {
        registrar.registerNode(
            { parser, trigger, tokens -> PageBreakNode() },
            { token, tokenListNibbler -> token as? PageBreakToken }
        )
    }

    private class PageBreakToken : Lexer.Token(";;;;;\n\n") {
        override fun isBoundary(): Boolean {
            return true
        }
    }

    private class PageBreakNode : Parser.Node() {
        override fun visitStart(compiler: MarkdownCompiler<*>) {
            (compiler as HtmlPageCompiler).visitPageBreak()
        }

        override fun visitEnd(compiler: MarkdownCompiler<*>?) {}
    }
}

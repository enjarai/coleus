package mod.master_bw3.coleus.lavender.feature

import com.mojang.logging.LogUtils
import io.wispforest.lavendermd.Lexer
import io.wispforest.lavendermd.Lexer.LexFunction
import io.wispforest.lavendermd.MarkdownFeature
import io.wispforest.lavendermd.MarkdownFeature.NodeRegistrar
import io.wispforest.lavendermd.MarkdownFeature.TokenRegistrar
import io.wispforest.lavendermd.Parser
import io.wispforest.lavendermd.Parser.ParseFunction
import io.wispforest.lavendermd.compiler.MarkdownCompiler
import io.wispforest.lavendermd.util.ListNibbler
import io.wispforest.lavendermd.util.StringNibbler
import j2html.tags.DomContent
import mod.master_bw3.coleus.htmlBook.HtmlTemplateRegistry
import mod.master_bw3.coleus.lavender.compiler.HtmlCompiler
import net.minecraft.util.Identifier
import org.slf4j.Logger
import java.nio.file.Path
import java.util.function.BiFunction

class HtmlTemplateFeature(
    private val templateSource: TemplateProvider = TemplateProvider { modelId: Identifier, templateName: String, templateParams: Map<String, String> ->
        val templateId = Identifier.of(modelId.namespace, templateName)
        val templateExpander = HtmlTemplateRegistry.registry[templateId]
        if (templateExpander == null) throw Exception("No template with id '$templateId is currently loaded")

        return@TemplateProvider { pagePath: Path, extraResourcesDir: Path ->
            templateExpander(templateParams, pagePath, extraResourcesDir)
        }
    }
) : MarkdownFeature {
    override fun name(): String {
        return "html_templates"
    }

    override fun supportsCompiler(compiler: MarkdownCompiler<*>): Boolean {
        return compiler is HtmlCompiler
    }

    override fun registerTokens(registrar: TokenRegistrar) {
        registrar.registerToken(LexFunction { nibbler: StringNibbler, tokens: MutableList<Lexer.Token> ->
            nibbler.skip()
            if (!nibbler.tryConsume('|')) return@LexFunction false

            val templateLocation = nibbler.consumeUntil('|')
            if (templateLocation == null) return@LexFunction false

            val splitLocation: List<String> = templateLocation.split("@")
            if (splitLocation.size != 2) return@LexFunction false

            val modelId = Identifier.tryParse(splitLocation[1])
            if (modelId == null) return@LexFunction false

            var templateParams: String? = ""
            if (!nibbler.tryConsume('>')) {
                templateParams = nibbler.consumeUntil('|')
                if (templateParams == null || !nibbler.tryConsume('>')) return@LexFunction false
            } else {
                nibbler.skip()
            }

            tokens.add(TemplateToken(modelId, splitLocation[0], templateParams!!))
            true
        }, '<')
    }

    override fun registerNodes(registrar: NodeRegistrar) {
        registrar.registerNode<TemplateToken>(
            ParseFunction { parser: Parser, templateToken: TemplateToken, tokens: ListNibbler<Lexer.Token> ->
                TemplateNode(
                    templateToken.modelId, templateToken.templateName, templateToken.params
                )
            },
            BiFunction { token: Lexer.Token, tokens: ListNibbler<Lexer.Token> -> token as? TemplateToken }
        )
    }

    private class TemplateToken(val modelId: Identifier, val templateName: String, val params: String) :
        Lexer.Token("<|$modelId|$params|>") {
        override fun isBoundary(): Boolean {
            return true
        }
    }

    private inner class TemplateNode(
        private val modelId: Identifier,
        private val templateName: String,
        private val params: String
    ) : Parser.Node() {
        override fun visitStart(compiler: MarkdownCompiler<*>) {
            var paramReader = StringNibbler(params);
            var builtParams = HashMap<String, String>();

            while (paramReader.hasNext()) {
                var paramName = paramReader.consumeUntil('=')!!;
                var paramValue = paramReader.consumeEscapedString(',', true)!!;

                builtParams[paramName] = paramValue;
            }

            (compiler as HtmlCompiler).visitTemplate(
                this@HtmlTemplateFeature.templateSource.template(
                    modelId,
                    templateName,
                    builtParams
                )
            );
        }


        override fun visitEnd(compiler: MarkdownCompiler<*>) {}
    }

    fun interface TemplateProvider {
        fun template(
            templateId: Identifier,
            templateName: String,
            templateParams: MutableMap<String, String>
        ): (pagePath: Path, extraResourcesDir: Path) -> DomContent
    }

    companion object {
        private val LOGGER: Logger = LogUtils.getLogger()
    }
}

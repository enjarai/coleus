package mod.master_bw3.coleus.lavender.feature

import io.wispforest.lavendermd.Lexer
import io.wispforest.lavendermd.Lexer.LexFunction
import io.wispforest.lavendermd.MarkdownFeature
import io.wispforest.lavendermd.MarkdownFeature.NodeRegistrar
import io.wispforest.lavendermd.MarkdownFeature.TokenRegistrar
import io.wispforest.lavendermd.Parser
import io.wispforest.lavendermd.Parser.ParseFunction
import io.wispforest.lavendermd.compiler.MarkdownCompiler
import io.wispforest.lavendermd.feature.OwoUITemplateFeature
import io.wispforest.lavendermd.util.ListNibbler
import io.wispforest.lavendermd.util.StringNibbler
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.parsing.UIModelLoader
import io.wispforest.owo.ui.parsing.UIModelParsingException
import j2html.TagCreator.p
import j2html.tags.DomContent
import mod.master_bw3.coleus.Components
import mod.master_bw3.coleus.Components.owo
import mod.master_bw3.coleus.HtmlTemplateRegistry
import mod.master_bw3.coleus.lavender.compiler.HtmlCompiler
import net.minecraft.util.Identifier
import java.nio.file.Path
import java.util.UUID
import java.util.function.BiFunction

internal typealias TemplateFn = (pagePath: Path, extraResourcesDir: Path) -> DomContent

public class HtmlTemplateFeature(
    private val htmlTemplateSource: HtmlTemplateProvider = defaultHtmlTemplateProvider,
    private val owoUITemplateSource: OwoUITemplateFeature.TemplateProvider = defaultOwoUITemplateFeature,
    private val extraParams: Map<String, String> = mapOf()
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
            var paramReader = StringNibbler(params)
            var builtParams = HashMap<String, String>()

            while (paramReader.hasNext()) {
                var paramName = paramReader.consumeUntil('=')!!
                var paramValue = paramReader.consumeEscapedString(',', true)!!

                builtParams[paramName] = paramValue
            }
            builtParams += extraParams

            var template = this@HtmlTemplateFeature.htmlTemplateSource.template(
                modelId,
                templateName,
                builtParams
            ) ?: { pagePath: Path, extraResourcesDir: Path ->
                p("no html template registered for $modelId $templateName")
                    .withStyle("color: red")
            }

//            if (template == null) {
//                val component = this@HtmlTemplateFeature.owoUITemplateSource.template(
//                    modelId,
//                    Component::class.java,
//                    templateName,
//                    builtParams,
//                ) ?: throw Exception("no template found for $modelId")
//
//                template = { pagePath: Path, extraResourcesDir: Path ->
//                    val imagePath = extraResourcesDir.resolve(modelId.namespace).resolve("${modelId.path}_${UUID.randomUUID()}.png")
//                    owo(component, pagePath, imagePath, 500)
//                }
//            } TODO: owo component to image


            (compiler as HtmlCompiler).visitTemplate(template)
        }

        override fun visitEnd(compiler: MarkdownCompiler<*>) {}
    }

    public fun interface HtmlTemplateProvider {
        public fun template(
            templateId: Identifier,
            templateName: String,
            templateParams: MutableMap<String, String>
        ): TemplateFn?
    }

    private companion object {
        private val defaultHtmlTemplateProvider =
            HtmlTemplateProvider { modelId: Identifier, templateName: String, templateParams: Map<String, String> ->
                val templateId = Identifier.of(modelId.namespace, templateName)
                val templateExpander = HtmlTemplateRegistry.registry[templateId]
                if (templateExpander == null) return@HtmlTemplateProvider null

                return@HtmlTemplateProvider { pagePath: Path, extraResourcesDir: Path ->
                    templateExpander.expand(templateParams, pagePath, extraResourcesDir)
                }
            }

        private val defaultOwoUITemplateFeature = object : OwoUITemplateFeature.TemplateProvider {
            override fun <C : Component> template(
                model: Identifier,
                expectedClass: Class<C>,
                templateName: String,
                templateParams: Map<String, String>
            ): C? {
                var uiModel = UIModelLoader.get(model);
                if (uiModel == null) return null

                return uiModel.expandTemplate(expectedClass, templateName, templateParams);
            }
        }
    }
}

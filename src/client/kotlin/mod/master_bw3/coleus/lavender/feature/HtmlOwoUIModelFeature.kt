package mod.master_bw3.coleus.lavender.feature

import io.wispforest.lavender.Lavender
import io.wispforest.lavendermd.Lexer
import io.wispforest.lavendermd.MarkdownFeature
import io.wispforest.lavendermd.MarkdownFeature.NodeRegistrar
import io.wispforest.lavendermd.MarkdownFeature.TokenRegistrar
import io.wispforest.lavendermd.Parser
import io.wispforest.lavendermd.compiler.MarkdownCompiler
import io.wispforest.lavendermd.util.ListNibbler
import io.wispforest.lavendermd.util.StringNibbler
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.Insets
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.Surface
import io.wispforest.owo.ui.parsing.UIModel
import mod.master_bw3.coleus.lavender.compiler.HtmlCompiler
import net.minecraft.text.Text
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

public class HtmlOwoUIModelFeature public constructor() : MarkdownFeature {
    override fun name(): String {
        return "owo_ui_models"
    }

    override fun supportsCompiler(compiler: MarkdownCompiler<*>): Boolean {
        return compiler is HtmlCompiler
    }

    override fun registerTokens(registrar: TokenRegistrar) {
        registrar.registerToken({ nibbler: StringNibbler, tokens: MutableList<Lexer.Token> ->
            if (!nibbler.tryConsume("```xml owo-ui")) return@registerToken false
            val content = nibbler.consumeUntil('`')
            if (content == null || !nibbler.tryConsume("``")) return@registerToken false

            tokens.add(UIModelToken(content, content))
            true
        }, '`')
    }

    override fun registerNodes(registrar: NodeRegistrar) {
        registrar.registerNode(
            { parser: Parser, stackToken: UIModelToken, tokens: ListNibbler<Lexer.Token?>? -> UIModelNode(stackToken.xmlContent) },
            { token: Lexer.Token, tokens: ListNibbler<Lexer.Token?>? -> if (token is UIModelToken) token else null }
        )
    }

    private class UIModelToken(content: String?, val xmlContent: String) : Lexer.Token(content)

    private class UIModelNode(xmlContent: String) : Parser.Node() {
        private val modelString: String

        init {
            this.modelString = MODEL_TEMPLATE.replaceFirst("\\{\\{template-content}}".toRegex(), xmlContent)
        }

        override fun visitStart(compiler: MarkdownCompiler<*>) {
            try {
                val model = UIModel.load(ByteArrayInputStream(modelString.toByteArray(StandardCharsets.UTF_8)))
                (compiler as HtmlCompiler).visitComponent(
                    model.expandTemplate(
                        Component::class.java,
                        "__model-feature-generated__",
                        mapOf()
                    ), ""
                )
            } catch (e: Exception) {
                Lavender.LOGGER.warn("Failed to build owo-ui model markdown element", e)
                (compiler as HtmlCompiler).visitComponent(
                    Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                        .child(Components.label(Text.literal(e.message)).horizontalSizing(Sizing.fill(100)))
                        .padding(Insets.of(10))
                        .surface(Surface.flat(0x77A00000).and(Surface.outline(0x77FF0000))),
                    "model-load-error"
                )
            }
        }

        override fun visitEnd(compiler: MarkdownCompiler<*>?) {}

        companion object {
            private val MODEL_TEMPLATE = """
                <owo-ui>
                    <templates>
                        <template name="__model-feature-generated__">
                            {{template-content}}
                        </template>
                    </templates>
                </owo-ui>
                
                """.trimIndent()
        }
    }
}

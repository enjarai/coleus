package mod.master_bw3.coleus.lavender.feature

import com.mojang.brigadier.exceptions.CommandSyntaxException
import io.wispforest.lavendermd.Lexer
import io.wispforest.lavendermd.MarkdownFeature
import io.wispforest.lavendermd.MarkdownFeature.NodeRegistrar
import io.wispforest.lavendermd.MarkdownFeature.TokenRegistrar
import io.wispforest.lavendermd.Parser
import io.wispforest.lavendermd.compiler.MarkdownCompiler
import io.wispforest.lavendermd.compiler.OwoUICompiler
import io.wispforest.lavendermd.util.ListNibbler
import io.wispforest.lavendermd.util.StringNibbler
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.Sizing
import mod.master_bw3.coleus.lavender.compiler.HtmlCompiler
import net.minecraft.command.argument.BlockArgumentParser
import net.minecraft.command.argument.BlockArgumentParser.BlockResult
import net.minecraft.registry.Registries

public class HtmlBlockStateFeature public constructor() : MarkdownFeature {
    override fun name(): String {
        return "block_states"
    }

    override fun supportsCompiler(compiler: MarkdownCompiler<*>): Boolean {
        return compiler is HtmlCompiler
    }

    override fun registerTokens(registrar: TokenRegistrar) {
        registrar.registerToken({ nibbler: StringNibbler, tokens: MutableList<Lexer.Token> ->
            if (!nibbler.tryConsume("<block;")) return@registerToken false
            val blockStateString = nibbler.consumeUntil('>') ?: return@registerToken false
            try {
                tokens.add(
                    BlockStateToken(
                        blockStateString,
                        BlockArgumentParser.block(Registries.BLOCK.readOnlyWrapper, blockStateString, true)
                    )
                )
                return@registerToken true
            } catch (e: CommandSyntaxException) {
                return@registerToken false
            }
        }, '<')
    }

    override fun registerNodes(registrar: NodeRegistrar) {
        registrar.registerNode(
            { parser: Parser, stateToken: BlockStateToken, tokens: ListNibbler<Lexer.Token?>? ->
                BlockStateNode(
                    stateToken.state
                )
            },
            { token: Lexer.Token?, tokens: ListNibbler<Lexer.Token> -> if (token is BlockStateToken) token else null }
        )
    }

    private class BlockStateToken(content: String, val state: BlockResult) : Lexer.Token(content)

    private class BlockStateNode(private val state: BlockResult) : Parser.Node() {
        override fun visitStart(compiler: MarkdownCompiler<*>) {
            (compiler as HtmlCompiler).visitComponent(
                Components.block(
                    state.blockState(),
                    state.nbt()
                ).sizing(Sizing.fixed(48)), "blockstate",2
            )
        }

        override fun visitEnd(compiler: MarkdownCompiler<*>?) {}
    }
}

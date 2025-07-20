package mod.master_bw3.coleus.lavender.feature

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import io.wispforest.lavendermd.Lexer
import io.wispforest.lavendermd.MarkdownFeature
import io.wispforest.lavendermd.MarkdownFeature.NodeRegistrar
import io.wispforest.lavendermd.MarkdownFeature.TokenRegistrar
import io.wispforest.lavendermd.Parser
import io.wispforest.lavendermd.compiler.MarkdownCompiler
import io.wispforest.lavendermd.util.ListNibbler
import io.wispforest.lavendermd.util.StringNibbler
import io.wispforest.owo.ui.component.Components
import mod.master_bw3.coleus.lavender.compiler.HtmlPageCompiler
import net.minecraft.command.argument.ItemStringReader
import net.minecraft.item.ItemStack
import net.minecraft.registry.RegistryWrapper.WrapperLookup

public class HtmlItemStackFeature public constructor(private val registries: WrapperLookup) : MarkdownFeature {
    override fun name(): String {
        return "item_stacks"
    }

    override fun supportsCompiler(compiler: MarkdownCompiler<*>): Boolean {
        return compiler is HtmlPageCompiler
    }

    override fun registerTokens(registrar: TokenRegistrar) {
        registrar.registerToken({ nibbler: StringNibbler, tokens: MutableList<Lexer.Token> ->
            if (!nibbler.tryConsume("<item;")) return@registerToken false
            val itemStackString = nibbler.consumeUntil('>') ?: return@registerToken false
            try {
                val result = ItemStringReader(this.registries).consume(StringReader(itemStackString))

                val stack = result.item().value().defaultStack
                stack.applyUnvalidatedChanges(result.components())

                tokens.add(ItemStackToken(itemStackString, stack))
                return@registerToken true
            } catch (e: CommandSyntaxException) {
                return@registerToken false
            }
        }, '<')
    }

    override fun registerNodes(registrar: NodeRegistrar) {
        registrar.registerNode(
            { parser: Parser, stackToken: ItemStackToken, tokens: ListNibbler<Lexer.Token?>? ->
                ItemStackNode(
                    stackToken.stack
                )
            },
            { token: Lexer.Token, tokens: ListNibbler<Lexer.Token> -> if (token is ItemStackToken) token else null }
        )
    }

    private class ItemStackToken(content: String?, val stack: ItemStack) : Lexer.Token(content)

    private class ItemStackNode(private val stack: ItemStack) : Parser.Node() {
        override fun visitStart(compiler: MarkdownCompiler<*>) {
            (compiler as HtmlPageCompiler).visitComponent(
                Components.item(this.stack).setTooltipFromStack(true),
                "itemstack",
                5
            )
        }

        override fun visitEnd(compiler: MarkdownCompiler<*>?) {}
    }
}

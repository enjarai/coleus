package mod.master_bw3.coleus.lavender.feature

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
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
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.core.Sizing
import mod.master_bw3.coleus.lavender.compiler.HtmlPageCompiler
import net.minecraft.entity.EntityType
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.StringNbtReader
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import java.util.function.BiFunction

public class HtmlEntityFeature public constructor() : MarkdownFeature {
    override fun name(): String {
        return "entities"
    }

    override fun supportsCompiler(compiler: MarkdownCompiler<*>?): Boolean {
        return compiler is HtmlPageCompiler
    }

    override fun registerTokens(registrar: TokenRegistrar) {
        registrar.registerToken(LexFunction { nibbler: StringNibbler?, tokens: MutableList<Lexer.Token?>? ->
            if (!nibbler!!.tryConsume("<entity;")) return@LexFunction false
            var entityString = nibbler.consumeUntil('>')
            if (entityString == null) return@LexFunction false
            try {
                var nbt: NbtCompound? = null

                val nbtIndex = entityString.indexOf('{')
                if (nbtIndex != -1) {
                    nbt = StringNbtReader(StringReader(entityString.substring(nbtIndex))).parseCompound()
                    entityString = entityString.substring(0, nbtIndex)
                }

                val entityType = Registries.ENTITY_TYPE.getOrEmpty(Identifier.of(entityString)).orElseThrow()
                tokens!!.add(EntityToken(entityString, entityType, nbt))
                return@LexFunction true
            } catch (e: CommandSyntaxException) {
                return@LexFunction false
            } catch (e: NoSuchElementException) {
                return@LexFunction false
            }
        }, '<')
    }

    override fun registerNodes(registrar: NodeRegistrar) {
        registrar.registerNode<EntityToken?>(
            ParseFunction { parser: Parser?, entityToken: EntityToken?, tokens: ListNibbler<Lexer.Token?>? ->
                EntityNode(
                    entityToken!!.type, entityToken.nbt
                )
            },
            BiFunction { token: Lexer.Token?, tokens: ListNibbler<Lexer.Token?>? -> token as? EntityToken }
        )
    }

    private class EntityToken public constructor(
        content: String?,
        public val type: EntityType<*>,
        public val nbt: NbtCompound?
    ) : Lexer.Token(content)

    private class EntityNode public constructor(public val type: EntityType<*>, public val nbt: NbtCompound?) :
        Parser.Node() {
        override fun visitStart(compiler: MarkdownCompiler<*>) {
            (compiler as HtmlPageCompiler).visitComponent(
                Components.entity(Sizing.fixed(32), this.type, this.nbt).scaleToFit(true),
                "entity"
            )
        }

        override fun visitEnd(compiler: MarkdownCompiler<*>?) {}
    }
}

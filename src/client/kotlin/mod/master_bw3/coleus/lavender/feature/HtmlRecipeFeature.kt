package mod.master_bw3.coleus.lavender.feature

import io.wispforest.lavender.md.compiler.BookCompiler.ComponentSource
import io.wispforest.lavender.md.features.RecipeFeature
import io.wispforest.lavender.md.features.RecipeFeature.RecipePreviewBuilder
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
import io.wispforest.owo.ui.core.*
import mod.master_bw3.coleus.lavender.compiler.HtmlPageCompiler
import net.minecraft.client.MinecraftClient
import net.minecraft.recipe.*
import net.minecraft.recipe.input.RecipeInput
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier

public class HtmlRecipeFeature(
    private val bookComponentSource: ComponentSource,
    previewBuilders: Map<RecipeType<*>, RecipeFeature.RecipePreviewBuilder<*>>?
) :
    MarkdownFeature {
    private val previewBuilders: MutableMap<RecipeType<*>, RecipeFeature.RecipePreviewBuilder<*>>

    init {
        this.previewBuilders = HashMap(
            previewBuilders ?: java.util.Map.of()
        )
        this.previewBuilders.putIfAbsent(RecipeType.CRAFTING, RecipeFeature.CRAFTING_PREVIEW_BUILDER)
        this.previewBuilders.putIfAbsent(RecipeType.SMELTING, RecipeFeature.SMELTING_PREVIEW_BUILDER)
        this.previewBuilders.putIfAbsent(RecipeType.BLASTING, RecipeFeature.SMELTING_PREVIEW_BUILDER)
        this.previewBuilders.putIfAbsent(RecipeType.SMOKING, RecipeFeature.SMELTING_PREVIEW_BUILDER)
        this.previewBuilders.putIfAbsent(RecipeType.CAMPFIRE_COOKING, RecipeFeature.SMELTING_PREVIEW_BUILDER)
        this.previewBuilders.putIfAbsent(RecipeType.SMITHING, RecipeFeature.SMITHING_PREVIEW_BUILDER)
        this.previewBuilders.putIfAbsent(RecipeType.STONECUTTING, RecipeFeature.STONECUTTING_PREVIEW_BUILDER)
    }

    override fun name(): String {
        return "recipes"
    }

    override fun supportsCompiler(compiler: MarkdownCompiler<*>): Boolean {
        return compiler is HtmlPageCompiler
    }

    override fun registerTokens(registrar: TokenRegistrar) {
        registrar.registerToken({ nibbler: StringNibbler, tokens: MutableList<Lexer.Token> ->
            if (!nibbler.tryConsume("<recipe;")) return@registerToken false
            val recipeIdString = nibbler.consumeUntil('>') ?: return@registerToken false

            val recipeId = Identifier.tryParse(recipeIdString) ?: return@registerToken false

            val recipe = MinecraftClient.getInstance().world!!.recipeManager[recipeId]
            if (recipe.isEmpty) return@registerToken false

            tokens.add(RecipeToken(recipeIdString, recipe.get() as RecipeEntry<Recipe<*>>))
            true
        }, '<')
    }

    override fun registerNodes(registrar: NodeRegistrar) {
        registrar.registerNode(
            { parser: Parser, recipeToken: RecipeToken, tokens: ListNibbler<Lexer.Token> ->
                RecipeNode<RecipeInput>(
                    recipeToken.recipe as RecipeEntry<Recipe<RecipeInput>>
                )
            },
            { token: Lexer.Token, tokens: ListNibbler<Lexer.Token> -> if (token is RecipeToken) token else null }
        )
    }

    private class RecipeToken(content: String, val recipe: RecipeEntry<Recipe<*>>) : Lexer.Token(content)

    private inner class RecipeNode<T : RecipeInput>(private val recipe: RecipeEntry<Recipe<T>>) : Parser.Node() {
        override fun visitStart(compiler: MarkdownCompiler<*>) {
            val previewBuilder = previewBuilders[recipe.value().type] as RecipePreviewBuilder<Recipe<T>>?
            if (previewBuilder != null) {
                (compiler as HtmlPageCompiler).visitComponent(
                    previewBuilder.buildRecipePreview(
                        this@HtmlRecipeFeature.bookComponentSource,
                        this.recipe
                    ), "recipe-preview",4
                )
            } else {
                (compiler as HtmlPageCompiler).visitComponent(
                    Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                        .child(
                            Components.label(
                                Text.literal(
                                    "No preview builder registered for recipe type '" + Registries.RECIPE_TYPE.getId(
                                        recipe.value().type
                                    ) + "'"
                                )
                            ).horizontalSizing(Sizing.fill(100))
                        )
                        .padding(Insets.of(10))
                        .surface(Surface.flat(0x77A00000).and(Surface.outline(0x77FF0000))),
                    "recipe-preview-load-error"
                )
            }
        }

        override fun visitEnd(compiler: MarkdownCompiler<*>) {}
    }
}
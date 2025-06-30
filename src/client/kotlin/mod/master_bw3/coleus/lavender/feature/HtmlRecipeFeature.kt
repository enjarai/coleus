package mod.master_bw3.coleus.lavender.feature

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
import mod.master_bw3.coleus.lavender.compiler.HtmlCompiler
import net.minecraft.client.MinecraftClient
import net.minecraft.recipe.*
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import java.nio.file.Path
import java.util.Map
import java.util.function.BiFunction
import j2html.TagCreator.*
import mod.master_bw3.coleus.Components
import net.minecraft.registry.DynamicRegistryManager


public class HtmlRecipeFeature(
    private val previewBuilders: MutableMap<RecipeType<*>, RecipePreviewBuilder> = Map.of<RecipeType<*>, RecipePreviewBuilder>(),
    private val registryManager: DynamicRegistryManager
) : MarkdownFeature {

    init {
        this.previewBuilders.putIfAbsent(RecipeType.CRAFTING, CRAFTING_PREVIEW_BUILDER)
        this.previewBuilders.putIfAbsent(RecipeType.SMELTING, SMELTING_PREVIEW_BUILDER)
        this.previewBuilders.putIfAbsent(RecipeType.BLASTING, SMELTING_PREVIEW_BUILDER)
        this.previewBuilders.putIfAbsent(RecipeType.SMOKING, SMELTING_PREVIEW_BUILDER)
        this.previewBuilders.putIfAbsent(RecipeType.CAMPFIRE_COOKING, SMELTING_PREVIEW_BUILDER)
        this.previewBuilders.putIfAbsent(RecipeType.SMITHING, SMITHING_PREVIEW_BUILDER)
        this.previewBuilders.putIfAbsent(RecipeType.STONECUTTING, STONECUTTING_PREVIEW_BUILDER)
    }

    override fun name(): String {
        return "html_recipes"
    }

    override fun supportsCompiler(compiler: MarkdownCompiler<*>): Boolean {
        return compiler is HtmlCompiler
    }

    override fun registerTokens(registrar: TokenRegistrar) {
        registrar.registerToken(LexFunction { nibbler: StringNibbler, tokens: MutableList<Lexer.Token> ->
            if (!nibbler.tryConsume("<recipe;")) return@LexFunction false
            val recipeIdString = nibbler.consumeUntil('>')
            if (recipeIdString == null) return@LexFunction false

            val recipeId = Identifier.tryParse(recipeIdString)
            if (recipeId == null) return@LexFunction false

            val recipe = MinecraftClient.getInstance().world!!.recipeManager.get(recipeId)
            if (recipe.isEmpty) return@LexFunction false

            @Suppress("UNCHECKED_CAST")
            tokens.add(RecipeToken(recipeIdString, recipe.get() as RecipeEntry<Recipe<*>>))
            true
        }, '<')
    }

    override fun registerNodes(registrar: NodeRegistrar) {
        registrar.registerNode<RecipeToken>(
            ParseFunction { parser: Parser, recipeToken: RecipeToken, tokens: ListNibbler<Lexer.Token> ->
                RecipeNode(
                    recipeToken.recipe
                )
            },
            BiFunction { token: Lexer.Token, tokens: ListNibbler<Lexer.Token> -> token as? RecipeToken }
        )
    }

    private class RecipeToken(content: String, val recipe: RecipeEntry<Recipe<*>>) : Lexer.Token(content)

    private inner class RecipeNode(private val recipe: RecipeEntry<Recipe<*>>) : Parser.Node() {
        override fun visitStart(compiler: MarkdownCompiler<*>) {
            val previewBuilder = this@HtmlRecipeFeature.previewBuilders[this.recipe.value().type]
            if (previewBuilder != null) {
                (compiler as HtmlCompiler).visitTemplate(
                    previewBuilder.buildRecipePreview(this.recipe, registryManager)
                )
            } else {
                (compiler as HtmlCompiler).visitTemplate { _, _ ->
                    div(
                        h2(
                            "No preview builder registered for recipe type '" + Registries.RECIPE_TYPE.getId(
                                this.recipe.value()!!.getType()
                            ) + "'"
                        )
                    ).withClass("unregistered-recipe-warning")
                }
            }
        }

        override fun visitEnd(compiler: MarkdownCompiler<*>?) {}
    }

    public interface RecipePreviewBuilder {
        public fun buildRecipePreview(
            recipeEntry: RecipeEntry<*>,
            registryManager: DynamicRegistryManager
        ): (pagePath: Path, extraResourcesDir: Path) -> DomContent

    }

    public companion object {
        public val CRAFTING_PREVIEW_BUILDER: RecipePreviewBuilder =
            object : RecipePreviewBuilder {
                override fun buildRecipePreview(
                    recipeEntry: RecipeEntry<*>,
                    registryManager: DynamicRegistryManager
                ): (pagePath: Path, extraResourcesDir: Path) -> DomContent {
                    val recipe = recipeEntry.value()
                    val ingredients = recipe.ingredients
                    val result = recipe.getResult(registryManager)
                    val resultId = Registries.ITEM.getId(result.item)

                    return { pagePath: Path, extraResourcesDir: Path ->
                        div().withClass("crafting-preview").with(
                            div().withClass("crafting-preview-ingredient-grid").with(
                                ingredients.map {
                                    it.matchingStacks.firstOrNull()?.let { stack ->
                                        val id = Registries.ITEM.getId(stack.item)
                                        Components.item(
                                            stack,
                                            pagePath,
                                            extraResourcesDir.resolve("item/${id.namespace}/${id.path}.png")
                                        )
                                    } ?: div()
                                }
                            ),
                            div().withClass("crafting-preview-arrow"),
                            div().withClass("crafting-preview-result").with(
                                Components.item(
                                    result,
                                    pagePath,
                                    extraResourcesDir.resolve("item/${resultId.namespace}/${resultId.path}.png")
                                )
                            )
                        )

                    }

                }
            }

        public val SMELTING_PREVIEW_BUILDER: RecipePreviewBuilder =
            object : RecipePreviewBuilder {
                override fun buildRecipePreview(
                    recipeEntry: RecipeEntry<*>,
                    registryManager: DynamicRegistryManager
                ): (pagePath: Path, extraResourcesDir: Path) -> DomContent {
                    return { pagePath: Path, extraResourcesDir: Path -> text("ohno") }
                }
            }

        public val SMITHING_PREVIEW_BUILDER: RecipePreviewBuilder =
            object : RecipePreviewBuilder {
                override fun buildRecipePreview(
                    recipeEntry: RecipeEntry<*>,
                    registryManager: DynamicRegistryManager
                ): (pagePath: Path, extraResourcesDir: Path) -> DomContent {
                    return { pagePath: Path, extraResourcesDir: Path -> text("ohno") }
                }
            }

        public val STONECUTTING_PREVIEW_BUILDER: RecipePreviewBuilder =
            object : RecipePreviewBuilder {
                override fun buildRecipePreview(
                    recipeEntry: RecipeEntry<*>,
                    registryManager: DynamicRegistryManager
                ): (pagePath: Path, extraResourcesDir: Path) -> DomContent {
                    return { pagePath: Path, extraResourcesDir: Path -> text("ohno") }
                }
            }

    }
}

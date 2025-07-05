package mod.master_bw3.coleus.internal

import io.wispforest.lavender.Lavender
import io.wispforest.lavender.book.Book
import io.wispforest.lavender.book.Category
import io.wispforest.lavender.book.Entry
import io.wispforest.lavender.md.compiler.BookCompiler.ComponentSource
import io.wispforest.lavendermd.MarkdownProcessor
import io.wispforest.lavendermd.feature.*
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.parsing.UIModel
import j2html.TagCreator.*
import j2html.rendering.FlatHtml
import j2html.tags.specialized.DivTag
import j2html.tags.specialized.OlTag
import mod.master_bw3.coleus.Base16Theme
import mod.master_bw3.coleus.ColeusClient
import mod.master_bw3.coleus.lavender.compiler.HtmlCompiler
import mod.master_bw3.coleus.lavender.feature.*
import mod.master_bw3.coleus.mixin.client.LavenderBookScreenAccessor
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.util.Identifier
import java.nio.file.Path
import kotlin.io.path.outputStream
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText

internal class HtmlBookGenerator(private val book: Book) {

    private val bookDir: Path =
        FabricLoader.getInstance().gameDir.resolve(ColeusClient.NAME).resolve(book.id().namespace)
            .resolve(book.id().path)

    private val assetDir: Path = bookDir.resolve("assets")


    internal fun generate() {

        bookDir.toFile().deleteRecursively()

        book.entries().forEach { entry ->
            generatePage(entry.id, entry.title, entry.content)
        }

        book.categories().forEach { category ->
            generatePage(category.id, category.title, category.content)
        }

        book.landingPage()?.run {
            generatePage(id, title, content, "index")
        }

        assetDir.toFile().mkdirs()
        includeResource(Identifier.of(ColeusClient.NAME, "style.css"), "style.css")
        includeResource(Identifier.of(ColeusClient.NAME, "font/karla/karla-variablefont_wght.ttf"), "Karla-VariableFont_wght.ttf")
        includeResource(Identifier.of(ColeusClient.NAME, "font/karla/ofl.txt"), "OFL.txt")

        writeThemeCss()
    }

    private fun includeResource(id: Identifier, outPath: String? = null) {
        val client = MinecraftClient.getInstance()
        val outFile = outPath?.let { assetDir.resolve(it) } ?: assetDir.resolve(id.path)
        outFile.parent.toFile().mkdirs()

        val fileWriter = outFile.outputStream()
        val resource = client.resourceManager.getResource(id).get()
        resource.inputStream.transferTo(fileWriter)
        fileWriter.close()
    }

    private fun writeThemeCss() {
        val client = MinecraftClient.getInstance()
        val outFile =  assetDir.resolve("theme.css")
        outFile.parent.toFile().mkdirs()
        val resource = client.resourceManager.getResource(Identifier.of(ColeusClient.NAME, "base16theme/gruvbox.json")).get()
        val css = Base16Theme.fromJsonResource(resource).toCss()
        outFile.writeText(css)
    }

    private fun generatePage(id: Identifier, title: String, content: String, filename: String? = null) {
        val world = MinecraftClient.getInstance().world
            ?: throw AssertionError("world must be present to generate book")

        val path = bookDir.resolve("${filename ?: id.path}.html")
        val file = path.toFile()
        file.parentFile.mkdirs()

        val bookTexture = book.texture() ?: Lavender.id("textures/gui/brown_book.png")
        val processor = MarkdownProcessor(
            { HtmlCompiler(path, bookDir, bookDir.resolve("assets")) },
            BasicFormattingFeature(),
            ColorFeature(),
            LinkFeature(),
            ListFeature(),
            BlockQuoteFeature(),
            HtmlPageBreakFeature(),
            HtmlTemplateFeature(extraParams = mapOf("book-texture" to bookTexture.toString())),
            HtmlRecipeFeature(template, LavenderBookScreenAccessor.getRecipeHandler()[book.id()]),
            HtmlOwoUIModelFeature(),
            HtmlItemStackFeature(world.registryManager),
            HtmlBlockStateFeature(),
            ImageFeature(),
        )

        val writer = file.writer()

        val html = html(
            head(
                link()
                    .withRel("stylesheet")
                    .withHref("${assetDir.resolve("theme.css").relativeTo(path.parent)}"),
                link()
                    .withRel("stylesheet")
                    .withHref("${assetDir.resolve("style.css").relativeTo(path.parent)}")
            ),
            body(
                sidebar(id),
                div(
                    main(
                        h1(title),
                        processor.process(content)
                    )
                ).withClass("page")
            )
        )

        html.render(FlatHtml.into(writer))
        writer.close()
    }


    private fun sidebar(currentPage: Identifier): DivTag {
        val sidebar = div(buildEntryList(book.entries().filter { it.categories.isEmpty() }, currentPage))
            .withClass("sidebar")
        return sidebar.with(buildCategoryList(book.categories(), currentPage))
    }

    private fun buildCategoryList(categories: Collection<Category>, currentPage: Identifier): OlTag {
        return unlabeledOl().with(categories.sortedBy(Category::ordinal).mapIndexed { index, category ->
            val a = if (category.id == currentPage)
                a(strong("${index + 1} ${category.title}"))
            else
                a(strong("${index + 1}"), text(" ${category.title}"))

            li(
                div(
                    a.withHref(
                        bookDir.resolve("${category.id.path}.html").relativeTo(bookDir.resolve(currentPage.path).parent)
                            .toString()
                    ),
                    book.entriesByCategory(category)?.let {
                        buildEntryList(it, currentPage, index)
                    }
                )
            )
        })
    }

    private fun buildEntryList(entries: Collection<Entry>, currentPage: Identifier, categoryIndex: Int? = null): OlTag {
        return unlabeledOl().with(
            entries.sortedBy(Entry::ordinal).mapIndexed { index, entry ->
                val a = a()
                if (categoryIndex != null) {
                    a.with(strong("${categoryIndex + 1}.${index + 1} "))
                }
                if (entry.id == currentPage) {
                    a.with(strong(entry.title))
                } else {
                    a.withText(entry.title)
                }

                li(
                    a.withHref(
                        bookDir.resolve(
                            "${entry.id.path}.html"
                        ).relativeTo(bookDir.resolve(currentPage.path).parent).toString()
                    )
                )
            })
    }

    private fun unlabeledOl(): OlTag = ol().withStyle("list-style-type: none")

    private val template = object : ComponentSource {
        override fun <C : Component> template(
            model: UIModel,
            expectedComponentClass: Class<C>,
            name: String,
            parameters: MutableMap<String, String>
        ): C {
            val params = HashMap<String, String>()
            params["book-texture"] = book.texture().toString()
            params.putAll(parameters)

            return model.expandTemplate(expectedComponentClass, name, params)
        }

    }
}
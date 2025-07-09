package mod.master_bw3.coleus.internal

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.wispforest.lavender.Lavender
import io.wispforest.lavender.book.Book
import io.wispforest.lavender.book.Category
import io.wispforest.lavender.book.Entry
import io.wispforest.lavender.md.compiler.BookCompiler.ComponentSource
import io.wispforest.lavendermd.MarkdownProcessor
import io.wispforest.lavendermd.compiler.TextCompiler
import io.wispforest.lavendermd.feature.*
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.parsing.UIModel
import j2html.TagCreator.*
import j2html.rendering.FlatHtml
import j2html.tags.UnescapedText
import j2html.tags.specialized.DivTag
import j2html.tags.specialized.OlTag
import mod.master_bw3.coleus.ColeusClient
import mod.master_bw3.coleus.SearchEntry
import mod.master_bw3.coleus.ThemeRegistry
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

    private val searchEntries: MutableList<SearchEntry> = mutableListOf()

    internal fun generate() {
        bookDir.toFile().deleteRecursively()

        book.entries().forEach { entry ->
            val path = bookDir.resolve("${entry.id.path}.html")
            generatePage(entry.id, entry.title, entry.content, path)
            generateSearchEntry(entry.id, entry.title, entry.content, path)
        }

        book.categories().forEach { category ->
            val path = bookDir.resolve("${category.id.path}.html")
            generatePage(category.id, category.title, category.content, path)
        }

        book.landingPage()?.run {
            val path = bookDir.resolve("index.html")
            generatePage(id, title, content, path)
        }

        assetDir.toFile().mkdirs()
        includeResource(Identifier.of(ColeusClient.NAME, "style.css"), "style.css")
        includeResource(
            Identifier.of(ColeusClient.NAME, "font/karla/karla-variablefont_wght.ttf"),
            "Karla-VariableFont_wght.ttf"
        )
        includeResource(Identifier.of(ColeusClient.NAME, "font/karla/ofl.txt"), "OFL.txt")
        includeResource(Identifier.of(ColeusClient.NAME, "search.js"), "search.js")
        includeThemes()

        val searchEntriesFile = assetDir.resolve("searchEntries.json").toFile()
        searchEntriesFile.writeText(GsonBuilder().create().toJson(searchEntries))
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

    private fun includeThemes() {
        val themeDir = assetDir.resolve("themes")
        themeDir.toFile().mkdirs()

        ThemeRegistry.forEach { (name, theme) ->
            val outFile = themeDir.resolve("${name}.css")
            val css = theme.toCss()
            outFile.writeText(css)
        }
    }

    private fun generateSearchEntry(id: Identifier, title: String, content: String, path: Path) {
        val processor = MarkdownProcessor(
            { TextCompiler() }, BasicFormattingFeature(),
            ColorFeature(),
            LinkFeature(),
            ListFeature(),
            BlockQuoteFeature(),
            ImageFeature(),
        )
        val body = processor.process(content).string

        searchEntries.add(
            SearchEntry(
                title,
                body,
                path.toString(),
            )
        )
    }

    private fun generatePage(id: Identifier, title: String, content: String, path: Path) {
        val world = MinecraftClient.getInstance().world
            ?: throw AssertionError("world must be present to generate book")

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
            ImageFeature(),
            HtmlPageBreakFeature(),
            HtmlTemplateFeature(extraParams = mapOf("book-texture" to bookTexture.toString())),
            HtmlRecipeFeature(template, LavenderBookScreenAccessor.getRecipeHandler()[book.id()]),
            HtmlOwoUIModelFeature(),
            HtmlItemStackFeature(world.registryManager),
            HtmlBlockStateFeature(),
        )

        val writer = file.writer()
        writer.write("<!DOCTYPE html>")
        val html = html(
            head(
                meta()
                    .withName("viewport")
                    .attr("content", "width=device-width,initial-scale=1"),
                link()
                    .withRel("stylesheet")
                    .withHref("${assetDir.resolve("themes/coleus:default.css").relativeTo(path.parent)}"),
                link()
                    .withRel("stylesheet")
                    .withHref("${assetDir.resolve("style.css").relativeTo(path.parent)}"),
                script()
                    .withType("module")
                    .withSrc("${assetDir.resolve("search.js").relativeTo(path.parent)}")
                    .attr("data-assetspath", assetDir.relativeTo(path.parent).toString())
                    .withId("search-script")
            ),
            body().with(
                sidebar(id).withId("sidebar").attr("data-open", "true"),
                div().withClass("page").with(
                    div().withClass("toolbar").with(
                        menuButton().withId("menu-button"),
                        searchButton().withId("search-button")
                    ),
                    main(
                        h1(title),
                        processor.process(content)
                    )
                )
            )
        )

        html.render(FlatHtml.into(writer))
        writer.close()
    }

    private fun sidebar(currentPage: Identifier): DivTag {
        val sidebar = div(buildEntryList(book.entries().filter { it.categories.isEmpty() }, currentPage))
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

    private fun menuButton() =
        button().attr(
            "onclick",
            """
            const btn = this;
            const sidebar = document.getElementById('sidebar');
            const visible = sidebar.dataset.open === 'true';
            sidebar.style.display = visible ? 'none' : 'block';
            sidebar.dataset.open = visible ? 'false' : 'true';
            """.trimIndent()
        ).with(UnescapedText(
            """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512">
               <!--!Font Awesome Free 6.7.2 by @fontawesome - https://fontawesome.com License - https://fontawesome.com/license/free Copyright 2025 Fonticons, Inc.-->
               <path d="M0 96C0 78.3 14.3 64 32 64l384 0c17.7 0 32 14.3 32 32s-14.3 32-32 32L32 128C14.3 128 0 113.7 0 96zM0 256c0-17.7 14.3-32 32-32l384 0c17.7 0 32 14.3 32 32s-14.3 32-32 32L32 288c-17.7 0-32-14.3-32-32zM448 416c0 17.7-14.3 32-32 32L32 448c-17.7 0-32-14.3-32-32s14.3-32 32-32l384 0c17.7 0 32 14.3 32 32z"/>
            </svg>
            """.trimIndent()
        )).attr("aria-label", "toggle menu")

    private fun searchButton() =
        button(UnescapedText(
            """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">
               <!--!Font Awesome Free 6.7.2 by @fontawesome - https://fontawesome.com License - https://fontawesome.com/license/free Copyright 2025 Fonticons, Inc.-->
               <path d="M416 208c0 45.9-14.9 88.3-40 122.7L502.6 457.4c12.5 12.5 12.5 32.8 0 45.3s-32.8 12.5-45.3 0L330.7 376c-34.4 25.2-76.8 40-122.7 40C93.1 416 0 322.9 0 208S93.1 0 208 0S416 93.1 416 208zM208 352a144 144 0 1 0 0-288 144 144 0 1 0 0 288z"/>
            </svg>
            """.trimIndent()
        ))
}
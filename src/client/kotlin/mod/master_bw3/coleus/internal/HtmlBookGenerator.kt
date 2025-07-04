package mod.master_bw3.coleus.internal

import io.wispforest.lavender.Lavender
import io.wispforest.lavender.book.Book
import io.wispforest.lavender.book.Category
import io.wispforest.lavender.book.Entry
import io.wispforest.lavendermd.MarkdownProcessor
import io.wispforest.lavendermd.feature.BasicFormattingFeature
import io.wispforest.lavendermd.feature.BlockQuoteFeature
import io.wispforest.lavendermd.feature.ColorFeature
import io.wispforest.lavendermd.feature.LinkFeature
import io.wispforest.lavendermd.feature.ListFeature
import j2html.TagCreator
import j2html.TagCreator.a
import j2html.TagCreator.strong
import j2html.rendering.FlatHtml
import j2html.tags.specialized.DivTag
import j2html.tags.specialized.OlTag
import mod.master_bw3.coleus.ColeusClient
import mod.master_bw3.coleus.lavender.compiler.HtmlCompiler
import mod.master_bw3.coleus.lavender.feature.HtmlPageBreakFeature
import mod.master_bw3.coleus.lavender.feature.HtmlRecipeFeature
import mod.master_bw3.coleus.lavender.feature.HtmlTemplateFeature
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.util.Identifier
import java.nio.file.Path
import kotlin.io.path.outputStream
import kotlin.io.path.relativeTo

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
        val cssFileWriter = assetDir.resolve("style.css").outputStream()
        val cssResource =
            MinecraftClient.getInstance().resourceManager.getResource(Identifier.of(ColeusClient.NAME, "style.css"))
                .get()
        cssResource.inputStream.transferTo(cssFileWriter)
        cssFileWriter.close()
    }

    private fun generatePage(id: Identifier, title: String, content: String, filename: String? = null) {
        val path = bookDir.resolve("${filename ?: id.path}.html")
        val file = path.toFile()
        file.parentFile.mkdirs()

        val bookTexture = book.texture() ?: Lavender.id("textures/gui/brown_book.png")
        val processor = MarkdownProcessor(
            { HtmlCompiler(path, bookDir.resolve("assets")) },
            BasicFormattingFeature(),
            ColorFeature(),
            LinkFeature(),
            ListFeature(),
            BlockQuoteFeature(),
            HtmlPageBreakFeature(),
            HtmlTemplateFeature(extraParams = mapOf("book-texture" to bookTexture.toString())),
            HtmlRecipeFeature(mutableMapOf(), MinecraftClient.getInstance().world!!.registryManager)
        )

        val writer = file.writer()

        val html = TagCreator.html(
            TagCreator.head(
                TagCreator.link()
                    .withRel("stylesheet")
                    .withHref("${assetDir.resolve("style.css").relativeTo(path.parent)}")
            ),
            TagCreator.body(
                sidebar(id),
                TagCreator.main(
                    TagCreator.h1(title),
                    processor.process(content)
                )
            )
        )

        html.render(FlatHtml.into(writer))
        writer.close()
    }


    private fun sidebar(currentPage: Identifier): DivTag {
        val sidebar = TagCreator.div(buildEntryList(book.entries().filter { it.categories.isEmpty() }, currentPage))
            .withClass("sidebar")
        return sidebar.with(buildCategoryList(book.categories(), currentPage))
    }

    private fun buildCategoryList(categories: Collection<Category>, currentPage: Identifier): OlTag {
        return unlabeledOl().with(categories.sortedBy(Category::ordinal).mapIndexed { index, category ->
            val a = if (category.id == currentPage)
                a(strong("${index + 1} ${category.title}"))
            else
                a(strong("${index + 1}"), TagCreator.text(" ${category.title}"))

            TagCreator.li(
                TagCreator.div(
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

                TagCreator.li(
                    a.withHref(
                        bookDir.resolve(
                            "${entry.id.path}.html"
                        ).relativeTo(bookDir.resolve(currentPage.path).parent).toString()
                    )
                )
            })
    }

    private fun unlabeledOl(): OlTag = TagCreator.ol().withStyle("list-style-type: none")

}
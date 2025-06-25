package mod.master_bw3.coleus.htmlBook

import io.wispforest.lavender.book.Book
import io.wispforest.lavender.book.Category
import io.wispforest.lavender.book.Entry
import io.wispforest.lavendermd.MarkdownProcessor
import io.wispforest.lavendermd.feature.BasicFormattingFeature
import io.wispforest.lavendermd.feature.BlockQuoteFeature
import io.wispforest.lavendermd.feature.ColorFeature
import io.wispforest.lavendermd.feature.LinkFeature
import io.wispforest.lavendermd.feature.ListFeature
import j2html.TagCreator.*
import j2html.rendering.IndentedHtml
import j2html.tags.specialized.DivTag
import j2html.tags.specialized.OlTag
import mod.master_bw3.coleus.Coleus
import mod.master_bw3.coleus.lavender.compiler.HtmlCompiler
import mod.master_bw3.coleus.lavender.feature.HtmlPageBreakFeature
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.resource.ResourceManager
import net.minecraft.util.Identifier
import java.io.File
import java.nio.file.Path
import kotlin.io.path.outputStream
import kotlin.io.path.relativeTo
import kotlin.io.path.writer

class HtmlBookGenerator(val book: Book) {

    val processor = MarkdownProcessor(
        ::HtmlCompiler,
        BasicFormattingFeature(),
        ColorFeature(),
        LinkFeature(),
        ListFeature(),
        BlockQuoteFeature(),
        HtmlPageBreakFeature()
    )

    val bookDir: Path by lazy {
        FabricLoader.getInstance().gameDir.resolve(Coleus.NAME).resolve(book.id().namespace).resolve(book.id().path)
    }

    fun generate() {

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

        val cssFileWriter =  bookDir.resolve("style.css").outputStream()
        val cssResource = MinecraftClient.getInstance().resourceManager.getResource(Identifier.of(Coleus.NAME, "style.css")).get()
        cssResource.inputStream.transferTo(cssFileWriter)
        cssFileWriter.close()
    }

    private fun generatePage(id: Identifier, title: String, content: String, filename: String? = null) {
        val path = bookDir.resolve("${filename ?: id.path}.html")
        val file = path.toFile()
        file.parentFile.mkdirs()

        val writer = file.writer()

        val html = html(
            head(
                link()
                    .withRel("stylesheet")
                    .withHref("${bookDir.resolve("style.css").relativeTo(path.parent)}")
            ),
            body(
                sidebar(id),
                main(
                    h1(title),
                    processor.process(content)
                )
            )
        )

        html.render(IndentedHtml.into(writer))
        writer.close()
    }



    private fun sidebar(currentPage: Identifier): DivTag {
        return div(buildCategoryList(book.categories(), currentPage)).withClass("sidebar")
    }

    private fun buildCategoryList(categories: Collection<Category>, currentPage: Identifier): OlTag {
        return unlabeledOl().with(book.categories().sortedBy(Category::ordinal).mapIndexed { index, category ->
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
                        buildEntryList(it, index, currentPage)
                    }
                )
            )
        })
    }

    private fun buildEntryList(entries: Collection<Entry>, categoryIndex: Int, currentPage: Identifier): OlTag {
        return unlabeledOl().with(
            entries.sortedBy(Entry::ordinal).mapIndexed { index, entry ->
                val entryIndex = "${categoryIndex + 1}.${index + 1}"
                val a = if (entry.id == currentPage)
                    a(strong("$entryIndex ${entry.title}"))
                else
                    a(strong(entryIndex), text(" ${entry.title}"))

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

}

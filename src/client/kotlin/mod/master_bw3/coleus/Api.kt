package mod.master_bw3.coleus

import io.wispforest.lavender.book.Book
import mod.master_bw3.coleus.internal.HtmlBookGenerator
import java.nio.file.Path

public object Api {

    @JvmStatic
    public fun generateBook(book: Book): Path {
        return HtmlBookGenerator(book).generate()
    }
}
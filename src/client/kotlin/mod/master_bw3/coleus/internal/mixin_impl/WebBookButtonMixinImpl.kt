package mod.master_bw3.coleus.internal.mixin_impl

import io.wispforest.lavender.book.Book
import mod.master_bw3.coleus.ColeusClient.bookServerPort
import mod.master_bw3.coleus.internal.HtmlBookGenerator
import net.minecraft.util.Util

public object WebBookButtonMixinImpl {
    public fun webBookButtonCallback(book: Book) {
        HtmlBookGenerator(book).generate();
        Util.getOperatingSystem().open("http://localhost:"+bookServerPort+"/"+book.id().namespace +"/"+book.id().path +"/index.html");
    }
}
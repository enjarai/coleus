package mod.master_bw3.coleus

import io.wispforest.lavendermd.MarkdownProcessor
import io.wispforest.lavendermd.feature.*
import mod.master_bw3.coleus.lavender.compiler.HtmlCompiler
import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory
import java.util.function.Supplier

object Coleus : ModInitializer {
    private val logger = LoggerFactory.getLogger("coleus")

    override fun onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        logger.info("Hello Fabric world!")


        var processor = MarkdownProcessor(
            Supplier { HtmlCompiler(StringBuilder()) },
            BasicFormattingFeature(),
            ColorFeature(),
            LinkFeature(),
            ListFeature(),
            BlockQuoteFeature()
        )
        val formatted = processor.process(x);

        logger.info(formatted.toString())
    }

    val x = "HEY THERE \nhow goes it \n\n b \n\n\n\n c \n d"
}
package mod.master_bw3.coleus

import io.wispforest.lavender.book.BookLoader
import io.wispforest.lavendermd.MarkdownProcessor
import io.wispforest.lavendermd.feature.*
import mod.master_bw3.coleus.lavender.compiler.HtmlCompiler
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents
import org.slf4j.LoggerFactory
import java.util.function.Supplier

object Coleus : ModInitializer {

    const val NAME = "coleus"
    private val logger = LoggerFactory.getLogger(NAME)

    override fun onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        logger.info("Hello Fabric world!")



    }


}
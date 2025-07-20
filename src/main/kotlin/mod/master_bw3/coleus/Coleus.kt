package mod.master_bw3.coleus

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

internal object Coleus : ModInitializer {

    internal const val NAME: String = "coleus"
    internal val logger = LoggerFactory.getLogger(NAME)

    override fun onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
    }


}
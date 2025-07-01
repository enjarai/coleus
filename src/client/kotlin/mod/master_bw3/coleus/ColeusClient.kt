package mod.master_bw3.coleus

import io.wispforest.lavender.book.BookLoader
import mod.master_bw3.coleus.internal.HtmlBookGenerator
import mod.master_bw3.coleus.internal.registerHtmlTemplates
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

internal object ColeusClient : ClientModInitializer {
	internal const val NAME: String = "coleus"

	@Suppress("unused")
	internal val logger = LoggerFactory.getLogger(NAME)

	override fun onInitializeClient() {
		registerHtmlTemplates()

		ClientPlayConnectionEvents.JOIN.register { handler, sender, client ->
			client.execute {
				println("generating books")
				BookLoader.loadedBooks().forEach {
					println("generating book: ${it.id()}")
					HtmlBookGenerator(it).generate()
				}
			}
		}
	}
}
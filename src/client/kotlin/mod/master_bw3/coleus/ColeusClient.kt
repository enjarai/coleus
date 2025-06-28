package mod.master_bw3.coleus

import io.wispforest.lavender.book.BookLoader
import io.wispforest.owo.ui.parsing.UIModelLoader
import mod.master_bw3.coleus.htmlBook.HtmlBookGenerator
import mod.master_bw3.coleus.htmlBook.HtmlTemplateRegistry
import mod.master_bw3.coleus.htmlBook.registerHtmlTemplates
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.util.Identifier

object ColeusClient : ClientModInitializer {
	override fun onInitializeClient() {
		registerHtmlTemplates()

		HtmlTemplateRegistry.register(Identifier.of("trickster", "pattern"), "owo what's this")
		HtmlTemplateRegistry.register(Identifier.of("trickster", "glyph"), "owo what's this")
		HtmlTemplateRegistry.register(Identifier.of("trickster", "spell-preview"), "owo what's this")
		HtmlTemplateRegistry.register(Identifier.of("trickster", "spell-preview-unloadable"), "owo what's this")
		HtmlTemplateRegistry.register(Identifier.of("trickster", "item-tag"), "owo what's this")
		HtmlTemplateRegistry.register(Identifier.of("trickster", "cost-rule"), "owo what's this")

		ClientPlayConnectionEvents.JOIN.register { handler, sender, client ->

			client.execute {
				println("generating books")
				BookLoader.loadedBooks().forEach {
					println("generating book: ${it.id()}")
					HtmlBookGenerator(it).generate()
				}
			}
		};
	}
}
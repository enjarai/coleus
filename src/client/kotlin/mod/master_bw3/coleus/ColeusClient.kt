package mod.master_bw3.coleus

import io.wispforest.lavender.book.BookLoader
import mod.master_bw3.coleus.htmlBook.HtmlBookGenerator
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents

object ColeusClient : ClientModInitializer {
	override fun onInitializeClient() {
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
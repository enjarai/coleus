package mod.master_bw3.coleus

import com.sun.net.httpserver.SimpleFileServer;
import io.wispforest.lavender.book.BookLoader
import io.wispforest.owo.Owo
import mod.master_bw3.coleus.internal.HtmlBookGenerator
import mod.master_bw3.coleus.internal.registerHtmlTemplates
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.net.InetSocketAddress

internal object ColeusClient : ClientModInitializer {
	internal const val NAME: String = "coleus"

	@Suppress("unused")
	internal val logger = LoggerFactory.getLogger(NAME)

	override fun onInitializeClient() {
		registerHtmlTemplates()

		val booksDir = FabricLoader.getInstance().gameDir.resolve("coleus")
		booksDir.toFile().mkdirs()

		val socket = InetSocketAddress(InetAddress.getLoopbackAddress(), 1984)
		val loggingLevel = if (FabricLoader.getInstance().isDevelopmentEnvironment) SimpleFileServer.OutputLevel.INFO else SimpleFileServer.OutputLevel.NONE
		SimpleFileServer.createFileServer(socket, booksDir, loggingLevel).start()


		ClientLifecycleEvents.CLIENT_STARTED.register { client ->
			val resource = client.resourceManager.getResource(Identifier.of(NAME, "base16theme/base16.json")).get()
			val themes = Base16Theme.collectionFromJsonResource(resource)
			ThemeRegistry.putAll(themes.mapKeys { Identifier.of(NAME, it.key.lowercase()) })
		}


		ClientPlayConnectionEvents.JOIN.register { handler, sender, client ->
			client.execute {
				BookLoader.loadedBooks().forEach {
					HtmlBookGenerator(it).generate()
				}
			}
		}
	}
}
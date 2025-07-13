package mod.master_bw3.coleus

import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import io.wispforest.lavender.book.BookLoader
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.Sizing
import mod.master_bw3.coleus.Components.owo
import mod.master_bw3.coleus.internal.HtmlBookGenerator
import mod.master_bw3.coleus.internal.registerHtmlTemplates
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.InvalidateRenderStateCallback
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourceReloader
import net.minecraft.resource.ResourceType
import net.minecraft.resource.SynchronousResourceReloader
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.profiler.Profiler
import org.slf4j.LoggerFactory
import java.lang.Void
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

internal object ColeusClient : ClientModInitializer {
	internal const val NAME: String = "coleus"

	@Suppress("unused")
	internal val logger = LoggerFactory.getLogger(NAME)

	override fun onInitializeClient() {
		registerHtmlTemplates()

		val booksDir = FabricLoader.getInstance().gameDir.resolve("coleus")
		booksDir.toFile().mkdirs()
		val app = Javalin.create { javalinConfig -> javalinConfig.staticFiles.add(booksDir.toString(), Location.EXTERNAL) }.start(7070)

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
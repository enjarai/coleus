package mod.master_bw3.coleus

import io.wispforest.lavender.book.BookLoader
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.Sizing
import mod.master_bw3.coleus.Components.owo
import mod.master_bw3.coleus.internal.HtmlBookGenerator
import mod.master_bw3.coleus.internal.registerHtmlTemplates
import net.fabricmc.api.ClientModInitializer
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

		ClientPlayConnectionEvents.JOIN.register { handler, sender, client ->
			client.execute {
				println("generating books")
				BookLoader.loadedBooks().forEach {
					println("generating book: ${it.id()}")
					HtmlBookGenerator(it).generate()
				}
			}
		}

		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
			object : SimpleSynchronousResourceReloadListener {

				override fun getFabricId(): Identifier? {
					return Identifier.of(NAME, "test")
				}

				override fun reload(manager: ResourceManager) {
					if (MinecraftClient.getInstance().world == null) return
					MinecraftClient.getInstance().execute {
                        val component = Components.button(Text.literal("hey there!")) {  } as Component
						println("GENERATING IMAGE")
                        owo(
                            component,
                            FabricLoader.getInstance().gameDir.resolve("test.png"),
                            FabricLoader.getInstance().gameDir.resolve("test.png"),
							600
                        )
                    }

                }


			} as IdentifiableResourceReloadListener
		)

	}


}